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

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.TransactionIsolation;
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
 * TODO MID-6318 - review this decision:
 * All {@link SQLException}s are translated to {@link SystemException}.
 */
public class JdbcSession implements AutoCloseable {

    private static final Trace LOGGER = TraceManager.getTrace(JdbcSession.class);

    private final Connection connection;
    private final SqlRepositoryConfiguration configuration;

    private boolean rollbackForReadOnly;

    public JdbcSession(
            @NotNull Connection connection,
            @NotNull SqlRepositoryConfiguration configuration) {
        this.connection = Objects.requireNonNull(connection);
        this.configuration = configuration;

        try {
            connection.setAutoCommit(false);
            // Connection has its transaction isolation set by Hikari, except for obscure ones.
            if (configuration.getTransactionIsolation() == TransactionIsolation.SNAPSHOT) {
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
            if (configuration.isUseReadOnlyTransactions()) {
                executeStatement("SET TRANSACTION READ ONLY");
            } else {
                rollbackForReadOnly = true;
            }
        }
        // TODO
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

    public Connection connection() {
        return connection;
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
}
