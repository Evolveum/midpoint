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
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DiscoverConnectivityEndpointActivityHandler
        extends AbstractConnDevActivityHandler<DiscoverConnectivityEndpointActivityHandler.WorkDefinition, DiscoverConnectivityEndpointActivityHandler> {

    public DiscoverConnectivityEndpointActivityHandler() {
        super(
                ConnDevDiscoverConnectivityEndpointWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_DISCOVER_CONNECTIVITY_ENDPOINT,
                ConnDevDiscoverConnectivityEndpointWorkStateType.COMPLEX_TYPE,
                DiscoverConnectivityEndpointActivityHandler.WorkDefinition.class,
                DiscoverConnectivityEndpointActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<WorkDefinition, DiscoverConnectivityEndpointActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<WorkDefinition, DiscoverConnectivityEndpointActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevDiscoverConnectivityEndpointWorkDefinitionType> {

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<WorkDefinition, DiscoverConnectivityEndpointActivityHandler, FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<WorkDefinition, DiscoverConnectivityEndpointActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var backend = ConnectorDevelopmentBackend.backendFor(
                    getWorkDefinition().connectorDevelopmentOid, getRunningTask(), result);
            var skipCache = Boolean.TRUE.equals(getWorkDefinition().typedDefinition.getSkipCache());
            var endpoints = backend.discoverConnectivityEndpoints(skipCache);
            backend.populateConnectivityEndpoints(endpoints);

            var state = getActivityState();
            state.setWorkStateItemRealValues(
                    FocusTypeSuggestionWorkStateType.F_RESULT,
                    new ConnDevDiscoverConnectivityEndpointResultType());
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
