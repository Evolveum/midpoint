/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.handlers;

import static com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.NumericIntervalBucketUtil;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.NumericIntervalBucketUtil.Interval;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.stereotype.Component;

/**
 * Just a dummy activity to be used for demonstration and testing purposes.
 *
 * Supersedes `NoOpTaskHandler`.
 */
@Component
public class NoOpActivityHandler implements ActivityHandler<NoOpActivityHandler.MyWorkDefinition, NoOpActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(NoOpActivityHandler.class);

    @Autowired ActivityHandlerRegistry handlerRegistry;

    @PostConstruct
    public void register() {
        handlerRegistry.register(NoOpWorkDefinitionType.COMPLEX_TYPE, TaskConstants.NOOP_TASK_HANDLER_URI,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(NoOpWorkDefinitionType.COMPLEX_TYPE, TaskConstants.NOOP_TASK_HANDLER_URI,
                MyWorkDefinition.class);
    }

    @Override
    public AbstractActivityExecution<MyWorkDefinition, NoOpActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<MyWorkDefinition, NoOpActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyExecution(context);
    }

    private static class MyExecution
            extends PlainIterativeActivityExecution<Integer, MyWorkDefinition, NoOpActivityHandler, AbstractActivityWorkStateType> {

        MyExecution(
                @NotNull ExecutionInstantiationContext<MyWorkDefinition, NoOpActivityHandler> context) {
            super(context, "NoOp");
        }

        @Override
        public ActivityReportingOptions getDefaultReportingOptions() {
            return super.getDefaultReportingOptions()
                    .defaultDetermineBucketSize(ActivityItemCountingOptionType.ALWAYS)
                    .defaultDetermineOverallSize(ActivityOverallItemCountingOptionType.ALWAYS);
        }

        @Override
        public void beforeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
            MyWorkDefinition def = getWorkDefinition();
            LOGGER.info("Execution starting; steps to be executed = {}, delay for one step = {}, step interruptibility = {}"
                            + " in task {}", def.steps, def.delay, def.stepInterruptibility, getRunningTask());
        }

        @Override
        public @Nullable Integer determineOverallSize(OperationResult result) throws CommonException {
            return getWorkDefinition().getInterval().getSize();
        }

        @Override
        public @Nullable Integer determineCurrentBucketSize(OperationResult result) throws CommonException {
            return NumericIntervalBucketUtil.getNarrowedInterval(
                            bucket,
                            getWorkDefinition().getInterval())
                    .getSize();
        }

        @Override
        public void iterateOverItemsInBucket(OperationResult result) {
            Interval narrowed = NumericIntervalBucketUtil.getNarrowedInterval(bucket, getWorkDefinition().getInterval());

            for (int step = narrowed.from; step < narrowed.to; step++) {
                ItemProcessingRequest<Integer> request = new GenericProcessingRequest<>(step, step, this);
                if (!coordinator.submit(request, result)) {
                    break;
                }
            }
        }

        @Override
        public void afterExecution(OperationResult result) throws CommonException, ActivityExecutionException {
            LOGGER.info("Execution stopping; canRun = {}", canRun());
        }

        @Override
        public boolean processItem(@NotNull ItemProcessingRequest<Integer> request, @NotNull RunningTask workerTask,
                @NotNull OperationResult result) {
            MyWorkDefinition def = getWorkDefinition();
            Interval interval = NumericIntervalBucketUtil.getNarrowedInterval(bucket, def.getInterval());

            LOGGER.info("Executing step #{} (numbered from zero) of {} in bucket [{}-{}) in task {}",
                    request.getItem(), def.steps, interval.from, interval.to, getRunningTask());

            sleep(def);
            return true;
        }

        private void sleep(MyWorkDefinition def) {
            switch (def.stepInterruptibility) {
                case FULL:
                    MiscUtil.sleepWatchfully(System.currentTimeMillis() + def.delay, 100, this::canRun);
                    return;
                case HARD:
                    MiscUtil.sleepCatchingInterruptedException(def.delay);
                    return;
                case NONE:
                    MiscUtil.sleepNonInterruptibly(def.delay);
                    return;
                default:
                    throw new AssertionError(def.stepInterruptibility);
            }
        }

        @Override
        @NotNull
        public ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
            return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
        }

        @Override
        public AbstractWorkSegmentationType resolveImplicitSegmentation(@NotNull ImplicitWorkSegmentationType segmentation) {
            return NumericIntervalBucketUtil.resolveImplicitSegmentation(
                    segmentation,
                    getWorkDefinition().getInterval());
        }
    }

    protected static class MyWorkDefinition extends AbstractWorkDefinition {

        private final long delay;
        private final int steps;
        @NotNull private final NoOpActivityStepInterruptibilityType stepInterruptibility;

        MyWorkDefinition(WorkDefinitionSource source) {
            Integer rawSteps;
            Integer rawDelay;
            NoOpActivityStepInterruptibilityType rawStepInterruptibility;
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
                rawSteps = legacy.getExtensionItemRealValue(SchemaConstants.NOOP_STEPS_QNAME, Integer.class);
                rawDelay = legacy.getExtensionItemRealValue(SchemaConstants.NOOP_DELAY_QNAME, Integer.class);
                rawStepInterruptibility = null;
            } else {
                NoOpWorkDefinitionType bean = (NoOpWorkDefinitionType) ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
                rawSteps = bean.getSteps();
                rawDelay = bean.getDelay();
                rawStepInterruptibility = bean.getStepInterruptibility();
            }
            delay = MoreObjects.firstNonNull(rawDelay, 0);
            steps = MoreObjects.firstNonNull(rawSteps, 1);
            stepInterruptibility = MoreObjects.firstNonNull(rawStepInterruptibility, NoOpActivityStepInterruptibilityType.NONE);
        }

        private Interval getInterval() {
            return Interval.of(0, steps);
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "delay", delay, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "steps", steps, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "stepInterruptibility", stepInterruptibility, indent + 1);
        }
    }
}
