/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class MappingActivityHandler implements ActivityHandler<MappingWorkDefinition, MappingActivityHandler> {

    private final ActivityHandlerRegistry activityHandlerRegistry;
    private final ProvisioningService provisioningService;
    private final CorrelationService correlationService;
    private final SystemObjectCache systemObjectCache;

    public MappingActivityHandler(ActivityHandlerRegistry activityHandlerRegistry,
            ProvisioningService provisioningService, CorrelationService correlationService,
            SystemObjectCache systemObjectCache) {
        this.activityHandlerRegistry = activityHandlerRegistry;
        this.provisioningService = provisioningService;
        this.correlationService = correlationService;
        this.systemObjectCache = systemObjectCache;
    }

    @PostConstruct
    public void init() {
        this.activityHandlerRegistry.register(
                MappingWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_MAPPINGS,
                MappingWorkDefinition.class,
                this::workDefFactory,
                this
        );
    }

    @Override
    public MappingActivityRun createActivityRun(
            @NotNull ActivityRunInstantiationContext<MappingWorkDefinition, MappingActivityHandler> ctx,
            @NotNull OperationResult result) {
        return new MappingActivityRun(ctx, this.provisioningService, this.correlationService, this.systemObjectCache);
    }

    private MappingWorkDefinition workDefFactory(WorkDefinitionFactory.WorkDefinitionInfo info) {
        return new MappingWorkDefinition(info);
    }
}
