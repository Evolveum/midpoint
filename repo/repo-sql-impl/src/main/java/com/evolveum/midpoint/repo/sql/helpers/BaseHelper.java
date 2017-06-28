/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.repo.sql.*;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.*;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

import static com.evolveum.midpoint.repo.sql.SqlBaseService.LOCKING_EXP_THRESHOLD;
import static com.evolveum.midpoint.repo.sql.SqlBaseService.LOCKING_MAX_ATTEMPTS;
import static com.evolveum.midpoint.repo.sql.SqlBaseService.LOCKING_TIMEOUT_STEP;

/**
 * Core functionality needed in all members of SQL service family.
 * Taken out of SqlBaseService in order to be accessible from other helpers without having to autowire SqlRepositoryServiceImpl
 * (as it causes problems with Spring AOP proxies.)
 *
 * @author lazyman
 * @author mederly
 */
@Component
public class BaseHelper {

	private static final Trace LOGGER = TraceManager.getTrace(BaseHelper.class);

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private SqlRepositoryFactory repositoryFactory;

	@Autowired
	private LocalSessionFactoryBean sessionFactoryBean;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		RUtil.fixCompositeIDHandling(sessionFactory);

		this.sessionFactory = sessionFactory;
	}

	public LocalSessionFactoryBean getSessionFactoryBean() {
		return sessionFactoryBean;
	}

	public Session beginReadOnlyTransaction() {
		return beginTransaction(getConfiguration().isUseReadOnlyTransactions());
	}

	public Session beginTransaction() {
		return beginTransaction(false);
	}

	public Session beginTransaction(boolean readOnly) {
		Session session = getSessionFactory().openSession();
		session.beginTransaction();

		if (getConfiguration().getTransactionIsolation() == TransactionIsolation.SNAPSHOT) {
			LOGGER.trace("Setting transaction isolation level SNAPSHOT.");
			session.doWork(new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					connection.createStatement().execute("SET TRANSACTION ISOLATION LEVEL SNAPSHOT");
				}
			});
		}

		if (readOnly) {
			// we don't want to flush changes during readonly transactions (they should never occur,
			// but if they occur transaction commit would still fail)
			session.setFlushMode(FlushMode.MANUAL);

			LOGGER.trace("Marking transaction as read only.");
			session.doWork(new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					connection.createStatement().execute("SET TRANSACTION READ ONLY");
				}
			});
		}
		return session;
	}

	public SqlRepositoryConfiguration getConfiguration() {
		return repositoryFactory.getSqlConfiguration();
	}

	public void rollbackTransaction(Session session) {
		rollbackTransaction(session, null, null, false);
	}

	public void rollbackTransaction(Session session, Exception ex, OperationResult result, boolean fatal) {
		String message = ex != null ? ex.getMessage() : "null";
		rollbackTransaction(session, ex, message, result, fatal);
	}

	public void rollbackTransaction(Session session, Exception ex, String message, OperationResult result,
			boolean fatal) {
		if (StringUtils.isEmpty(message) && ex != null) {
			message = ex.getMessage();
		}

		// non-fatal errors will NOT be put into OperationResult, not to confuse the user
		if (result != null && fatal) {
			result.recordFatalError(message, ex);
		}

		if (session == null || session.getTransaction() == null || !session.getTransaction().isActive()) {
			return;
		}

		session.getTransaction().rollback();
	}

	public void cleanupSessionAndResult(Session session, OperationResult result) {
		if (session != null && session.isOpen()) {
			session.close();
		}

		if (result != null && result.isUnknown()) {
			result.computeStatus();
		}
	}

	public void handleGeneralException(Exception ex, Session session, OperationResult result) {
		if (ex instanceof RuntimeException) {
			handleGeneralRuntimeException((RuntimeException) ex, session, result);
		} else {
			handleGeneralCheckedException(ex, session, result);
		}
		throw new IllegalStateException("Shouldn't get here");			// just a marker to be obvious that this method never returns normally
	}

	public void handleGeneralRuntimeException(RuntimeException ex, Session session, OperationResult result) {
		LOGGER.debug("General runtime exception occurred.", ex);

		if (isExceptionRelatedToSerialization(ex)) {
			rollbackTransaction(session, ex, result, false);
			// this exception will be caught and processed in logOperationAttempt,
			// so it's safe to pass any RuntimeException here
			throw ex;
		} else {
			rollbackTransaction(session, ex, result, true);
			if (ex instanceof SystemException) {
				throw (SystemException) ex;
			} else {
				throw new SystemException(ex.getMessage(), ex);
			}
		}
	}

	public void handleGeneralCheckedException(Exception ex, Session session, OperationResult result) {
		LOGGER.error("General checked exception occurred.", ex);

		boolean fatal = !isExceptionRelatedToSerialization(ex);
		rollbackTransaction(session, ex, result, fatal);
		throw new SystemException(ex.getMessage(), ex);
	}

	public int logOperationAttempt(String oid, String operation, int attempt, RuntimeException ex,
			OperationResult result) {

		boolean serializationException = isExceptionRelatedToSerialization(ex);

		if (!serializationException) {
			// to be sure that we won't miss anything related to deadlocks, here is an ugly hack that checks it (with some probability...)
			boolean serializationTextFound = ex.getMessage() != null && (exceptionContainsText(ex, "deadlock") || exceptionContainsText(ex, "could not serialize access"));
			if (serializationTextFound) {
				LOGGER.error("Transaction serialization-related problem (e.g. deadlock) was probably not caught correctly!", ex);
			}
			throw ex;
		}

		double waitTimeInterval = LOCKING_TIMEOUT_STEP * Math.pow(2, attempt > LOCKING_EXP_THRESHOLD ? LOCKING_EXP_THRESHOLD : (attempt - 1));
		long waitTime = Math.round(Math.random() * waitTimeInterval);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Waiting: attempt = " + attempt + ", waitTimeInterval = 0.." + waitTimeInterval + ", waitTime = " + waitTime);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("A serialization-related problem occurred when {} object with oid '{}', retrying after "
					+ "{} ms (this was attempt {} of {})\n{}: {}", operation, oid, waitTime,
					attempt, LOCKING_MAX_ATTEMPTS, ex.getClass().getSimpleName(), ex.getMessage());
		}

		if (attempt >= LOCKING_MAX_ATTEMPTS) {
			LOGGER.error("A serialization-related problem occurred, maximum attempts (" + attempt + ") reached.", ex);
			if (ex != null && result != null) {
				result.recordFatalError("A serialization-related problem occurred.", ex);
			}
			throw new SystemException(ex.getMessage() + " [attempts: " + attempt + "]", ex);
		}

		if (waitTime > 0) {
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException ex1) {
				// ignore this
			}
		}
		return ++attempt;
	}

	private boolean isExceptionRelatedToSerialization(Exception ex) {
		boolean rv = isExceptionRelatedToSerializationInternal(ex);
		LOGGER.trace("Considering if exception {} is related to serialization: returning {}", ex, rv, ex);
		return rv;
	}

	private boolean isExceptionRelatedToSerializationInternal(Exception ex) {

		if (ex instanceof SerializationRelatedException
				|| ex instanceof PessimisticLockException
				|| ex instanceof LockAcquisitionException
				|| ex instanceof HibernateOptimisticLockingFailureException
				|| ex instanceof StaleObjectStateException) {                       // todo the last one is questionable
			return true;
		}

		// it's not locking exception (optimistic, pesimistic lock or simple lock acquisition) understood by hibernate
		// however, it still could be such exception... wrapped in e.g. TransactionException
		// so we have a look inside - we try to find SQLException there

		SQLException sqlException = findSqlException(ex);
		if (sqlException == null) {
			return false;
		}

		// these error codes / SQL states we consider related to locking:
		//  code 50200 [table timeout lock in H2, 50200 is LOCK_TIMEOUT_1 error code]
		//  code 40001 [DEADLOCK_1 in H2]
		//  state 40001 [serialization failure in PostgreSQL - http://www.postgresql.org/docs/9.1/static/transaction-iso.html - and probably also in other systems]
		//  state 40P01 [deadlock in PostgreSQL]
		//  code ORA-08177: can't serialize access for this transaction in Oracle
		//  code ORA-01466 ["unable to read data - table definition has changed"] in Oracle
		//  code ORA-01555: snapshot too old: rollback segment number  with name "" too small
		//  code ORA-22924: snapshot too old
		//
		// sql states should be somewhat standardized; sql error codes are vendor-specific
		// todo: so it is probably not very safe to test for codes without testing for specific database (h2, oracle)
		// but the risk of problem is quite low here, so let it be...

		// strange exception occurring in MySQL when doing multithreaded org closure maintenance
		// alternatively we might check for error code = 1030, sql state = HY000
		// but that would cover all cases of "Got error XYZ from storage engine"
		if ((getConfiguration().isUsingMySQL() || getConfiguration().isUsingMariaDB())
				&& sqlException.getMessage() != null
				&& sqlException.getMessage().contains("Got error -1 from storage engine")) {
			return true;
		}

		// this is some recent H2 weirdness (MID-3969)
		if (getConfiguration().isUsingH2() && sqlException.getMessage() != null
				&& sqlException.getMessage().contains("Referential integrity constraint violation: \"FK_AUDIT_ITEM: PUBLIC.M_AUDIT_ITEM FOREIGN KEY(RECORD_ID) REFERENCES PUBLIC.M_AUDIT_EVENT(ID)")) {
			return true;
		}

		return sqlException.getErrorCode() == 50200
				|| sqlException.getErrorCode() == 40001
				|| "40001".equals(sqlException.getSQLState())
				|| "40P01".equals(sqlException.getSQLState())
				|| sqlException.getErrorCode() == 8177
				|| sqlException.getErrorCode() == 1466
				|| sqlException.getErrorCode() == 1555
				|| sqlException.getErrorCode() == 22924
				|| sqlException.getErrorCode() == 3960;         // Snapshot isolation transaction aborted due to update conflict.
	}

	public SQLException findSqlException(Throwable ex) {
		while (ex != null) {
			if (ex instanceof SQLException) {
				return (SQLException) ex;
			}
			ex = ex.getCause();
		}
		return null;
	}

	private boolean exceptionContainsText(Throwable ex, String text) {
		while (ex != null) {
			if (ex.getMessage() != null && ex.getMessage().contains(text)) {
				return true;
			}
			ex = ex.getCause();
		}
		return false;
	}
}
