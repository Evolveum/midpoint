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
public class GenerateObjectClassArtifactActivityHandler
        extends ModelActivityHandler<GenerateObjectClassArtifactActivityHandler.WorkDefinition, GenerateObjectClassArtifactActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(GenerateObjectClassArtifactActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ConnDevGenerateObjectClassArtifactDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_GENERATE_CONNECTOR_OBJECT_CLASS_ARTIFACT,
                GenerateObjectClassArtifactActivityHandler.WorkDefinition.class,
                GenerateObjectClassArtifactActivityHandler.WorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ConnDevGenerateObjectClassArtifactDefinitionType.COMPLEX_TYPE, GenerateObjectClassArtifactActivityHandler.WorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ConnDevCreateConnectorWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<GenerateObjectClassArtifactActivityHandler.WorkDefinition, GenerateObjectClassArtifactActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<GenerateObjectClassArtifactActivityHandler.WorkDefinition, GenerateObjectClassArtifactActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "create-connector";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }


    public static class WorkDefinition extends AbstractWorkDefinition {

        final String connectorDevelopmentOid;
        final String templateUrl;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ConnDevCreateConnectorWorkDefinitionType) info.getBean();
            connectorDevelopmentOid = MiscUtil.configNonNull(Referencable.getOid(typedDefinition.getConnectorDevelopmentRef()), "No resource OID specified");
            templateUrl  = MiscUtil.configNonNull(typedDefinition.getBaseTemplateUrl(), "No Base template URL specified");
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException {
            return AffectedObjectsInformation.ObjectSet.repository(new BasicObjectSetType()
                    .objectRef(connectorDevelopmentOid, ConnectorDevelopmentType.COMPLEX_TYPE)
            );
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {

        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            GenerateObjectClassArtifactActivityHandler.WorkDefinition,
            GenerateObjectClassArtifactActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<GenerateObjectClassArtifactActivityHandler.WorkDefinition, GenerateObjectClassArtifactActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            //var developmentUri = getWorkDefinition().templateUrl;

            var connDev = beans.repositoryService.getObject(ConnectorDevelopmentType.class, getWorkDefinition().connectorDevelopmentOid, null, result);
            var connDef = connDev.asObjectable().getConnector();

            var targetDir = connDef.getGroupId() + "." + connDef.getArtifactId() + "." + connDef.getVersion();

            // Download template
            var template = beans.connectorService.downloadConnector(getWorkDefinition().templateUrl, targetDir + ".template" ,result);

            // Unpack template
            var editable = template.unpack(targetDir, result);

            template.remove();
            // Rename template to connector
            editable.asEditable().renameBundle(connDef.getGroupId(), connDef.getArtifactId(), connDef.getVersion());

            // Install template
            var lookups = editable.install(result);

            var lookup = lookups.get(0);

            var query = PrismContext.get().queryFor(ConnectorType.class)
                    .item(ConnectorType.F_CONNECTOR_BUNDLE).eq(lookup.getConnectorBundle())
                    .and().item(ConnectorType.F_CONNECTOR_TYPE).eq(lookup.getConnectorType())
                    .and().item(ConnectorType.F_CONNECTOR_VERSION).eq(lookup.getConnectorVersion())
                    .build();


            var connectors = beans.modelService.searchObjects(ConnectorType.class, query, null, task, result);
            var connector = connectors.get(0);

            var state = getActivityState();

            // FIXME: Write connectorRef + connectorDirectory to ConnectorDevelopmentType

            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,new ConnDevCreateConnectorResultType()
                    .connectorRef(connector.getOid(), ConnectorType.COMPLEX_TYPE));
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
