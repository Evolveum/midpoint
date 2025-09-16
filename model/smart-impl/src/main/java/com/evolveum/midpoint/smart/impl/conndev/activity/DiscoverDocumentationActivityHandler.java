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

@Component
public class DiscoverDocumentationActivityHandler
        extends AbstractConnDevActivityHandler<DiscoverDocumentationActivityHandler.WorkDefinition, DiscoverDocumentationActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DiscoverDocumentationActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public DiscoverDocumentationActivityHandler() {
        super(
                ConnDevDiscoverDocumentationWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_DISCOVER_DOCUMENTATION,
                ConnDevDiscoverDocumentationWorkStateType.COMPLEX_TYPE,
                DiscoverDocumentationActivityHandler.WorkDefinition.class,
                DiscoverDocumentationActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<DiscoverDocumentationActivityHandler.WorkDefinition, DiscoverDocumentationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<DiscoverDocumentationActivityHandler.WorkDefinition, DiscoverDocumentationActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevDiscoverDocumentationWorkDefinitionType> {
        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            DiscoverDocumentationActivityHandler.WorkDefinition,
            DiscoverDocumentationActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<DiscoverDocumentationActivityHandler.WorkDefinition, DiscoverDocumentationActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            var documentation = backend.discoverDocumentation();

            var ret = new ConnDevDiscoverDocumentationResultType();
            documentation.stream().map(ConnDevDocumentationSourceType::clone).forEach(ret::documentation);

            var state = getActivityState();
            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,ret);
            state.flushPendingTaskModifications(result);

            return ActivityRunResult.success();
        }
    }
}
