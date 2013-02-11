package com.evolveum.midpoint.repo.sql;

import java.sql.Connection;

/**
 * @author mederly
 */
public enum TransactionIsolation {

    READ_COMMITTED("readCommitted", Connection.TRANSACTION_READ_COMMITTED),
    SERIALIZABLE("serializable", Connection.TRANSACTION_SERIALIZABLE);
    private final String value;
    private final int jdbcValue;

    TransactionIsolation(String value, int jdbcValue) {
        this.value = value;
        this.jdbcValue = jdbcValue;
    }

    public String value() {
        return value;
    }

    public int jdbcValue() {
        return jdbcValue;
    }

    public static TransactionIsolation fromValue(String v) {
        for (TransactionIsolation c: TransactionIsolation.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
