/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.session;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QSessionData extends QAssignmentHolder<MSessionObject> {

    public static final String TABLE_NAME = "m_role_analysis_session_table";

    public QSessionData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSessionData(String variable, String schema, String table) {
        super(MSessionObject.class, variable, schema, table);
    }

}
