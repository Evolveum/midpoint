/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyClob;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.sql.SQLException;

/**
 * @author lazyman
 */
public class SqlBaseService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlBaseService.class);
    // how many times we want to repeat operation after lock acquisition,
    // pessimistic, optimistic exception
    static final int LOCKING_MAX_ATTEMPTS = 40;

    // timeout will be a random number between 0 and LOCKING_TIMEOUT_STEP * 2^exp where exp is either real attempt # minus 1, or LOCKING_EXP_THRESHOLD (whatever is lesser)
    private static final long LOCKING_TIMEOUT_STEP = 50;
    private static final int LOCKING_EXP_THRESHOLD = 7;       // i.e. up to 6400 msec wait time

    @Autowired(required = true)
    private PrismContext prismContext;
    @Autowired(required = true)
    private SessionFactory sessionFactory;

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        RUtil.fixCompositeIDHandling(sessionFactory);

        this.sessionFactory = sessionFactory;
    }

    protected int logOperationAttempt(String oid, String operation, int attempt, RuntimeException ex,
            OperationResult result) {
        if (!(ex instanceof PessimisticLockException) && !(ex instanceof LockAcquisitionException)
                && !(ex instanceof HibernateOptimisticLockingFailureException)) {
            // it's not locking exception (optimistic, pesimistic lock or simple lock acquisition) understood by hibernate
            // however, it still could be such exception... wrapped in e.g. TransactionException
            // so we have a look inside - we try to find SQLException there

            // these error codes / SQL states we consider related to locking:
            //  code 50200 [table timeout lock in H2, 50200 is LOCK_TIMEOUT_1 error code]
            //  code 40001 [DEADLOCK_1 in H2]
            //  state 40001 [serialization failure in PostgreSQL - http://www.postgresql.org/docs/9.1/static/transaction-iso.html - and probably also in other systems]
            //  state 40P01 [deadlock in PostgreSQL]
            //
            // sql states should be somewhat standardized; sql error codes are vendor-specific
            // todo: so it is probably not very safe to test for code 50200/40001 without testing for h2
            // but the risk of problem is quite low here, so let it be...

            SQLException sqlException = findSqlException(ex);
            boolean serializationCodeFound = sqlException != null && (sqlException.getErrorCode() == 50200 || sqlException.getErrorCode() == 40001 ||
                    "40001".equals(sqlException.getSQLState()) || "40P01".equals(sqlException.getSQLState()));

            //oracle 08177 code check (serialization problem
            serializationCodeFound = serializationCodeFound || (sqlException != null) && (sqlException.getErrorCode() == 8177);

            if (!serializationCodeFound) {
                // to be sure that we won't miss anything related to deadlocks, here is an ugly hack that checks it (with some probability...)
                boolean serializationTextFound = ex.getMessage() != null && (exceptionContainsText(ex, "deadlock") || exceptionContainsText(ex, "could not serialize access"));
                if (serializationTextFound) {
                    LOGGER.error("Transaction serialization-related problem (e.g. deadlock) was not caught correctly!", ex);
                }
                throw ex;
            }
        }

        double waitTimeInterval = LOCKING_TIMEOUT_STEP * Math.pow(2, attempt > LOCKING_EXP_THRESHOLD ? LOCKING_EXP_THRESHOLD : (attempt-1));
        long waitTime = Math.round(Math.random() * waitTimeInterval);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Waiting: attempt = " + attempt + ", waitTimeInterval = 0.." + waitTimeInterval + ", waitTime = " + waitTime);
        }

//        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("A serialization-related problem occurred when {} object with oid '{}', retrying after "
                    + "{}ms (this was attempt {} of {})\n{}: {}", new Object[]{operation, oid, waitTime,
                    attempt, LOCKING_MAX_ATTEMPTS, ex.getClass().getSimpleName(), ex.getMessage()});
//        }

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

    private SQLException findSqlException(Throwable ex) {
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
            if (StringUtils.isNotEmpty(ex.getMessage())) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    protected Session beginTransaction() {
        Session session = getSessionFactory().openSession();
        // we're forcing transaction isolation throught MidPointConnectionCustomizer
        // session.doWork(new Work() {
        //
        //      @Override
        //      public void execute(Connection connection) throws SQLException {
        //          connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        //      }
        // });
        session.beginTransaction();

        return session;
    }

    protected void rollbackTransaction(Session session) {
        rollbackTransaction(session, null, null, false);
    }

    protected void rollbackTransaction(Session session, Exception ex, OperationResult result, boolean fatal) {
        String message = ex != null ? ex.getMessage() : "null";
        rollbackTransaction(session, ex, message, result, fatal);
    }

    protected void rollbackTransaction(Session session, Exception ex, String message, OperationResult result, boolean fatal) {
        if (StringUtils.isEmpty(message) && ex != null) {
            message = ex.getMessage();
        }

        if (result != null) {
            if (fatal) {
                result.recordFatalError(message, ex);
            } else {
                result.recordHandledError(message);
            }
        }
    	 
    	if (session == null || session.getTransaction() == null || !session.getTransaction().isActive()) {
            return;
        }

        session.getTransaction().rollback();
    }

    protected void cleanupSessionAndResult(Session session, OperationResult result) {
        if (session != null && session.isOpen()) {
            session.close();
        }

        if (result != null && result.isUnknown()) {
            result.computeStatus();
        }
    }

    protected void handleGeneralException(Exception ex, Session session, OperationResult result) {
        LOGGER.debug("General exception occurred.", ex);

        //fix for ORA-08177 can't serialize access for this transaction
        if (ex instanceof QueryTimeoutException) {
            if (ex.getCause() != null && !(ex.getCause() instanceof SQLException)) {
                SQLException sqlEx = (SQLException) ex.getCause();
                if (sqlEx.getErrorCode() == 8177) {
                    //todo improve exception handling, maybe "javax.persistence.query.timeout" property can be used
                    rollbackTransaction(session);
                    throw (QueryTimeoutException)ex;
                }
            }
        }

        rollbackTransaction(session, ex, result, true);
        if (ex instanceof GenericJDBCException) {
            //fix for table timeout lock in H2, this exception will be wrapped as system exception
            //in SqlRepositoryServiceImpl#logOperationAttempt if necessary
            throw (GenericJDBCException) ex;
        }
        if (ex instanceof SystemException) {
            throw (SystemException) ex;
        }
        throw new SystemException(ex.getMessage(), ex);
    }
}
