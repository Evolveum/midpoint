/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.task;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
@Component
public class MultiPropagationActivityHandler implements ActivityHandler<MultiPropagationWorkDefinition, MultiPropagationActivityHandler> {

    public static final String LEGACY_HANDLER_URI = SchemaConstants.NS_PROVISIONING_TASK + "/propagation/multi-handler-3";
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();

    @Autowired WorkDefinitionFactory workDefinitionFactory;
    @Autowired ActivityHandlerRegistry handlerRegistry;
    @Autowired ProvisioningService provisioningService;
    @Autowired ShadowsFacade shadowsFacade;

    @PostConstruct
    public void register() {
        handlerRegistry.register(MultiPropagationWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                MultiPropagationWorkDefinition.class, MultiPropagationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(MultiPropagationWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                MultiPropagationWorkDefinition.class);
    }

    @Override
    public AbstractActivityExecution<MultiPropagationWorkDefinition, MultiPropagationActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<MultiPropagationWorkDefinition, MultiPropagationActivityHandler> context,
            @NotNull OperationResult result) {
        return new MultiPropagationActivityExecution(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "multi-propagation";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
