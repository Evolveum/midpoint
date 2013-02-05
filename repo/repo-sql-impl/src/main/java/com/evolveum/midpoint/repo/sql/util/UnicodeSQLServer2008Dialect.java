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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.dialect.SQLServer2008Dialect;

import java.sql.Types;

/**
 * @author lazyman
 */
public class UnicodeSQLServer2008Dialect extends SQLServer2008Dialect {

    private static final int MAX_LENGTH = 8000;

    public UnicodeSQLServer2008Dialect() {
        registerColumnType(Types.CLOB, "nvarchar(MAX)");
        registerColumnType(Types.LONGVARCHAR, "nvarchar(MAX)");
        registerColumnType(Types.VARCHAR, "nvarchar(MAX)");
        registerColumnType(Types.VARCHAR, MAX_LENGTH, "nvarchar($l)");
    }
}
