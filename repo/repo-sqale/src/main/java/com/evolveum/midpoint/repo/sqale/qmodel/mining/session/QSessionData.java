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

    public static final ColumnMetadata SIMILARITY =
            ColumnMetadata.named("similarityOption").ofType(Types.LONGNVARCHAR);

    public final NumberPath<Long> similarityOption =
            createLong("similarityOption", SIMILARITY);

    public static final ColumnMetadata MIN_CLUSTER_MEMBERS =
            ColumnMetadata.named("minMembersOption").ofType(Types.INTEGER);

    public final NumberPath<Integer> minMembersOption =
            createInteger("minMembersOption", MIN_CLUSTER_MEMBERS);

    public static final ColumnMetadata OVERLAP_OPTION =
            ColumnMetadata.named("overlapOption").ofType(Types.INTEGER);

    public final NumberPath<Integer> overlapOption =
            createInteger("overlapOption", OVERLAP_OPTION);

    public static final ColumnMetadata PROCESSED_OBJECTS =
            ColumnMetadata.named("processedObjectCount").ofType(Types.INTEGER);

    public final NumberPath<Integer> processedObjectCount =
            createInteger("processedObjectCount", PROCESSED_OBJECTS);

    public static final ColumnMetadata MEAN_DENSITY =
            ColumnMetadata.named("clustersMeanDensity").ofType(Types.LONGNVARCHAR);

    public final NumberPath<Long> clustersMeanDensity =
            createLong("clustersMeanDensity", MEAN_DENSITY);

    public static final ColumnMetadata CLUSTER_COUNT =
            ColumnMetadata.named("clusterCount").ofType(Types.INTEGER);

    public final NumberPath<Integer> clusterCount =
            createInteger("clusterCount", CLUSTER_COUNT);

    public static final ColumnMetadata ROLE_ANALYSIS_CLUSTERS_REF =
            ColumnMetadata.named("roleAnalysisClusterRef").ofType(Types.ARRAY);

    public final ArrayPath<String[], String> roleAnalysisClusterRef = createArray("roleAnalysisClusterRef",
            String[].class, ROLE_ANALYSIS_CLUSTERS_REF);

    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR);

    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);

    public static final ColumnMetadata PROCESS_MODE =
            ColumnMetadata.named("processMode").ofType(Types.OTHER);

    public final EnumPath<RoleAnalysisProcessModeType> processMode =
            createEnum("processMode", RoleAnalysisProcessModeType.class, PROCESS_MODE);

    public QSessionData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSessionData(String variable, String schema, String table) {
        super(MSessionObject.class, variable, schema, table);
    }

}
