/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class TaskSqlTransformer extends ObjectSqlTransformer<TaskType, QTask, MTask> {

    public TaskSqlTransformer(SqlTransformerSupport transformerSupport, QTaskMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MTask toRowObjectWithoutFullObject(
            TaskType schemaObject, JdbcSession jdbcSession) {
        MTask row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.taskIdentifier = schemaObject.getTaskIdentifier();
        row.binding = schemaObject.getBinding();
        row.category = schemaObject.getCategory();
        row.completionTimestamp = MiscUtil.asInstant(schemaObject.getCompletionTimestamp());
        row.executionStatus = schemaObject.getExecutionStatus();
//        row.fullResult = TODO
        row.handlerUriId = processCacheableUri(schemaObject.getHandlerUri(), jdbcSession);
        row.lastRunStartTimestamp = MiscUtil.asInstant(schemaObject.getLastRunStartTimestamp());
        row.lastRunFinishTimestamp = MiscUtil.asInstant(schemaObject.getLastRunFinishTimestamp());
        row.node = schemaObject.getNode();
        setReference(schemaObject.getObjectRef(), jdbcSession,
                o -> row.objectRefTargetOid = o,
                t -> row.objectRefTargetType = t,
                r -> row.objectRefRelationId = r);
        setReference(schemaObject.getOwnerRef(), jdbcSession,
                o -> row.ownerRefTargetOid = o,
                t -> row.ownerRefTargetType = t,
                r -> row.ownerRefRelationId = r);
        row.parent = schemaObject.getParent();
        row.recurrence = schemaObject.getRecurrence();
        row.resultStatus = schemaObject.getResultStatus();
        row.threadStopAction = schemaObject.getThreadStopAction();
        row.waitingReason = schemaObject.getWaitingReason();
        row.dependentTaskIdentifiers = schemaObject.getDependent().toArray(String[]::new);

        return row;
    }
}
