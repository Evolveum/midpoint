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

    public static final ColumnMetadata IDENTIFIER =
            ColumnMetadata.named("identifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR);
    public static final ColumnMetadata ROLES_COUNT =
            ColumnMetadata.named("rolesCount").ofType(Types.INTEGER);
    public static final ColumnMetadata MEMBERS_COUNT =
            ColumnMetadata.named("membersCount").ofType(Types.INTEGER);
    public static final ColumnMetadata SIMILAR_GROUPS_COUNT =
            ColumnMetadata.named("similarGroupsCount").ofType(Types.INTEGER);

    public final StringPath identifier = createString("identifier", IDENTIFIER);
    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);
    public final NumberPath<Integer> rolesCount = createInteger("rolesCount", ROLES_COUNT);
    public final NumberPath<Integer> membersCount = createInteger("membersCount", MEMBERS_COUNT);
    public final NumberPath<Integer> similarGroupsCount = createInteger("similarGroupsCount", SIMILAR_GROUPS_COUNT);

    public static final ColumnMetadata CLUSTER_MEAN =
            ColumnMetadata.named("mean").ofType(Types.VARCHAR);
    public final StringPath mean = createString("mean", CLUSTER_MEAN);

    public static final ColumnMetadata CLUSTER_DENSITY =
            ColumnMetadata.named("density").ofType(Types.VARCHAR);
    public final StringPath density = createString("density", CLUSTER_DENSITY);


    public static final ColumnMetadata MIN_OCCUPATION =
            ColumnMetadata.named("minOccupation").ofType(Types.INTEGER);
    public final NumberPath<Integer> minOccupation = createInteger("minOccupation", MIN_OCCUPATION);

    public static final ColumnMetadata MAX_OCCUPATION =
            ColumnMetadata.named("maxOccupation").ofType(Types.INTEGER);
    public final NumberPath<Integer> maxOccupation = createInteger("maxOccupation", MAX_OCCUPATION);

    public static final ColumnMetadata ROLES =
            ColumnMetadata.named("roles").ofType(Types.ARRAY);

    public final ArrayPath<String[], String> roles =
            createArray("roles", String[].class, ROLES);

    public static final ColumnMetadata MEMBERS =
            ColumnMetadata.named("members").ofType(Types.ARRAY);

    public final ArrayPath<String[], String> members =
            createArray("members", String[].class, MEMBERS);

    public static final ColumnMetadata SIMILAR_GROUP =
            ColumnMetadata.named("similarGroups").ofType(Types.ARRAY);

    public final ArrayPath<String[], String> similarGroups =
            createArray("similarGroups", String[].class, SIMILAR_GROUP);

    public QClusterData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QClusterData(String variable, String schema, String table) {
        super(MClusterObject.class, variable, schema, table);
    }

}
