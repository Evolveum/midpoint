/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.sql.Types;

import com.querydsl.sql.SQLServer2012Templates;

public class MidpointSQLServerTemplates extends SQLServer2012Templates {

    public static final MidpointSQLServerTemplates DEFAULT = new MidpointSQLServerTemplates();

    public MidpointSQLServerTemplates() {
        // In ColumnMetadata we use VARCHAR uniformly, but in SQL Server it should be nvarchar.
        addTypeNameToCode("nvarchar", Types.VARCHAR, true);
    }
}
