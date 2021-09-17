/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
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
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.*;

/**
 * Execution of a set of child activities. These can be fixed, semi-fixed or custom.
 *
 * Responsibilities:
 *
 * 1. create and initialize all child executions,
 * 2. execute children, honoring the control flow definition (this is not implemented yet!),
 * 3. derive composite execution result from partial (children) execution results.
 *
 * Note: Do not extend this class by subclassing unless really necessary.
 *
 * @param <WD> Type of work definition.
 * @param <AH> Type of activity handler.
 * @param <WS> Type of the work state.
 */
public class CompositeActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends LocalActivityExecution<WD, AH, WS> {

    private static final Trace LOGGER = TraceManager.getTrace(CompositeActivityExecution.class);

    /**
     * Final execution result. Provided as a class field to keep the history e.g. for debug dumping, etc.
     * (Is that a reason strong enough? Maybe.)
     */
    @NotNull private final ActivityExecutionResult executionResult = new ActivityExecutionResult();

    public CompositeActivityExecution(ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws ActivityExecutionException, CommonException {

        activity.initializeChildrenMapIfNeeded();

        logStart();
        List<Activity<?, ?>> children = activity.getChildrenCopyExceptSkipped();

        initializeChildrenState(children, result);
        executeChildren(children, result);
        logEnd();

        return executionResult;
    }

    /**
     * We create state before starting the execution in order to allow correct progress information reporting.
     * (It needs to know the total number of activity executions at any open level.)
     * An alternative would be to provide the count of executions explicitly.
     *
     * We initialize the state in both full and overview state.
     */
    private void initializeChildrenState(List<Activity<?, ?>> children, OperationResult result)
            throws ActivityExecutionException {
        for (Activity<?, ?> child : children) {
            child.createExecution(taskExecution, result)
                    .initializeState(result);
        }
        getTreeStateOverview().recordChildren(this, children, result);
    }

    /** Executes child activities. */
    private void executeChildren(Collection<Activity<?, ?>> children, OperationResult result)
            throws ActivityExecutionException {

        List<ActivityExecutionResult> childResults = new ArrayList<>();
        boolean allChildrenFinished = true;
        for (Activity<?, ?> child : children) {
            ActivityExecutionResult childExecutionResult = child.getExecution().execute(result);
            childResults.add(childExecutionResult);
            updateRunResultStatus(childExecutionResult);
            if (!childExecutionResult.isFinished()) {
                allChildrenFinished = false;
                break; // Can be error, waiting, or interruption.
            }
        }

        if (allChildrenFinished) {
            executionResult.setRunResultStatus(canRun() ? FINISHED : INTERRUPTED);
        } else {
            // keeping run result status as updated by the last child
        }
        updateOperationResultStatus(childResults);
    }

    private void updateRunResultStatus(@NotNull ActivityExecutionResult childExecutionResult) {
        // Non-null aggregate run result status means that some upstream child ended in a state that
        // should have caused the whole execution to stop. So we wouldn't be here.
        assert executionResult.getRunResultStatus() == null;
        if (childExecutionResult.isInterrupted() || !canRun()) {
            executionResult.setRunResultStatus(INTERRUPTED);
        } else if (childExecutionResult.isPermanentError()) {
            executionResult.setRunResultStatus(PERMANENT_ERROR);
        } else if (childExecutionResult.isTemporaryError()) {
            executionResult.setRunResultStatus(TEMPORARY_ERROR);
        } else if (childExecutionResult.isWaiting()) {
            executionResult.setRunResultStatus(IS_WAITING);
        }
    }

    private void updateOperationResultStatus(List<ActivityExecutionResult> childResults) {
        if (executionResult.isWaiting() || executionResult.isInterrupted()) {
            executionResult.setOperationResultStatus(IN_PROGRESS);
            return;
        }

        Set<OperationResultStatus> childStatuses = childResults.stream()
                .map(ActivityExecutionResult::getOperationResultStatus)
                .collect(Collectors.toSet());

        // Note that we intentionally do not check the _run_result_ being error here.
        // We rely on the fact that in the case of temporary/permanent error the appropriate
        // operation result status should be set as well.
        executionResult.setOperationResultStatus(OperationResultUtil.aggregateFinishedResults(childStatuses));
    }

    private void logEnd() {
        LOGGER.trace("After children execution ({}):\n{}", executionResult.shortDumpLazily(), debugDumpLazily());
    }

    private void logStart() {
        LOGGER.trace("Activity before execution:\n{}", activity.debugDumpLazily());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "result=" + executionResult +
                '}';
    }

    @Override
    public void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "result", String.valueOf(executionResult), indent+1);
    }

    @Override
    public boolean doesSupportStatistics() {
        return false;
    }
}
