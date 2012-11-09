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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.mchange.v2.c3p0.impl.DefaultConnectionTester;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * When getting serialization-related SQLException (as when getting any exception), c3p0 checks the connection
 * whether it is valid. However, in case of PostgreSQL, ANY 'select' command used for testing
 * (including 'select 1') fails in this situation with a message "current transaction is aborted,
 * commands ignored until end of transaction block". So the connection is released, and new one is
 * created, which leads to performance problems.
 *
 * So it is probably best to roll the transaction back before checking the connection validity. To be safe,
 * we currently do this only for PostgreSQL; other databases (H2, MySQL) do not seem to have problems of this kind:
 *  - MySQL does not even notice any serialization issues (why??? that's a bit suspicious)
 *  - H2 checks the connection, but it can do that without problems within current transaction.
 *
 * @author mederly
 */

public class MidPointConnectionTester extends DefaultConnectionTester {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointConnectionTester.class);

    public static final String POSTGRESQL_PRODUCT_NAME = "PostgreSQL";

    private void rollbackChecked(Connection c) {
        try {
            if (POSTGRESQL_PRODUCT_NAME.equals(c.getMetaData().getDatabaseProductName())) {
                c.rollback();
            }
        } catch (SQLException e) {
            LOGGER.debug("An exception got when rolling back current transaction on suspicious DB connection", e);
        }
    }

    @Override
    public int activeCheckConnection(Connection c, String query, Throwable[] rootCauseOutParamHolder) {
        rollbackChecked(c);
        return super.activeCheckConnection(c, query, rootCauseOutParamHolder);
    }

    @Override
    public int statusOnException(Connection c, Throwable t, String query, Throwable[] rootCauseOutParamHolder) {
        rollbackChecked(c);
        return super.statusOnException(c, t, query, rootCauseOutParamHolder);
    }
}
