/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;

import com.evolveum.midpoint.report.impl.ReportServiceImpl;

import com.evolveum.midpoint.report.impl.controller.fileformat.ImportController;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.AbstractIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.IMPORT;

/**
 * Activity execution for report import.
 */
class ClassicReportImportActivityExecution
        extends AbstractIterativeActivityExecution
        <InputReportLine,
                ClassicReportImportWorkDefinition,
                ClassicReportImportActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ClassicReportImportActivityExecution.class);

    @NotNull private final ActivityImportSupport support;

    @NotNull private ReportType report;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    @NotNull private ImportController controller;

    ClassicReportImportActivityExecution(
            @NotNull ExecutionInstantiationContext<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> context) {
        super(context, "Report import");
        reportService = context.getActivity().getHandler().reportService;
        support = new ActivityImportSupport(context);
    }

    @Override
    protected void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.initializeExecution(result);
        report = support.getReport();

        support.stateCheck(result);

        controller = new ImportController(
                report, reportService, support.existCollectionConfiguration() ? support.getCompiledCollectionView(result) : null);
        controller.initialize(getRunningTask(), result);
    }

    @Override
    protected void processItems(OperationResult result) throws CommonException {
        BiConsumer<Integer, VariablesMap> handler = (lineNumber, variables) -> {
            InputReportLine line = new InputReportLine(lineNumber, variables);
            // TODO determine the correlation value, if possible

            coordinator.submit(
                    new InputReportLineProcessingRequest(line, this),
                    result);
        };
        try {
            controller.processVariableFromFile(report, support.getReportData(), handler);
        } catch (IOException e) {
            LOGGER.error("Couldn't read content of imported file", e);
            return;
        }
    }

    @Override
    protected @NotNull ItemProcessor<InputReportLine> createItemProcessor(OperationResult opResult) {
        return (request, workerTask, parentResult) -> {
            InputReportLine line = request.getItem();
            controller.handleDataRecord(line, workerTask, parentResult);
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
