package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.reports.ReportSupportUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class ExportConnectorActivityHandler
        extends AbstractConnDevActivityHandler<ExportConnectorActivityHandler.WorkDefinition, ExportConnectorActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ExportConnectorActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    private static final String DEVELOPMENT_MODE_PROPERTY = "developmentMode";
    private static final String CONFIGURATION_OVERRIDE_FILE = "configurationOverride.properties";

    public ExportConnectorActivityHandler() {
        super(
                ConnDevExportConnectorWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_EXPORT_CONNECTOR,
                ConnDevExportConnectorWorkStateType.COMPLEX_TYPE,
                ExportConnectorActivityHandler.WorkDefinition.class,
                ExportConnectorActivityHandler.WorkDefinition::new
                );
    }

    @Override
    public AbstractActivityRun<ExportConnectorActivityHandler.WorkDefinition, ExportConnectorActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ExportConnectorActivityHandler.WorkDefinition, ExportConnectorActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevExportConnectorWorkDefinitionType> {

        final String explicitVersion;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            explicitVersion = typedDefinition.getVersion();
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            ExportConnectorActivityHandler.WorkDefinition,
            ExportConnectorActivityHandler,
            ConnDevExportConnectorWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<ExportConnectorActivityHandler.WorkDefinition, ExportConnectorActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var beans = ConnDevBeans.get();

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            var connDef = backend.developmentObject().getConnector();

            var version = getWorkDefinition().explicitVersion != null
                    ? getWorkDefinition().explicitVersion
                    : connDef.getVersion();

            // Fixes the manifest in the midPoint home bundle directory to the exported version.
            // This is a local, in-place write; it does not create a new ConnId bundle URI, so no
            // re-registration with the running ConnId framework happens (and none is needed for
            // producing the export artifact).
            beans.connectorService.editableConnectorFor(connDef.getDirectory())
                    .renameBundle(connDef.getGroupId(), connDef.getArtifactId(), version);

            var fileName = MiscUtil.fixFileName(connDef.getArtifactId() + "-connector-" + version + ".jar");
            var targetFile = new File(ReportSupportUtil.getOrCreateExportDir(), fileName);
            try {
                beans.connectorExportService.packBundle(
                        connDef.getDirectory(),
                        targetFile,
                        Map.of(CONFIGURATION_OVERRIDE_FILE, Map.of(DEVELOPMENT_MODE_PROPERTY, "false")));
            } catch (IOException e) {
                throw new SystemException("Couldn't pack connector bundle into " + targetFile, e);
            }

            var nodeRef = currentNodeRef(task, result);

            var state = getActivityState();
            state.setWorkStateItemRealValues(ConnDevExportConnectorWorkStateType.F_RESULT, new ConnDevExportConnectorResultType()
                    .fileName(fileName)
                    .nodeRef(nodeRef)
                    .version(version)
                    .connectorRef(connDef.getConnectorRef().clone()));
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }

        private ObjectReferenceType currentNodeRef(RunningTask task, OperationResult result)
                throws CommonException {
            var beans = ConnDevBeans.get();
            var nodeId = task.getNode();
            var query = PrismContext.get().queryFor(NodeType.class)
                    .item(NodeType.F_NODE_IDENTIFIER).eq(nodeId)
                    .build();
            List<PrismObject<NodeType>> nodes = beans.modelService.searchObjects(NodeType.class, query, null, task, result);
            if (nodes.isEmpty()) {
                throw new ObjectNotFoundException("Could not find node where the connector export was stored", NodeType.class, nodeId);
            }
            return new ObjectReferenceType().oid(nodes.get(0).getOid()).type(NodeType.COMPLEX_TYPE);
        }
    }
}
