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
            TaskType task, JdbcSession jdbcSession) {
        MTask row = super.toRowObjectWithoutFullObject(task, jdbcSession);

        row.taskIdentifier = task.getTaskIdentifier();
        row.binding = task.getBinding();
        row.category = task.getCategory();
        row.completionTimestamp = MiscUtil.asInstant(task.getCompletionTimestamp());
        row.executionStatus = task.getExecutionStatus();
//        row.fullResult = TODO
        row.handlerUriId = processCacheableUri(task.getHandlerUri(), jdbcSession);
        row.lastRunStartTimestamp = MiscUtil.asInstant(task.getLastRunStartTimestamp());
        row.lastRunFinishTimestamp = MiscUtil.asInstant(task.getLastRunFinishTimestamp());
        row.node = task.getNode();
        setReference(task.getObjectRef(), jdbcSession,
                o -> row.objectRefTargetOid = o,
                t -> row.objectRefTargetType = t,
                r -> row.objectRefRelationId = r);
        setReference(task.getOwnerRef(), jdbcSession,
                o -> row.ownerRefTargetOid = o,
                t -> row.ownerRefTargetType = t,
                r -> row.ownerRefRelationId = r);
        row.parent = task.getParent();
        row.recurrence = task.getRecurrence();
        row.resultStatus = task.getResultStatus();
        row.threadStopAction = task.getThreadStopAction();
        row.waitingReason = task.getWaitingReason();
        row.dependentTaskIdentifiers = task.getDependent().toArray(String[]::new);

        return row;
    }
}
