/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.AbstractIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

/**
 * Activity execution for report import.
 */
class ClassicReportImportActivityExecution
        extends AbstractIterativeActivityExecution
        <InputReportLine,
                ClassicReportImportWorkDefinition,
                ClassicReportImportActivityHandler,
                AbstractActivityWorkStateType> {

    ClassicReportImportActivityExecution(
            @NotNull ExecutionInstantiationContext<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> context) {
        super(context, "Report import");
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        // TODO Prepare everything for execution, e.g. resolve report object, check authorization, check the data object, etc.
    }

    @Override
    protected void processItems(OperationResult result) throws CommonException {
        // Open the data object and parse its content
        // Feed the lines to the following handler

        BiConsumer<Integer, String> handler = (lineNumber, text) -> {
            InputReportLine line = new InputReportLine(lineNumber, text);
            // TODO determine the correlation value, if possible

            coordinator.submit(
                    new InputReportLineProcessingRequest(line, this),
                    result);
        };
    }

    @Override
    protected @NotNull ItemProcessor<InputReportLine> createItemProcessor(OperationResult opResult) {
        return (request, workerTask, parentResult) -> {
            InputReportLine line = request.getItem();

            // TODO process the input line

            return true;
        };
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        return false;
    }

    @Override
    protected @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE; // TODO or STOP ?
    }
}
