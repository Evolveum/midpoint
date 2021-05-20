/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * TODO
 */
class IterativeMockActivityExecution
        extends AbstractIterativeActivityExecution<Integer, IterativeMockWorkDefinition, IterativeMockActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeMockActivityExecution.class);

    IterativeMockActivityExecution(@NotNull ActivityInstantiationContext<IterativeMockWorkDefinition> context,
            @NotNull IterativeMockActivityHandler handler) {
        super(context, handler, "Iterative Mock Activity");
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions();
    }

    @Override
    protected void processItems(OperationResult result) throws CommonException, PreconditionViolationException {
        IterativeMockWorkDefinition workDef = getActivityDefinition().getWorkDefinition();
        for (int item = workDef.getFrom(); item <= workDef.getTo(); item++) {
            ItemProcessingRequest<Integer> request = new IterativeMockProcessingRequest(item, this);
            coordinator.submit(request, result);
        }
    }

    @Override
    protected @NotNull ItemProcessor<Integer> createItemProcessor(OperationResult opResult) {
        return (request, workerTask, parentResult) -> {
            String message = activityDefinition.getWorkDefinition().getMessage() + request.getItem();
            LOGGER.info("Message: {}", message);
            activityHandler.getRecorder().recordExecution(message);
            return true;
        };
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        return false;
    }

    @Override
    @NotNull
    protected ErrorHandlingStrategyExecutor.Action getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.Action.CONTINUE;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", activityHandler.getRecorder(), indent+1);
        return sb.toString();
    }

}
