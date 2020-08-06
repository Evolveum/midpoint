/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import com.google.common.base.Strings;
import com.querydsl.sql.*;
import org.hibernate.*;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.sql.*;
import com.evolveum.midpoint.repo.sql.pure.querymodel.support.InstantType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.backoff.BackoffComputer;
import com.evolveum.midpoint.util.backoff.ExponentialBackoffComputer;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
    private static final Trace CONTENTION_LOGGER =
            TraceManager.getTrace(SqlRepositoryServiceImpl.CONTENTION_LOG_NAME);

    /**
     * How many times we want to repeat operation after lock acquisition,
     * pessimistic, optimistic exception.
     */
    public static final int LOCKING_MAX_RETRIES = 40;

    /**
     * Timeout will be a random number between 0 and LOCKING_DELAY_INTERVAL_BASE * 2^exp
     * where exp is either real attempt # minus 1, or LOCKING_EXP_THRESHOLD (whatever is lesser).
     */
    public static final long LOCKING_DELAY_INTERVAL_BASE = 50;

    public static final int LOCKING_EXP_THRESHOLD = 7; // i.e. up to 6400ms wait time

    @NotNull
    private final SqlRepositoryConfiguration sqlRepositoryConfiguration;
    private final SessionFactory sessionFactory;

    // TODO MID-6318 remove, needed only for Dialect determination in audit service
    private final LocalSessionFactoryBean sessionFactoryBean;
    private final DataSource dataSource;

    private Configuration querydslConfiguration;

    // used for non-bean creation
    public BaseHelper(
            @NotNull SqlRepositoryConfiguration sqlRepositoryConfiguration,
            SessionFactory sessionFactory,
            LocalSessionFactoryBean sessionFactoryBean,
            DataSource dataSource) {
        this.sqlRepositoryConfiguration = sqlRepositoryConfiguration;
        this.sessionFactory = sessionFactory;
        this.sessionFactoryBean = sessionFactoryBean;
        this.dataSource = dataSource;
    }

    @Autowired
    public BaseHelper(
            SqlRepositoryFactory repositoryFactory,
            SessionFactory sessionFactory,
            LocalSessionFactoryBean sessionFactoryBean,
            DataSource dataSource) {
        this(repositoryFactory.getSqlConfiguration(),
                sessionFactory, sessionFactoryBean, dataSource);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
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
            session.doWork(connection -> RUtil.executeStatement(connection, "SET TRANSACTION ISOLATION LEVEL SNAPSHOT"));
        }

        if (readOnly) {
            // we don't want to flush changes during readonly transactions (they should never occur,
            // but if they occur transaction commit would still fail)
            session.setHibernateFlushMode(FlushMode.MANUAL);

            LOGGER.trace("Marking transaction as read only.");
            session.doWork(connection -> RUtil.executeStatement(connection, "SET TRANSACTION READ ONLY"));
        }
        return session;
    }

    @NotNull
    public SqlRepositoryConfiguration getConfiguration() {
        return sqlRepositoryConfiguration;
    }

    void rollbackTransaction(Session session, Throwable ex, OperationResult result, boolean fatal) {
        String message = ex != null ? ex.getMessage() : "null";
        rollbackTransaction(session, ex, message, result, fatal);
    }

    void rollbackTransaction(Session session, Throwable ex, String message, OperationResult result, boolean fatal) {
        if (Strings.isNullOrEmpty(message) && ex != null) {
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
        if (session != null && session.getTransaction().isActive()) {
            session.getTransaction().commit();
        }

        if (session != null && session.isOpen()) {
            session.close();
        }

        if (result != null && result.isUnknown()) {
            result.computeStatus();
        }
    }

    public void handleGeneralException(Throwable ex, Session session, OperationResult result) {
        if (ex instanceof RuntimeException) {
            handleGeneralRuntimeException((RuntimeException) ex, session, result);
        } else {
            handleGeneralCheckedException(ex, session, result);
        }
        throw new IllegalStateException("Shouldn't get here");            // just a marker to be obvious that this method never returns normally
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
                throw ex;
            } else {
                throw new SystemException(ex.getMessage(), ex);
            }
        }
    }

    public void handleGeneralCheckedException(Throwable ex, Session session, OperationResult result) {
        LOGGER.error("General checked exception occurred.", ex);

        boolean fatal = !isExceptionRelatedToSerialization(ex);
        rollbackTransaction(session, ex, result, fatal);
        throw new SystemException(ex.getMessage(), ex);
    }

    public int logOperationAttempt(String oid, String operation, int attempt, @NotNull RuntimeException ex,
            OperationResult result) {

        if (ex instanceof RestartOperationRequestedException) {
            // This is a special case: we would like to restart
        }

        boolean serializationException = isExceptionRelatedToSerialization(ex);

        if (!serializationException) {
            // to be sure that we won't miss anything related to deadlocks, here is an ugly hack that checks it (with some probability...)
            boolean serializationTextFound = ex.getMessage() != null && (exceptionContainsText(ex, "deadlock") || exceptionContainsText(ex, "could not serialize access"));
            if (serializationTextFound) {
                LOGGER.error("Transaction serialization-related problem (e.g. deadlock) was probably not caught correctly!", ex);
            }
            throw ex;
        }

        BackoffComputer backoffComputer = new ExponentialBackoffComputer(LOCKING_MAX_RETRIES, LOCKING_DELAY_INTERVAL_BASE, LOCKING_EXP_THRESHOLD, null);
        long waitTime;
        try {
            waitTime = backoffComputer.computeDelay(attempt);
        } catch (BackoffComputer.NoMoreRetriesException e) {
            CONTENTION_LOGGER.error("A serialization-related problem occurred, maximum attempts ({}) reached.", attempt, ex);
            LOGGER.error("A serialization-related problem occurred, maximum attempts ({}) reached.", attempt, ex);
            if (result != null) {
                result.recordFatalError("A serialization-related problem occurred.", ex);
            }
            throw new SystemException(ex.getMessage() + " [attempts: " + attempt + "]", ex);
        }
        String message = "A serialization-related problem occurred when {} object with oid '{}', retrying after "
                + "{} ms (this is retry {} of {})\n{}: {}";
        Object[] objects = { operation, oid, waitTime, attempt, LOCKING_MAX_RETRIES, ex.getClass().getSimpleName(), ex.getMessage() };
        if (attempt >= SqlRepositoryServiceImpl.CONTENTION_LOG_DEBUG_THRESHOLD) {
            CONTENTION_LOGGER.debug(message, objects);
        } else {
            CONTENTION_LOGGER.trace(message, objects);
        }
        if (attempt >= SqlRepositoryServiceImpl.MAIN_LOG_WARN_THRESHOLD) {
            LOGGER.warn(message, objects);
        } else {
            LOGGER.debug(message, objects);
        }
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ex1) {
                // ignore this
            }
        }
        return attempt + 1;
    }

    private boolean isExceptionRelatedToSerialization(Throwable ex) {
        boolean rv = isExceptionRelatedToSerializationInternal(ex);
        LOGGER.trace("Considering if exception {} is related to serialization: returning {}", ex, rv, ex);
        return rv;
    }

    private boolean isExceptionRelatedToSerializationInternal(Throwable ex) {

        if (ExceptionUtil.findCause(ex, SerializationRelatedException.class) != null
                || ExceptionUtil.findCause(ex, PessimisticLockException.class) != null
                || ExceptionUtil.findCause(ex, LockAcquisitionException.class) != null
                || ExceptionUtil.findCause(ex, HibernateOptimisticLockingFailureException.class) != null
                || ExceptionUtil.findCause(ex, StaleObjectStateException.class) != null) {  // todo the last one is questionable
            return true;
        }

        // it's not locking exception (optimistic, pessimistic lock or simple lock acquisition) understood by hibernate
        // however, it still could be such exception... wrapped in e.g. TransactionException
        // so we have a look inside - we try to find SQLException there

        SQLException sqlException = findSqlException(ex);
        if (sqlException == null) {
            return false;
        }

        if (hasSerializationRelatedConstraintViolationException(ex)) {
            return true;
        }

        // error messages / error codes / SQL states listed below we consider related to locking
        // (sql states should be somewhat standardized; sql error codes are vendor-specific)

        boolean mySqlCompatible = getConfiguration().isUsingMySqlCompatible();
        boolean h2 = getConfiguration().isUsingH2();
        boolean oracle = getConfiguration().isUsingOracle();

        return "40001".equals(sqlException.getSQLState())           // serialization failure in PostgreSQL - http://www.postgresql.org/docs/9.1/static/transaction-iso.html - and probably also in other systems
                || "40P01".equals(sqlException.getSQLState())           // deadlock in PostgreSQL
                || mySqlCompatible && messageContains(sqlException.getMessage(), MY_SQL_SERIALIZATION_ERRORS)
                || h2 && messageContains(sqlException.getMessage(), H_2_SERIALIZATION_ERRORS)
                || h2 && sqlException.getErrorCode() == 50200           // table timeout lock in H2, 50200 is LOCK_TIMEOUT_1 error code
                || h2 && sqlException.getErrorCode() == 40001           // DEADLOCK_1 in H2
                || oracle && sqlException.getErrorCode() == 8177        // ORA-08177: can't serialize access for this transaction in Oracle
                || oracle && sqlException.getErrorCode() == 1466        // ORA-01466 ["unable to read data - table definition has changed"] in Oracle
                || oracle && sqlException.getErrorCode() == 1555        // ORA-01555: snapshot too old: rollback segment number  with name "" too small
                || oracle && sqlException.getErrorCode() == 22924       // ORA-22924: snapshot too old
                || sqlException.getErrorCode() == 3960;                 // Snapshot isolation transaction aborted due to update conflict. (todo which db?)
    }

    private boolean messageContains(String message, String[] substrings) {
        if (message != null) {
            for (String substring : substrings) {
                if (message.contains(substring)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final String[] MY_SQL_SERIALIZATION_ERRORS = {
            // strange exception occurring in MySQL when doing multithreaded org closure maintenance
            // alternatively we might check for error code = 1030, sql state = HY000
            // but that would cover all cases of "Got error XYZ from storage engine"
            "Got error -1 from storage engine",

            // Another error that cannot be detected using standard mechanisms (MID-5561)
            "Lock wait timeout exceeded"
    };

    private static final String[] H_2_SERIALIZATION_ERRORS = {
            // this is some recent H2 weirdness (MID-3969)
            "Referential integrity constraint violation: \"FK_AUDIT_ITEM: PUBLIC.M_AUDIT_ITEM FOREIGN KEY(RECORD_ID) REFERENCES PUBLIC.M_AUDIT_EVENT(ID)"
    };

    private static final Pattern[] OK_PATTERNS = new Pattern[] {
            Pattern.compile(".*Duplicate entry '.*' for key 'iExtItemDefinition'.*"),               // MySQL
            Pattern.compile(".*ORA-00001:.*\\.IEXTITEMDEFINITION\\).*")                             // Oracle
    };

    private static final String[] OK_STRINGS = new String[] {
            "Unique index or primary key violation: \"IEXTITEMDEFINITION",              // H2
            "Violation of UNIQUE KEY constraint 'iExtItemDefinition'",                  // SQL Server
            "duplicate key value violates unique constraint \"iextitemdefinition\"",    // PostgreSQL

            // SQL Server
            "Violation of PRIMARY KEY constraint 'PK__m_org_cl__",
            "Violation of PRIMARY KEY constraint 'PK__m_refere__",
            "Violation of PRIMARY KEY constraint 'PK__m_assign__",
            "Violation of PRIMARY KEY constraint 'PK__m_operat__",
            "Violation of PRIMARY KEY constraint 'PK__m_audit___",      // MID-4815

            // ???
            "is not present in table \"m_ext_item\"",

            // PostgreSQL
            "duplicate key value violates unique constraint \"m_audit_item_pkey\"",     // MID-4815
            "duplicate key value violates unique constraint \"m_audit_event_pkey\"",    // MID-4815
            "duplicate key value violates unique constraint \"m_org_closure_pkey\"",
            "duplicate key value violates unique constraint \"m_reference_pkey\"",
            "duplicate key value violates unique constraint \"m_assignment_pkey\"",
            "duplicate key value violates unique constraint \"m_operation_execution_pkey\""     // TODO resolve more intelligently (and completely!)
    };

    private boolean hasSerializationRelatedConstraintViolationException(Throwable ex) {
        ConstraintViolationException cve = ExceptionUtil.findException(ex, ConstraintViolationException.class);
        return cve != null && isSerializationRelatedConstraintViolationException(cve);
    }

    boolean isSerializationRelatedConstraintViolationException(ConstraintViolationException cve) {
        // BRUTAL HACK - serialization-related issues sometimes manifest themselves as ConstraintViolationException.
        //
        // For PostgreSQL and Microsoft SQL Server this happens mainly when automatically generated identifiers are to be
        // persisted (presumably because of their optimistic approach to serialization isolation).
        //
        // The "solution" provided here is not complete, as it does not recognize all places where IDs are generated.
        //
        // Moreover:
        //
        // For all databases the serialization issues can occur when m_ext_item records are created in parallel: see
        // ExtDictionaryConcurrencyTest but with "synchronized" keyword removed from state-changing ExtItemDictionary methods.
        // The solution is to check for ConstraintViolationExceptions related to iExtItemDefinition and treat these as
        // serialization-related issues.
        //
        // TODO: somewhat generalize this approach
        //
        // see MID-4698

        SQLException sqlException = findSqlException(cve);
        if (sqlException != null) {
            SQLException nextException = sqlException.getNextException();
            LOGGER.debug("ConstraintViolationException = {}; SQL exception = {}; embedded SQL exception = {}", cve, sqlException, nextException);
            String msg1;
            if (sqlException.getMessage() != null) {
                msg1 = sqlException.getMessage().trim();
            } else {
                msg1 = "";
            }
            String msg2;
            if (nextException != null && nextException.getMessage() != null) {
                msg2 = nextException.getMessage().trim();
            } else {
                msg2 = "";
            }
            for (String okString : OK_STRINGS) {
                if (msg1.contains(okString) || msg2.contains(okString)) {
                    return true;
                }
            }
            for (Pattern okPattern : OK_PATTERNS) {
                if (okPattern.matcher(msg1).matches() || okPattern.matcher(msg2).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private SQLException findSqlException(Throwable ex) {
        return ExceptionUtil.findException(ex, SQLException.class);
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

    public DataSource dataSource() {
        return dataSource;
    }

    public Configuration querydslConfiguration() {
        if (querydslConfiguration == null) {
            synchronized (this) {
                SqlRepositoryConfiguration.Database database =
                        sqlRepositoryConfiguration.getDatabaseType();
                switch (database) {
                    case H2:
                        querydslConfiguration =
                                new Configuration(H2Templates.DEFAULT);
                        break;
                    case MYSQL:
                    case MARIADB:
                        querydslConfiguration =
                                new Configuration(MySQLTemplates.DEFAULT);
                        break;
                    case POSTGRESQL:
                        querydslConfiguration =
                                new Configuration(PostgreSQLTemplates.DEFAULT);
                        break;
                    case SQLSERVER:
                        querydslConfiguration =
                                new Configuration(SQLServer2012Templates.DEFAULT);
                        break;
                    case ORACLE:
                        querydslConfiguration =
                                new Configuration(OracleTemplates.DEFAULT);
                        break;
                    default:
                        throw new SystemException(
                                "Unsupported database type " + database + " for Querydsl config");
                }

                // See InstantType javadoc for the reasons why we need this to support Instant.
                querydslConfiguration.register(new InstantType());
                // Alternatively we may stick to Timestamp and go on with our miserable lives. ;-)
            }
        }
        return querydslConfiguration;
    }
}
