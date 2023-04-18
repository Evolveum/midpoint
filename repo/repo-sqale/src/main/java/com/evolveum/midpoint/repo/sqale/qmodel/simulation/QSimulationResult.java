/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

public class QSimulationResult extends QAssignmentHolder<MSimulationResult> {

    public static final String TABLE_NAME = "m_simulation_result";

    public static final ColumnMetadata PARTITIONED =
            ColumnMetadata.named("partitioned").ofType(Types.BOOLEAN);

    public static final ColumnMetadata ROOT_TASK_REF_TARGET_OID =
            ColumnMetadata.named("rootTaskRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ROOT_TASK_REF_TARGET_TYPE =
            ColumnMetadata.named("rootTaskRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata ROOT_TASK_REF_RELATION_ID =
            ColumnMetadata.named("rootTaskRefRelationId").ofType(Types.INTEGER);

    public static final ColumnMetadata START_TIMESTAMP =
            ColumnMetadata.named("startTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    public static final ColumnMetadata END_TIMESTAMP =
            ColumnMetadata.named("endTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    public QSimulationResult(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSimulationResult(String variable, String schema, String table) {
        super(MSimulationResult.class, variable, schema, table);
    }

    public final BooleanPath partitioned = createBoolean("partitioned", PARTITIONED);

    public final DateTimePath<Instant> startTimestamp = createInstant("startTimestamp", START_TIMESTAMP);
    public final DateTimePath<Instant> endTimestamp = createInstant("endTimestamp", END_TIMESTAMP);

    public final UuidPath rootTaskRefTargetOid =
            createUuid("rootTaskRefTargetOid", ROOT_TASK_REF_TARGET_OID);
    public final EnumPath<MObjectType> rootTaskRefTargetType =
            createEnum("rootTaskRefTargetType", MObjectType.class, ROOT_TASK_REF_TARGET_TYPE);
    public final NumberPath<Integer> rootTaskRefRelationId =
            createInteger("rootTaskRefRelationId", ROOT_TASK_REF_RELATION_ID);

}
