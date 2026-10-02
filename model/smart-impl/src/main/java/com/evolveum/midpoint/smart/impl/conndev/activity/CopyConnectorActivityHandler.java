/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * Copies the bundle of the existing low-code connector a development was imported from
 * (the development's {@code connector/sourceConnectorRef}) to a new bundle directory with the
 * development's connector coordinates (group, artifact, version) and installs the copy - the
 * import counterpart of {@link CreateConnectorActivityHandler}, which downloads a fresh
 * framework template instead. The original connector stays installed.
 */
@Component
public class CopyConnectorActivityHandler
        extends AbstractConnDevActivityHandler<CopyConnectorActivityHandler.WorkDefinition, CopyConnectorActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(CopyConnectorActivityHandler.class);

    private static final String MESSAGES_PROPERTIES = "Messages.properties";
    private static final String DISPLAY_NAME_KEY = "manifest.connector.display";

    public CopyConnectorActivityHandler() {
        super(
                ConnDevCopyConnectorWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_COPY_CONNECTOR,
                ConnDevCopyConnectorWorkStateType.COMPLEX_TYPE,
                CopyConnectorActivityHandler.WorkDefinition.class,
                CopyConnectorActivityHandler.WorkDefinition::new
                );
    }

    @Override
    public AbstractActivityRun<CopyConnectorActivityHandler.WorkDefinition, CopyConnectorActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<CopyConnectorActivityHandler.WorkDefinition, CopyConnectorActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevCopyConnectorWorkDefinitionType> {

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            CopyConnectorActivityHandler.WorkDefinition,
            CopyConnectorActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<CopyConnectorActivityHandler.WorkDefinition, CopyConnectorActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);

            var connDev = backend.developmentObject();
            var connDef = connDev.getConnector();
            var sourceRef = connDef.getSourceConnectorRef();
            if (sourceRef == null || sourceRef.getOid() == null) {
                throw new SystemException("No source connector (connector/sourceConnectorRef) specified for the "
                        + "copy connector operation on " + connDev.getOid());
            }
            var source = beans.modelService.getObject(ConnectorType.class, sourceRef.getOid(), null, task, result).asObjectable();

            var targetDir = connDef.getGroupId() + "." + connDef.getArtifactId() + "." + connDef.getVersion();

            // Copy the source bundle
            var editable = beans.connectorService.copyBundle(source, targetDir, result);

            // Keep the display name in sync - only when the bundle declares the standard key
            // (custom connector classes may use a different Messages.properties key).
            try {
                var properties = new Properties();
                properties.load(new StringReader(editable.readFile(MESSAGES_PROPERTIES)));
                if (properties.containsKey(DISPLAY_NAME_KEY)) {
                    editable.updateProperty(MESSAGES_PROPERTIES, DISPLAY_NAME_KEY, backend.connectorDisplayName());
                }
            } catch (IOException e) {
                LOGGER.warn("Couldn't update the connector display name in the copied bundle", e);
            }

            editable.renameBundle(connDef.getGroupId(), connDef.getArtifactId(), connDef.getVersion());

            // Install the copy
            var lookups = editable.install(result);

            if (lookups.isEmpty()) {
                throw new SystemException(
                        "Copying the connector produced no connector definition for '" + targetDir
                                + "'; the target connector version is either already installed or the bundle is not a valid ConnId bundle");
            }
            var lookup = lookups.get(0);

            var query = PrismContext.get().queryFor(ConnectorType.class)
                    .item(ConnectorType.F_CONNECTOR_BUNDLE).eq(lookup.getConnectorBundle())
                    .and().item(ConnectorType.F_CONNECTOR_TYPE).eq(lookup.getConnectorType())
                    .and().item(ConnectorType.F_CONNECTOR_VERSION).eq(lookup.getConnectorVersion())
                    .build();

            var connectors = beans.modelService.searchObjects(ConnectorType.class, query, null, task, result);
            if (connectors.isEmpty()) {
                throw new SystemException(
                        "Copied connector was not found in the repository for bundle '" + lookup.getConnectorBundle()
                                + "', type '" + lookup.getConnectorType() + "', version '" + lookup.getConnectorVersion() + "'");
            }
            var connector = connectors.get(0);

            var state = getActivityState();

            backend.linkEditableConnector(targetDir, connector.getOid());

            try {
                backend.recomputeConnectorManifest();
            } catch (IOException e) {
                throw new SystemException("Couldn't recompute connector manifest", e);
            }

            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT, new ConnDevCreateConnectorResultType()
                    .connectorRef(connector.getOid(), ConnectorType.COMPLEX_TYPE));
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
