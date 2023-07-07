/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import java.sql.Types;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QClusterData extends QAssignmentHolder<MClusterObject> {

    public static final String TABLE_NAME = "m_cluster_table";

    public static final ColumnMetadata PARENT_REF =
            ColumnMetadata.named("parentRef").ofType(Types.VARCHAR);

    public static final ColumnMetadata IDENTIFIER =
            ColumnMetadata.named("identifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR);
    public static final ColumnMetadata CLUSTER_MEAN =
            ColumnMetadata.named("mean").ofType(Types.VARCHAR);
    public static final ColumnMetadata CLUSTER_DENSITY =
            ColumnMetadata.named("density").ofType(Types.VARCHAR);
    public static final ColumnMetadata MIN_OCCUPATION =
            ColumnMetadata.named("minOccupation").ofType(Types.INTEGER);
    public static final ColumnMetadata MAX_OCCUPATION =
            ColumnMetadata.named("maxOccupation").ofType(Types.INTEGER);
    public static final ColumnMetadata POINTS =
            ColumnMetadata.named("points").ofType(Types.ARRAY);
    public static final ColumnMetadata POINT_COUNT =
            ColumnMetadata.named("pointCount").ofType(Types.INTEGER);

    public static final ColumnMetadata ELEMENTS =
            ColumnMetadata.named("elements").ofType(Types.ARRAY);
    public static final ColumnMetadata ELEMENT_COUNT =
            ColumnMetadata.named("elementCount").ofType(Types.INTEGER);

    public static final ColumnMetadata DEFAULT_DETECTION =
            ColumnMetadata.named("defaultDetection").ofType(Types.ARRAY);

    public final ArrayPath<String[], String> elements =
            createArray("elements", String[].class, ELEMENTS);

    public final ArrayPath<String[], String> points =
            createArray("points", String[].class, POINTS);

    public final ArrayPath<String[], String> defaultDetection =
            createArray("defaultDetection", String[].class, DEFAULT_DETECTION);

    public final StringPath parentRef = createString("parentRef", PARENT_REF);
    public final StringPath identifier = createString("identifier", IDENTIFIER);
    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);
    public final NumberPath<Integer> maxOccupation = createInteger("maxOccupation", MAX_OCCUPATION);
    public final NumberPath<Integer> minOccupation = createInteger("minOccupation", MIN_OCCUPATION);
    public final StringPath density = createString("density", CLUSTER_DENSITY);
    public final StringPath mean = createString("mean", CLUSTER_MEAN);

    public final NumberPath<Integer> pointCount = createInteger("pointCount", POINT_COUNT);
    public final NumberPath<Integer> elementCount = createInteger("elementCount", ELEMENT_COUNT);

    public QClusterData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QClusterData(String variable, String schema, String table) {
        super(MClusterObject.class, variable, schema, table);
    }

}
