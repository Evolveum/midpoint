/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Activity handler for classic report export.
 */
@Component
public class ClassicReportExportActivityHandler
        implements ActivityHandler<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ClassicReportExportActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK.value();

    @Autowired ActivityHandlerRegistry registry;
    @Autowired ReportServiceImpl reportService;
    @Autowired @Qualifier("modelObjectResolver") ObjectResolver objectResolver;

    @PostConstruct
    public void register() {
        registry.register(
                ClassicReportExportWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_REPORT_EXPORT,
                ClassicReportExportWorkDefinition.class, ClassicReportExportWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(ClassicReportExportWorkDefinitionType.COMPLEX_TYPE,
                ClassicReportExportWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> context,
            @NotNull OperationResult result) {
        return resolveExecution(context, result);
    }

    private AbstractActivityRun<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler, ?> resolveExecution(
            ActivityRunInstantiationContext<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> context,
            OperationResult result) {
        @NotNull ClassicReportExportWorkDefinition workDefinition = context.getActivity().getWorkDefinition();
        ReportType report;
        try {
            report = reportService.getObjectResolver().resolve(workDefinition.getReportRef(), ReportType.class,
                    null, "resolving report", context.getTaskRun().getRunningTask(), result);
        } catch (CommonException e) {
            throw new IllegalArgumentException(e);
        }

        if (report.getDashboard() != null) {
            return new ClassicDashboardReportExportActivityRun(context);
        }
        if (report.getObjectCollection() != null) {
            return new ClassicCollectionReportExportActivityRun<>(context);
        }
        LOGGER.error("Report don't contains engine");
        throw new IllegalArgumentException("Report don't contains engine");

    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ReportExportWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public String getIdentifierPrefix() {
        return "report-export";
    }

    @Override
    public @Nullable String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
