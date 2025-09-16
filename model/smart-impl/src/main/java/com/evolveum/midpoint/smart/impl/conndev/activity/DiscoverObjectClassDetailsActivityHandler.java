package com.evolveum.midpoint.smart.impl.conndev.activity;

import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class DiscoverObjectClassDetailsActivityHandler
        extends AbstractConnDevActivityHandler<DiscoverObjectClassDetailsActivityHandler.WorkDefinition, DiscoverObjectClassDetailsActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DiscoverObjectClassDetailsActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public DiscoverObjectClassDetailsActivityHandler() {
        super(
                ConnDevDiscoverObjectClassDetailsDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_INFORMATION,
                ConnDevDiscoverObjectClassDetailsWorkStateType.COMPLEX_TYPE,
                DiscoverObjectClassDetailsActivityHandler.WorkDefinition.class,
                DiscoverObjectClassDetailsActivityHandler.WorkDefinition::new);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ConnDevCreateConnectorWorkDefinitionType.COMPLEX_TYPE, DiscoverObjectClassDetailsActivityHandler.WorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<DiscoverObjectClassDetailsActivityHandler.WorkDefinition, DiscoverObjectClassDetailsActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<DiscoverObjectClassDetailsActivityHandler.WorkDefinition, DiscoverObjectClassDetailsActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevDiscoverObjectClassDetailsDefinitionType> {

        final String objectClass;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ConnDevDiscoverObjectClassDetailsDefinitionType) info.getBean();
            this.objectClass = typedDefinition.getObjectClass();
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            DiscoverObjectClassDetailsActivityHandler.WorkDefinition,
            DiscoverObjectClassDetailsActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<DiscoverObjectClassDetailsActivityHandler.WorkDefinition, DiscoverObjectClassDetailsActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            //var developmentUri = getWorkDefinition().templateUrl;

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);

            var objectClass = getWorkDefinition().objectClass;

            backend.ensureObjectClass(objectClass);
            var endpoints = backend.discoverObjectClassEndpoints(objectClass);
            backend.updateApplicationObjectClassEndpoints(objectClass, endpoints);

            var attributes = backend.discoverObjectClassAttributes(objectClass);
            backend.updateConnectorObjectClassAttributes(objectClass, attributes);

            var state = getActivityState();

            // FIXME: Write connectorRef + connectorDirectory to ConnectorDevelopmentType

            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,
                    new ConnDevDiscoverObjectClassDetailsResultType());
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
