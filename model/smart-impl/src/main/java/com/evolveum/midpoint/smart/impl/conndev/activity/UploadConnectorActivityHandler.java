package com.evolveum.midpoint.smart.impl.conndev.activity;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
 * Packages the connector into a lib/-less bundle (a plain zip, not a deployable connector jar). See
 * {@link ExportConnectorActivityHandler} for the full-export sibling action - which format each
 * activity produces is fixed by which of these two classes ran, not by a flag passed between them.
 * <p>
 * Currently used by the wizard's "Upload connector" action, which just downloads this bundle; a
 * future version of that action is expected to upload it to another microservice instead.
 */
@Component
public class UploadConnectorActivityHandler
        extends AbstractConnDevActivityHandler<UploadConnectorActivityHandler.WorkDefinition, UploadConnectorActivityHandler> {

    private static final String DEVELOPMENT_MODE_PROPERTY = "developmentMode";
    private static final String CONFIGURATION_OVERRIDE_FILE = "configurationOverride.properties";

    /** Not meant to be deployable, so third-party dependency jars are left out. */
    private static final Set<String> ZIP_EXCLUDED_PATH_PREFIXES = Set.of("lib/");

    public UploadConnectorActivityHandler() {
        super(
                ConnDevUploadConnectorWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_UPLOAD_CONNECTOR,
                ConnDevUploadConnectorWorkStateType.COMPLEX_TYPE,
                UploadConnectorActivityHandler.WorkDefinition.class,
                UploadConnectorActivityHandler.WorkDefinition::new
                );
    }

    @Override
    public AbstractActivityRun<UploadConnectorActivityHandler.WorkDefinition, UploadConnectorActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<UploadConnectorActivityHandler.WorkDefinition, UploadConnectorActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevUploadConnectorWorkDefinitionType> {

        final String explicitVersion;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            explicitVersion = typedDefinition.getVersion();
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            UploadConnectorActivityHandler.WorkDefinition,
            UploadConnectorActivityHandler,
            ConnDevUploadConnectorWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<UploadConnectorActivityHandler.WorkDefinition, UploadConnectorActivityHandler> context) {
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

            var fileName = MiscUtil.fixFileName(connDef.getArtifactId() + "-connector-" + version + ".zip");
            var targetFile = new File(ReportSupportUtil.getOrCreateExportDir(), fileName);
            try {
                beans.connectorExportService.packAsZip(
                        connDef.getDirectory(),
                        targetFile,
                        Map.of(CONFIGURATION_OVERRIDE_FILE, Map.of(DEVELOPMENT_MODE_PROPERTY, "false")),
                        ZIP_EXCLUDED_PATH_PREFIXES);
            } catch (IOException e) {
                throw new SystemException("Couldn't pack connector bundle into " + targetFile, e);
            }

            var uploadResult = new ConnDevExportConnectorResultType()
                    .fileName(fileName)
                    .nodeRef(AbstractConnDevActivityHandler.currentNodeRef(task, result))
                    .version(version)
                    .connectorRef(connDef.getConnectorRef().clone())
                    .contentType("application/zip");

            var state = getActivityState();
            state.setWorkStateItemRealValues(ConnDevUploadConnectorWorkStateType.F_RESULT, uploadResult);
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
