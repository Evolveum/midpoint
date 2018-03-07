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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
//import com.mchange.v2.c3p0.impl.DefaultConnectionTester;

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

public class MidPointConnectionTester { // extends DefaultConnectionTester {

//    private static final Trace LOGGER = TraceManager.getTrace(MidPointConnectionTester.class);
//
//    public static final String POSTGRESQL_PRODUCT_NAME = "PostgreSQL";
//
//    private void rollbackChecked(Connection c) {
//        try {
//            if (POSTGRESQL_PRODUCT_NAME.equals(c.getMetaData().getDatabaseProductName())) {
//                c.rollback();
//            }
//        } catch (SQLException e) {
//            LOGGER.debug("An exception got when rolling back current transaction on suspicious DB connection", e);
//        }
//    }
//
//    @Override
//    public int activeCheckConnection(Connection c, String query, Throwable[] rootCauseOutParamHolder) {
//        rollbackChecked(c);
//        return super.activeCheckConnection(c, query, rootCauseOutParamHolder);
//    }
//
//    @Override
//    public int statusOnException(Connection c, Throwable t, String query, Throwable[] rootCauseOutParamHolder) {
//        rollbackChecked(c);
//        return super.statusOnException(c, t, query, rootCauseOutParamHolder);
//    }
}
