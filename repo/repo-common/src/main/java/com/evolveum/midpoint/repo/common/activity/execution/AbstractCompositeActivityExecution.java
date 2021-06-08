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
     * TODO
     */
    @NotNull private final ActivityExecutionResult executionResult = new ActivityExecutionResult();

    protected AbstractCompositeActivityExecution(ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws ActivityExecutionException, CommonException {

        activity.initializeChildrenMapIfNeeded();

        logStart();
        executeChildren(result);
        logEnd();

        return executionResult;
    }

    /** Executes child activities. */
    private void executeChildren(OperationResult result) throws ActivityExecutionException, CommonException {
        for (Activity<?, ?> child : activity.getChildrenMap().values()) {
            ActivityExecutionResult childExecutionResult = child
                    .createExecution(taskExecution, result)
                    .execute(result);
            executionResult.update(childExecutionResult);
            if (executionResult.isError()) {
                break;
            }
        }
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
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "result", String.valueOf(executionResult), indent+1);
        return sb.toString();
    }
}
