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
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.repo.sql.data.common.RContainer;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RTask;
import com.evolveum.midpoint.repo.sql.util.MidPointNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.*;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import javax.xml.datatype.Duration;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

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

    @Autowired
    private PrismContext prismContext;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private LocalSessionFactoryBean sessionFactoryBean;

    private SqlRepositoryFactory repositoryFactory;

    public SqlBaseService(SqlRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    public SqlRepositoryConfiguration getConfiguration() {
        return repositoryFactory.getSqlConfiguration();
    }

    public SqlPerformanceMonitor getPerformanceMonitor() {
        return repositoryFactory.getPerformanceMonitor();
    }

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
                    + "{}ms (this was attempt {} of {})\n{}: {}", new Object[]{operation, oid, waitTime,
                    attempt, LOCKING_MAX_ATTEMPTS, ex.getClass().getSimpleName(), ex.getMessage()});
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

        if (ex instanceof PessimisticLockException
                || ex instanceof LockAcquisitionException
                || ex instanceof HibernateOptimisticLockingFailureException) {
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

        return sqlException.getErrorCode() == 50200
                || sqlException.getErrorCode() == 40001
                || "40001".equals(sqlException.getSQLState())
                || "40P01".equals(sqlException.getSQLState())
                || sqlException.getErrorCode() == 8177
                || sqlException.getErrorCode() == 1466
                || sqlException.getErrorCode() == 1555
                || sqlException.getErrorCode() == 22924;
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
            if (ex.getMessage() != null && ex.getMessage().contains(text)) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    protected Session beginTransaction() {
        return beginTransaction(false);
    }

    protected Session beginTransaction(boolean readOnly) {
        Session session = getSessionFactory().openSession();
        session.beginTransaction();
        if (readOnly) {
            LOGGER.trace("Marking transaction as read only.");
            session.createSQLQuery("SET TRANSACTION READ ONLY").executeUpdate();
        }
        return session;
    }

    protected void rollbackTransaction(Session session) {
        rollbackTransaction(session, null, null, false);
    }

    protected void rollbackTransaction(Session session, Exception ex, OperationResult result, boolean fatal) {
        String message = ex != null ? ex.getMessage() : "null";
        rollbackTransaction(session, ex, message, result, fatal);
    }

    protected void rollbackTransaction(Session session, Exception ex, String message, OperationResult result,
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

    protected void cleanupSessionAndResult(Session session, OperationResult result) {
        if (session != null && session.isOpen()) {
            session.close();
        }

        if (result != null && result.isUnknown()) {
            result.computeStatus();
        }
    }

    protected void handleGeneralRuntimeException(RuntimeException ex, Session session, OperationResult result) {
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

    protected void handleGeneralCheckedException(Exception ex, Session session, OperationResult result) {
        LOGGER.error("General checked exception occurred.", ex);

        boolean fatal = !isExceptionRelatedToSerialization(ex);
        rollbackTransaction(session, ex, result, fatal);
        throw new SystemException(ex.getMessage(), ex);
    }

    protected void cleanup(Class entity, CleanupPolicyType policy, OperationResult subResult) {
        Validate.notNull(policy, "Cleanup policy must not be null.");
        Validate.notNull(subResult, "Operation result must not be null.");

        final String operation = "deleting";
        int attempt = 1;

        SqlPerformanceMonitor pm = repositoryFactory.getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("deleteObject");

        try {
            while (true) {
                try {
                    cleanupAttempt(entity, policy, subResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private void cleanupAttempt(Class entity, CleanupPolicyType policy, OperationResult subResult) {
        if (policy.getMaxAge() == null) {
            return;
        }

        Duration duration = policy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date minValue = new Date();
        duration.addTo(minValue);
        LOGGER.info("Starting cleanup for entity {} deleting up to {} (duration '{}').",
                new Object[]{entity.getSimpleName(), minValue, duration});

        Session session = null;
        try {
            session = beginTransaction();

            int count;
            if (RAuditEventRecord.class.equals(entity)) {
                count = cleanupAudit(minValue, session);
            } else if (RTask.class.equals(entity)) {
                count = cleanupTasks(minValue, session);
            } else {
                throw new SystemException("Unknown cleanup entity type '" + entity + "'.");
            }

            LOGGER.info("Cleanup in {} performed, {} records deleted up to {} (duration '{}').",
                    new Object[]{entity.getSimpleName(), count, minValue, duration});

            session.getTransaction().commit();
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, subResult);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }
    }

    private int cleanupAudit(Date minValue, Session session) {
        Query query = session.createQuery("delete from " + RAuditEventRecord.class.getSimpleName()
                + " as a where a.timestamp < :timestamp");
        query.setParameter("timestamp", new Timestamp(minValue.getTime()));

        return query.executeUpdate();
    }

    /**
     * This is attempt to do task cleanup by custom native queries. Hibernate tries to delete task
     * through temporary table with columns oid, id. That's not a bad idea until it attempts to call
     * delete from statement with "in" clause with two columns (oid, id). H2 and SQL Server fails
     * with in clause that contains more than one column.
     * <p/>
     * Therefore this method do cleanup by creating temporary table only with oid (id is always 0).
     * Maybe big schema cleanup would help or something like that.
     * <p/>
     * Somebody improve this when there's time for it.
     *
     * @param minValue
     * @param session
     * @return number of deleted tasks
     */
    private int cleanupTasks(Date minValue, Session session) {
        MidPointNamingStrategy namingStrategy = new MidPointNamingStrategy();
        final String taskTableName = namingStrategy.classToTableName(RTask.class.getSimpleName());
        final String objectTableName = namingStrategy.classToTableName(RObject.class.getSimpleName());
        final String containerTableName = namingStrategy.classToTableName(RContainer.class.getSimpleName());

        final String completionTimestampColumn = "completionTimestamp";

        Dialect dialect = Dialect.getDialect(sessionFactoryBean.getHibernateProperties());
        if (!dialect.supportsTemporaryTables()) {
            LOGGER.error("Dialect {} doesn't support temporary tables, couldn't cleanup tasks.",
                    new Object[]{dialect});
            throw new SystemException("Dialect " + dialect
                    + " doesn't support temporary tables, couldn't cleanup tasks.");
        }

        //create temporary table
        String prefix = "";
        if (UnicodeSQLServer2008Dialect.class.equals(dialect.getClass())) {
            prefix = "#";   //this creates temporary table as with global scope
        }
        final String tempTable = prefix + dialect.generateTemporaryTableName(taskTableName);

        StringBuilder sb = new StringBuilder();
        sb.append(dialect.getCreateTemporaryTableString());
        sb.append(' ').append(tempTable).append(" (oid ");
        sb.append(dialect.getTypeName(Types.VARCHAR, 36, 0, 0));
        sb.append(" not null)");
        sb.append(dialect.getCreateTemporaryTablePostfix());

        SQLQuery query = session.createSQLQuery(sb.toString());
        query.executeUpdate();

        //fill temporary table
        sb = new StringBuilder();
        sb.append("insert into ").append(tempTable).append(' ');            //todo improve this insert
        sb.append("select t.oid as oid from ").append(taskTableName).append(" t");
        sb.append(" inner join ").append(objectTableName).append(" o on t.id = o.id and t.oid = o.oid");
        sb.append(" inner join ").append(containerTableName).append(" c on t.id = c.id and t.oid = c.oid");
        sb.append(" where t.").append(completionTimestampColumn).append(" < ?");

        query = session.createSQLQuery(sb.toString());
        query.setParameter(0, new Timestamp(minValue.getTime()));
        query.executeUpdate();

        //drop records from m_task, m_object, m_container
        sb = new StringBuilder();
        sb.append("delete from ").append(taskTableName);
        sb.append(" where id = 0 and (oid in (select oid from ").append(tempTable).append("))");
        session.createSQLQuery(sb.toString()).executeUpdate();

        sb = new StringBuilder();
        sb.append("delete from ").append(objectTableName);
        sb.append(" where id = 0 and (oid in (select oid from ").append(tempTable).append("))");
        session.createSQLQuery(sb.toString()).executeUpdate();

        sb = new StringBuilder();
        sb.append("delete from ").append(containerTableName);
        sb.append(" where id = 0 and (oid in (select oid from ").append(tempTable).append("))");
        int count = session.createSQLQuery(sb.toString()).executeUpdate();

        //drop temporary table
        if (dialect.dropTemporaryTableAfterUse()) {
            LOGGER.debug("Dropping temporary table.");
            sb = new StringBuilder();
            sb.append(dialect.getDropTemporaryTableString());
            sb.append(' ').append(tempTable);

            session.createSQLQuery(sb.toString()).executeUpdate();
        }

        return count;
    }
}
