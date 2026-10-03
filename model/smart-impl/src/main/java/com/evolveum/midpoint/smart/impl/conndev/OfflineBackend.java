package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.IOException;
import java.util.List;

/**
 * Base of the offline backends (no connector-generation service configured). Everything runs
 * locally against the development object and the testing resource:
 *
 * <ul>
 *   <li>documentation discovery is skipped (nothing to discover, nothing to process),</li>
 *   <li>object classes and attributes come from the connector's development-mode
 *       ({@code conndev_ObjectClass}) metadata via the inherited
 *       {@link #discoverObjectClassesUsingConnector()}/{@link #discoverObjectClassAttributesFromDevMode(String)},</li>
 *   <li>scripts are generated as near-bare skeletons by {@link OfflineArtifactGenerator} that the
 *       user is expected to edit manually (see {@code OfflineRestBackend}/{@code OfflineSqlBackend}).</li>
 * </ul>
 */
public class OfflineBackend extends ConnectorDevelopmentBackend {

    public OfflineBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    protected List<ProcessedDocumentation> synchronizeDocumentation(List<DevShadowDocument> documentation) {
        // No generation service to synchronize with: the dev-shadow documents are read directly
        // from the testing resource where they are needed (development-mode object classes).
        return List.of();
    }

    @Override
    public void refreshConnDevDocumentation() {
        // Offline there is no generation service to feed with the dev-shadow documentation, and
        // the development-mode object classes/attributes are read directly from the testing
        // resource (see discoverObjectClassesUsingConnector), so the refresh is a no-op.
    }

    @Override
    public ConnDevApplicationInfoType discoverBasicInformation(boolean skipCache) {
        // No generation service to scrape: return the information already stored on the
        // development (prefilled by the wizard or the import). An empty result keeps
        // populateBasicApplicationInformation a no-op instead of wiping stored values.
        var application = developmentObject().getApplication();
        return application != null ? application.clone() : new ConnDevApplicationInfoType();
    }

    @Override
    public List<ConnDevAuthInfoType> discoverAuthorizationInformation(boolean skipCache) {
        // HTTP authorization schemes require the generation service to scrape; the protocol
        // specific offline backends override this (REST/SCIM offer the standard catalog).
        return List.of();
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation(boolean skipCache) {
        // Documentation discovery requires the generation service; offline it is skipped.
        return List.of();
    }

    @Override
    public void processDocumentation(boolean skipCache) {
        // NOOP - there is no processed documentation offline.
    }

    @Override
    public List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(List<ConnDevBasicObjectClassInfoType> connectorDiscovered, boolean includeUnrelated, boolean skipCache) {
        // Object classes offline come exclusively from the development-mode metadata
        // (discoverObjectClassesUsingConnector); documentation-based discovery is unavailable.
        return connectorDiscovered;
    }

    @Override
    public List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered, boolean skipCache) {
        // Relations require the generation service; offline nothing is derived (an imported
        // connector already carries its relations from the manifest).
        return List.of();
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverConnectivityEndpoints(boolean skipCache) {
        // Connectivity-endpoint discovery requires the generation service.
        return List.of();
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass, boolean skipCache) {
        // Endpoint discovery requires the generation service; the user selects endpoints manually.
        return List.of();
    }

    @Override
    public List<ConnDevAttributeInfoType> discoverObjectClassAttributes(String objectClass, boolean skipCache) {
        // Attributes offline come from the development-mode metadata; nothing else is available.
        return discoverObjectClassAttributesFromDevMode(objectClass);
    }

    /**
     * Offline the generation service cannot repair scripts: the current scripts are returned
     * unchanged. The wizard hides the repair action when offline, so this is defensive only.
     */
    @Override
    public ConnDevFixObjectClassResultType fixObjectClass(
            String objectClass, List<String> midpointErrors, List<ConnDevArtifactType> currentScripts, boolean skipCache) {
        var ret = new ConnDevFixObjectClassResultType();
        if (developmentObject().getConnector() != null && developmentObject().getConnector().getConnectorRef() != null) {
            ret.connectorRef(developmentObject().getConnector().getConnectorRef());
        }
        if (currentScripts != null) {
            for (var script : currentScripts) {
                ret.artifact(script.clone());
            }
        }
        return ret;
    }

    @Override
    protected void restoreSession(ServiceClient.RestorationClient client) throws IOException {
        // NOOP - offline backend has no remote session to restore
    }

    // Artifact generation is protocol-specific (REST/SCIM vs SQL skeleton shapes); see the
    // OfflineRestBackend/OfflineSqlBackend overrides delegating to OfflineArtifactGenerator.
    @Override
    public ConnDevArtifactType generateArtifact(ConnDevGenerateArtifactDefinitionType input, boolean skipCache) {
        var classification = ConnectorDevelopmentArtifacts.classify(input.getArtifact());
        return OfflineArtifactGenerator.generate(this, input, classification);
    }

    @Override
    public ConnDevArtifactType generateObjectClassArtifact(ConnDevGenerateArtifactDefinitionType input, boolean skipCache) {
        var classification = ConnectorDevelopmentArtifacts.classify(input.getArtifact());
        return OfflineArtifactGenerator.generate(this, input, classification);
    }

    @Override
    public void updateApplicationObjectClassEndpoints(String objectClass, List<ConnDevHttpEndpointType> endpoints) throws CommonException {
        // Intentional noop
    }
}
