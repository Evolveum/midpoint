/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.util.ResourceUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.api.EditableConnector;
import com.evolveum.midpoint.repo.common.reports.ReportSupportUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.smart.api.conndev.ConnDevArtifactValidationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnDevDocumentationTopic;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;

import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.smart.impl.StatusInfoImpl;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.ClusterExecutionOptions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

@Component
public class ConnectorDevelopmentServiceImpl implements ConnectorDevelopmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDevelopmentServiceImpl.class);

    /** Auto cleanup time for background tasks created by the service. Will be shorter, probably. */
    private static final Duration AUTO_CLEANUP_TIME = XmlTypeConverter.createDuration("P1D");

    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private TaskManager taskManager;
    @Autowired private ModelService modelService;
    @Autowired private ClusterExecutionHelper clusterExecutionHelper;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private ConndevDocumentationService conndevDocumentationService;

    private static ConnectorDevelopmentServiceImpl instance;

    @Override
    public ConnectorDevelopmentOperation startFromNew(ConnDevApplicationInfoType basicInfo, OperationResult result) {
        return null;
    }

    @Override
    public ConnectorDevelopmentOperation continueFrom(ConnectorDevelopmentType type) {
        return new OperationWrapper(type);
    }

    @Override
    public ConnectorDevelopmentType startFromExisting(ConnectorType sourceConnector, Task task, OperationResult result)
            throws CommonException {
        securityEnforcer.authorize(AuthorizationConstants.AUTZ_UI_CONNECTOR_WIZARD_URL, task, result);

        // Reuse a development that is already linked to this connector - either the connector it
        // produced (connectorRef) or the connector it was imported from (sourceConnectorRef).
        var existing = findDevelopmentForConnector(sourceConnector, task, result);
        if (existing != null) {
            LOGGER.info("Reusing existing connector development {} for connector {}",
                    existing.getOid(), sourceConnector.getOid());
            return existing;
        }

        var beans = ConnDevBeans.get();
        var editable = beans.connectorService.editableConnectorFor(sourceConnector);
        if (editable == null) {
            throw new SystemException("Connector " + sourceConnector.getOid()
                    + " has no local bundle directory; only local (directory-based) connector bundles can be imported");
        }
        if (!isManifestBased(editable)) {
            throw new ConfigurationException("Connector " + sourceConnector.getOid()
                    + " is not a low-code (manifest-based) connector - no "
                    + ConnectorManifestReader.MANIFEST_YAML + "/" + ConnectorManifestReader.MANIFEST_JSON
                    + " file in its bundle directory");
        }

        ConnectorManifestReader.Manifest manifest;
        try {
            var manifestContent = editable.fileExists(ConnectorManifestReader.MANIFEST_YAML)
                    ? editable.readFile(ConnectorManifestReader.MANIFEST_YAML)
                    : editable.readFile(ConnectorManifestReader.MANIFEST_JSON);
            manifest = ConnectorManifestReader.read(manifestContent);
        } catch (IOException e) {
            throw new SystemException("Couldn't read the manifest of connector " + sourceConnector.getOid(), e);
        }

        // The bundle name is {@code <groupId>.<artifactId>}; the group id itself may contain
        // dots (e.g. {@code com.evolveum.polygon.scimrest}) while the artifact id cannot, so the
        // coordinates are split at the *last* dot.
        var bundle = sourceConnector.getConnectorBundle();
        var bundleDot = bundle != null ? bundle.lastIndexOf('.') : -1;
        if (bundleDot <= 0 || bundleDot == bundle.length() - 1) {
            throw new SystemException("Couldn't derive the connector coordinates from the bundle name '" + bundle + "'");
        }
        var groupId = bundle.substring(0, bundleDot);
        var artifactId = bundle.substring(bundleDot + 1);
        // A new minor version, so the imported bundle can coexist with the original connector.
        var version = ConnectorVersionUtil.bumpMinor(sourceConnector.getConnectorVersion());
        var integrationType = integrationTypeFor(beans.connectorService.getConnectorClass(sourceConnector));

        var manifestApplication = manifest.application();
        var displayName = sourceConnector.getDisplayName() != null
                ? sourceConnector.getDisplayName().getOrig()
                : (manifestApplication != null ? manifestApplication.name() : null);
        if (displayName == null || displayName.isBlank()) {
            displayName = artifactId;
        }
        var application = new ConnDevApplicationInfoType()
                .applicationName(manifestApplication != null && manifestApplication.name() != null
                        ? manifestApplication.name() : displayName)
                .integrationType(integrationType)
                .detectedSchema(new ConnDevSchemaType());
        if (manifestApplication != null && manifestApplication.description() != null) {
            application.description(manifestApplication.description());
        }

        var connector = new ConnDevConnectorType()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .integrationType(integrationType)
                .sourceConnectorRef(sourceConnector.getOid(), ConnectorType.COMPLEX_TYPE);
        connector.displayName(displayName);
        populateConnectorFromManifest(connector, manifest, editable, sourceConnector);

        // The object classes (names) are also reflected in the application's detected schema - the
        // discovery steps then enrich it (and the connector object classes) from the development-mode
        // metadata and/or documentation.
        if (!connector.getObjectClass().isEmpty()) {
            var detectedSchema = new ConnDevSchemaType();
            for (var objectClass : connector.getObjectClass()) {
                detectedSchema.objectClass(new ConnDevObjectClassInfoType().name(objectClass.getName()).relevant(true));
            }
            application.detectedSchema(detectedSchema);
        }

        var development = new ConnectorDevelopmentType()
                .name(groupId + ":" + artifactId + ":" + version)
                .application(application)
                .connector(connector);
        var oid = beans.repositoryService.addObject(development.asPrismObject(), null, result);
        LOGGER.info("Created connector development {} imported from connector {} ({}:{}:{} -> {})",
                oid, sourceConnector.getOid(), groupId, artifactId, sourceConnector.getConnectorVersion(), version);
        return modelService.getObject(ConnectorDevelopmentType.class, oid, null, task, result).asObjectable();
    }

    @Override
    public boolean isManifestBasedConnector(ConnectorType connector, OperationResult result) {
        try {
            var editable = ConnDevBeans.get().connectorService.editableConnectorFor(connector);
            return editable != null && isManifestBased(editable);
        } catch (Exception e) {
            LOGGER.warn("Couldn't determine whether connector {} is manifest-based", connector.getOid(), e);
            return false;
        }
    }

    private boolean isManifestBased(EditableConnector editable) {
        return editable.fileExists(ConnectorManifestReader.MANIFEST_YAML)
                || editable.fileExists(ConnectorManifestReader.MANIFEST_JSON);
    }

    /**
     * Finds a development already linked to the given connector - either the connector it
     * produced ({@code connector/connectorRef}) or the connector it was imported from
     * ({@code connector/sourceConnectorRef}). The linkage is matched in memory (the number of
     * connector developments is small) rather than with a repository query, because the
     * {@code connector} item is not a searchable relation in the sqale mapping.
     */
    private ConnectorDevelopmentType findDevelopmentForConnector(ConnectorType sourceConnector, Task task, OperationResult result)
            throws CommonException {
        var query = PrismContext.get().queryFor(ConnectorDevelopmentType.class).build();
        var developments = modelService.searchObjects(ConnectorDevelopmentType.class, query, null, task, result);
        var sourceOid = sourceConnector.getOid();
        return developments.stream()
                .map(prismObject -> prismObject.asObjectable())
                .filter(development -> isLinkedToConnector(development, sourceOid))
                .findFirst()
                .orElse(null);
    }

    private static boolean isLinkedToConnector(ConnectorDevelopmentType development, String sourceOid) {
        var connector = development.getConnector();
        if (connector == null) {
            return false;
        }
        var connectorRef = connector.getConnectorRef();
        if (connectorRef != null && sourceOid.equals(connectorRef.getOid())) {
            return true;
        }
        var sourceRef = connector.getSourceConnectorRef();
        return sourceRef != null && sourceOid.equals(sourceRef.getOid());
    }

    /**
     * Maps the manifest script entries onto the connector item: object class script slots,
     * relation schema scripts, the authentication script and the test-connection operation.
     * Script contents are read from the source bundle (the files are carried over by the
     * copy-connector step).
     */
    private void populateConnectorFromManifest(ConnDevConnectorType connector,
            ConnectorManifestReader.Manifest manifest, EditableConnector bundle, ConnectorType sourceConnector) {
        var objectClasses = new LinkedHashMap<String, ConnDevObjectClassInfoType>();
        var relations = new ArrayList<ConnDevRelationInfoType>();

        for (var script : manifest.scripts()) {
            var filename = stripLeadingSlash(script.path());
            String content = null;
            try {
                if (bundle.fileExists(filename)) {
                    content = bundle.readFile(filename);
                } else {
                    LOGGER.warn("Manifest script '{}' of connector {} is missing from the bundle; the artifact will have no content",
                            filename, sourceConnector.getOid());
                }
            } catch (IOException e) {
                LOGGER.warn("Couldn't read manifest script '{}' of connector {}", filename, sourceConnector.getOid(), e);
            }
            var artifact = new ConnDevArtifactType()
                    .objectClass(script.objectClass())
                    .operation(script.operation())
                    .intent(script.intent())
                    .filename(filename)
                    .content(content)
                    .disabled(script.disabled());

            if (ConnDevScriptIntentType.RELATION.equals(script.intent())) {
                var relation = relations.stream()
                        .filter(r -> script.objectClass() != null && script.objectClass().equals(r.getName()))
                        .findFirst()
                        .orElse(null);
                if (relation == null) {
                    relation = new ConnDevRelationInfoType().name(script.objectClass());
                    relations.add(relation);
                }
                relation.schemaScript(artifact);
            } else if (script.operation() == null) {
                connector.authenticationScript(artifact);
            } else if (ConnDevOperationType.TEST_CONNECTION.equals(script.operation())) {
                connector.testOperation(artifact);
            } else if (script.objectClass() == null) {
                LOGGER.warn("Manifest script '{}' of connector {} has no object class; it will be ignored",
                        filename, sourceConnector.getOid());
            } else {
                var objectClass = objectClasses.computeIfAbsent(script.objectClass(),
                        name -> new ConnDevObjectClassInfoType().name(name).relevant(true));
                setArtifactSlot(objectClass, script, artifact, sourceConnector);
            }
        }

        if (!objectClasses.isEmpty()) {
            connector.getObjectClass().addAll(objectClasses.values());
        }
        if (!relations.isEmpty()) {
            connector.getRelation().addAll(relations);
        }
    }

    private void setArtifactSlot(ConnDevObjectClassInfoType objectClass, ConnectorManifestReader.ManifestScript script,
            ConnDevArtifactType artifact, ConnectorType sourceConnector) {
        switch (script.operation()) {
            case SCHEMA -> objectClass.nativeSchemaScript(artifact);
            case SEARCH -> {
                switch (script.intent()) {
                    case ALL -> objectClass.searchAllOperation(artifact);
                    case ID -> objectClass.searchIdOperation(artifact);
                    case FILTER -> objectClass.searchFilterOperation(artifact);
                    default -> LOGGER.warn("No script slot for {} search (intent {}) of object class {} in connector {}",
                            script.operation(), script.intent(), objectClass.getName(), sourceConnector.getOid());
                }
            }
            case CREATE -> objectClass.createScript(artifact);
            case UPDATE -> objectClass.updateScript(artifact);
            case DELETE -> objectClass.deleteScript(artifact);
            default -> LOGGER.warn("No script slot for {} operation of object class {} in connector {}",
                    script.operation(), objectClass.getName(), sourceConnector.getOid());
        }
    }

    /**
     * Infers the integration type of an imported connector from its {@code @ConnectorClass}:
     * SQL framework packages → {@code sql}, SCIM/REST framework packages → {@code scim}
     * (the user can switch to {@code rest} in the wizard). Defaults to {@code scim}.
     */
    private static ConnDevIntegrationType integrationTypeFor(String connectorClass) {
        if (connectorClass != null) {
            if (connectorClass.contains(".sql.")) {
                return ConnDevIntegrationType.SQL;
            }
            if (connectorClass.contains(".scimrest.")) {
                return ConnDevIntegrationType.SCIM;
            }
        }
        return ConnDevIntegrationType.SCIM;
    }

    private static String stripLeadingSlash(String path) {
        return path != null && path.startsWith("/") ? path.substring(1) : path;
    }

    private class OperationWrapper implements ConnectorDevelopmentOperation {
        public OperationWrapper(ConnectorDevelopmentType type) {
            this.stateObject = type;
        }

        private final ConnectorDevelopmentType stateObject;

        @Override
        public ConnectorDevelopmentType getObject() {
            return stateObject;
        }

        public String submitCreateConnector(Task task, OperationResult result) {
            return submitTask("Creating editable connector for " + connectorNameForTasks(),
                    new WorkDefinitionsType().createConnector(new ConnDevCreateConnectorWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                            .baseTemplateUrl(connectorTemplateFor(stateObject.getConnector().getIntegrationType()))
                    ), task, result);
        }

        public String submitCopyConnector(Task task, OperationResult result) {
            return submitTask("Copying connector for " + connectorNameForTasks(),
                    new WorkDefinitionsType().copyConnector(new ConnDevCopyConnectorWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        public String submitExportConnector(Task task, OperationResult result) {
            return submitTask("Exporting connector for " + connectorNameForTasks(),
                    new WorkDefinitionsType().exportConnector(new ConnDevExportConnectorWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        public String submitUploadConnector(Task task, OperationResult result) {
            return submitTask("Uploading connector for " + connectorNameForTasks(),
                    new WorkDefinitionsType().uploadConnector(new ConnDevUploadConnectorWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        public String submitDiscoverBasicInformation(Task task, OperationResult result) {
            return submitTask("Discover Basic Information for " + connectorNameForTasks(),
                    new WorkDefinitionsType().discoverGlobalInformation(new ConnDevDiscoverGlobalInformationWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        public String submitDiscoverConnectivityEndpoint(Task task, OperationResult result) {
            return submitTask("Discover Connectivity Endpoint for " + connectorNameForTasks(),
                    new WorkDefinitionsType().discoverConnectivityEndpoint(new ConnDevDiscoverConnectivityEndpointWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        public String submitDiscoverDocumentation(Task task, OperationResult result) {
            return submitTask("Discovering documentation for " + connectorNameForTasks(),
                    new WorkDefinitionsType().discoverDocumentation(new ConnDevDiscoverDocumentationWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        @Override
        public String submitProcessDocumentation(Task task, OperationResult result) {
            return submitTask("Processing documentation for " +stateObject.getOid(),
                    new WorkDefinitionsType().processDocumentation(new ConnDevProcessDocumentationWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        @Override
        public String submitDiscoverObjectClasses(Task task, OperationResult result) {
            return submitTask("Discovering object classes for for " + connectorNameForTasks(),
                    new WorkDefinitionsType().discoverObjectClassInformation(new ConnDevDiscoverObjectClassInformationDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        @Override
        public String submitDiscoverObjectClassAttributes(String objectClass, Task task, OperationResult result) {
            return submitTask(
                    "Discovering attributes for object class '" + objectClass + "'",
                    new WorkDefinitionsType().discoverObjectClassAttributes(new ConnDevDiscoverObjectClassAttributesDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                            .objectClass(objectClass)),
                    task, result);
        }

        @Override
        public String submitDiscoverObjectClassEndpoints(String objectClass, Task task, OperationResult result) {
            return submitTask(
                    "Discovering endpoints for object class '" + objectClass + "'",
                    new WorkDefinitionsType().discoverObjectClassEndpoints(new ConnDevDiscoverObjectClassEndpointsDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                            .objectClass(objectClass)),
                    task, result);
        }

        @Deprecated
        @Override
        public String submitDiscoverObjectClassDetails(String objectClass, Task task, OperationResult result) {
            submitDiscoverObjectClassEndpoints(objectClass, task, result);
            return submitDiscoverObjectClassAttributes(objectClass, task, result);
        }

        @Override
        public StatusInfo<ConnectorDevelopmentType> processDocumentation(PrismContainer<ConnDevDocumentationSourceType> sources) {
            return null;
        }

        @Override
        public void basicConnectorInfoUpdated(ConnectorDevelopmentType updated) {

        }

        @Override
        public StatusInfo<PrismContainer<ConnDevAuthInfoType>> selectBaseApiInformation(String basicInfo) {
            return null;
        }

        @Override
        public String submitGenerateArtifact(ConnDevArtifactType artifact, boolean retry, Task task, OperationResult result) {
            return submitGenerateArtifact(artifact, noop -> {}, retry, task, result);
        }


        @Override
        public String submitGenerateArtifact(ConnDevArtifactType artifact, Consumer<ConnDevGenerateArtifactDefinitionType> customizer, boolean retry, Task task, OperationResult result) {
            var definition =  new ConnDevGenerateArtifactDefinitionType()
                    .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    .skipCache(retry)
                    .artifact(artifact.clone());
            customizer.accept(definition);
            return submitTask("Generating script " + artifact.getFilename() + " for " + connectorNameForTasks(),
                    new WorkDefinitionsType().generateConnectorArtifact(definition), task, result);
        }

        @Override
        public String submitFixObjectClass(
                String objectClass, List<String> midpointErrors, List<ConnDevArtifactType> currentScripts,
                boolean retry, Task task, OperationResult result) {
            var definition = new ConnDevFixObjectClassDefinitionType()
                    .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    .skipCache(retry)
                    .objectClass(objectClass);
            midpointErrors.forEach(definition::midpointError);
            if (currentScripts != null) {
                currentScripts.forEach(script -> definition.artifact(script.clone()));
            }
            return submitTask("Fixing object class '" + objectClass + "' for " + connectorNameForTasks(),
                    new WorkDefinitionsType().fixObjectClass(definition), task, result);
        }

        @Override
        public ResourceType testConnection(ConnectorConfigurationType type) {
            return null;
        }

        @Override
        public StatusInfo<PrismContainer<ConnDevBasicObjectClassInfoType>> discoverObjectClasses() {
            return null;
        }

        @Override
        public StatusInfo<PrismContainer<ConnDevAttributeInfoType>> generateAttributes(ConnDevBasicObjectClassInfoType type) {
            return null;
        }

        @Override
        public StatusInfo<ConnDevArtifactType> generateNativeSchemaScript(PrismContainer<ConnDevAttributeInfoType> type) {
            return null;
        }

        @Override
        public String getArtifactContent(ConnDevArtifactType type, Task task, OperationResult result) throws IOException {
            var artifact = ConnectorDevelopmentBackend.backendFor(stateObject, task, result).getArtifactContent(type);
            return artifact.getContent();
        }

        @Override
        public BareResourceSchema testSchema(ConnDevArtifactType type) {
            return null;
        }

        @Override
        public StatusInfo<PrismContainer<ConnDevHttpEndpointType>> getSearchEndpoints(String objectClass) {
            return null;
        }

        @Override
        public StatusInfo<ConnDevArtifactType> generateSearchAll(String objectClass, ConnDevHttpEndpointType endpoint) {
            return null;
        }

        @Override
        public void testSearchAll(String objectClass, ConnDevArtifactType script) {

        }

        @Override
        public void saveArtifact(ConnDevArtifactType artifact, Task task, OperationResult result) throws IOException, CommonException {
            ConnectorDevelopmentBackend.backendFor(stateObject, task, result)
                    .saveArtifact(artifact);
            if (ConnDevOperationType.SCHEMA.equals(artifact.getOperation())) {
                resetResourceSchema(task, result);
            }
        }

        @Override
        public void disableArtifact(String filename, Task task, OperationResult result) throws IOException, CommonException {
            ConnectorDevelopmentBackend.backendFor(stateObject, task, result)
                    .disableArtifact(filename);
        }

        @Override
        public ConnDevArtifactValidationResult validateArtifact(ConnDevArtifactType artifact, Task task, OperationResult result) {
            return ConnectorDevelopmentBackend.backendFor(stateObject, task, result)
                    .validateArtifact(artifact);
        }

        public void comfirmApplicationInformation(Task task, OperationResult result) {
            ConnectorDevelopmentBackend.backendFor(stateObject, task, result).suggestConnectorCoordinates();
        }

        @Override
        public List<ConnDevHttpEndpointType> suggestedEndpointsFor(String user, ConnectorDevelopmentArtifacts.KnownArtifactType knownArtifactType) {
            var use = switch (knownArtifactType.scriptIntent) {
                case ALL -> ConnDevHttpEndpointIntentType.GET_ALL;
                default -> throw new IllegalArgumentException(
                        "Unsupported artifact type for endpoint suggestion: " + knownArtifactType);
            };

            var obj = stateObject.getApplication().getDetectedSchema().getObjectClass().stream()
                    .filter(o -> o.getName().equals(user)).findFirst().orElse(null);
            if (obj == null) {
                return List.of();
            }

            return obj.getEndpoint().stream().filter(e -> e.getSuggestedUse().contains(use)).toList();
        }

        @Override
        public void resetResourceSchema(Task task, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException,
                SubscriptionComplianceException {
            if (stateObject.getTesting() != null && stateObject.getTesting().getTestingResource() != null) {
                var resource = stateObject.getTesting().getTestingResource();
                ResourceUtils.deleteSchema(resource.getOid(), modelService, task, result);
            }
        }

        @Override
        public void authenticationSelectionUpdated(Task task, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException,
                SubscriptionComplianceException {
            ConnectorDevelopmentBackend.backendFor(stateObject, task, result)
                    .updateConfigurationOverride();
        }

        @Override
        public String submitRefreshSchema(Task task, OperationResult result) {
            return submitTask("Refreshing schema for " + connectorNameForTasks(),
                    new WorkDefinitionsType().refreshSchema(new ConnDevRefreshSchemaWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        private String connectorNameForTasks() {
            return stateObject.getName().getOrig();
        }
    }

    private String submitTask(String name, WorkDefinitionsType work, Task task, OperationResult result) {
        try {
            securityEnforcer.authorize(AuthorizationConstants.AUTZ_UI_CONNECTOR_WIZARD_URL, task, result);

            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(work),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name(name)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            return oid;
        } catch (Exception e) {
            throw new SystemException("Couldn't submit task '" + name + "'", e);
        }
    }

    private String connectorTemplateFor(ConnDevIntegrationType integrationType) {
        var beans = ConnDevBeans.get();
        var result = new OperationResult("Empty");
        return switch (integrationType) {
            case REST, SCIM -> beans.getFrameworkUrl(result);
            case SQL -> beans.getSqlFrameworkUrl(result);
        };
    }

    private static @NotNull Collection<SelectorOptions<GetOperationOptions>> taskRetrievalOptions() {
        return GetOperationOptionsBuilder.create()
                .noFetch()
                .item(TaskType.F_RESULT).retrieve()
                .build();
    }

    private @NotNull TaskType getTask(String oid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SubscriptionComplianceException {
        securityEnforcer.authorize(AuthorizationConstants.AUTZ_UI_CONNECTOR_WIZARD_URL, task, result);
        return taskManager
                .getObject(TaskType.class, oid, taskRetrievalOptions(), result)
                .asObjectable();
    }

    @Override
    public StatusInfo<ConnDevCreateConnectorResultType> getCreateConnectorStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevCreateConnectorResultType.class);
    }

    @Override
    public StatusInfo<ConnDevCreateConnectorResultType> getCopyConnectorStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCopyConnectorWorkStateType.F_RESULT,
                ConnDevCreateConnectorResultType.class);
    }

    @Override
    public StatusInfoImpl<ConnDevDiscoverGlobalInformationResultType> getDiscoverBasicInformationStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverGlobalInformationResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevDiscoverDocumentationResultType> getDiscoverDocumentationStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverDocumentationResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevProcessDocumentationResultType> getProcessDocumentationStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevProcessDocumentationResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevGenerateArtifactResultType> getGenerateArtifactStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevGenerateArtifactResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevFixObjectClassResultType> getFixObjectClassStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevFixObjectClassResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevDiscoverObjectClassInformationResultType> getDiscoverObjectClassInformationStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverObjectClassInformationResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevDiscoverObjectClassAttributesResultType> getDiscoverObjectClassAttributesStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverObjectClassAttributesResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevDiscoverObjectClassEndpointsResultType> getDiscoverObjectClassEndpointsStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverObjectClassEndpointsResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevRefreshSchemaResultType> getRefreshSchemaStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevRefreshSchemaWorkStateType.F_RESULT,
                ConnDevRefreshSchemaResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevDiscoverConnectivityEndpointResultType> getDiscoverConnectivityEndpointStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverConnectivityEndpointResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevExportConnectorResultType> getExportConnectorStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevExportConnectorWorkStateType.F_RESULT,
                ConnDevExportConnectorResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevExportConnectorResultType> getUploadConnectorStatus(String token, Task task, OperationResult result) throws CommonException {
        return new StatusInfoImpl<>(
                getTask(token, task, result),
                ConnDevUploadConnectorWorkStateType.F_RESULT,
                ConnDevExportConnectorResultType.class
        );
    }

    @Override
    public InputStream getExportedConnectorFileStream(String fileName, String nodeOid, Task task, OperationResult result)
            throws CommonException, IOException {
        var localFile = new File(ReportSupportUtil.getExportDir(), fileName);
        if (localFile.exists()) {
            return FileUtils.openInputStream(localFile);
        }

        Holder<InputStream> inputStreamHolder = new Holder<>();
        clusterExecutionHelper.executeWithFallback(nodeOid,
                (client, node, result1) -> {
                    client.path(ModelPublicConstants.CLUSTER_REPORT_FILE_PATH);
                    client.query(ModelPublicConstants.CLUSTER_REPORT_FILE_FILENAME_PARAMETER, fileName);
                    client.accept(MediaType.APPLICATION_OCTET_STREAM);
                    var response = client.get();
                    var statusInfo = response.getStatusInfo();
                    if (statusInfo.getFamily() == Response.Status.Family.SUCCESSFUL) {
                        Object entity = response.getEntity();
                        if (entity == null || entity instanceof InputStream) {
                            inputStreamHolder.setValue((InputStream) entity);
                            // do NOT close the response; input stream will be closed later by the caller(s)
                        } else {
                            response.close();
                        }
                    } else {
                        result1.recordFatalError("Could not retrieve exported connector file '" + fileName + "': Got "
                                + statusInfo.getStatusCode() + ": " + statusInfo.getReasonPhrase());
                        response.close();
                    }
                }, new ClusterExecutionOptions().tryNodesInTransition().skipDefaultAccept(), "get exported connector file", result);

        return inputStreamHolder.getValue();
    }

    @Override
    public List<ConnDevDocumentationTopic> getDocumentationTopics(String key, String protocol) {
        return conndevDocumentationService.getTopics(key, protocol);
    }
}
