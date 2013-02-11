/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Setting connection transaction isolation to " + transactionIsolation.jdbcValue() + " (" + transactionIsolation.value() + ")");
        }
        connection.setTransactionIsolation(transactionIsolation.jdbcValue());
    }

    public static TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public static void setTransactionIsolation(TransactionIsolation transactionIsolation) {
        MidPointConnectionCustomizer.transactionIsolation = transactionIsolation;
    }
}
