/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.PlainIterativeActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.IMPORT;

/**
 * Contains common functionality for import activity executions.
 * This is an experiment - using object composition instead of inheritance.
 */
class ImportActivitySupport extends ReportActivitySupport {

    private static final Trace LOGGER = TraceManager.getTrace(ImportActivitySupport.class);

    private final @NotNull Activity<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> activity;

    /**
     * Resolved report data object.
     */
    private ReportDataType reportData;

    ImportActivitySupport(PlainIterativeActivityRun<InputReportLine, ClassicReportImportWorkDefinition,
                ClassicReportImportActivityHandler, ?> activityRun) {
        super(activityRun,
                activityRun.getActivity().getHandler().reportService,
                activityRun.getActivity().getHandler().objectResolver,
                activityRun.getActivity().getWorkDefinition());
        activity = activityRun.getActivity();
    }

    @Override
    void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        super.beforeRun(result);
        setupReportDataObject(result);
    }

    private void setupReportDataObject(OperationResult result) throws CommonException {
        @NotNull ClassicReportImportWorkDefinition workDefinition = activity.getWorkDefinition();
        reportData = resolver.resolve(workDefinition.getReportDataRef(), ReportDataType.class,
                null, "resolving report data", runningTask, result);
    }

    /** Should be called only after initialization. */
    @NotNull ReportDataType getReportData() {
        return MiscUtil.requireNonNull(
                reportData, () -> new IllegalStateException("No report data object (are we uninitialized?)"));
    }

    private boolean existImportScript() {
        return getReport().getBehavior() != null && getReport().getBehavior().getImportScript() != null;
    }

    boolean existCollectionConfiguration() {
        return getReport().getObjectCollection() != null;
    }

    @Override
    public void stateCheck(OperationResult result) throws CommonException {
        MiscUtil.stateCheck(getDirection(report) == IMPORT, "Only report import are supported here");
        MiscUtil.stateCheck(existCollectionConfiguration() || existImportScript(),
                "Report of 'import' direction without import script supports only the object collection engine."
                + " Please define ObjectCollectionReportEngineConfigurationType in report type.");

        //noinspection unchecked
        if (!reportService.isAuthorizedToImportReport(report.asPrismContainer(), runningTask, result)) {
            LOGGER.error("User is not authorized to import report {}", report);
            throw new SecurityViolationException("Not authorized");
        }

        String pathToFile = getReportData().getFilePath();
        MiscUtil.stateCheck(StringUtils.isNotEmpty(pathToFile), "Path to file for import report is empty.");
        MiscUtil.stateCheck(new File(pathToFile).exists(), "File " + pathToFile + " for import report not exist.");
        MiscUtil.stateCheck(new File(pathToFile).isFile(), "Object " + pathToFile + " for import report isn't file.");
    }
}
