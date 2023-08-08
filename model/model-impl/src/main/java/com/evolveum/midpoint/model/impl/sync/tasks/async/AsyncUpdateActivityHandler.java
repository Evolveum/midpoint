/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.async;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Activity handler for controlled processing of asynchronous updates.
 */
@Component
public class AsyncUpdateActivityHandler
        extends ModelActivityHandler<AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_ASYNC_UPDATE_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                AsyncUpdateWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_ASYNCHRONOUS_UPDATE,
                AsyncUpdateWorkDefinition.class, AsyncUpdateWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                AsyncUpdateWorkDefinitionType.COMPLEX_TYPE,
                AsyncUpdateWorkDefinition.class);
    }

    @Override
    public @NotNull AbstractActivityRun<AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler, AbstractActivityWorkStateType> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler> context,
            @NotNull OperationResult result) {
        return new AsyncUpdateActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "async-update";
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.perpetual();
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
