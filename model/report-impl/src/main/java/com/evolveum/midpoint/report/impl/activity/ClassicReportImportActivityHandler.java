/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassicReportImportWorkDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Activity handler for report import.
 */
@Component
public class ClassicReportImportActivityHandler
        implements ActivityHandler<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_REPORT_IMPORT_CLASSIC_TASK.value();

    @Autowired ActivityHandlerRegistry registry;
    @Autowired ReportServiceImpl reportService;
    @Autowired @Qualifier("modelObjectResolver") ObjectResolver objectResolver;

    @PostConstruct
    public void register() {
        registry.register(
                ClassicReportImportWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_REPORT_IMPORT,
                ClassicReportImportWorkDefinition.class, ClassicReportImportWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(ClassicReportImportWorkDefinitionType.COMPLEX_TYPE,
                ClassicReportImportWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> context,
            @NotNull OperationResult result) {
        return new ClassicReportImportActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "report-import";
    }

    @Override
    public @Nullable String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
