/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LiveSyncWorkDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Task handler for controlled processing of asynchronous updates.
 */
@Component
public class LiveSyncActivityHandler
        extends ModelActivityHandler<LiveSyncWorkDefinition, LiveSyncActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/live-sync/handler-3";
    private static final Trace LOGGER = TraceManager.getTrace(LiveSyncActivityHandler.class);
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value(); // TODO

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
    public @NotNull LiveSyncActivityExecution createExecution(
            @NotNull ExecutionInstantiationContext<LiveSyncWorkDefinition, LiveSyncActivityHandler> context,
            @NotNull OperationResult result) {
        return new LiveSyncActivityExecution(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "live-sync";
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.perpetual(); // todo custom type
    }
}
