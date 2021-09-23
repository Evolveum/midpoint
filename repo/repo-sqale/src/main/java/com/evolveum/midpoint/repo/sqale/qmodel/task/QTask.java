/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QTask extends QAssignmentHolder<MTask> {

    private static final long serialVersionUID = 6249403929032616177L;

    public static final String TABLE_NAME = "m_task";

    public static final ColumnMetadata TASK_IDENTIFIER =
            ColumnMetadata.named("taskIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata BINDING =
            ColumnMetadata.named("binding").ofType(Types.OTHER);
    public static final ColumnMetadata CATEGORY =
            ColumnMetadata.named("category").ofType(Types.VARCHAR);
    public static final ColumnMetadata COMPLETION_TIMESTAMP =
            ColumnMetadata.named("completionTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata EXECUTION_STATE =
            ColumnMetadata.named("executionState").ofType(Types.OTHER);
    public static final ColumnMetadata FULL_RESULT =
            ColumnMetadata.named("fullResult").ofType(Types.BINARY);
    public static final ColumnMetadata RESULT_STATUS =
            ColumnMetadata.named("resultStatus").ofType(Types.OTHER);
    public static final ColumnMetadata HANDLER_URI_ID =
            ColumnMetadata.named("handlerUriId").ofType(Types.INTEGER);
    public static final ColumnMetadata LAST_RUN_START_TIMESTAMP =
            ColumnMetadata.named("lastRunStartTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata LAST_RUN_FINISH_TIMESTAMP =
            ColumnMetadata.named("lastRunFinishTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata NODE =
            ColumnMetadata.named("node").ofType(Types.VARCHAR);
    public static final ColumnMetadata OBJECT_REF_TARGET_OID =
            ColumnMetadata.named("objectRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata OBJECT_REF_TARGET_TYPE =
            ColumnMetadata.named("objectRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata OBJECT_REF_RELATION_ID =
            ColumnMetadata.named("objectRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata OWNER_REF_TARGET_OID =
            ColumnMetadata.named("ownerRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata OWNER_REF_TARGET_TYPE =
            ColumnMetadata.named("ownerRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata OWNER_REF_RELATION_ID =
            ColumnMetadata.named("ownerRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata PARENT =
            ColumnMetadata.named("parent").ofType(Types.VARCHAR);
    public static final ColumnMetadata RECURRENCE =
            ColumnMetadata.named("recurrence").ofType(Types.OTHER);
    public static final ColumnMetadata SCHEDULING_STATE =
            ColumnMetadata.named("schedulingState").ofType(Types.OTHER);
    public static final ColumnMetadata AUTO_SCALING_MODE =
            ColumnMetadata.named("autoScalingMode").ofType(Types.OTHER);
    public static final ColumnMetadata THREAD_STOP_ACTION =
            ColumnMetadata.named("threadStopAction").ofType(Types.OTHER);
    public static final ColumnMetadata WAITING_REASON =
            ColumnMetadata.named("waitingReason").ofType(Types.OTHER);
    public static final ColumnMetadata DEPENDENT_TASK_IDENTIFIERS =
            ColumnMetadata.named("dependentTaskIdentifiers").ofType(Types.ARRAY);

    // columns and relations
    public final StringPath taskIdentifier = createString("taskIdentifier", TASK_IDENTIFIER);
    public final EnumPath<TaskBindingType> binding =
            createEnum("binding", TaskBindingType.class, BINDING);
    public final StringPath category = createString("category", CATEGORY);
    public final DateTimePath<Instant> completionTimestamp =
            createInstant("completionTimestamp", COMPLETION_TIMESTAMP);
    public final EnumPath<TaskExecutionStateType> executionState =
            createEnum("executionState", TaskExecutionStateType.class, EXECUTION_STATE);
    public final ArrayPath<byte[], Byte> fullResult = createByteArray("fullResult", FULL_RESULT);
    public final EnumPath<OperationResultStatusType> resultStatus =
            createEnum("resultStatus", OperationResultStatusType.class, RESULT_STATUS);
    public final NumberPath<Integer> handlerUriId = createInteger("handlerUriId", HANDLER_URI_ID);
    public final DateTimePath<Instant> lastRunStartTimestamp =
            createInstant("lastRunStartTimestamp", LAST_RUN_START_TIMESTAMP);
    public final DateTimePath<Instant> lastRunFinishTimestamp =
            createInstant("lastRunFinishTimestamp", LAST_RUN_FINISH_TIMESTAMP);
    public final StringPath node = createString("node", NODE);
    public final UuidPath objectRefTargetOid =
            createUuid("objectRefTargetOid", OBJECT_REF_TARGET_OID);
    public final EnumPath<MObjectType> objectRefTargetType =
            createEnum("objectRefTargetType", MObjectType.class, OBJECT_REF_TARGET_TYPE);
    public final NumberPath<Integer> objectRefRelationId =
            createInteger("objectRefRelationId", OBJECT_REF_RELATION_ID);
    public final UuidPath ownerRefTargetOid = createUuid("ownerRefTargetOid", OWNER_REF_TARGET_OID);
    public final EnumPath<MObjectType> ownerRefTargetType =
            createEnum("ownerRefTargetType", MObjectType.class, OWNER_REF_TARGET_TYPE);
    public final NumberPath<Integer> ownerRefRelationId =
            createInteger("ownerRefRelationId", OWNER_REF_RELATION_ID);
    public final StringPath parent = createString("parent", PARENT);
    public final EnumPath<TaskRecurrenceType> recurrence =
            createEnum("recurrence", TaskRecurrenceType.class, RECURRENCE);
    public final EnumPath<TaskSchedulingStateType> schedulingState =
            createEnum("schedulingState", TaskSchedulingStateType.class, SCHEDULING_STATE);
    public final EnumPath<TaskAutoScalingModeType> autoScalingMode =
            createEnum("autoScalingMode", TaskAutoScalingModeType.class, AUTO_SCALING_MODE);
    public final EnumPath<ThreadStopActionType> threadStopAction =
            createEnum("threadStopAction", ThreadStopActionType.class, THREAD_STOP_ACTION);
    public final EnumPath<TaskWaitingReasonType> waitingReason =
            createEnum("waitingReason", TaskWaitingReasonType.class, WAITING_REASON);
    public final ArrayPath<String[], String> dependentTaskIdentifiers =
            createArray("dependentTaskIdentifiers", String[].class, DEPENDENT_TASK_IDENTIFIERS);

    public QTask(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QTask(String variable, String schema, String table) {
        super(MTask.class, variable, schema, table);
    }
}
