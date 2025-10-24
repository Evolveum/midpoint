/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.*;

/**
 * Run of a set of child activities. These can be fixed, semi-fixed or custom.
 *
 * Responsibilities:
 *
 * 1. create and initialize all child runs,
 * 2. execute children, honoring the control flow definition (this is not implemented yet!),
 * 3. derive composite run result from partial (children) run results.
 *
 * Note: Do not extend this class by subclassing unless really necessary.
 *
 * @param <WD> Type of work definition.
 * @param <AH> Type of activity handler.
 * @param <WS> Type of the work state.
 */
public abstract class AbstractCompositeActivityRun<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends LocalActivityRun<WD, AH, WS> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCompositeActivityRun.class);

    /**
     * Final run result. Provided as a class field to keep the history e.g. for debug dumping, etc.
     * (Is that a reason strong enough? Maybe.)
     */
    @NotNull private final ActivityRunResult runResult = new ActivityRunResult();

    public AbstractCompositeActivityRun(ActivityRunInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .statisticsSupported(false)
                .progressSupported(false);
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result)
            throws ActivityRunException, CommonException {

        ensureNotInWorkerTask("This composite activity cannot be run in multiple worker tasks. If you need "
                + "the multi-node capability, use activity tailoring to specify worker tasks for individual sub-activities.");

        // Note that we _allow_ workerThreads setting here. It is ignored by this run
        // (as it is treated in iterative and custom activities only), so it does no harm.
        // It may be even useful as a shortcut to define multithreading in child activities.
        // Those children that don't like it (e.g. report data aggregation) have to check for
        // it themselves.

        activity.initializeChildrenMapIfNeeded();

        logStart();
        List<Activity<?, ?>> children = activity.getChildrenCopyExceptSkipped();

        initializeChildrenState(children, result);
        executeChildren(children, result);
        logEnd();

        return runResult;
    }

    /**
     * We create state before starting the run in order to allow correct progress information reporting.
     * (It needs to know the total number of activity runs at any open level.)
     * An alternative would be to provide the count of runs explicitly.
     *
     * We initialize the state in both full and overview state.
     */
    private void initializeChildrenState(List<Activity<?, ?>> children, OperationResult result)
            throws ActivityRunException {
        for (Activity<?, ?> child : children) {
            child.createRun(taskRun, result)
                    .initializeState(result);
        }
        getTreeStateOverview().recordChildren(this, children, result);
    }

    /** Executes child activities. */
    private void executeChildren(Collection<Activity<?, ?>> children, OperationResult result)
            throws ActivityRunException {

        List<ActivityRunResult> childResults = new ArrayList<>();
        ActivityRunResult unfinishedChildResult = null; // Result of a child that did not finish (error, waiting, interruption).
        for (Activity<?, ?> child : children) {
            @NotNull ActivityRunResult childRunResult = child.getRun().run(result);
            childResults.add(childRunResult);

            if (childRunResult.isSkipActivityError()) {
                continue; // That child was skipped. We should continue with next one.
            }

            if (!childRunResult.isFinished()) {
                unfinishedChildResult = childRunResult;
                break;
            }
        }

        if (unfinishedChildResult == null) {
            // All children finished.
            runResult.setRunResultStatus(canRun() ? FINISHED : INTERRUPTED);
        } else {
            runResult.setRunResultStatus(
                    canRun() ? unfinishedChildResult.getRunResultStatus() : INTERRUPTED,
                    unfinishedChildResult.getThrowable());
        }
        updateOperationResultStatus(childResults);
    }

    private void updateOperationResultStatus(List<ActivityRunResult> childResults) {
        if (runResult.isWaiting() || runResult.isInterrupted()) {
            runResult.setOperationResultStatus(IN_PROGRESS);
            return;
        }

        Set<OperationResultStatus> childStatuses = childResults.stream()
                .map(ActivityRunResult::getOperationResultStatus)
                .collect(Collectors.toSet());

        // Note that we intentionally do not check the _run_result_ being error here.
        // We rely on the fact that in the case of temporary/permanent error the appropriate
        // operation result status should be set as well.
        runResult.setOperationResultStatus(OperationResultUtil.aggregateFinishedResults(childStatuses));

    }

    private void logEnd() {
        LOGGER.trace("After children run ({}):\n{}", runResult.shortDumpLazily(), debugDumpLazily());
    }

    private void logStart() {
        LOGGER.trace("Activity before run:\n{}", activity.debugDumpLazily());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "result=" + runResult +
                '}';
    }

    @Override
    public void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "result", String.valueOf(runResult), indent+1);
    }
}
