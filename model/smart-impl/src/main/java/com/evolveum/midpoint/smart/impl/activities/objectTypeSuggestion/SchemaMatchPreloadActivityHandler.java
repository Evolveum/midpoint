/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.objectTypeSuggestion;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.smart.impl.SchemaMatchService;
import com.evolveum.midpoint.task.api.TaskManager;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaMatchPreloadWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class SchemaMatchPreloadActivityHandler
        extends ModelActivityHandler<SchemaMatchPreloadWorkDefinition, SchemaMatchPreloadActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @Autowired SchemaMatchService schemaMatchService;
    @Autowired TaskManager taskManager;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                SchemaMatchPreloadWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_SCHEMA_MATCH_PRELOAD,
                SchemaMatchPreloadWorkDefinition.class,
                SchemaMatchPreloadWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                SchemaMatchPreloadWorkDefinitionType.COMPLEX_TYPE,
                SchemaMatchPreloadWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal();
    }

    @Override
    public AbstractActivityRun<SchemaMatchPreloadWorkDefinition, SchemaMatchPreloadActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<SchemaMatchPreloadWorkDefinition, SchemaMatchPreloadActivityHandler> context,
            @NotNull OperationResult result) {
        return new SchemaMatchPreloadActivityRun(context, schemaMatchService, taskManager);
    }

    @Override
    public String getIdentifierPrefix() {
        return "schema-match-preload";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
