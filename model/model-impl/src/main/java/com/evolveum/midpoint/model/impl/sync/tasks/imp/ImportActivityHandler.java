/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

@Component
public class ImportActivityHandler
        extends ModelActivityHandler<ImportWorkDefinition, ImportActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_IMPORT_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ImportWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_IMPORT,
                ImportWorkDefinition.class, ImportWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ImportWorkDefinitionType.COMPLEX_TYPE,
                ImportWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<ImportWorkDefinition, ImportActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ImportWorkDefinition, ImportActivityHandler> context,
            @NotNull OperationResult result) {
        return new ImportActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "import";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
