package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
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
public class FixObjectClassActivityHandler
        extends AbstractConnDevActivityHandler<FixObjectClassActivityHandler.WorkDefinition, FixObjectClassActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(FixObjectClassActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public FixObjectClassActivityHandler() {
        super(
                ConnDevFixObjectClassDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_FIX_OBJECT_CLASS,
                ConnDevFixObjectClassWorkStateType.COMPLEX_TYPE,
                FixObjectClassActivityHandler.WorkDefinition.class,
                FixObjectClassActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<FixObjectClassActivityHandler.WorkDefinition, FixObjectClassActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<FixObjectClassActivityHandler.WorkDefinition, FixObjectClassActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevFixObjectClassDefinitionType> {

        final String connectorDevelopmentOid;
        final String objectClass;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            connectorDevelopmentOid = MiscUtil.configNonNull(Referencable.getOid(typedDefinition.getConnectorDevelopmentRef()), "No resource OID specified");
            objectClass = MiscUtil.configNonNull(typedDefinition.getObjectClass(), "Object class must be specified");
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            FixObjectClassActivityHandler.WorkDefinition,
            FixObjectClassActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<FixObjectClassActivityHandler.WorkDefinition, FixObjectClassActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            backend.ensureDocumentationIsProcessed();
            var skipCache = Boolean.TRUE.equals(getWorkDefinition().typedDefinition.getSkipCache());
            var resultObj = backend.fixObjectClass(
                    getWorkDefinition().objectClass,
                    getWorkDefinition().typedDefinition.getMidpointError(),
                    getWorkDefinition().typedDefinition.getArtifact(),
                    skipCache);
            for (var artifact : resultObj.getArtifact()) {
                if (artifact.getContent() != null) {
                    // Mark as AI
                    SmartMetadataUtil.markAsAiProvided(artifact.asPrismContainerValue().findItem(ConnDevArtifactType.F_CONTENT).getValue());
                }
            }
            var state = getActivityState();
            state.setWorkStateItemRealValues(ConnDevFixObjectClassWorkStateType.F_RESULT, resultObj);
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
