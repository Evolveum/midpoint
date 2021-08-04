/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.function.BiConsumer;

import com.evolveum.midpoint.repo.common.task.*;

import com.evolveum.midpoint.task.api.RunningTask;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Activity execution for report import.
 */
class ClassicReportImportActivityExecutionSpecifics
        extends BasePlainIterativeExecutionSpecificsImpl
        <InputReportLine,
                ClassicReportImportWorkDefinition,
                ClassicReportImportActivityHandler> {

    ClassicReportImportActivityExecutionSpecifics(@NotNull PlainIterativeActivityExecution<InputReportLine,
            ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public void beforeExecution(OperationResult opResult) {
        // TODO Prepare everything for execution, e.g. resolve report object, check authorization, check the data object, etc.
    }

    @Override
    public void iterateOverItems(OperationResult result) throws CommonException {
        // Open the data object and parse its content
        // Feed the lines to the following handler

        BiConsumer<Integer, String> handler = (lineNumber, text) -> {
            InputReportLine line = new InputReportLine(lineNumber, text);
            // TODO determine the correlation value, if possible

            getProcessingCoordinator().submit(
                    new InputReportLineProcessingRequest(line, activityExecution),
                    result);
        };
    }

    @Override
    public boolean processItem(ItemProcessingRequest<InputReportLine> request, RunningTask workerTask, OperationResult parentResult)
            throws CommonException, ActivityExecutionException {
        InputReportLine line = request.getItem();
        // TODO
        return true;
    }

    @Override
    @NotNull
    public ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE; // TODO or STOP ?
    }
}
