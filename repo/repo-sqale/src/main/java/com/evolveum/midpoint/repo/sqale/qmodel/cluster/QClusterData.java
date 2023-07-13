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
public class QClusterData extends QAssignmentHolder<MClusterObject> {

    public static final String TABLE_NAME = "m_role_analysis_cluster_table";

    public static final ColumnMetadata ELEMENTS =
            ColumnMetadata.named("elements").ofType(Types.ARRAY);

    public final ArrayPath<String[], String> elements =
            createArray("elements", String[].class, ELEMENTS);

    public static final ColumnMetadata ELEMENTS_COUNT =
            ColumnMetadata.named("elementsCount").ofType(Types.INTEGER);
    public final NumberPath<Integer> elementsCount = createInteger("elementsCount", ELEMENTS_COUNT);

    public static final ColumnMetadata PARENT_REF =
            ColumnMetadata.named("parentRef").ofType(Types.VARCHAR);
    public final StringPath parentRef = createString("parentRef", PARENT_REF);

    public static final ColumnMetadata POINTS_COUNT =
            ColumnMetadata.named("pointsCount").ofType(Types.INTEGER);
    public final NumberPath<Integer> pointsCount = createInteger("pointsCount", POINTS_COUNT);

    public static final ColumnMetadata DEFAULT_DETECTION =
            ColumnMetadata.named("defaultDetection").ofType(Types.ARRAY);
    public final ArrayPath<String[], String> defaultDetection =
            createArray("defaultDetection", String[].class, DEFAULT_DETECTION);

    public static final ColumnMetadata POINTS_MIN_OCCUPATION =
            ColumnMetadata.named("pointsMinOccupation").ofType(Types.INTEGER);
    public final NumberPath<Integer> pointsMinOccupation = createInteger("minOccupation", POINTS_MIN_OCCUPATION);

    public static final ColumnMetadata POINTS_MAX_OCCUPATION =
            ColumnMetadata.named("pointsMaxOccupation").ofType(Types.INTEGER);
    public final NumberPath<Integer> pointsMaxOccupation = createInteger("maxOccupation", POINTS_MAX_OCCUPATION);

    public static final ColumnMetadata POINTS_MEAN =
            ColumnMetadata.named("pointsMean").ofType(Types.VARCHAR);
    public final StringPath pointsMean = createString("pointsMean", POINTS_MEAN);

    public static final ColumnMetadata POINTS_DENSITY =
            ColumnMetadata.named("pointsDensity").ofType(Types.VARCHAR);
    public final StringPath pointsDensity = createString("pointsDensity", POINTS_DENSITY);

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
