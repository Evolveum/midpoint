/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content.NumericIntervalBucketUtil;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content.NumericIntervalBucketUtil.Interval;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Execution for iterative mock activity.
 */
final class IterativeMockActivityRun
        extends PlainIterativeActivityRun
        <Integer,
                IterativeMockWorkDefinition,
                IterativeMockActivityHandler,
                AbstractActivityWorkStateType> implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeMockActivityRun.class);

    IterativeMockActivityRun(
            @NotNull ActivityRunInstantiationContext<IterativeMockWorkDefinition, IterativeMockActivityHandler> context) {
        super(context, "Iterative mock activity");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .determineBucketSizeDefault(ActivityItemCountingOptionType.ALWAYS)
                .determineOverallSizeDefault(ActivityOverallItemCountingOptionType.ALWAYS)
                .synchronizationStatisticsSupported(true)
                .actionsExecutedStatisticsSupported(true)
                .progressCommitPointsSupported(false);
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
    public Integer determineOverallSize(OperationResult result) {
        return getWorkDefinition().getInterval().getSize();
    }

    @Override
    public Integer determineCurrentBucketSize(OperationResult result) {
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
