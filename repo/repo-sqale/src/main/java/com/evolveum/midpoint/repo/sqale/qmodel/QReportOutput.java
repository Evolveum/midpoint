/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qbean.MReportOutput;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QReportOutput extends QObject<MReportOutput> {

    private static final long serialVersionUID = -544485328996889511L;

    public static final String TABLE_NAME = "m_report_output";

    public static final ColumnMetadata REPORT_REF_TARGET_OID =
            ColumnMetadata.named("reportRef_targetOid").ofType(UUID_TYPE);
    public static final ColumnMetadata REPORT_REF_TARGET_TYPE =
            ColumnMetadata.named("reportRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata REPORT_REF_RELATION_ID =
            ColumnMetadata.named("reportRef_relation_id").ofType(Types.INTEGER);

    public final UuidPath reportRefTargetOid =
            createUuid("reportRefTargetOid", REPORT_REF_TARGET_OID);
    public final NumberPath<Integer> reportRefTargetType =
            createInteger("reportRefTargetType", REPORT_REF_TARGET_TYPE);
    public final NumberPath<Integer> reportRefRelationId =
            createInteger("reportRefRelationId", REPORT_REF_RELATION_ID);

    public QReportOutput(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QReportOutput(String variable, String schema, String table) {
        super(MReportOutput.class, variable, schema, table);
    }
}
