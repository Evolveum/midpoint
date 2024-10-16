/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QOutlierPartition extends QContainer<MOutlierPartition, MOutlier> {

    public static final String TABLE_NAME = "m_role_analysis_outlier_partition";
    public static final String ALIAS = "op";


    public static final ColumnMetadata CLUSTER_REF_OID =
            ColumnMetadata.named("clusterRefOid").ofType(UuidPath.UUID_TYPE);

    public static final ColumnMetadata CLUSTER_REF_TARGET_TYPE =
            ColumnMetadata.named("clusterRefTargetType").ofType(Types.OTHER);

    public static final ColumnMetadata CLUSTER_REF_RELATION_ID =
            ColumnMetadata.named("clusterRefRelationId").ofType(Types.INTEGER);

    public static final ColumnMetadata OVERALL_CONFIDENCE =
            ColumnMetadata.named("overallConfidence").ofType(Types.DOUBLE);

    public QOutlierPartition(String variable) {
        super(MOutlierPartition.class, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public final UuidPath clusterRefOid =
            createUuid("clusterRefOid", CLUSTER_REF_OID);

    public final EnumPath<MObjectType> clusterRefTargetType =
            createEnum("clusterRefTargetType", MObjectType.class, CLUSTER_REF_TARGET_TYPE);

    public final NumberPath<Integer> clusterRefRelationId =
            createInteger("clusterRefRelationId", CLUSTER_REF_RELATION_ID);

    public final NumberPath<Double> overallConfidence = createNumber("overallConfidence", Double.class, OVERALL_CONFIDENCE);

}
