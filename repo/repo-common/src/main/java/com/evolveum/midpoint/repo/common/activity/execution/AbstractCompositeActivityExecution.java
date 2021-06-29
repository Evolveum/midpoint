/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
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

import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.FINISHED;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.INTERRUPTED;

/**
 * Abstract superclass for both pure- and semi-composite activities.
 *
 * @param <WD> Type of work definition.
 */
public abstract class AbstractCompositeActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        BS extends AbstractActivityWorkStateType>
        extends LocalActivityExecution<WD, AH, BS> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCompositeActivityExecution.class);

    /**
     * TODO
     */
    @NotNull private final ActivityExecutionResult executionResult = new ActivityExecutionResult();

    protected AbstractCompositeActivityExecution(ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws ActivityExecutionException, CommonException {

        activity.initializeChildrenMapIfNeeded();

        logStart();
        executeChildren(result);
        logEnd();

        return executionResult;
    }

    /** Executes child activities. */
    private void executeChildren(OperationResult result) throws ActivityExecutionException {
        List<ActivityExecutionResult> childResults = new ArrayList<>();

        boolean allChildrenFinished = true;

        Collection<Activity<?, ?>> children = activity.getChildrenCopy();

        // We create state before starting the execution in order to allow correct progress information reporting.
        // (It needs to know the total number of activity executions at any open level.)
        // An alternative would be to provide the count of executions explicitly.
        for (Activity<?, ?> child : children) {
            child.createExecution(taskExecution, result)
                    .initializeState(result);
        }

        for (Activity<?, ?> child : children) {
            ActivityExecutionResult childExecutionResult = child.getExecution().execute(result);
            childResults.add(childExecutionResult);
            executionResult.updateRunResultStatus(childExecutionResult, canRun());
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
        executionResult.updateOperationResultStatus(childResults);
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
    public boolean supportsStatistics() {
        return false;
    }
}
