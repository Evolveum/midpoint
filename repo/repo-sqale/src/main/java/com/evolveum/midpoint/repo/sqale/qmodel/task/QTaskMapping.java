/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.ArrayPath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.SinglePathItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAutoScalingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Mapping between {@link QTask} and {@link TaskType}.
 */
public class QTaskMapping
        extends QAssignmentHolderMapping<TaskType, QTask, MTask> {

    public static final String DEFAULT_ALIAS_NAME = "t";

    private static QTaskMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QTaskMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QTaskMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QTaskMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QTaskMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QTask.TABLE_NAME, DEFAULT_ALIAS_NAME,
                TaskType.class, QTask.class, repositoryContext);

        addItemMapping(F_TASK_IDENTIFIER, stringMapper(q -> q.taskIdentifier));
        addItemMapping(F_BINDING, enumMapper(q -> q.binding));
        addItemMapping(F_CATEGORY, stringMapper(q -> q.category));
        addItemMapping(F_COMPLETION_TIMESTAMP,
                timestampMapper(q -> q.completionTimestamp));
        addItemMapping(F_EXECUTION_STATE, enumMapper(q -> q.executionState));
        addItemMapping(F_RESULT, new SqaleItemSqlMapper<>(
                ctx -> new FullResultDeltaProcessor(ctx, q -> q.fullResult)));
        addItemMapping(F_RESULT_STATUS, enumMapper(q -> q.resultStatus));
        addItemMapping(F_HANDLER_URI, uriMapper(q -> q.handlerUriId));
        addItemMapping(F_LAST_RUN_FINISH_TIMESTAMP,
                timestampMapper(q -> q.lastRunFinishTimestamp));
        addItemMapping(F_LAST_RUN_START_TIMESTAMP,
                timestampMapper(q -> q.lastRunStartTimestamp));
        addItemMapping(F_NODE, stringMapper(q -> q.node));
        addRefMapping(F_OBJECT_REF,
                q -> q.objectRefTargetOid,
                q -> q.objectRefTargetType,
                q -> q.objectRefRelationId,
                QObjectMapping::getObjectMapping);
        addRefMapping(F_OWNER_REF,
                q -> q.ownerRefTargetOid,
                q -> q.ownerRefTargetType,
                q -> q.ownerRefRelationId,
                QUserMapping::getUserMapping);
        addItemMapping(F_PARENT, stringMapper(q -> q.parent));
        addItemMapping(F_SCHEDULING_STATE, enumMapper(q -> q.schedulingState));
        addNestedMapping(F_AUTO_SCALING, TaskAutoScalingType.class)
                .addItemMapping(TaskAutoScalingType.F_MODE, enumMapper(q -> q.autoScalingMode));
        addItemMapping(F_THREAD_STOP_ACTION, enumMapper(q -> q.threadStopAction));
        addItemMapping(F_WAITING_REASON, enumMapper(q -> q.waitingReason));
        addItemMapping(F_DEPENDENT, multiStringMapper(q -> q.dependentTaskIdentifiers));

        addNestedMapping(F_SCHEDULE, ScheduleType.class)
                .addItemMapping(F_RECURRENCE, enumMapper(q -> q.recurrence));
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            QTask entity, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (SelectorOptions.hasToLoadPath(F_RESULT, options)) {
            return new Path[] { entity.oid, entity.fullObject, entity.fullResult };
        }
        return new Path[] { entity.oid, entity.fullObject };
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
    protected Collection<? extends QName> fullObjectItemsToSkip() {
        return Collections.singletonList(F_RESULT);
    }

    @Override
    public @NotNull MTask toRowObjectWithoutFullObject(
            TaskType task, JdbcSession jdbcSession) {
        MTask row = super.toRowObjectWithoutFullObject(task, jdbcSession);

        row.taskIdentifier = task.getTaskIdentifier();
        row.binding = task.getBinding();
        row.category = task.getCategory();
        row.completionTimestamp = MiscUtil.asInstant(task.getCompletionTimestamp());
        row.executionState = task.getExecutionState();

        // Logically resultStatus is task.getResult().getStatus(), but repo is NOT responsible for
        // this synchronization - Task manager is - for repo these are two separate attributes.
        OperationResultType operationResult = task.getResult();
        if (operationResult != null) {
            row.fullResult = repositoryContext().createFullResult(operationResult);
        }
        row.resultStatus = task.getResultStatus();

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
        // Using effective recurrence instead of specified one might be questionable, but
        // it's needed to reasonably use filtering based on recurrence.
        // (Otherwise the null value of recurrence is really ambiguous.)
        row.recurrence = TaskTypeUtil.getEffectiveRecurrence(task);
        row.schedulingState = task.getSchedulingState();
        TaskAutoScalingType autoScaling = task.getAutoScaling();
        if (autoScaling != null) {
            row.autoScalingMode = autoScaling.getMode();
        }
        row.threadStopAction = task.getThreadStopAction();
        row.waitingReason = task.getWaitingReason();
        row.dependentTaskIdentifiers = stringsToArray(task.getDependent());

        return row;
    }

    @Override
    public TaskType toSchemaObject(@NotNull Tuple row,
            @NotNull QTask entityPath,
            @NotNull JdbcSession jdbcSession,
            Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException {
        TaskType task = super.toSchemaObject(row, entityPath, jdbcSession, options);
        // We need to check options too for proper setting of incompleteness.
        byte[] fullResult = row.get(entityPath.fullResult);
        if (fullResult != null) {
            PrismObject<TaskType> taskPrismObject = task.asPrismObject();
            PrismProperty<OperationResultType> resultProperty =
                    taskPrismObject.findOrCreateProperty(TaskType.F_RESULT);
            resultProperty.setRealValue(
                    parseSchemaObject(fullResult, "opResult", OperationResultType.class));
            resultProperty.setIncomplete(false);
        } else if (SelectorOptions.hasToLoadPath(F_RESULT, options)) {
            PrismUtil.setPropertyNullAndComplete(task.asPrismObject(), TaskType.F_RESULT);
        }
        return task;
    }

    // Specific to Task, so we leave it as nested class right here.
    public static class FullResultDeltaProcessor
            extends SinglePathItemDeltaProcessor<byte[], ArrayPath<byte[], Byte>> {

        public <Q extends FlexibleRelationalPathBase<R>, R> FullResultDeltaProcessor(
                SqaleUpdateContext<?, Q, R> context,
                Function<Q, ArrayPath<byte[], Byte>> rootToQueryItem) {
            super(context, rootToQueryItem);
        }

        @Override
        public byte[] convertRealValue(Object realValue) {
            return context.repositoryContext().createFullResult((OperationResultType) realValue);
        }
    }
}
