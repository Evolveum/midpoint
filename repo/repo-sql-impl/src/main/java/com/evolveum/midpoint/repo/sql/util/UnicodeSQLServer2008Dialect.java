/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.dialect.SQLServerDialect;

import java.sql.Types;

/**
 * @author lazyman
 */
public class UnicodeSQLServer2008Dialect extends SQLServerDialect {

    private static final int MAX_LENGTH = 8000;

    public UnicodeSQLServer2008Dialect() {
        // FIXME: Add support for dialect
        //registerColumnType(Types.CLOB, "nvarchar(MAX)");
        //registerColumnType(Types.LONGVARCHAR, "nvarchar(MAX) collate database_default");
        //registerColumnType(Types.VARCHAR, "nvarchar(MAX) collate database_default");
        //registerColumnType(Types.VARCHAR, MAX_LENGTH, "nvarchar($l) collate database_default");
    }
}
