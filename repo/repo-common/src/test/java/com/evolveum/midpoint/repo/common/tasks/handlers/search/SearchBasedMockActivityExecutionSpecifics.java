/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.search;

import com.evolveum.midpoint.repo.common.task.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * TODO
 */
class SearchBasedMockActivityExecutionSpecifics
        extends BaseSearchBasedExecutionSpecificsImpl
        <ObjectType, SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBasedMockActivityExecutionSpecifics.class);

    SearchBasedMockActivityExecutionSpecifics(@NotNull SearchBasedActivityExecution<ObjectType,
            SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .enableSynchronizationStatistics(true)
                .enableActionsExecutedStatistics(true);
    }

    @Override
    public boolean processObject(@NotNull PrismObject<ObjectType> object,
            @NotNull ItemProcessingRequest<PrismObject<ObjectType>> request, RunningTask workerTask, OperationResult result) {
        String message = getWorkDefinition().getMessage() + object.getName().getOrig();
        LOGGER.info("Message: {}", message);
        getRecorder().recordExecution(message);

        provideSomeMockStatistics(request, workerTask);
        return true;
    }

    private void provideSomeMockStatistics(ItemProcessingRequest<PrismObject<ObjectType>> request, RunningTask workerTask) {
        PrismObject<ObjectType> object = request.getItem();
        workerTask.onSynchronizationStart(request.getIdentifier(), object.getOid(), SynchronizationSituationType.UNLINKED);
        workerTask.onSynchronizationSituationChange(request.getIdentifier(), object.getOid(), SynchronizationSituationType.LINKED);
        workerTask.recordObjectActionExecuted(object, ChangeType.MODIFY, null);
        workerTask.recordObjectActionExecuted(object, ChangeType.MODIFY, null);
    }

    @NotNull
    private MockRecorder getRecorder() {
        return getActivityHandler().getRecorder();
    }
}
