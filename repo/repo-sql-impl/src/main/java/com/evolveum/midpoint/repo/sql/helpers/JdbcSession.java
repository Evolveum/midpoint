/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database.ORACLE;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Objects;

import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            // Connection has its transaction isolation set by Hikari, except for obscure ones.
            if (repoConfiguration.getTransactionIsolation() == TransactionIsolation.SNAPSHOT) {
                LOGGER.trace("Setting transaction isolation level SNAPSHOT.");
                // bit rough from a constructor, but it's safe, connection field is already set
                executeStatement("SET TRANSACTION ISOLATION LEVEL SNAPSHOT");
            }
        } catch (SystemException e) {
            // even for SystemException we want to rewrap it to add this message
            throw new SystemException("SQL connection setup problem for JDBC session", e);
        }
    }

    /**
     * Starts transaction and returns {@code this}.
     */
    public JdbcSession startTransaction() {
        return startTransaction(false, repoConfiguration.getReadOnlyTransactionStatement());
    }

    /**
     * Starts transaction with different transaction isolation level.
     * This level will NOT be reverted to previous level after the end of transaction.
     * It is advisable to use this only for short-lived JDBC sessions with special requirements.
     */
    public JdbcSession startTransaction(int transactionLevel) {
        try {
            connection.setTransactionIsolation(transactionLevel);
        } catch (SQLException e) {
            throw new SystemException(
                    "Couldn't change transaction isolation level for JDBC session", e);
        }
        return startTransaction(false, null);
    }

    /**
     * Starts read-only transaction and returns {@code this}.
     */
    public JdbcSession startReadOnlyTransaction() {
        return startTransaction(true, repoConfiguration.getReadOnlyTransactionStatement());
    }

    private JdbcSession startTransaction(boolean readonly, String readOnlyTrnStatement) {
        LOGGER.debug("Starting {}transaction", readonly ? "readonly " : "");

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new SystemException("SQL connection setup problem for JDBC session", e);
        }

        rollbackForReadOnly = false;
        // Configuration check really means: "Does it support read-only transactions?"
        if (readonly) {
            if (readOnlyTrnStatement != null) {
                executeStatement(readOnlyTrnStatement);
            } else {
                rollbackForReadOnly = true;
            }
        }
        return this;
    }

    /**
     * Commits current transaction.
     * If read-only transaction is not supported by database it rolls back read-only transaction.
     */
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

    /**
     * Rolls back the transaction.
     * See also various {@code handle*Exception()} methods that do the same thing
     * adding exception logging and changes to the operation result.
     */
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
     * Alters table adding another column - intended for custom/extension columns.
     */
    public void addColumn(String tableName, ColumnMetadata column) {
        LOGGER.info("Altering table {}, adding column {}.", tableName, column.getName());
        StringBuilder type = new StringBuilder(getNativeTypeName(column.getJdbcType()));
        if (column.hasSize()) {
            type.append('(').append(column.getSize());
            if (databaseType() == ORACLE && isVarcharType(column.getJdbcType())) {
                // this properly sizes the varchar for Unicode
                type.append(" CHAR");
            }
            if (column.hasDigits()) {
                type.append(',').append(column.getDigits());
            }
            type.append(')');
        }
        if (!column.isNullable()) {
            type.append(" NOT NULL");
        }
        // we don't expect defaults and other features now, can be extended later

        executeStatement("ALTER TABLE " + tableName + " ADD " + column.getName() + " " + type);
    }

    private boolean isVarcharType(int jdbcType) {
        return jdbcType == Types.VARCHAR
                || jdbcType == Types.NVARCHAR
                || jdbcType == Types.LONGVARCHAR
                || jdbcType == Types.LONGNVARCHAR;
    }

    /**
     * Creates Querydsl query based on current Querydsl configuration and session's connection.
     */
    public SQLQuery<?> query() {
        return new SQLQuery<>(connection, querydslConfiguration);
    }

    /**
     * Starts insert clause for specified entity.
     * Check <a href="http://www.querydsl.com/static/querydsl/4.1.3/reference/html_single/#d0e1316">Querydsl docs on insert</a>
     * for more about various ways how to use it.
     */
    public SQLInsertClause insert(RelationalPath<?> entity) {
        return new SQLInsertClause(connection, querydslConfiguration, entity);
    }

    public SQLDeleteClause delete(RelationalPath<?> entity) {
        return new SQLDeleteClause(connection, querydslConfiguration, entity);
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

    /**
     * Rolls back the transaction and throws exception.
     * Uses {@link #handleGeneralCheckedException} or {@link #handleGeneralRuntimeException}
     * depending on the exception type.
     *
     * @throws SystemException wrapping the exception used as parameter
     * @throws RuntimeException rethrows input exception if related to transaction serialization
     */
    public void handleGeneralException(
            @NotNull Throwable ex,
            @Nullable OperationResult result) {
        if (ex instanceof RuntimeException) {
            handleGeneralRuntimeException((RuntimeException) ex, result);
        } else {
            handleGeneralCheckedException(ex, result);
        }
        throw new AssertionError("Shouldn't get here");
    }

    /**
     * Rolls back the transaction and throws exception.
     * If the exception is related to transaction serialization problems, the operation result
     * does not record the error (non-fatal).
     *
     * @throws SystemException wrapping the exception used as parameter
     * @throws RuntimeException rethrows input exception if related to transaction serialization
     */
    public void handleGeneralRuntimeException(
            @NotNull RuntimeException ex,
            @Nullable OperationResult result) {
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

    /**
     * Rolls back the transaction and throws exception.
     *
     * @throws SystemException wrapping the exception used as parameter
     */
    public void handleGeneralCheckedException(
            @NotNull Throwable ex,
            @Nullable OperationResult result) {
        LOGGER.error("General checked exception occurred.", ex);

        boolean fatal = !isExceptionRelatedToSerialization(ex);
        rollbackTransaction(ex, result, fatal);
        throw new SystemException(ex.getMessage(), ex);
    }

    private void rollbackTransaction(
            @NotNull Throwable ex,
            @Nullable OperationResult result,
            boolean fatal) {
        // non-fatal errors will NOT be put into OperationResult, not to confuse the user
        if (result != null && fatal) {
            result.recordFatalError(ex);
        }

        rollback();
    }

    private boolean isExceptionRelatedToSerialization(@NotNull Throwable ex) {
        return new TransactionSerializationProblemDetector(repoConfiguration, LOGGER)
                .isExceptionRelatedToSerialization(ex);
    }
}
