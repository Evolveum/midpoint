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

import com.evolveum.midpoint.prism.path.ItemName;

import java.util.Set;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Discovers attributes of an object class.
 */
@Component
public class DiscoverObjectClassAttributesActivityHandler
        extends AbstractConnDevActivityHandler<DiscoverObjectClassAttributesActivityHandler.WorkDefinition, DiscoverObjectClassAttributesActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DiscoverObjectClassAttributesActivityHandler.class);

    public DiscoverObjectClassAttributesActivityHandler() {
        super(
                ConnDevDiscoverObjectClassAttributesDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ATTRIBUTES,
                ConnDevDiscoverObjectClassAttributesWorkStateType.COMPLEX_TYPE,
                DiscoverObjectClassAttributesActivityHandler.WorkDefinition.class,
                DiscoverObjectClassAttributesActivityHandler.WorkDefinition::new);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ConnDevDiscoverObjectClassAttributesDefinitionType.COMPLEX_TYPE,
                DiscoverObjectClassAttributesActivityHandler.WorkDefinition.class);
    }

    @Override
    public @NotNull Set<ItemName> getSiblingActivityTypes() {
        return Set.of(WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ENDPOINTS);
    }

    @Override
    public AbstractActivityRun<WorkDefinition, DiscoverObjectClassAttributesActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<WorkDefinition, DiscoverObjectClassAttributesActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevDiscoverObjectClassAttributesDefinitionType> {
        final String objectClass;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ConnDevDiscoverObjectClassAttributesDefinitionType) info.getBean();
            this.objectClass = typedDefinition.getObjectClass();
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            DiscoverObjectClassAttributesActivityHandler.WorkDefinition,
            DiscoverObjectClassAttributesActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                @NotNull ActivityRunInstantiationContext<DiscoverObjectClassAttributesActivityHandler.WorkDefinition, DiscoverObjectClassAttributesActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            String objectClass = getWorkDefinition().objectClass;
            String connectorDevelopmentOid = getWorkDefinition().connectorDevelopmentOid;

            LOGGER.info("Discovering attributes for object class '{}' in task {}", objectClass, getRunningTask().getName());
            try {
                var backend = ConnectorDevelopmentBackend.backendFor(connectorDevelopmentOid, getRunningTask(), result);
                backend.ensureDocumentationIsProcessed();
                backend.ensureObjectClass(objectClass);

                var attributes = backend.discoverObjectClassAttributes(objectClass);
                backend.updateConnectorObjectClassAttributes(objectClass, attributes);
            } catch (CommonException | RuntimeException e) {
                getActivityHandler().suspendSiblings(connectorDevelopmentOid, this, result);
                throw e;
            }
            LOGGER.info("Successfully discovered attributes for object class '{}'", objectClass);

            return getActivityHandler().waitForSiblingByPolling(connectorDevelopmentOid, this, result);
        }
    }
}
