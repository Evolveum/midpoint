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
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
public class QClusterObject extends QAssignmentHolder<MClusterObject> {

    public static final String TABLE_NAME = "m_role_analysis_cluster";

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

    public QClusterObject(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QClusterObject(String variable, String schema, String table) {
        super(MClusterObject.class, variable, schema, table);
    }

}
