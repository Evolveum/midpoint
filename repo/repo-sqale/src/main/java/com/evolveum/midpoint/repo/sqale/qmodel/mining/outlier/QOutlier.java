/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import java.sql.Types;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
public class QOutlier extends QAssignmentHolder<MOutlier> {

    public static final String TABLE_NAME = "m_role_analysis_outlier";

    public static final ColumnMetadata TARGET_OBJECT_REF_TARGET_OID =
            ColumnMetadata.named("targetObjectRefTargetOid").ofType(UuidPath.UUID_TYPE);

    public static final ColumnMetadata TARGET_OBJECT_REF_TARGET_TYPE =
            ColumnMetadata.named("targetObjectRefTargetType").ofType(Types.OTHER);

    public static final ColumnMetadata TARGET_OBJECT_REF_RELATION_ID =
            ColumnMetadata.named("targetObjectRefRelationId").ofType(Types.INTEGER);

    public static final ColumnMetadata OVERALL_CONFIDENCE =
            ColumnMetadata.named("overallConfidence").ofType(Types.DOUBLE);

    public QOutlier(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QOutlier(String variable, String schema, String table) {
        super(MOutlier.class, variable, schema, table);
    }

    public final UuidPath targetObjectRefTargetOid =
            createUuid("targetObjectRefTargetOid", TARGET_OBJECT_REF_TARGET_OID);

    public final EnumPath<MObjectType> targetObjectRefTargetType =
            createEnum("targetObjectRefTargetType", MObjectType.class, TARGET_OBJECT_REF_TARGET_TYPE);

    public final NumberPath<Integer> targetObjectRefRelationId =
            createInteger("targetObjectRefRelationId", TARGET_OBJECT_REF_RELATION_ID);

    public final NumberPath<Double> overallConfidence = createNumber("overallConfidence", Double.class, OVERALL_CONFIDENCE);


}
