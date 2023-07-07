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

    public static final String TABLE_NAME = "m_parent_cluster_table";

    public static final ColumnMetadata CLUSTERS_REF =
            ColumnMetadata.named("clustersRef").ofType(Types.ARRAY);
    public static final ColumnMetadata CLUSTERS_OPTIONS =
            ColumnMetadata.named("options").ofType(Types.VARCHAR);
    public static final ColumnMetadata IDENTIFIER =
            ColumnMetadata.named("identifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR);
    public static final ColumnMetadata CONSIST =
            ColumnMetadata.named("consist").ofType(Types.INTEGER);
    public static final ColumnMetadata CLUSTER_DENSITY =
            ColumnMetadata.named("density").ofType(Types.VARCHAR);
    public static final ColumnMetadata MODE =
            ColumnMetadata.named("mode").ofType(Types.VARCHAR);

    public final StringPath density = createString("density", CLUSTER_DENSITY);
    public final StringPath identifier = createString("identifier", IDENTIFIER);
    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);
    public final NumberPath<Integer> consist = createInteger("consist", CONSIST);
    public final StringPath mode = createString("mode", MODE);

    public final ArrayPath<String[], String> clustersRef = createArray("clustersRef", String[].class, CLUSTERS_REF);

    public final StringPath options = createString("options", CLUSTERS_OPTIONS);



    public QParentClusterData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QParentClusterData(String variable, String schema, String table) {
        super(MParentClusterObject.class, variable, schema, table);
    }

}
