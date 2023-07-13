/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QParentClusterData extends QAssignmentHolder<MParentClusterObject> {

    public static final String TABLE_NAME = "m_role_analysis_session_table";
    public static final ColumnMetadata ROLE_ANALYSIS_CLUSTERS_REF =
            ColumnMetadata.named("roleAnalysisClusterRef").ofType(Types.ARRAY);
    public static final ColumnMetadata OPTIONS =
            ColumnMetadata.named("options").ofType(Types.VARCHAR);
    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR);
    public static final ColumnMetadata ELEMENT_CONSIST =
            ColumnMetadata.named("elementConsist").ofType(Types.INTEGER);
    public static final ColumnMetadata MEAN_DENSITY =
            ColumnMetadata.named("meanDensity").ofType(Types.VARCHAR);
    public static final ColumnMetadata PROCESS_MODE =
            ColumnMetadata.named("processMode").ofType(Types.VARCHAR);

    public final StringPath options = createString("options", OPTIONS);
    public final StringPath meanDensity = createString("meanDensity", MEAN_DENSITY);
    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);
    public final NumberPath<Integer> elementConsist = createInteger("elementConsist", ELEMENT_CONSIST);
    public final StringPath processMode = createString("processMode", PROCESS_MODE);
    public final ArrayPath<String[], String> roleAnalysisClusterRef = createArray("roleAnalysisClusterRef",
            String[].class, ROLE_ANALYSIS_CLUSTERS_REF);

    public QParentClusterData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QParentClusterData(String variable, String schema, String table) {
        super(MParentClusterObject.class, variable, schema, table);
    }

}
