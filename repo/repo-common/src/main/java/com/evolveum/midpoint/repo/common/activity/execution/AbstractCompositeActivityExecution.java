/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Abstract superclass for both pure- and semi-composite activities.
 *
 * @param <WD> Type of work definition.
 */
public abstract class AbstractCompositeActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        BS extends AbstractActivityWorkStateType>
        extends AbstractActivityExecution<WD, AH, BS> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCompositeActivityExecution.class);

    /**
     * Execution result from the last activity executed so far.
     */
    private ActivityExecutionResult executionResult;

    protected AbstractCompositeActivityExecution(ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        activity.initializeChildrenMapIfNeeded();

        LOGGER.trace("Activity before execution:\n{}", activity.debugDumpLazily());

        executeChildren(result);

        LOGGER.trace("After children execution ({}):\n{}", executionResult.shortDumpLazily(), debugDumpLazily());

        return executionResult;
    }

    /** Executes child activities. */
    private void executeChildren(OperationResult result)
            throws TaskException, CommonException, PreconditionViolationException {
        for (Activity<?, ?> child : activity.getChildrenMap().values()) {
            executionResult = child
                    .createExecution(taskExecution, result)
                    .execute(result);
            applyErrorCriticality();
        }
        treatNullExecutionResult();
    }

    private void applyErrorCriticality() {
        LOGGER.warn("Error criticality checking is not implemented yet."); // TODO
    }

    private void treatNullExecutionResult() {
        if (executionResult == null) {
            executionResult = ActivityExecutionResult.finished();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "result=" + executionResult +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "result", String.valueOf(executionResult), indent+1);
        return sb.toString();
    }
}
