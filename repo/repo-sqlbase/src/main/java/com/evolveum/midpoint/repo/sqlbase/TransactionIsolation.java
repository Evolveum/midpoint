/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import java.sql.Connection;

/**
 * @author mederly
 */
public enum TransactionIsolation {

    READ_COMMITTED("readCommitted", Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ("repeatableRead", Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE("serializable", Connection.TRANSACTION_SERIALIZABLE),

    /** This is a non-standard setting for MS SQL Server, but supported by other DBs too. */
    SNAPSHOT("snapshot", null);

    private final String value;
    private final Integer jdbcValue;

    TransactionIsolation(String value, Integer jdbcValue) {
        this.value = value;
        this.jdbcValue = jdbcValue;
    }

    public String value() {
        return value;
    }

    public Integer jdbcValue() {
        return jdbcValue;
    }

    public static TransactionIsolation fromValue(String v) {
        for (TransactionIsolation c : TransactionIsolation.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
