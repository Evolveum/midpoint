package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

@Component
public class CreateConnectorActivityHandler
        extends AbstractConnDevActivityHandler<CreateConnectorActivityHandler.WorkDefinition, CreateConnectorActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(CreateConnectorActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public CreateConnectorActivityHandler() {
        super(
                ConnDevCreateConnectorWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_CREATE_CONNECTOR,
                ConnDevCreateConnectorWorkStateType.COMPLEX_TYPE,
                CreateConnectorActivityHandler.WorkDefinition.class,
                CreateConnectorActivityHandler.WorkDefinition::new
                );
    }

    @Override
    public AbstractActivityRun<CreateConnectorActivityHandler.WorkDefinition, CreateConnectorActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<CreateConnectorActivityHandler.WorkDefinition, CreateConnectorActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition {

        final String templateUrl;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ConnDevCreateConnectorWorkDefinitionType) info.getBean();
            templateUrl  = MiscUtil.configNonNull(typedDefinition.getBaseTemplateUrl(), "No Base template URL specified");
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            CreateConnectorActivityHandler.WorkDefinition,
            CreateConnectorActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<CreateConnectorActivityHandler.WorkDefinition, CreateConnectorActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            //var developmentUri = getWorkDefinition().templateUrl;


            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);

            var connDev = backend.developmentObject();
            var connDef = connDev.getConnector();

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

            backend.linkEditableConnector(targetDir, connector.getOid());

            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,new ConnDevCreateConnectorResultType()
                    .connectorRef(connector.getOid(), ConnectorType.COMPLEX_TYPE));
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
