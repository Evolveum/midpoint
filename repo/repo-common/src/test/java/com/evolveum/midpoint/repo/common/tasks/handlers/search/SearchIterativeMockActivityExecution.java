/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.search;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
                        SearchIterativeMockActivityExecution> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchIterativeMockActivityExecution.class);

    SearchIterativeMockActivityExecution(@NotNull ActivityInstantiationContext<SearchIterativeMockWorkDefinition> context,
            @NotNull SearchIterativeMockActivityHandler handler) {
        super(context, handler, "Search-iterative mock activity");
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions();
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ObjectType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(
                (object, request, workerTask, result) -> {
                    String message = activityDefinition.getWorkDefinition().getMessage() + object.getName().getOrig();
                    LOGGER.info("Message: {}", message);
                    activityHandler.getRecorder().recordExecution(message);
                    return true;
                });
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
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", activityHandler.getRecorder(), indent + 1);
        return sb.toString();
    }
}
