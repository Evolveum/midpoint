package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnDevArtifactValidationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class GenerateConnectorArtifactActivityHandler
        extends AbstractConnDevActivityHandler<GenerateConnectorArtifactActivityHandler.WorkDefinition, GenerateConnectorArtifactActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(GenerateConnectorArtifactActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public GenerateConnectorArtifactActivityHandler() {
        super(
                ConnDevGenerateArtifactDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_GENERATE_CONNECTOR_ARTIFACT,
                ConnDevGenerateArtifactWorkStateType.COMPLEX_TYPE,
                GenerateConnectorArtifactActivityHandler.WorkDefinition.class,
                GenerateConnectorArtifactActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<GenerateConnectorArtifactActivityHandler.WorkDefinition, GenerateConnectorArtifactActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<GenerateConnectorArtifactActivityHandler.WorkDefinition, GenerateConnectorArtifactActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevGenerateArtifactDefinitionType> {

        final String connectorDevelopmentOid;
        final ConnDevArtifactType artifactSpec;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            connectorDevelopmentOid = MiscUtil.configNonNull(Referencable.getOid(typedDefinition.getConnectorDevelopmentRef()), "No resource OID specified");
            artifactSpec = MiscUtil.configNonNull(typedDefinition.getArtifact(), "Artifact must be specified");
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            GenerateConnectorArtifactActivityHandler.WorkDefinition,
            GenerateConnectorArtifactActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        /** How many times a script is (re)generated before handing the last attempt to the wizard. */
        private static final int MAX_VALIDATION_ATTEMPTS = 3;

        MyActivityRun(
                ActivityRunInstantiationContext<GenerateConnectorArtifactActivityHandler.WorkDefinition, GenerateConnectorArtifactActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            //var developmentUri = getWorkDefinition().templateUrl;

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            backend.ensureDocumentationIsProcessed();
            var resultObj = new ConnDevGenerateArtifactResultType();
            var skipCache = Boolean.TRUE.equals(getWorkDefinition().typedDefinition.getSkipCache());
            var state = getActivityState();

            ConnDevGenerateArtifactDefinitionType currentDefinition = getWorkDefinition().typedDefinition;

            ConnDevArtifactType script = null;
            for (int attempt = 1; attempt <= MAX_VALIDATION_ATTEMPTS; attempt++) {
                script = backend.generateArtifact(currentDefinition, skipCache);
                if (script == null || script.getContent() == null) {
                    break;
                }

                ConnDevArtifactValidationResult validation = backend.validateArtifact(script);
                if (validation.ok() || attempt == MAX_VALIDATION_ATTEMPTS) {
                    break;
                }
                currentDefinition = retryDefinition(currentDefinition, script, validation);
            }

            if (script != null) {
                if (script.getContent() != null) {
                    // Mark as AI
                    SmartMetadataUtil.markAsAiProvided(script.asPrismContainerValue().findItem(ConnDevArtifactType.F_CONTENT).getValue());
                }
                resultObj.artifact(script);
            }
            // FIXME: Write connectorRef + connectorDirectory to ConnectorDevelopmentType
            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT, resultObj);
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }

        /** Same definition, with the failed script's content and validation errors attached for repair. */
        private static ConnDevGenerateArtifactDefinitionType retryDefinition(
                ConnDevGenerateArtifactDefinitionType previous, ConnDevArtifactType failedScript,
                ConnDevArtifactValidationResult validation) {
            ConnDevGenerateArtifactDefinitionType retry = previous.clone();
            retry.getArtifact().setContent(failedScript.getContent());
            retry.getArtifact().setFilename(failedScript.getFilename());
            retry.getMidpointError().clear();
            for (var error : validation.errors()) {
                retry.getMidpointError().add(formatValidationError(error, failedScript.getFilename()));
            }
            return retry;
        }

        private static String formatValidationError(ConnDevArtifactValidationResult.Error error, String fallbackFileName) {
            String source = error.source() != null ? error.source() : fallbackFileName;
            String location = error.line() != null ? source + ":" + error.line() : source;
            return location + " - " + error.message();
        }
    }
}
