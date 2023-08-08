/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiPropagationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

@Component
public class MultiPropagationActivityHandler
        implements ActivityHandler<MultiPropagationWorkDefinition, MultiPropagationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();

    @Autowired ActivityHandlerRegistry handlerRegistry;
    @Autowired ShadowsFacade shadowsFacade;

    @PostConstruct
    public void register() {
        handlerRegistry.register(MultiPropagationWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_MULTI_PROPAGATION,
                MultiPropagationWorkDefinition.class, MultiPropagationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(MultiPropagationWorkDefinitionType.COMPLEX_TYPE,
                MultiPropagationWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<MultiPropagationWorkDefinition, MultiPropagationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MultiPropagationWorkDefinition, MultiPropagationActivityHandler> context,
            @NotNull OperationResult result) {
        return new MultiPropagationActivityRun(context);
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
