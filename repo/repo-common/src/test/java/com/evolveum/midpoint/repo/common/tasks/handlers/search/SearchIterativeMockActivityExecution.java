/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.search;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * TODO
 */
@DefaultHandledObjectType(ObjectType.class)
class SearchIterativeMockActivityExecution
        extends AbstractSearchIterativeActivityExecution
        <ObjectType,
                SearchIterativeMockWorkDefinition,
                SearchIterativeMockActivityHandler,
                SearchIterativeMockActivityExecution,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchIterativeMockActivityExecution.class);

    SearchIterativeMockActivityExecution(
            @NotNull ExecutionInstantiationContext<SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler> context) {
        super(context, "Search-iterative mock activity");
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions();
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ObjectType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(
                (object, request, workerTask, result) -> {
                    String message = activity.getWorkDefinition().getMessage() + object.getName().getOrig();
                    LOGGER.info("Message: {}", message);
                    getRecorder().recordExecution(message);
                    return true;
                });
    }

    @NotNull
    private MockRecorder getRecorder() {
        return activity.getHandler().getRecorder();
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        return false;
    }

    @Override
    @NotNull
    protected ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }

    @Override
    public void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", getRecorder(), indent + 1);
    }
}
