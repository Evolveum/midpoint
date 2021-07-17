/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.activity;

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
import com.evolveum.midpoint.report.impl.ReportTaskHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassicReportExportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DistributedReportExportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType.F_REPORT_DATA_REF;

/**
 * Activity handler for classic report export.
 *
 * TODO use legacy handler URI when registering/unregistering - but only after existing
 *  ReportTaskHandler functionality is fully migrated.
 */
@Component
public class ClassicReportExportActivityHandler
        implements ActivityHandler<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ClassicReportExportActivityHandler.class);
    private static final String LEGACY_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/report/handler-3";

    @Autowired ActivityHandlerRegistry registry;
    @Autowired WorkDefinitionFactory workDefinitionFactory;
    @Autowired CommonTaskBeans commonTaskBeans;
    @Autowired ReportServiceImpl reportService;
    @Autowired @Qualifier("modelObjectResolver") ObjectResolver objectResolver;
    @Autowired ReportTaskHandler reportTaskHandler;

    @PostConstruct
    public void register() {
        registry.register(ClassicReportExportWorkDefinitionType.COMPLEX_TYPE, null, // use LEGACY_HANDLER_URI here after the transition is over
                ClassicReportExportWorkDefinition.class, ClassicReportExportWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(ClassicReportExportWorkDefinitionType.COMPLEX_TYPE, null, // use LEGACY_HANDLER_URI here after the transition is over
                ClassicReportExportWorkDefinition.class);
    }

    @NotNull
    @Override
    public ClassicReportExportActivityExecution createExecution(
            @NotNull ExecutionInstantiationContext<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> context,
            @NotNull OperationResult result) {
        return new ClassicReportExportActivityExecution(context);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ReportExportWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public String getIdentifierPrefix() {
        return "report-export";
    }
}
