/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querydsl;

import java.sql.Types;

import com.querydsl.sql.SQLServer2012Templates;

public class MidpointSQLServerTemplates extends SQLServer2012Templates {

    public static final MidpointSQLServerTemplates DEFAULT = new MidpointSQLServerTemplates();

    public MidpointSQLServerTemplates() {
        // In ColumnMetadata we use VARCHAR uniformly, but in SQL Server it should be nvarchar.
        addTypeNameToCode("nvarchar", Types.VARCHAR, true);
    }
}
