/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import com.google.common.base.Strings;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLInsertClause;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.TransactionIsolation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Wrapper around JDBC {@link java.sql.Connection} representing "session", typically a transaction.
 * Connection is prepared for the transaction (auto-commit is disabled), but
 * {@link #startTransaction()} or {@link #startReadOnlyTransaction()} must be called explicitly.
 * While not typical, multiple transactions can be executed in sequence (not concurrently).
 * Object is {@link AutoCloseable} and can be used in try-with-resource blocks.
 * If transaction is still active during closing the JDBC session, it commits the transaction.
 * If database does not support read-only transactions directly,
 * {@link #commit()} executes rollback instead.
 * <p>
 * Provides convenient methods for handling exceptions and {@link OperationResult}s.
 * <p>
 * TODO MID-6318 - review this decision:
 * All {@link SQLException}s are translated to {@link SystemException}.
 */
public class JdbcSession implements AutoCloseable {

    private static final Trace LOGGER = TraceManager.getTrace(JdbcSession.class);

    private final Connection connection;
    private final SqlRepositoryConfiguration repoConfiguration;
    private final Configuration querydslConfiguration;

    private boolean rollbackForReadOnly;

    public JdbcSession(
            @NotNull Connection connection,
            @NotNull SqlRepositoryConfiguration repoConfiguration,
            @NotNull Configuration querydslConfiguration) {
        this.connection = Objects.requireNonNull(connection);
        this.repoConfiguration = repoConfiguration;
        this.querydslConfiguration = querydslConfiguration;

        try {
            connection.setAutoCommit(false);
            // Connection has its transaction isolation set by Hikari, except for obscure ones.
            if (repoConfiguration.getTransactionIsolation() == TransactionIsolation.SNAPSHOT) {
                LOGGER.trace("Setting transaction isolation level SNAPSHOT.");
                // bit rough from a constructor, but it's safe, connection field is already set
                executeStatement("SET TRANSACTION ISOLATION LEVEL SNAPSHOT");
            }
        } catch (SQLException | SystemException e) {
            // even for SystemException we want to rewrap it to add this message
            throw new SystemException("SQL connection setup problem for JDBC session", e);
        }
    }

    public JdbcSession startTransaction() {
        return startTransaction(false);
    }

    public JdbcSession startReadOnlyTransaction() {
        return startTransaction(true);
    }

    public JdbcSession startTransaction(boolean readonly) {
        LOGGER.debug("Starting {}transaction", readonly ? "readonly " : "");

        rollbackForReadOnly = false;
        // Configuration check really means: "Does it support read-only transactions?"
        if (readonly) {
            if (repoConfiguration.isUseReadOnlyTransactions()) {
                executeStatement("SET TRANSACTION READ ONLY");
            } else {
                rollbackForReadOnly = true;
            }
        }
        return this;
    }

    public void commit() {
        try {
            if (rollbackForReadOnly) {
                LOGGER.debug("Commit - rolling back read-only transaction without direct DB support");
                connection.rollback();
                return;
            }

            LOGGER.debug("Committing transaction");
            connection.commit();
        } catch (SQLException e) {
            throw new SystemException("Couldn't commit transaction", e);
        }
    }

    public void rollback() {
        try {
            LOGGER.debug("Rolling back transaction");
            connection.rollback();
        } catch (SQLException e) {
            throw new SystemException("Couldn't rollback transaction", e);
        }
    }

    /**
     * This is used for technical statements and throws {@link SystemException}.
     * Don't use this for unsafe concatenated statements with parameters!
     */
    public void executeStatement(String sql) throws SystemException {
        try {
            LOGGER.debug("Executing technical statement: " + sql);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new SystemException("Couldn't execute statement: " + sql, e);
        }
    }

    /**
     * Creates Querydsl query based on current Querydsl configuration and session's connection.
     */
    public SQLQuery<?> query() {
        return new SQLQuery<>(connection, querydslConfiguration);
    }

    public SQLInsertClause insert(RelationalPath<?> entity) {
        return new SQLInsertClause(connection, querydslConfiguration, entity);
    }

    public String getNativeTypeName(int typeCode) {
        return querydslConfiguration.getTemplates().getTypeNameForCode(typeCode);
    }

    public Connection connection() {
        return connection;
    }

    public SqlRepositoryConfiguration.Database databaseType() {
        return repoConfiguration.getDatabaseType();
    }

    @Override
    public void close() {
        try {
            commit();
            connection.close();
        } catch (SQLException e) {
            throw new SystemException(e);
        }
    }

    // exception and operation result handling (mostly from BaseHelper and adapted for JDBC)

    public void handleGeneralException(Throwable ex, OperationResult result) {
        if (ex instanceof RuntimeException) {
            handleGeneralRuntimeException((RuntimeException) ex, result);
        } else {
            handleGeneralCheckedException(ex, result);
        }
        throw new IllegalStateException("Shouldn't get here");
    }

    public void handleGeneralRuntimeException(RuntimeException ex, OperationResult result) {
        LOGGER.debug("General runtime exception occurred.", ex);

        if (isExceptionRelatedToSerialization(ex)) {
            rollbackTransaction(ex, result, false);
            // this exception will be caught and processed in logOperationAttempt,
            // so it's safe to pass any RuntimeException here
            throw ex;
        } else {
            rollbackTransaction(ex, result, true);
            if (ex instanceof SystemException) {
                throw ex;
            } else {
                throw new SystemException(ex.getMessage(), ex);
            }
        }
    }

    public void handleGeneralCheckedException(Throwable ex, OperationResult result) {
        LOGGER.error("General checked exception occurred.", ex);

        boolean fatal = !isExceptionRelatedToSerialization(ex);
        rollbackTransaction(ex, result, fatal);
        throw new SystemException(ex.getMessage(), ex);
    }

    void rollbackTransaction(Throwable ex, OperationResult result, boolean fatal) {
        String message = ex != null ? ex.getMessage() : "null";
        rollbackTransaction(ex, message, result, fatal);
    }

    void rollbackTransaction(Throwable ex, String message, OperationResult result, boolean fatal) {
        if (Strings.isNullOrEmpty(message) && ex != null) {
            message = ex.getMessage();
        }

        // non-fatal errors will NOT be put into OperationResult, not to confuse the user
        if (result != null && fatal) {
            result.recordFatalError(message, ex);
        }

        rollback();
    }

    private boolean isExceptionRelatedToSerialization(Throwable ex) {
        return new TransactionSerializationProblemDetector(repoConfiguration, LOGGER)
                .isExceptionRelatedToSerialization(ex);
    }
}
