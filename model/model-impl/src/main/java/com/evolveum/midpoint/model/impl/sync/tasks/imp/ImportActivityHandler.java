/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.model.api.ModelPublicConstants;

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

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/import/handler-3";
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_IMPORT_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(ImportWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                ImportWorkDefinition.class, ImportWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(ImportWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
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
