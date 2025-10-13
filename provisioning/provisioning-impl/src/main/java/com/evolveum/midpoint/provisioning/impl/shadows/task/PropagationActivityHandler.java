/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropagationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Activity handler for the propagation activity.
 */
@Component
public class PropagationActivityHandler implements ActivityHandler<PropagationWorkDefinition, PropagationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();

    @Autowired ActivityHandlerRegistry handlerRegistry;
    @Autowired ProvisioningService provisioningService;
    @Autowired ShadowsFacade shadowsFacade;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                PropagationWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_PROPAGATION,
                PropagationWorkDefinition.class, PropagationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                PropagationWorkDefinitionType.COMPLEX_TYPE, PropagationWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<PropagationWorkDefinition, PropagationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<PropagationWorkDefinition, PropagationActivityHandler> context,
            @NotNull OperationResult result) {
        return new PropagationActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "propagation";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
