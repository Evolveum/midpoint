/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Mapping between {@link QTask} and {@link TaskType}.
 */
public class QTaskMapping
        extends QAssignmentHolderMapping<TaskType, QTask, MTask> {

    public static final String DEFAULT_ALIAS_NAME = "t";

    public static QTaskMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QTaskMapping(repositoryContext);
    }

    private QTaskMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QTask.TABLE_NAME, DEFAULT_ALIAS_NAME,
                TaskType.class, QTask.class, repositoryContext);

        addItemMapping(TaskType.F_TASK_IDENTIFIER, stringMapper(q -> q.taskIdentifier));
        addItemMapping(TaskType.F_BINDING, enumMapper(q -> q.binding));
        addItemMapping(TaskType.F_CATEGORY, stringMapper(q -> q.category));
        addItemMapping(TaskType.F_COMPLETION_TIMESTAMP,
                timestampMapper(q -> q.completionTimestamp));
        addItemMapping(TaskType.F_EXECUTION_STATUS, enumMapper(q -> q.executionStatus));
        addItemMapping(TaskType.F_HANDLER_URI, uriMapper(q -> q.handlerUriId));
        addItemMapping(TaskType.F_LAST_RUN_FINISH_TIMESTAMP,
                timestampMapper(q -> q.lastRunFinishTimestamp));
        addItemMapping(TaskType.F_LAST_RUN_START_TIMESTAMP,
                timestampMapper(q -> q.lastRunStartTimestamp));
        addItemMapping(TaskType.F_NODE, stringMapper(q -> q.node));
        addRefMapping(TaskType.F_OBJECT_REF,
                q -> q.objectRefTargetOid,
                q -> q.objectRefTargetType,
                q -> q.objectRefRelationId,
                QObjectMapping::getObjectMapping);
        addRefMapping(TaskType.F_OWNER_REF,
                q -> q.ownerRefTargetOid,
                q -> q.ownerRefTargetType,
                q -> q.ownerRefRelationId,
                QUserMapping::getUserMapping);
        addItemMapping(TaskType.F_PARENT, stringMapper(q -> q.parent));
        addItemMapping(TaskType.F_RECURRENCE, enumMapper(q -> q.recurrence));
        addItemMapping(TaskType.F_RESULT_STATUS, enumMapper(q -> q.resultStatus));
        addItemMapping(TaskType.F_THREAD_STOP_ACTION, enumMapper(q -> q.threadStopAction));
        addItemMapping(TaskType.F_WAITING_REASON, enumMapper(q -> q.waitingReason));
        // TODO dependentTaskIdentifiers String[] mapping not supported yet
    }

    @Override
    protected QTask newAliasInstance(String alias) {
        return new QTask(alias);
    }

    @Override
    public MTask newRowObject() {
        return new MTask();
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
        row.handlerUriId = processCacheableUri(task.getHandlerUri());
        row.lastRunStartTimestamp = MiscUtil.asInstant(task.getLastRunStartTimestamp());
        row.lastRunFinishTimestamp = MiscUtil.asInstant(task.getLastRunFinishTimestamp());
        row.node = task.getNode();
        setReference(task.getObjectRef(),
                o -> row.objectRefTargetOid = o,
                t -> row.objectRefTargetType = t,
                r -> row.objectRefRelationId = r);
        setReference(task.getOwnerRef(),
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
