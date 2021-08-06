/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import com.evolveum.midpoint.prism.PrismContext;
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

import java.math.BigInteger;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * TODO
 */
class IterativeMockActivityExecutionSpecifics
        extends BasePlainIterativeExecutionSpecificsImpl<
                        Integer,
                        IterativeMockWorkDefinition,
                        IterativeMockActivityHandler> implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeMockActivityExecutionSpecifics.class);

    IterativeMockActivityExecutionSpecifics(@NotNull PlainIterativeActivityExecution<Integer, IterativeMockWorkDefinition,
            IterativeMockActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .enableSynchronizationStatistics(true)
                .enableActionsExecutedStatistics(true);
    }

    @Override
    public void iterateOverItemsInBucket(@NotNull WorkBucketType bucket, OperationResult result) {
        IterativeMockWorkDefinition workDef = activityExecution.getActivity().getWorkDefinition();

        AbstractWorkBucketContentType content = bucket.getContent();
        int from, to;
        if (content instanceof NullWorkBucketContentType) {
            from = workDef.getFrom();
            to = workDef.getTo();
        } else if (content instanceof NumericIntervalWorkBucketContentType) {
            from = ((NumericIntervalWorkBucketContentType) content).getFrom().intValue();
            to = ((NumericIntervalWorkBucketContentType) content).getTo().intValue() - 1;
        } else {
            throw new IllegalStateException("Unexpected bucket content: " + content);
        }

        for (int item = from; item <= to; item++) {
            ItemProcessingRequest<Integer> request = new IterativeMockProcessingRequest(item, activityExecution);
            activityExecution.getCoordinator().submit(request, result);
        }
    }

    @Override
    public boolean processItem(ItemProcessingRequest<Integer> request, RunningTask workerTask, OperationResult parentResult) {
        Integer item = request.getItem();
        String message = getActivity().getWorkDefinition().getMessage() + item;
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
    public AbstractWorkSegmentationType resolveConfiguration(@NotNull ImplicitWorkSegmentationType configuration) {
        argCheck(configuration.getMatchingRule() == null, "Explicit matching rules are not supported");
        argCheck(configuration.getDiscriminator() == null, "Explicit discriminator specification is not supported");
        argCheck(configuration.getNumberOfBuckets() != null, "Number of buckets must be specified");

        IterativeMockWorkDefinition workDef = activityExecution.getActivity().getWorkDefinition();

        return new NumericWorkSegmentationType(PrismContext.get())
                .from(BigInteger.valueOf(workDef.getFrom()))
                .to(BigInteger.valueOf(workDef.getTo() + 1))
                .numberOfBuckets(configuration.getNumberOfBuckets());
    }
}
