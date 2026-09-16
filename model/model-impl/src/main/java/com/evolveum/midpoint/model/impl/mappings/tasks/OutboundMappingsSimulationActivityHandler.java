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
import com.evolveum.midpoint.model.impl.lens.projector.projection.outbounds.OutboundMappingProcessing;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class OutboundMappingsSimulationActivityHandler implements
        ActivityHandler<MappingSimulationWorkDef<OutboundMappingType>, OutboundMappingsSimulationActivityHandler> {

    private final ActivityHandlerRegistry activityHandlerRegistry;
    private final ProvisioningService provisioningService;
    private final CorrelationService correlationService;
    private final OutboundMappingProcessing outboundProcessing;
    private final SystemObjectCache systemObjectCache;

    public OutboundMappingsSimulationActivityHandler(ActivityHandlerRegistry activityHandlerRegistry,
            ProvisioningService provisioningService, CorrelationService correlationService,
            OutboundMappingProcessing outboundProcessing,
            SystemObjectCache systemObjectCache) {
        this.activityHandlerRegistry = activityHandlerRegistry;
        this.provisioningService = provisioningService;
        this.correlationService = correlationService;
        this.outboundProcessing = outboundProcessing;
        this.systemObjectCache = systemObjectCache;
    }

    @PostConstruct
    public void init() {
        this.activityHandlerRegistry.register(
                OutboundMappingsSimulationWorkDefType.COMPLEX_TYPE,
                WorkDefinitionsType.F_OUTBOUND_MAPPINGS_SIMULATION,
                OutboundMappingSimulationWorkDef.class,
                MappingSimulationWorkDef::of,
                this
        );
    }

    @Override
    public IterativeActivityRun<? extends ObjectType, MappingSimulationWorkDef<OutboundMappingType>, OutboundMappingsSimulationActivityHandler,
            AbstractActivityWorkStateType> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MappingSimulationWorkDef<OutboundMappingType>, OutboundMappingsSimulationActivityHandler> ctx,
            @NotNull OperationResult result) {
        return new OutboundMappingSimulationActivityRun(ctx, this.provisioningService, this.correlationService,
                this.systemObjectCache, this.outboundProcessing);
    }
}
