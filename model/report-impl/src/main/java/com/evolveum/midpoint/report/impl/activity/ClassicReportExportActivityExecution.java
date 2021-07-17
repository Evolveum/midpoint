/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

/**
 * Activity execution for classical report export.
 *
 * TODO finish the implementation
 */
class ClassicReportExportActivityExecution
        extends AbstractIterativeActivityExecution
        <Containerable,
                ClassicReportExportWorkDefinition,
                ClassicReportExportActivityHandler,
                ReportExportWorkStateType> {

    ClassicReportExportActivityExecution(
            @NotNull ExecutionInstantiationContext<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> context) {
        super(context, "Report export");
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        // TODO Prepare everything for execution, e.g. resolve report object, check authorization, etc.
        //  (somehow reuse parts of the code in ReportDataCreationActivityExecution.initializeExecution, do not duplicate the code)
        //  (maybe ActivityExecutionSupport is the right way to go here?)
    }

    @Override
    protected void processItems(OperationResult result) throws CommonException {
        // Issue the search to audit or model/repository
        // And use the following handler to handle the results

        AtomicInteger sequence = new AtomicInteger(0);

        Handler<Containerable> handler = record -> {
            ItemProcessingRequest<Containerable> request = new ContainerableProcessingRequest<>(
                    sequence.getAndIncrement(), record, ClassicReportExportActivityExecution.this);
            coordinator.submit(request, result);
            return true;
        };
    }

    @Override
    protected @NotNull ItemProcessor<Containerable> createItemProcessor(OperationResult opResult) {
        return (request, workerTask, parentResult) -> {
            Containerable record = request.getItem();

            // TODO process the record

            return true;
        };
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        return false;
    }

    @Override
    protected @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }
}
