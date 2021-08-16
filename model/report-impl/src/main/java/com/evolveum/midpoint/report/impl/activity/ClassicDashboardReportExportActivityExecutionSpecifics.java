/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.report.impl.activity.ExportActivitySupport.SearchSpecificationHolder;
import com.evolveum.midpoint.report.impl.activity.ExportDashboardActivitySupport.DashboardWidgetHolder;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity execution specifics for classical report export.
 */
public class ClassicDashboardReportExportActivityExecutionSpecifics
        extends BasePlainIterativeExecutionSpecificsImpl
        <ExportDashboardReportLine<Containerable>,
                ClassicReportExportWorkDefinition,
                ClassicReportExportActivityHandler> {

    @NotNull private final ExportDashboardActivitySupport support;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    /**
     * Data writer which completize context of report.
     */
    private ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter;

    /**
     * Map of controllers for objects searched based on widgets.
     */
    private Map<String, DashboardWidgetHolder> mapOfWidgetsController;

    /**
     * Controller for widgets.
     */
    private DashboardWidgetBasedExportController<Containerable> basicWidgetController;

    ClassicDashboardReportExportActivityExecutionSpecifics(
            @NotNull PlainIterativeActivityExecution<ExportDashboardReportLine<Containerable>, ClassicReportExportWorkDefinition,
                    ClassicReportExportActivityHandler, ?> activityExecution) {
        super(activityExecution);
        reportService = activityExecution.getActivity().getHandler().reportService;
        support = new ExportDashboardActivitySupport(activityExecution, activityExecution.getActivityHandler().reportService,
                activityExecution.getActivityHandler().objectResolver, activityExecution.getActivity().getWorkDefinition());
    }

    @Override
    public void beforeExecution(OperationResult result) throws ActivityExecutionException, CommonException {
        RunningTask task = getRunningTask();
        support.beforeExecution(result);
        @NotNull ReportType report = support.getReport();

        support.stateCheck(result);

        List<DashboardWidgetType> widgets = support.getDashboard().getWidget();

        mapOfWidgetsController = new LinkedHashMap<>();

        dataWriter = ReportUtils.createDashboardDataWriter(
                report, getActivityHandler().reportService, support.getMapOfCompiledViews());
        basicWidgetController = new DashboardWidgetBasedExportController<>(dataWriter, report, reportService);
        basicWidgetController.initialize();
        basicWidgetController.beforeBucketExecution(1, result);

        for (DashboardWidgetType widget : widgets) {
            if (support.isWidgetTableVisible()) {
                String widgetIdentifier = widget.getIdentifier();
                SearchSpecificationHolder searchSpecificationHolder = new SearchSpecificationHolder();
                DashboardBasedExportController<Containerable> controller = new DashboardBasedExportController(
                        searchSpecificationHolder,
                        dataWriter,
                        report,
                        reportService,
                        support.getCompiledCollectionView(widgetIdentifier),
                        widgetIdentifier,
                        support.getReportParameters());
                controller.initialize(task, result);
                controller.beforeBucketExecution(1, result);

                mapOfWidgetsController.put(widgetIdentifier, new DashboardWidgetHolder(searchSpecificationHolder, controller));
            }
        }
    }

    @Override
    public void iterateOverItemsInBucket(@NotNull WorkBucketType bucket, OperationResult result) throws CommonException {
        // Issue the search to audit or model/repository
        // And use the following handler to handle the results

        List<DashboardWidgetType> widgets = support.getDashboard().getWidget();
        AtomicInteger widgetSequence = new AtomicInteger(1);
        for (DashboardWidgetType widget : widgets) {

            ExportDashboardReportLine<Containerable> widgetLine = new ExportDashboardReportLine<>(widgetSequence.getAndIncrement(), widget);
            ItemProcessingRequest<ExportDashboardReportLine<Containerable>> widgetRequest = new ExportDashboardReportLineProcessingRequest(
                    widgetLine, activityExecution);
            getProcessingCoordinator().submit(widgetRequest, result);

            if (support.isWidgetTableVisible()) {
                AtomicInteger sequence = new AtomicInteger(1);
                Handler<Containerable> handler = record -> {
                    ExportDashboardReportLine<Containerable> line = new ExportDashboardReportLine<>(sequence.getAndIncrement(),
                            record,
                            widget.getIdentifier());
                    ItemProcessingRequest<ExportDashboardReportLine<Containerable>> request = new ExportDashboardReportLineProcessingRequest(
                            line, activityExecution);
                    getProcessingCoordinator().submit(request, result);
                    return true;
                };

                DashboardWidgetHolder holder = mapOfWidgetsController.get(widget.getIdentifier());
                SearchSpecificationHolder searchSpecificationHolder = holder.getSearchSpecificationHolder();
                List<? extends Containerable> objects = support.searchRecords(
                        searchSpecificationHolder.getType(),
                        searchSpecificationHolder.getQuery(),
                        searchSpecificationHolder.getOptions(),
                        result);
                objects.forEach(handler::handle);
            }
        }
    }

    @Override
    public boolean processItem(ItemProcessingRequest<ExportDashboardReportLine<Containerable>> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        Containerable record = request.getItem().getContainer();

        getController(request).handleDataRecord(request.getSequentialNumber(), record, workerTask, result);
        return true;
    }

    private ExportController<Containerable> getController(ItemProcessingRequest<ExportDashboardReportLine<Containerable>> request) {
        if (request.getItem().isBasicWidgetRow()) {
            return basicWidgetController;
        }
        DashboardWidgetHolder holder = mapOfWidgetsController.get(request.getItem().getWidgetIdentifier());
        return holder.getController();
    }

    @Override
    public void afterExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.saveReportFile(dataWriter, result);
    }

    @Override
    public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }
}
