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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity handler for the simulation of inbound mappings
 */
@Component
public class InboundMappingsSimulationActivityHandler implements
        ActivityHandler<MappingSimulationWorkDef<InboundMappingType>, InboundMappingsSimulationActivityHandler> {

    private final ActivityHandlerRegistry activityHandlerRegistry;
    private final ProvisioningService provisioningService;
    private final CorrelationService correlationService;
    private final SystemObjectCache systemObjectCache;
    private final PrismContext prismContext;

    public InboundMappingsSimulationActivityHandler(ActivityHandlerRegistry activityHandlerRegistry,
            ProvisioningService provisioningService, CorrelationService correlationService,
            SystemObjectCache systemObjectCache, PrismContext prismContext) {
        this.activityHandlerRegistry = activityHandlerRegistry;
        this.provisioningService = provisioningService;
        this.correlationService = correlationService;
        this.systemObjectCache = systemObjectCache;
        this.prismContext = prismContext;
    }

    @PostConstruct
    public void init() {
        this.activityHandlerRegistry.register(
                InboundMappingsSimulationWorkDefType.COMPLEX_TYPE,
                WorkDefinitionsType.F_INBOUND_MAPPINGS_SIMULATION,
                InboundMappingSimulationWorkDef.class,
                MappingSimulationWorkDef::of,
                this
        );
    }

    @Override
    public IterativeActivityRun<? extends ObjectType, MappingSimulationWorkDef<InboundMappingType>,
            InboundMappingsSimulationActivityHandler, AbstractActivityWorkStateType> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MappingSimulationWorkDef<InboundMappingType>,
                    InboundMappingsSimulationActivityHandler> ctx,
            @NotNull OperationResult result) {
        return new InboundMappingsSimulationActivityRun(ctx, this.provisioningService, this.correlationService,
                this.systemObjectCache, this.prismContext);
    }

}
