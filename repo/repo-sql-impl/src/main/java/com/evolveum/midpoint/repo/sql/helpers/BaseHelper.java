/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import java.sql.SQLException;
import javax.sql.DataSource;

import com.google.common.base.Strings;
import com.querydsl.sql.*;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.sql.*;
import com.evolveum.midpoint.repo.sql.pure.querydsl.MidpointOracleTemplates;
import com.evolveum.midpoint.repo.sql.pure.querymodel.support.InstantType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.backoff.BackoffComputer;
import com.evolveum.midpoint.util.backoff.ExponentialBackoffComputer;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Core functionality needed in all members of SQL service family.
 * Taken out of SqlBaseService in order to be accessible from other components by autowiring.
 * <p>
 * Originally more Hibernate oriented, some pure JDBC parts are added ({@link #newJdbcSession()}
 * and {@link #dataSource} field) which may be basis of pure JDBC repository in the future.
 * It may go away from this class, but for now, hybrid approach is chosen.
 * But while helper methods are used for ORM/Hibernate stuff, after creating {@link JdbcSession}
 * all other methods are called on the returned object.
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

    private final DataSource dataSource;

    // don't access outside of querydslConfiguration() method, always use the method to lazy-init
    private Configuration querydslConfiguration;

    // used for non-bean creation
    public BaseHelper(
            @NotNull SqlRepositoryConfiguration sqlRepositoryConfiguration,
            SessionFactory sessionFactory,
            DataSource dataSource) {
        this.sqlRepositoryConfiguration = sqlRepositoryConfiguration;
        this.sessionFactory = sessionFactory;
        this.dataSource = dataSource;
    }

    @Autowired
    public BaseHelper(
            SqlRepositoryFactory repositoryFactory,
            SessionFactory sessionFactory,
            DataSource dataSource) {
        this(repositoryFactory.getSqlConfiguration(), sessionFactory, dataSource);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
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
        return new TransactionSerializationProblemDetector(sqlRepositoryConfiguration, LOGGER)
                .isExceptionRelatedToSerialization(ex);
    }

    boolean isSerializationRelatedConstraintViolationException(ConstraintViolationException cve) {
        return new TransactionSerializationProblemDetector(sqlRepositoryConfiguration, LOGGER)
                .isSerializationRelatedConstraintViolationException(cve);
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

    public synchronized Configuration querydslConfiguration() {
        if (querydslConfiguration != null) {
            return querydslConfiguration;
        }

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
                        new Configuration(MidpointOracleTemplates.DEFAULT);
                break;
            default:
                throw new SystemException(
                        "Unsupported database type " + database + " for Querydsl config");
        }

        // See InstantType javadoc for the reasons why we need this to support Instant.
        querydslConfiguration.register(new InstantType());
        // Alternatively we may stick to Timestamp and go on with our miserable lives. ;-)
        return querydslConfiguration;
    }

    /**
     * Creates {@link JdbcSession} that typically represents transactional work on JDBC connection.
     * All other lifecycle methods are to be called on the returned object.
     * Object is {@link AutoCloseable} and can be used in try-with-resource blocks.
     */
    public JdbcSession newJdbcSession() {
        try {
            return new JdbcSession(
                    dataSource().getConnection(),
                    sqlRepositoryConfiguration,
                    querydslConfiguration());
        } catch (SQLException e) {
            throw new SystemException("Cannot create JDBC connection", e);
        }
    }
}
