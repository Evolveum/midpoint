/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.exception.CommonException;

import java.util.Set;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Discovers HTTP endpoints of an object class.
 */
@Component
public class DiscoverObjectClassEndpointsActivityHandler
        extends AbstractConnDevActivityHandler<DiscoverObjectClassEndpointsActivityHandler.WorkDefinition, DiscoverObjectClassEndpointsActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DiscoverObjectClassEndpointsActivityHandler.class);

    public DiscoverObjectClassEndpointsActivityHandler() {
        super(
                ConnDevDiscoverObjectClassEndpointsDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ENDPOINTS,
                ConnDevDiscoverObjectClassEndpointsWorkStateType.COMPLEX_TYPE,
                DiscoverObjectClassEndpointsActivityHandler.WorkDefinition.class,
                DiscoverObjectClassEndpointsActivityHandler.WorkDefinition::new);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ConnDevDiscoverObjectClassEndpointsDefinitionType.COMPLEX_TYPE,
                DiscoverObjectClassEndpointsActivityHandler.WorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<DiscoverObjectClassEndpointsActivityHandler.WorkDefinition, DiscoverObjectClassEndpointsActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<DiscoverObjectClassEndpointsActivityHandler.WorkDefinition, DiscoverObjectClassEndpointsActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevDiscoverObjectClassEndpointsDefinitionType> {
        final String objectClass;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ConnDevDiscoverObjectClassEndpointsDefinitionType) info.getBean();
            this.objectClass = typedDefinition.getObjectClass();
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            DiscoverObjectClassEndpointsActivityHandler.WorkDefinition,
            DiscoverObjectClassEndpointsActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                @NotNull ActivityRunInstantiationContext<DiscoverObjectClassEndpointsActivityHandler.WorkDefinition, DiscoverObjectClassEndpointsActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            String objectClass = getWorkDefinition().objectClass;
            String connectorDevelopmentOid = getWorkDefinition().connectorDevelopmentOid;

            LOGGER.info("Discovering endpoints for object class '{}' in task {}", objectClass, getRunningTask().getName());

            try {
                var backend = ConnectorDevelopmentBackend.backendFor(connectorDevelopmentOid, getRunningTask(), result);
                backend.ensureDocumentationIsProcessed();
                backend.ensureObjectClass(objectClass);

                var endpoints = backend.discoverObjectClassEndpoints(objectClass);
                backend.updateApplicationObjectClassEndpoints(objectClass, endpoints);
            } catch (CommonException | RuntimeException e) {
                suspendSiblings(connectorDevelopmentOid, Set.of(WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ATTRIBUTES), this, result);
                throw e;
            }

            LOGGER.info("Successfully discovered endpoints for object class '{}'", objectClass);
            return ActivityRunResult.success();
        }
    }
}
