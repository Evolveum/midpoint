/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType.F_REPORT_DATA_REF;

import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Activity handler for distributed report export.
 *
 * It provides two sub-activities:
 *
 * 1. Partial reports creation: report data is created for each bucket of objects.
 * 2. Report summarization: partial report data objects are aggregated into summary one.
 */
@Component
public class DistributedReportExportActivityHandler
        implements ActivityHandler<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DistributedReportExportActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_REPORT_EXPORT_DISTRIBUTED_TASK.value();

    @Autowired ActivityHandlerRegistry registry;
    @Autowired CommonTaskBeans commonTaskBeans;
    @Autowired ReportServiceImpl reportService;
    @Autowired @Qualifier("modelObjectResolver") ObjectResolver objectResolver;

    @PostConstruct
    public void register() {
        registry.register(DistributedReportExportWorkDefinitionType.COMPLEX_TYPE, null,
                DistributedReportExportWorkDefinition.class, DistributedReportExportWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(DistributedReportExportWorkDefinitionType.COMPLEX_TYPE, null,
                DistributedReportExportWorkDefinition.class);
    }

    @NotNull
    @Override
    public CompositeActivityRun<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ReportExportWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> parentActivity) {
        ArrayList<Activity<?, ?>> children = new ArrayList<>();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new ReportDataCreationActivityRun(context),
                this::createEmptyAggregatedDataObject,
                (i) -> "data-creation",
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new ReportDataAggregationActivityRun(context),
                null,
                (i) -> "data-aggregation",
                ActivityStateDefinition.normal(ReportExportWorkStateType.COMPLEX_TYPE),
                parentActivity));
        return children;
    }

    private void createEmptyAggregatedDataObject(
            EmbeddedActivity<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> activity,
            RunningTask runningTask, OperationResult result) throws CommonException {
        ActivityState activityState =
                ActivityState.getActivityStateUpwards(
                        activity.getPath().allExceptLast(),
                        runningTask,
                        ReportExportWorkStateType.COMPLEX_TYPE,
                        commonTaskBeans,
                        result);
        if (activityState.getWorkStateReferenceRealValue(F_REPORT_DATA_REF) != null) {
            return;
        }

        ReportType report = objectResolver.resolve(
                activity.getWorkDefinition().getReportRef(),
                ReportType.class,
                null,
                "resolve report ref",
                runningTask,
                result);
        ReportDataType reportData = new ReportDataType(commonTaskBeans.prismContext)
                .name(SaveReportFileSupport.getNameOfExportedReportData(report, getType(report)));
        String oid = commonTaskBeans.repositoryService.addObject(reportData.asPrismObject(), null, result);

        activityState.setWorkStateItemRealValues(F_REPORT_DATA_REF, createObjectRef(oid, ObjectTypes.REPORT_DATA));
        activityState.flushPendingTaskModifications(result);

        LOGGER.info("Created empty report data object {}", reportData);
    }

    private String getType(ReportType report) {
        if (report == null || report.getFileFormat() == null || report.getFileFormat().getType() == null) {
            return FileFormatTypeType.CSV.name();
        }
        return report.getFileFormat().getType().name();
    }

    @Override
    public String getIdentifierPrefix() {
        return "distributed-report-export";
    }

    @Override
    public @Nullable String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
