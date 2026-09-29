/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.PERMANENT_ERROR;

import java.io.IOException;
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
import com.evolveum.midpoint.util.exception.SystemException;
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

    /** MID-11009: Total record count for progress reporting (streaming processing). */
    private int recordCount;

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
    public void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        support.beforeRun(result);
        ReportType report = support.getReport();

        support.stateCheck(result);

        controller = new ImportController(
                report, reportService, support.existCollectionConfiguration() ? support.getCompiledCollectionView(result) : null);
        controller.initialize();
        try {
            recordCount = controller.countRecords(support.getReportData());
        } catch (IOException e) {
            String message = "Couldn't read content of imported file: " + e.getMessage();
            result.recordFatalError(message, e);
            throw new ActivityRunException(message, FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return support.getReportRef();
    }

    @Override
    public Integer determineOverallSize(OperationResult result) {
        return recordCount;
    }

    @Override
    public void iterateOverItemsInBucket(OperationResult result) throws CommonException {
        try {
            // The parser is opened and closed here (not in beforeRun/afterRun), so that the file is closed
            // also when the processing fails: afterRun is not called in that case.
            controller.initializeCsvParser(support.getReportData());
            try {
                AtomicInteger sequence = new AtomicInteger(1);
                VariablesMap variablesMap;
                while ((variablesMap = controller.getNextVariablesMap()) != null) {
                    int lineNumber = sequence.getAndIncrement();
                    InputReportLine line = new InputReportLine(lineNumber, variablesMap);
                    boolean canContinue = coordinator.submit(
                            new InputReportLineProcessingRequest(line, this),
                            result);
                    if (!canContinue) {
                        break;
                    }
                }
            } finally {
                controller.close();
            }
        } catch (IOException e) {
            throw new SystemException("Couldn't read content of imported file: " + e.getMessage(), e);
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
