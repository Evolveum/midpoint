/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import java.sql.Connection;

/**
 * @author mederly
 */
public enum TransactionIsolation {

    READ_COMMITTED("readCommitted", Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ("repeatableRead", Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE("serializable", Connection.TRANSACTION_SERIALIZABLE),
    SNAPSHOT("snapshot", null);       // this is a non-standard setting for MS SQL Server

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
        for (TransactionIsolation c: TransactionIsolation.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
