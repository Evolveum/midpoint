/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.NumericIntervalBucketUtil;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.NumericIntervalBucketUtil.Interval;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Execution for iterative mock activity.
 */
class IterativeMockActivityExecution
        extends PlainIterativeActivityExecution
        <Integer,
                IterativeMockWorkDefinition,
                IterativeMockActivityHandler,
                AbstractActivityWorkStateType> implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeMockActivityExecution.class);

    IterativeMockActivityExecution(
            @NotNull ExecutionInstantiationContext<IterativeMockWorkDefinition, IterativeMockActivityHandler> context) {
        super(context, "Iterative mock activity");
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .defaultDetermineBucketSize(ActivityItemCountingOptionType.ALWAYS)
                .defaultDetermineOverallSize(ActivityOverallItemCountingOptionType.ALWAYS)
                .enableSynchronizationStatistics(true)
                .enableActionsExecutedStatistics(true);
    }

    @Override
    protected boolean hasProgressCommitPoints() {
        return false;
    }

    @Override
    public void iterateOverItemsInBucket(OperationResult result) {
        IterativeMockWorkDefinition workDef = getWorkDefinition();

        Interval narrowed = NumericIntervalBucketUtil.getNarrowedInterval(bucket, workDef.getInterval());

        for (int item = narrowed.from; item < narrowed.to; item++) {
            ItemProcessingRequest<Integer> request = new IterativeMockProcessingRequest(item, this);
            if (!coordinator.submit(request, result)) {
                break;
            }
        }
    }

    @Override
    public @Nullable Integer determineOverallSize(OperationResult result) {
        return getWorkDefinition().getInterval().getSize();
    }

    @Override
    public @Nullable Integer determineCurrentBucketSize(OperationResult result) {
        return NumericIntervalBucketUtil.getNarrowedInterval(
                        bucket,
                        getWorkDefinition().getInterval())
                .getSize();
    }

    @Override
    public boolean processItem(@NotNull ItemProcessingRequest<Integer> request, @NotNull RunningTask workerTask,
            @NotNull OperationResult parentResult) {
        IterativeMockWorkDefinition def = getActivity().getWorkDefinition();

        if (def.getDelay() > 0) {
            MiscUtil.sleepWatchfully(System.currentTimeMillis() + def.getDelay(), 100, workerTask::canRun);
        }

        Integer item = request.getItem();
        String message = emptyIfNull(def.getMessage()) + item;
        LOGGER.info("Message: {}", message);
        getRecorder().recordExecution(message);

        provideSomeMockStatistics(request, workerTask);
        return true;
    }

    private void provideSomeMockStatistics(ItemProcessingRequest<Integer> request, RunningTask workerTask) {
        Integer item = request.getItem();
        String objectName = String.valueOf(item);
        String objectOid = "oid-" + item;
        workerTask.onSynchronizationStart(request.getIdentifier(), objectOid, SynchronizationSituationType.UNLINKED);
        workerTask.onSynchronizationSituationChange(request.getIdentifier(), objectOid, SynchronizationSituationType.LINKED);
        workerTask.recordObjectActionExecuted(objectName, null, UserType.COMPLEX_TYPE, objectOid,
                ChangeType.ADD, null, null);
        workerTask.recordObjectActionExecuted(objectName, null, UserType.COMPLEX_TYPE, objectOid,
                ChangeType.MODIFY, null, null);
    }

    @Override
    @NotNull
    public ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", getRecorder(), indent+1);
        return sb.toString();
    }

    @NotNull
    private MockRecorder getRecorder() {
        return getActivity().getHandler().getRecorder();
    }

    @Override
    public AbstractWorkSegmentationType resolveImplicitSegmentation(@NotNull ImplicitWorkSegmentationType segmentation) {
        return NumericIntervalBucketUtil.resolveImplicitSegmentation(
                segmentation,
                getWorkDefinition().getInterval());
    }
}
