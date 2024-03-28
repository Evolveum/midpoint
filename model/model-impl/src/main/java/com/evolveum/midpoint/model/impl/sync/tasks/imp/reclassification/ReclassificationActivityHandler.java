/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.imp.reclassification;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowReclassificationWorkDefinitionType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@Component
public class ReclassificationActivityHandler
        extends ModelActivityHandler<ReclassificationWorkDefinition, ReclassificationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_SHADOW_RECLASSIFICATION_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ShadowReclassificationWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_SHADOW_RECLASSIFICATION,
                ReclassificationWorkDefinition.class, ReclassificationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ShadowReclassificationWorkDefinitionType.COMPLEX_TYPE,
                ReclassificationWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<ReclassificationWorkDefinition, ReclassificationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ReclassificationWorkDefinition, ReclassificationActivityHandler> context,
            @NotNull OperationResult result) {
        return new ReclassificationActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "shadow-reclassification";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
