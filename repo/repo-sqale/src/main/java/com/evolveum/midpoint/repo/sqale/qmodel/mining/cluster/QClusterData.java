/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster;

import java.sql.Types;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
public class QClusterData extends QAssignmentHolder<MClusterObject> {

    public static final String TABLE_NAME = "m_role_analysis_cluster_table";

    public static final ColumnMetadata USERS_COUNT =
            ColumnMetadata.named("usersCount").ofType(Types.INTEGER);

    public final NumberPath<Integer> usersCount =
            createInteger("usersCount", USERS_COUNT);

    public static final ColumnMetadata ROLES_COUNT =
            ColumnMetadata.named("rolesCount").ofType(Types.INTEGER);

    public final NumberPath<Integer> rolesCount =
            createInteger("rolesCount", ROLES_COUNT);

    public static final ColumnMetadata DENSITY =
            ColumnMetadata.named("membershipDensity").ofType(Types.LONGNVARCHAR);

    public final NumberPath<Long> membershipDensity =
            createLong("membershipDensity", DENSITY);

    public static final ColumnMetadata MEAN =
            ColumnMetadata.named("membershipMean").ofType(Types.LONGNVARCHAR);

    public final NumberPath<Long> membershipMean =
            createLong("membershipMean", MEAN);

    public static final ColumnMetadata DETECTED_REDUCTION_PATTERN =
            ColumnMetadata.named("detectedReductionMetric").ofType(Types.LONGNVARCHAR);

    public final NumberPath<Long> detectedReductionMetric =
            createLong("detectedReductionMetric", DETECTED_REDUCTION_PATTERN);

    public static final ColumnMetadata PARENT_REF_TARGET_OID =
            ColumnMetadata.named("parentRefTargetOid").ofType(UuidPath.UUID_TYPE);

    public final UuidPath parentRefTargetOid =
            createUuid("parentRefTargetOid", PARENT_REF_TARGET_OID);

    public static final ColumnMetadata PARENT_REF_TARGET_TYPE =
            ColumnMetadata.named("parentRefTargetType").ofType(Types.OTHER);

    public final EnumPath<MObjectType> parentRefTargetType =
            createEnum("parentRefTargetType", MObjectType.class, PARENT_REF_TARGET_TYPE);

    public static final ColumnMetadata PARENT_REF_TARGET_RELATION =
            ColumnMetadata.named("parentRefRelationId").ofType(Types.INTEGER);

    public final NumberPath<Integer> parentRefRelationId =
            createInteger("parentRefRelationId", PARENT_REF_TARGET_RELATION);

    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR);
    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);

    public QClusterData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QClusterData(String variable, String schema, String table) {
        super(MClusterObject.class, variable, schema, table);
    }

}
