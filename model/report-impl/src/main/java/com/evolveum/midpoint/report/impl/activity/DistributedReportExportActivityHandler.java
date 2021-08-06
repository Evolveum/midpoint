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

import com.evolveum.midpoint.repo.common.activity.execution.CompositeActivityExecution;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.report.impl.ReportTaskHandler;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DistributedReportExportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

/**
 * Activity handler for distributed report export.
 *
 * It provides two sub-activities:
 *
 * 1. Partial reports creation: report data is created for each bucket of objects.
 * 2. Report summarization: partial report data objects are aggregated into summary one.
 *
 * TODO simplify processing if no bucketing is defined
 */
@Component
public class DistributedReportExportActivityHandler
        implements ActivityHandler<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DistributedReportExportActivityHandler.class);

    @Autowired ActivityHandlerRegistry registry;
    @Autowired WorkDefinitionFactory workDefinitionFactory;
    @Autowired CommonTaskBeans commonTaskBeans;
    @Autowired ReportServiceImpl reportService;
    @Autowired @Qualifier("modelObjectResolver") ObjectResolver objectResolver;
    @Autowired ReportTaskHandler reportTaskHandler;

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
    public CompositeActivityExecution<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityExecution<>(context);
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
                parentActivity.getDefinition().clone(),
                (context, result) -> new SearchBasedActivityExecution<>(
                        context, "Data creation", ReportDataCreationExecutionSpecifics::new),
                this::createEmptyAggregatedDataObject,
                (i) -> "data-creation",
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().clone(),
                (context, result) -> new SearchBasedActivityExecution<>(
                        context, "Report data aggregation", ReportDataAggregationExecutionSpecifics::new),
                null,
                (i) -> "data-aggregation",
                ActivityStateDefinition.normal(),
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

        ReportDataType reportData = new ReportDataType(commonTaskBeans.prismContext)
                .name(RandomStringUtils.randomAlphabetic(10)); // TODO
        String oid = commonTaskBeans.repositoryService.addObject(reportData.asPrismObject(), null, result);

        activityState.setWorkStateItemRealValues(F_REPORT_DATA_REF, createObjectRef(oid, ObjectTypes.REPORT_DATA));
        activityState.flushPendingModifications(result);

        LOGGER.info("Created empty report data object {}", reportData);
    }

    @Override
    public String getIdentifierPrefix() {
        return "distributed-report-export";
    }
}
