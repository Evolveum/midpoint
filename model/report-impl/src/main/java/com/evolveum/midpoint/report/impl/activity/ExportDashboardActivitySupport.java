/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.fileformat.CollectionBasedExportController;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains common functionality for executions of dashboard export report-related activities.
 * This is an experiment - using object composition instead of inheritance.
 */
class ExportDashboardActivitySupport extends ExportActivitySupport{

    /**
     * Resolved dashboard object.
     */
    private DashboardType dashboard;

    /**
     * Map of compiled view for widgets.
     */
    private Map<String, CompiledObjectCollectionView> mapOfCompiledViews;

    ExportDashboardActivitySupport(AbstractActivityExecution<?, ?, ?> activityExecution, ReportServiceImpl reportService,
                                   ObjectResolver resolver, AbstractReportWorkDefinition workDefinition) {
        super(activityExecution, reportService, resolver, workDefinition);
    }

    @Override
    void beforeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        super.beforeExecution(result);
        setupDashboard(result);
        setupCompiledViewsForWidgets(result);
    }

    private void setupCompiledViewsForWidgets(OperationResult result) throws CommonException{
        mapOfCompiledViews = new LinkedHashMap<>();
        List<DashboardWidgetType> widgets = dashboard.getWidget();
        for (DashboardWidgetType widget : widgets) {
            if (isWidgetTableVisible()) {
                CompiledObjectCollectionView compiledView = reportService.createCompiledView(report.getDashboard(), widget, runningTask, result);
                DisplayType newDisplay = widget.getDisplay();
                if (compiledView.getDisplay() == null) {
                    compiledView.setDisplay(newDisplay);
                } else if (newDisplay != null){
                    MiscSchemaUtil.mergeDisplay(compiledView.getDisplay(), newDisplay);
                }
                compiledView.setViewIdentifier(widget.getIdentifier());
                mapOfCompiledViews.put(widget.getIdentifier(), compiledView);
            }
        }
    }

    public boolean isWidgetTableVisible() {
        return !Boolean.TRUE.equals(report.getDashboard().isShowOnlyWidgetsTable())
                && supportWidgetTables(report.getFileFormat());
    }

    private boolean supportWidgetTables(FileFormatConfigurationType fileFormat) {
        if (fileFormat != null && FileFormatTypeType.CSV.equals(fileFormat.getType())) {
            return false;
        }
        return true;
    }

    private void setupDashboard(OperationResult result) throws CommonException {
        dashboard = resolver.resolve(report.getDashboard().getDashboardRef(), DashboardType.class,
                null, "resolving dashboard", runningTask, result);
    }

    @NotNull public DashboardType getDashboard() {
        return dashboard;
    }

    @Override
    @NotNull CompiledObjectCollectionView getCompiledCollectionView(OperationResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stateCheck(OperationResult result) throws CommonException {
        MiscUtil.stateCheck(report.getDashboard() != null && report.getDashboard().getDashboardRef() != null,
                "Only dashboard-based reports are supported here");
        super.stateCheck(result);
    }

    @Nullable public CompiledObjectCollectionView getCompiledCollectionView(String widgetIdentifier, OperationResult result) {
        return mapOfCompiledViews.get(widgetIdentifier);
    }

    @NotNull public Map<String, CompiledObjectCollectionView> getMapOfCompiledViews() {
        return mapOfCompiledViews;
    }

    static class DashboardWidgetHolder {

        @NotNull final SearchSpecificationHolder searchSpecificationHolder;
        @NotNull final CollectionBasedExportController controller;

        @NotNull final DashboardWidget widgetData;

        DashboardWidgetHolder(SearchSpecificationHolder searchSpecificationHolder, CollectionBasedExportController controller,
                DashboardWidget widgetData) {
            this.searchSpecificationHolder = searchSpecificationHolder;
            this.controller = controller;
            this.widgetData = widgetData;
        }

        @NotNull public SearchSpecificationHolder getSearchSpecificationHolder() {
            return searchSpecificationHolder;
        }

        @NotNull  public CollectionBasedExportController getController() {
            return controller;
        }

        @NotNull public DashboardWidget getWidgetData() {
            return widgetData;
        }
    }
}
