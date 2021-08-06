/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.PlainIterativeActivityExecution;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportTaskHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassicReportImportWorkDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Activity handler for report import.
 *
 * Note that we cannot support legacy URI here. The reason is that from the URI itself we cannot distinguish report export
 * from report import. This is possible only after retrieving the report definition - and this is just too late for
 * the activity framework.
 *
 * So we simply do not support legacy URI for importing reports. (Or we could devise a separate URI for this, if needed.)
 */
@Component
public class ClassicReportImportActivityHandler
        implements ActivityHandler<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ClassicReportImportActivityHandler.class);

    @Autowired ActivityHandlerRegistry registry;
    @Autowired WorkDefinitionFactory workDefinitionFactory;
    @Autowired CommonTaskBeans commonTaskBeans;
    @Autowired ReportServiceImpl reportService;
    @Autowired @Qualifier("modelObjectResolver") ObjectResolver objectResolver;
    @Autowired ReportTaskHandler reportTaskHandler;

    @PostConstruct
    public void register() {
        registry.register(ClassicReportImportWorkDefinitionType.COMPLEX_TYPE, null,
                ClassicReportImportWorkDefinition.class, ClassicReportImportWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(ClassicReportImportWorkDefinitionType.COMPLEX_TYPE, null,
                ClassicReportImportWorkDefinition.class);
    }

    @Override
    public AbstractActivityExecution<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> context,
            @NotNull OperationResult result) {
        return new PlainIterativeActivityExecution<>(context, "Report import", ClassicReportImportActivityExecutionSpecifics::new);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal();
    }

    @Override
    public String getIdentifierPrefix() {
        return "report-import";
    }
}
