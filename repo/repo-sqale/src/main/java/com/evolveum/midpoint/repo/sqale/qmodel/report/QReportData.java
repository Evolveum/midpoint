/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import java.sql.Types;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QReportData extends QObject<MReportData> {

    private static final long serialVersionUID = -544485328996889511L;

    public static final String TABLE_NAME = "m_report_data";

    public static final ColumnMetadata REPORT_REF_TARGET_OID =
            ColumnMetadata.named("reportRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata REPORT_REF_TARGET_TYPE =
            ColumnMetadata.named("reportRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata REPORT_REF_RELATION_ID =
            ColumnMetadata.named("reportRef_relation_id").ofType(Types.INTEGER);

    public final UuidPath reportRefTargetOid =
            createUuid("reportRefTargetOid", REPORT_REF_TARGET_OID);
    public final EnumPath<MObjectType> reportRefTargetType =
            createEnum("reportRefTargetType", MObjectType.class, REPORT_REF_TARGET_TYPE);
    public final NumberPath<Integer> reportRefRelationId =
            createInteger("reportRefRelationId", REPORT_REF_RELATION_ID);

    public QReportData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QReportData(String variable, String schema, String table) {
        super(MReportData.class, variable, schema, table);
    }
}
