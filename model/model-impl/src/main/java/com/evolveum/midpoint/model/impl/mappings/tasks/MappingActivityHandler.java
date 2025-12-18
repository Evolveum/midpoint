/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class MappingActivityHandler
        implements ActivityHandler<MappingWorkDefinition, MappingActivityHandler> {

    private final ActivityHandlerRegistry activityHandlerRegistry;

    public MappingActivityHandler(ActivityHandlerRegistry activityHandlerRegistry) {
        this.activityHandlerRegistry = activityHandlerRegistry;
    }

    @PostConstruct
    public void init() {
        this.activityHandlerRegistry.register(
                MappingWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_MAPPING,
                MappingWorkDefinition.class,
                this::workDefFactory,
                this
        );
    }

    @Override
    public MappingActivityRun createActivityRun(
            @NotNull ActivityRunInstantiationContext<MappingWorkDefinition, MappingActivityHandler> ctx,
            @NotNull OperationResult result) {
        return new MappingActivityRun(ctx);
    }

    private MappingWorkDefinition workDefFactory(WorkDefinitionFactory.WorkDefinitionInfo info) {
        return new MappingWorkDefinition(info);
    }
}
