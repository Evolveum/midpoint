package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscoverObjectClassInformationActivityHandler
        extends AbstractConnDevActivityHandler<DiscoverObjectClassInformationActivityHandler.WorkDefinition, DiscoverObjectClassInformationActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DiscoverObjectClassInformationActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public DiscoverObjectClassInformationActivityHandler() {
        super(
                ConnDevDiscoverObjectClassInformationDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_DETAILS,
                ConnDevDiscoverObjectClassInformationWorkStateType.COMPLEX_TYPE,
                DiscoverObjectClassInformationActivityHandler.WorkDefinition.class,
                DiscoverObjectClassInformationActivityHandler.WorkDefinition::new);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ConnDevCreateConnectorWorkDefinitionType.COMPLEX_TYPE, DiscoverObjectClassInformationActivityHandler.WorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<DiscoverObjectClassInformationActivityHandler.WorkDefinition, DiscoverObjectClassInformationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<DiscoverObjectClassInformationActivityHandler.WorkDefinition, DiscoverObjectClassInformationActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition {

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ConnDevDiscoverObjectClassInformationDefinitionType) info.getBean();
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            DiscoverObjectClassInformationActivityHandler.WorkDefinition,
            DiscoverObjectClassInformationActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<DiscoverObjectClassInformationActivityHandler.WorkDefinition, DiscoverObjectClassInformationActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            //var developmentUri = getWorkDefinition().templateUrl;

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);

            var connectorDiscovered =  backend.discoverObjectClassesUsingConnector();

            var discovered = backend.discoverObjectClassesUsingDocumentation(connectorDiscovered);

            backend.updateApplicationObjectClasses(discovered);


            var state = getActivityState();

            // FIXME: Write connectorRef + connectorDirectory to ConnectorDevelopmentType

            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,
                    new ConnDevDiscoverObjectClassInformationResultType());
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
