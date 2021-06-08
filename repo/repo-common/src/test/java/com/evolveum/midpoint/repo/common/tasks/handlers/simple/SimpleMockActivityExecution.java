/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.simple;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.tasks.handlers.CommonMockActivityHelper;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * TODO
 */
class SimpleMockActivityExecution
        extends AbstractActivityExecution<SimpleMockWorkDefinition, SimpleMockActivityHandler, AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleMockActivityExecution.class);

    SimpleMockActivityExecution(
            @NotNull ExecutionInstantiationContext<SimpleMockWorkDefinition, SimpleMockActivityHandler> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        String message = activity.getWorkDefinition().getMessage();
        LOGGER.info("Message: {}", message);
        getRecorder().recordExecution(message);

        CommonMockActivityHelper helper = getActivityHandler().getMockHelper();
        helper.increaseExecutionCount(this, result);
        helper.failIfNeeded(this, activity.getWorkDefinition().getInitialFailures());

        return ActivityExecutionResult.finished(activityState.getResultStatus());
    }

    @NotNull
    private MockRecorder getRecorder() {
        return activity.getHandler().getRecorder();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", getRecorder(), indent+1);
        return sb.toString();
    }
}
