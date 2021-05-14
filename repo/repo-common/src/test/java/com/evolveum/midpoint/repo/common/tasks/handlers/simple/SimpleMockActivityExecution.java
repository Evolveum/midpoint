/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.simple;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.execution.ActivityContext;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecutionResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * TODO
 */
class SimpleMockActivityExecution extends AbstractActivityExecution<SimpleMockWorkDefinition> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleMockActivityExecution.class);

    @NotNull private final SimpleMockActivityHandler handler;

    SimpleMockActivityExecution(@NotNull ActivityContext<SimpleMockWorkDefinition> context,
            @NotNull SimpleMockActivityHandler handler) {
        super(context);
        this.handler = handler;
    }

    @Override
    public @NotNull ActivityExecutionResult execute(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        String message = activityDefinition.getWorkDefinition().getMessage();
        LOGGER.info("Message: {}", message);
        handler.getRecorder().recordExecution(message);
        return ActivityExecutionResult.finished();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", handler.getRecorder(), indent+1);
        return sb.toString();
    }
}
