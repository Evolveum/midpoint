package com.evolveum.midpoint.smart.impl.conndev.activity;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.reports.ReportSupportUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Packages the connector into a full, deployable connector jar. See
 * {@link UploadConnectorActivityHandler} for the lib/-less sibling action - which format each
 * activity produces is fixed by which of these two classes ran, not by a flag passed between them.
 */
@Component
public class ExportConnectorActivityHandler
        extends AbstractConnDevActivityHandler<ExportConnectorActivityHandler.WorkDefinition, ExportConnectorActivityHandler> {

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
                beans.connectorExportService.packAsJar(
                        connDef.getDirectory(),
                        targetFile,
                        Map.of(CONFIGURATION_OVERRIDE_FILE, Map.of(DEVELOPMENT_MODE_PROPERTY, "false")));
            } catch (IOException e) {
                throw new SystemException("Couldn't pack connector bundle into " + targetFile, e);
            }

            var exportResult = new ConnDevExportConnectorResultType()
                    .fileName(fileName)
                    .nodeRef(AbstractConnDevActivityHandler.currentNodeRef(task, result))
                    .version(version)
                    .connectorRef(connDef.getConnectorRef().clone())
                    .contentType("application/java-archive");

            var state = getActivityState();
            state.setWorkStateItemRealValues(ConnDevExportConnectorWorkStateType.F_RESULT, exportResult);
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
