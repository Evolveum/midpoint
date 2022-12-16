/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

/**
 * Execution of mock opening or closing activity.
 */
public abstract class MockComponentActivityRun
        extends LocalActivityRun<CompositeMockWorkDefinition, CompositeMockActivityHandler, AbstractActivityWorkStateType> {

    public static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/repo-common-test/extension";

    private static final Trace LOGGER = TraceManager.getTrace(MockComponentActivityRun.class);

    MockComponentActivityRun(
            @NotNull ActivityRunInstantiationContext<CompositeMockWorkDefinition, CompositeMockActivityHandler> context) {
        super(context);
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .progressCommitPointsSupported(false);
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

        CompositeMockWorkDefinition workDef = activity.getWorkDefinition();

        int steps = workDef.getSteps();
        long delay = workDef.getDelay();

        LOGGER.info("Mock activity starting: id={}, steps={}, delay={}, sub-activity={}:\n{}", workDef.getMessage(),
                steps, delay, getMockSubActivity(), debugDumpLazily());

        String itemName = workDef.getMessage() + ":" + getMockSubActivity();

        Operation operation = activityState.getLiveItemProcessingStatistics()
                .recordOperationStart(new IterativeOperationStartInfo(
                        new IterationItemInformation(itemName, null, null, null)));
        RunningTask task = taskRun.getRunningTask();

        if (delay > 0) {
            LOGGER.trace("Sleeping for {} msecs", delay);
            MiscUtil.sleepWatchfully(System.currentTimeMillis() + delay, 100, task::canRun);
        }

        result.recordSuccess();

        getRecorder().recordExecution(itemName);

        QualifiedItemProcessingOutcomeType qualifiedOutcome =
                new QualifiedItemProcessingOutcomeType()
                        .outcome(ItemProcessingOutcomeType.SUCCESS);
        operation.done(qualifiedOutcome, null);
        incrementProgress(qualifiedOutcome);

        LOGGER.info("Mock activity finished: id={}, sub-activity={}:\n{}", workDef.getMessage(), getMockSubActivity(),
                debugDumpLazily());

        return standardRunResult();
    }

    @NotNull
    private MockRecorder getRecorder() {
        return activity.getHandler().getRecorder();
    }

    abstract String getMockSubActivity();

    @Override
    public void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", getRecorder(), indent+1);
    }
}
