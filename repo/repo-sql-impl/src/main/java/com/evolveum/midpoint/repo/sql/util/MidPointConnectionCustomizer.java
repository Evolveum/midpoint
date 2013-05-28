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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.TransactionIsolation;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.mchange.v2.c3p0.AbstractConnectionCustomizer;
import org.h2.jdbc.JdbcConnection;

import java.sql.Connection;

/**
 * This connection customizer forces transaction isolation level to a specified value
 * for all connections. Because of some H2 bug, transaction isolation level has to be set
 * {@link com.mchange.v2.c3p0.ConnectionCustomizer#onCheckOut(java.sql.Connection, String)}
 * when using H2. With other databases connection is updated during
 * {@link com.mchange.v2.c3p0.ConnectionCustomizer#onAcquire(java.sql.Connection, String)}
 *
 * @author lazyman
 */
public class MidPointConnectionCustomizer extends AbstractConnectionCustomizer {

    // ugly hack that this is static; however, we have no way to influence instances of this class
    private static TransactionIsolation transactionIsolation = TransactionIsolation.SERIALIZABLE;

    private static final Trace LOGGER = TraceManager.getTrace(MidPointConnectionCustomizer.class);

    @Override
    public void onAcquire(Connection connection, String parentDataSourceIdentityToken) throws Exception {
        if (!(connection instanceof JdbcConnection)) {
            updateTransactionIsolation(connection);
        }
    }

    @Override
    public void onCheckOut(Connection connection, String parentDataSourceIdentityToken) throws Exception {
        if (connection instanceof JdbcConnection) {
            updateTransactionIsolation(connection);
        }
    }

    private void updateTransactionIsolation(Connection connection) throws Exception {

        if (transactionIsolation.jdbcValue() != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Setting connection transaction isolation to " + transactionIsolation.jdbcValue() + " (" + transactionIsolation.value() + ")");
            }
            connection.setTransactionIsolation(transactionIsolation.jdbcValue());

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Skipped setting transaction isolation '" + transactionIsolation.value() + "', because it has no JDBC mapping");
            }

        }
    }

    public static TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public static void setTransactionIsolation(TransactionIsolation transactionIsolation) {
        MidPointConnectionCustomizer.transactionIsolation = transactionIsolation;
    }
}
