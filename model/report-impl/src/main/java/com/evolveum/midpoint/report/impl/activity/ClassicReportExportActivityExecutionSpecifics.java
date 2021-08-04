/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.task.api.RunningTask;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Activity execution for classical report export.
 *
 * TODO finish the implementation
 */
class ClassicReportExportActivityExecutionSpecifics
        extends BasePlainIterativeExecutionSpecificsImpl
        <Containerable,
                ClassicReportExportWorkDefinition,
                ClassicReportExportActivityHandler> {

    ClassicReportExportActivityExecutionSpecifics(
            @NotNull PlainIterativeActivityExecution<Containerable, ClassicReportExportWorkDefinition,
                    ClassicReportExportActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public void beforeExecution(OperationResult opResult) {
        // TODO Prepare everything for execution, e.g. resolve report object, check authorization, etc.
        //  (somehow reuse parts of the code in ReportDataCreationActivityExecution.initializeExecution, do not duplicate the code)
        //  (maybe ActivityExecutionSupport is the right way to go here?)
    }

    @Override
    public void iterateOverItems(OperationResult result) throws CommonException {
        // Issue the search to audit or model/repository
        // And use the following handler to handle the results

        AtomicInteger sequence = new AtomicInteger(0);

        Handler<Containerable> handler = record -> {
            ItemProcessingRequest<Containerable> request = new ContainerableProcessingRequest<>(
                    sequence.getAndIncrement(), record, activityExecution);
            getProcessingCoordinator().submit(request, result);
            return true;
        };
    }

    @Override
    public boolean processItem(ItemProcessingRequest<Containerable> request, RunningTask workerTask, OperationResult parentResult)
            throws CommonException, ActivityExecutionException {
        // TODO
        return true;
    }

    @Override
    public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }
}
