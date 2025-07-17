/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.ImportController;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityOverallItemCountingOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * Activity execution for report import.
 */
final class ClassicReportImportActivityRun
        extends PlainIterativeActivityRun
        <InputReportLine,
                ClassicReportImportWorkDefinition,
                ClassicReportImportActivityHandler,
                AbstractActivityWorkStateType> {

    @NotNull private final ImportActivitySupport support;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    /** Parsed VariablesMap for lines of file. */
    private List<VariablesMap> variables;

    private ImportController controller;

    ClassicReportImportActivityRun(
            @NotNull ActivityRunInstantiationContext<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> activityRun) {
        super(activityRun, "Report import");
        reportService = activityRun.getActivity().getHandler().reportService;
        support = new ImportActivitySupport(this);
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .determineOverallSizeDefault(ActivityOverallItemCountingOptionType.ALWAYS);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        if (!super.beforeRun(result)) {
            return false;
        }

        support.beforeRun(result);
        ReportType report = support.getReport();

        support.stateCheck(result);

        controller = new ImportController(
                report, reportService, support.existCollectionConfiguration() ? support.getCompiledCollectionView(result) : null);
        controller.initialize();
        try {
            variables = controller.parseColumnsAsVariablesFromFile(support.getReportData());
        } catch (IOException e) {
            String message = "Couldn't read content of imported file: " + e.getMessage();
            result.recordFatalError(message, e);
            throw new ActivityRunException(message, FATAL_ERROR, PERMANENT_ERROR, e);
        }
        return true;
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return support.getReportRef();
    }

    @Override
    public Integer determineOverallSize(OperationResult result) {
        return variables.size();
    }

    @Override
    public void iterateOverItemsInBucket(OperationResult result) {
        AtomicInteger sequence = new AtomicInteger(1);
        for (VariablesMap variablesMap : variables) {
            int lineNumber = sequence.getAndIncrement();
            InputReportLine line = new InputReportLine(lineNumber, variablesMap);
            boolean canContinue = coordinator.submit(
                    new InputReportLineProcessingRequest(line, this),
                    result);
            if (!canContinue) {
                break;
            }
        }
    }

    @Override
    public boolean processItem(
            @NotNull ItemProcessingRequest<InputReportLine> request, @NotNull RunningTask workerTask, OperationResult result)
            throws CommonException {
        InputReportLine line = request.getItem();
        controller.handleDataRecord(line, workerTask, result);
        return true;
    }

    @Override
    public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }
}
