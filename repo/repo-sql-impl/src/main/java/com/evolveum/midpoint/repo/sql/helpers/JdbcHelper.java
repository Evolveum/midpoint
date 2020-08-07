/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Wrapper around JDBC {@link java.sql.Connection} representing "session", typically a transaction.
 * TODO MID-6318 - review this decision:
 * Helper also translates {@link SQLException}s to {@link SystemException}.
 */
public class JdbcHelper implements AutoCloseable {

    private final Connection connection;

    public JdbcHelper(@NotNull Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    public JdbcHelper startTransaction() {
        return startTransaction(false);
    }

    public JdbcHelper startReadOnlyTransaction() {
        return startTransaction(true);
    }

    public JdbcHelper startTransaction(boolean readonly) {
        return this;
    }

    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new SystemException(e);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new SystemException(e);
        }
    }
}
