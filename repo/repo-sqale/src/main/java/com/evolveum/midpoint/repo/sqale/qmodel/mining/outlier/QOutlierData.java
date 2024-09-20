/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
public class QOutlierData extends QAssignmentHolder<MOutlierObject> {

    public static final String TABLE_NAME = "m_role_analysis_outlier";

    public static final ColumnMetadata TARGET_OBJECT_REF_TARGET_OID =
            ColumnMetadata.named("targetObjectRefTargetOid").ofType(UuidPath.UUID_TYPE);

    public final UuidPath targetObjectRefTargetOid =
            createUuid("targetObjectRefTargetOid", TARGET_OBJECT_REF_TARGET_OID);

    public static final ColumnMetadata TARGET_OBJECT_REF_TARGET_TYPE =
            ColumnMetadata.named("targetObjectRefTargetType").ofType(Types.OTHER);

    public final EnumPath<MObjectType> targetObjectRefTargetType =
            createEnum("targetObjectRefTargetType", MObjectType.class, TARGET_OBJECT_REF_TARGET_TYPE);

    public static final ColumnMetadata TARGET_OBJECT_REF_RELATION_ID =
            ColumnMetadata.named("targetObjectRefRelationId").ofType(Types.INTEGER);

    public final NumberPath<Integer> targetObjectRefRelationId =
            createInteger("targetObjectRefRelationId", TARGET_OBJECT_REF_RELATION_ID);

    public static final ColumnMetadata TARGET_CLUSTER_REF_TARGET_OID =
            ColumnMetadata.named("targetCLUSTERRefTargetOid").ofType(UuidPath.UUID_TYPE);

    public final UuidPath targetClusterRefTargetOid =
            createUuid("targetClusterRefTargetOid", TARGET_CLUSTER_REF_TARGET_OID);

    public static final ColumnMetadata TARGET_CLUSTER_REF_TARGET_TYPE =
            ColumnMetadata.named("targetClusterRefTargetType").ofType(Types.OTHER);

    public final EnumPath<MObjectType> targetClusterRefTargetType =
            createEnum("targetClusterRefTargetType", MObjectType.class, TARGET_CLUSTER_REF_TARGET_TYPE);

    public static final ColumnMetadata TARGET_CLUSTER_REF_RELATION_ID =
            ColumnMetadata.named("targetClusterRefRelationId").ofType(Types.INTEGER);

    public final NumberPath<Integer> targetClusterRefRelationId =
            createInteger("targetClusterRefRelationId", TARGET_CLUSTER_REF_RELATION_ID);

    public QOutlierData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QOutlierData(String variable, String schema, String table) {
        super(MOutlierObject.class, variable, schema, table);
    }

}
