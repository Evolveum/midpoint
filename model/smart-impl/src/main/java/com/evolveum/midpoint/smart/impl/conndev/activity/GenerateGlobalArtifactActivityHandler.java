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
import com.evolveum.midpoint.schema.util.AiUtil;
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
public class GenerateGlobalArtifactActivityHandler
        extends AbstractConnDevActivityHandler<GenerateGlobalArtifactActivityHandler.WorkDefinition, GenerateGlobalArtifactActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(GenerateGlobalArtifactActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public GenerateGlobalArtifactActivityHandler() {
        super(
                ConnDevGenerateGlobalArtifactDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_GENERATE_CONNECTOR_GLOBAL_ARTIFACT,
                ConnDevGenerateGlobalArtifactWorkStateType.COMPLEX_TYPE,
                GenerateGlobalArtifactActivityHandler.WorkDefinition.class,
                GenerateGlobalArtifactActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<GenerateGlobalArtifactActivityHandler.WorkDefinition, GenerateGlobalArtifactActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<GenerateGlobalArtifactActivityHandler.WorkDefinition, GenerateGlobalArtifactActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition {

        final String connectorDevelopmentOid;
        final ConnDevArtifactType artifactSpec;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ConnDevGenerateGlobalArtifactDefinitionType) info.getBean();
            connectorDevelopmentOid = MiscUtil.configNonNull(Referencable.getOid(typedDefinition.getConnectorDevelopmentRef()), "No resource OID specified");
            artifactSpec = MiscUtil.configNonNull(typedDefinition.getArtifact(), "Artifact must be specified");
        }

    }

    public static class MyActivityRun
            extends LocalActivityRun<
            GenerateGlobalArtifactActivityHandler.WorkDefinition,
            GenerateGlobalArtifactActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<GenerateGlobalArtifactActivityHandler.WorkDefinition, GenerateGlobalArtifactActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            //var developmentUri = getWorkDefinition().templateUrl;

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            ConnDevArtifactType script = backend.generateGlobalArtifact(getWorkDefinition().artifactSpec);
            if (script.getContent() != null) {
                // Mark as AI
                AiUtil.markAsAiProvided(script.asPrismContainerValue().findItem(ConnDevArtifactType.F_CONTENT).getValue());
            }
            var state = getActivityState();
            // FIXME: Write connectorRef + connectorDirectory to ConnectorDevelopmentType

            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,new ConnDevGenerateGlobalArtifactResultType()
                    .artifact(script));
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
