/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LiveSyncWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LiveSyncWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Task handler for controlled processing of asynchronous updates.
 */
@Component
public class LiveSyncActivityHandler
        extends ModelActivityHandler<LiveSyncWorkDefinition, LiveSyncActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/live-sync/handler-3";
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(LiveSyncWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                LiveSyncWorkDefinition.class, LiveSyncWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(LiveSyncWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                LiveSyncWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<LiveSyncWorkDefinition, LiveSyncActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<LiveSyncWorkDefinition, LiveSyncActivityHandler> context,
            @NotNull OperationResult result) {
        return new LiveSyncActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "live-sync";
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.perpetual(LiveSyncWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
