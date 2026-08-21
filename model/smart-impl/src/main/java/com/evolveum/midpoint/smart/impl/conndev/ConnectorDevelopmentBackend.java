package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.model.api.util.ResourceUtils;
import com.evolveum.midpoint.provisioning.ucf.api.EditableConnector;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.conndev.ConnDevArtifactValidationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.smart.impl.mappings.ConnDevJsonMapper;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import jakarta.xml.bind.JAXBElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public abstract class ConnectorDevelopmentBackend {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentBackend.class);

    private static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;
    private static final String CONNECTOR_MANIFEST = "connector.manifest.yaml";
    private static final String CONNECTOR_MANIFEST_JSON_LEGACY = "connector.manifest.json";
    private static final String CONFIGURATION_OVERRIDE = "configurationOverride.properties";
    private static final int MAX_SUGGESTED_CONNECTIVITY_ENDPOINTS = 5;
    private static final String CONNDEV_OBJECT_CLASS = "conndev_ObjectClass";
    private static final String CONNDEV_CONTENT_TYPE = "application/com.evolveum.conndev+json";
    protected static final long SLEEP_TIME = 5 * 1000L;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    protected final Task task;
    protected final OperationResult result;

    ConnDevBeans beans;
    private ConnectorDevelopmentType development;
    private EditableConnector editableConnector;
    protected boolean deleteConnectorSchema = false;
    protected boolean skipConfigurationPropsUpgrade = true;

    public ConnectorDevelopmentBackend(ConnDevBeans beans, ConnectorDevelopmentType development, Task task, OperationResult result) {
        this.beans = beans;
        this.development = development;
        this.task = task;
        this.result = result;
    }

    /**
     * A dev-shadow document built from a {@code conndev_*} shadow: its stable {@code uuid}/{@code uri}
     * (derived from the discovered name), content type, and the unwrapped schema-mapping {@code content}.
     * Kept purely in memory (not a file-backed {@link ProcessedDocumentation}) because the content is only
     * a transient upload body — the stored {@link ProcessedDocumentation} is materialized from what the
     * generation service returns (see {@link #synchronizeDocumentation}).
     */
    protected record DevShadowDocument(String uuid, String uri, String contentType, String content) {}

    /**
     * Returns a supplier that operations must poll to implement cooperative cancellation.
     *
     * Because operations run in threads that cannot be forcibly killed, each operation is
     * responsible for periodically checking this supplier and stopping itself when it returns
     * false — which happens once the task has been suspended or stopped.
     */
    protected BooleanSupplier canRun() {
        return task instanceof RunningTask rt ? rt::canRun : () -> true;
    }


    public static ConnectorDevelopmentBackend backendFor(String connectorDevelopmentOid, Task task, OperationResult result) throws CommonException {
        var beans = ConnDevBeans.get();
        var connDev = beans.modelService.getObject(ConnectorDevelopmentType.class, connectorDevelopmentOid, null, task, result);
        if (beans.isOffline()) {
            return new OfflineBackend(beans, connDev.asObjectable(), task, result);
        }
        return backendFor(connDev.asObjectable(), task, result);
    }

    private static ConnectorDevelopmentBackend backendFor(ConnDevIntegrationType integrationType, ConnectorDevelopmentType connDev, ConnDevBeans beans, Task task, OperationResult result) {
        return switch (integrationType) {
            case REST -> new RestBackend(beans, connDev, task, result);
            case SCIM -> new ScimBackend(beans, connDev, task, result);
            case SQL -> new SqlBackend(beans, connDev, task, result);
            //case DUMMY -> new OfflineBackend(beans, connDev, task, result);
        };

    }

    @NotNull
    public static ConnectorDevelopmentBackend backendFor(ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        var beans = ConnDevBeans.get();

        if (connDev.getConnector() != null && connDev.getConnector().getIntegrationType() != null) {
            return backendFor(connDev.getConnector().getIntegrationType(), connDev, beans, task, result);
        }
        if (connDev.getApplication() != null) {
            return backendFor(connDev.getApplication().getIntegrationType(), connDev, beans, task, result);
        }
        throw new UnsupportedOperationException("No backend found for" + connDev.getOid());
    }

    public void populateBasicApplicationInformation(ConnDevApplicationInfoType type) throws CommonException {
        if (type.asPrismContainerValue().isEmpty()) {
            return;
        }
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_APPLICATION_NAME)
                .replace(type.getApplicationName() != null ? type.getApplicationName().toPolyString() : null)
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_VERSION)
                .replace(type.getVersion())
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_INTEGRATION_TYPE)
                .replace(type.getIntegrationType())
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_BASE_API_ENDPOINT)
                .replace(type.getBaseApiEndpoint())
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    protected void reload() throws SchemaException, ExpressionEvaluationException, SecurityViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
        development = beans.modelService.getObject(ConnectorDevelopmentType.class, development.getOid(), null, task, result).asObjectable();
    }

    public void populateApplicationAuthInfo(List<ConnDevAuthInfoType> authInfo) throws CommonException {
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_AUTH)
                .addRealValues(authInfo.stream()
                        .filter(info -> info.getType() != ConnDevHttpAuthTypeType.OTHER)
                        .map(ConnDevAuthInfoType::clone)
                        .toList())
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    };

    public void suggestConnectorCoordinates() {


    }

    public void populateApplicationDocumentation(List<ConnDevDocumentationSourceType> documentation) throws CommonException {
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_DOCUMENTATION_SOURCE).addRealValues( documentation.stream().map(ConnDevDocumentationSourceType::clone).toList())
            .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    };

    public ConnectorDevelopmentType developmentObject() {
        return development;
    }

    public void linkEditableConnector(String targetDir, String oid) throws CommonException {
        var connectorRef = new ObjectReferenceType();
        connectorRef.setOid(oid);
        connectorRef.setType(ConnectorType.COMPLEX_TYPE);
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_DIRECTORY).add(targetDir)
                .item(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_CONNECTOR_REF).add(connectorRef)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    public void saveArtifact(ConnDevArtifactType artifact) throws IOException, CommonException {
        var deltaBuilder = PrismContext.get().deltaFor(ConnectorDevelopmentType.class);
        final ItemPath itemPath;
        if (ConnDevScriptIntentType.RELATION.equals(artifact.getIntent())) {
            var maybePath = itemPathFor(artifact, ConnDevConnectorType.F_RELATION);
            if (maybePath != null) {
                itemPath = maybePath;
            } else {
                // We should copy relation info
                copyRelationToConnectorInfo(artifact.getObjectClass());
                reload();
                itemPath  = itemPathFor(artifact, ConnDevConnectorType.F_RELATION);
            }
        } else {
            itemPath = itemPathFor(artifact, ConnDevConnectorType.F_OBJECT_CLASS);
        }
        if (itemPath == null) {
            throw new UnsupportedOperationException("No connector class found for object class " + artifact.getObjectClass());
        }

        saveConnectorFile(artifact.getFilename(), artifact.getContent());
        // Saving through the script's own step is the user's declaration that it's fixed and back
        // in use - clears a stale disabled:true left over from disabling it as a broken sibling
        // (see disableArtifact()) after a schema change, rather than requiring a separate re-enable step.
        var modelArtifact = artifact.clone().content(null).disabled(false);

        var delta = deltaBuilder
                .item(itemPath).replace(modelArtifact)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
        recomputeConnectorManifest();
        invalidateConnector();
    }

    /**
     * Marks an already-deployed script as disabled in the manifest: the connector then skips it
     * both when initializing for real and when reloading siblings during script validation (see
     * conndev's manifest {@code disabled} flag). Lets the wizard offer a "Disable operation" action
     * for a sibling script a schema change breaks, without deleting the script's content.
     *
     * @throws IllegalArgumentException if no deployed artifact has this filename
     */
    public void disableArtifact(String filename) throws IOException, CommonException {
        var artifact = findArtifactByFilename(filename);
        if (artifact == null) {
            throw new IllegalArgumentException("No connector artifact found for filename " + filename);
        }
        var itemPath = ConnDevScriptIntentType.RELATION.equals(artifact.getIntent())
                ? itemPathFor(artifact, ConnDevConnectorType.F_RELATION)
                : itemPathFor(artifact, ConnDevConnectorType.F_OBJECT_CLASS);
        if (itemPath == null) {
            throw new IllegalArgumentException("No connector artifact found for filename " + filename);
        }

        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(itemPath.append(ConnDevArtifactType.F_DISABLED)).replace(true)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
        recomputeConnectorManifest();
        invalidateConnector();
    }

    /**
     * Finds the connector-scoped artifact whose filename matches {@code filename} (leading slash
     * optional, since a validation error's {@code source} carries one but {@link
     * ConnDevArtifactType#getFilename()} doesn't).
     */
    private ConnDevArtifactType findArtifactByFilename(String filename) {
        if (filename == null) {
            return null;
        }
        var normalized = filename.startsWith("/") ? filename.substring(1) : filename;
        return ConnectorDevelopmentArtifacts.allArtifacts(development.getConnector()).stream()
                .filter(a -> normalized.equals(a.getFilename()))
                .findFirst().orElse(null);
    }

    /**
     * Disposes cached connector instances so that the next operation re-initializes the connector
     * with the freshly saved scripts. Without this, provisioning keeps using the connector instance
     * that compiled the scripts during its init.
     */
    private void invalidateConnector() {
        var connectorRef = development.getConnector().getConnectorRef();
        if (connectorRef != null && connectorRef.getOid() != null) {
            beans.cacheDispatcher.dispatchInvalidation(ConnectorType.class, connectorRef.getOid(), false, null);
        }
    }

    private void copyRelationToConnectorInfo(String objectClass) throws CommonException {
        var relation = developmentObject().getApplication().getDetectedSchema().getRelation()
                .stream().filter(r -> r.getName().equals(objectClass)).findFirst();
        if (relation.isEmpty()) {
            throw new ConfigurationException("Supplied relation " + objectClass + "not found");
        }
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_RELATION)
                .add(relation.get().cloneWithoutId())
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
    }

    public void recomputeConnectorManifest() throws IOException {
        var manifest = new ConnectorManifestWriter(development).serialize();

        editableConnector().saveFile(CONNECTOR_MANIFEST, manifest);
        editableConnector().deleteFileIfExists(CONNECTOR_MANIFEST_JSON_LEGACY);
    }

    /**
     * Validates the script by the connector itself (supported only in development mode) using
     * the testing resource. If the testing resource isn't configured yet, or the artifact isn't
     * a groovy script, the script is considered valid (nothing to check against). Any failure to
     * even run the check (testing resource unreachable, deployed connector broken, doesn't
     * support script validation, ...) is reported as an error, blocking the save.
     */
    public ConnDevArtifactValidationResult validateArtifact(ConnDevArtifactType artifact) {
        if (artifact.getFilename() == null || !artifact.getFilename().endsWith(".groovy")) {
            return ConnDevArtifactValidationResult.success();
        }
        var testing = developmentObject().getTesting();
        if (testing == null || testing.getTestingResource() == null || testing.getTestingResource().getOid() == null) {
            return ConnDevArtifactValidationResult.success();
        }
        var testingResourceOid = testing.getTestingResource().getOid();

        var script = new ProvisioningScriptType()
                .language("groovy")
                .code(artifact.getContent())
                .host(ProvisioningScriptHostType.RESOURCE);
        script.getArgument().add(scriptArgument("operation", "build"));
        script.getArgument().add(scriptArgument("artifactKind",
                ConnDevOperationType.SCHEMA.equals(artifact.getOperation()) ? "schema" : "operation"));
        script.getArgument().add(scriptArgument("filename", "/" + artifact.getFilename()));

        Object response;
        try {
            response = beans.provisioningService.executeScript(
                    testingResourceOid, script, task, result);
        } catch (CommonException | RuntimeException e) {
            LOGGER.warn("Couldn't validate script {}.", artifact.getFilename(), e);
            return ConnDevArtifactValidationResult.errors(List.of(
                    new ConnDevArtifactValidationResult.Error(
                            "initialization", e.getMessage(), null, null, null)));
        }

        if (!(response instanceof Map<?, ?> map) || !"error".equals(map.get("status"))) {
            return ConnDevArtifactValidationResult.success();
        }
        if (map.get("errors") instanceof List<?> errorList) {
            return ConnDevArtifactValidationResult.errors(
                    errorList.stream()
                            .filter(Map.class::isInstance)
                            .map(entry -> validationError((Map<?, ?>) entry))
                            .toList());
        }
        return ConnDevArtifactValidationResult.errors(List.of(validationError(map)));
    }

    private static ConnDevArtifactValidationResult.Error validationError(Map<?, ?> map) {
        return new ConnDevArtifactValidationResult.Error(
                map.get("phase") != null ? map.get("phase").toString() : null,
                map.get("message") != null ? map.get("message").toString() : null,
                map.get("line") instanceof Integer line ? line : null,
                map.get("column") instanceof Integer column ? column : null,
                map.get("source") != null ? map.get("source").toString() : null);
    }

    private static ProvisioningScriptArgumentType scriptArgument(String name, String value) {
        var argument = new ProvisioningScriptArgumentType();
        argument.setName(name);
        var node = PrismContext.get().xnodeFactory().primitive(value, DOMUtil.XSD_STRING);
        argument.getExpressionEvaluator().add(
                new JAXBElement<>(SchemaConstants.C_VALUE, RawType.class, new RawType(node.frozen())));
        return argument;
    }

    private EditableConnector editableConnector() {
        if (editableConnector == null) {
            editableConnector = beans.connectorService.editableConnectorFor(development.getConnector().getDirectory());
        }
        return editableConnector;
    }

    private void saveConnectorFile(String filename, String content) throws IOException {
        editableConnector().saveFile(filename, content);
    }

    @Nullable
    private ItemPath itemPathFor(ConnDevArtifactType artifact, ItemName type) {
        ItemPath path = ConnectorDevelopmentType.F_CONNECTOR;
        if (artifact.getObjectClass() != null) {
            path = path.append(type);
            PrismContainer<ConnDevNamedInfoType> typeContainer = development.getConnector().asPrismContainerValue().findContainer(type);
            if (typeContainer == null) {
                return null;
            }
            var objClass  = typeContainer.valuesStream()
                    .filter(o -> o.asContainerable().getName().equals(artifact.getObjectClass())).findFirst().orElse(null);
            if (objClass == null) {
                return null;
            }
            path = path.append(objClass.getId());
        }
        var classification = ConnectorDevelopmentArtifacts.classify(artifact);
        if (classification != null) {
            path = path.append(classification.itemName);
        }
        return path;
    }

    /**
     * Discovers object classes using connector functionality.
     *
     * Ideal for connector frameworks with protocols which supports dynamic discovery of schema, such as SCIM or Database.
     * @return
     */
    public List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingConnector() {
        return List.of();
    }

    public void updateApplicationObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered) throws CommonException {
        List<ConnDevObjectClassInfoType> applicationClasses = discovered.stream().map(v -> {
            var oc = new ConnDevObjectClassInfoType()
                    .name(v.getName())
                    .description(v.getDescription())
                    .embedded(v.getEmbedded())
                    ._abstract(v.isAbstract())
                    .superclass(v.getSuperclass())
                    .relevant(v.getRelevant())
                    .relevancy(v.getRelevancy());
            v.getRelevantDocumentations().forEach(chunk ->
                    oc.relevantDocumentations(new ConnDevRelevantDocumentationsType().docId(chunk.getDocId()).chunkId(chunk.getChunkId())));
            return oc;
        }).toList();
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_DETECTED_SCHEMA, ConnDevSchemaType.F_OBJECT_CLASS).replaceRealValues(applicationClasses)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    public void updateApplicationObjectClassEndpoints(String objectClass, List<ConnDevHttpEndpointType> endpoints) throws CommonException {
        var target = applicationObjectClass(objectClass);
        var path = target.asPrismContainerValue().getPath();
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(path.append(ConnDevObjectClassInfoType.F_ENDPOINT)).replaceRealValues(endpoints)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    ConnDevObjectClassInfoType connectorObjectClass(String objectClass) {
        return development.getConnector()
                .getObjectClass().stream().filter(o -> o.getName().equals(objectClass)).findFirst().orElse(null);
    }

    protected ConnDevObjectClassInfoType applicationObjectClass(String objectClass) {
        return development.getApplication().getDetectedSchema()
                .getObjectClass().stream().filter(o -> o.getName().equals(objectClass)).findFirst().orElse(null);
    }

    public void updateConnectorObjectClassAttributes(String objectClass, List<ConnDevAttributeInfoType> attributes) throws CommonException {
        var target = connectorObjectClass(objectClass);
        var path = target.asPrismContainerValue().getPath();
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(path.append(ConnDevObjectClassInfoType.F_ATTRIBUTE)).replaceRealValues(attributes)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    public void ensureObjectClass(String objectClass) throws CommonException {
        var target = connectorObjectClass(objectClass);
        if (target != null) {
            return;
        }
        if (development.getApplication().getDetectedSchema() != null && development.getApplication().getDetectedSchema().getObjectClass() != null) {
            target = development.getApplication().getDetectedSchema()
                    .getObjectClass().stream().filter(o -> o.getName().equals(objectClass)).findFirst().orElse(null);
        } else {
            target = new ConnDevObjectClassInfoType().name(objectClass);
        }

        var copy = target.clone();
        copy.setId(null);
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_OBJECT_CLASS).add(copy)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    public abstract ConnDevApplicationInfoType discoverBasicInformation(boolean skipCache);
    public abstract List<ConnDevAuthInfoType> discoverAuthorizationInformation(boolean skipCache);
    public abstract List<ConnDevDocumentationSourceType> discoverDocumentation(boolean skipCache);

    /**
     * Generates a non-object-class artifact (authorization script or test-connection script) or,
     * if the artifact targets an object class, delegates to {@link #generateObjectClassArtifact}.
     * Shared across backends: every codegen path talks to the generation service through the same
     * {@code codegen/{sessionId}/...} routes, keyed by the {@link ConnectorDevelopmentArtifacts.KnownArtifactType}
     * classification of the requested artifact.
     */
    public ConnDevArtifactType generateArtifact(ConnDevGenerateArtifactDefinitionType input, boolean skipCache) {
        var artifactSpec = input.getArtifact();
        var ret = artifactSpec.clone();
        if (artifactSpec.getObjectClass() != null) {
            return generateObjectClassArtifact(input, skipCache);
        }

        var classification = ConnectorDevelopmentArtifacts.classify(artifactSpec);
        return switch (classification) {
            case AUTHENTICATION_CUSTOMIZATION -> generateAuthorizationScript(input, classification, skipCache);
            case TEST_CONNECTION_DEFINITION -> ret.content("""
                        test {
                            // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                            // how to write test connection part of the script.
                            // Usually it is only necessary to specify endpoint here.
                            endpoint("/my_preferences")
                        }
                        """);
            default -> throw new IllegalStateException("Unexpected value: " + artifactSpec.getIntent());
        };
    }

    private ConnDevArtifactType generateAuthorizationScript(ConnDevGenerateArtifactDefinitionType input, ConnectorDevelopmentArtifacts.KnownArtifactType classification, boolean skipCache) {
        var auths = developmentObject().getConnector().getAuth();
        if (auths.isEmpty()) {
            return null;
        }

        var body = repairContextBody(input);

        var authArray = JSON_FACTORY.arrayNode();
        for (var auth : auths) {
            if (auth.getType() == null) continue;
            var authNode = JSON_FACTORY.objectNode();
            authNode.set("name", JSON_FACTORY.textNode(auth.getName() != null ? auth.getName() : ""));
            authNode.set("type", JSON_FACTORY.textNode(auth.getType().value()));
            authNode.set("quirks", JSON_FACTORY.textNode(auth.getQuirks() != null ? auth.getQuirks() : ""));
            authArray.add(authNode);
        }
        body.set("preferredAuthorizations", authArray);

        try (var job = client().postJob("codegen/{sessionId}/authorization", body, apiType(), skipCache)) {
            String content = job.waitAndProcess(SLEEP_TIME, canRun(), json -> json.get("code").asText());
            if (content == null || content.isBlank()) {
                return null;
            }
            return classification.create().content(content);
        } catch (IOException e) {
            throw new SystemException("Couldn't generate authorization script", e);
        }
    }

    /**
     * Generates an object-class-scoped artifact (schema, search, create/update/delete or relation
     * script) by dispatching to the matching {@code codegen/{sessionId}/classes/{objectClass}/...}
     * route. Shared across backends: REST/SCIM and SQL connectors all resolve object-class scripts
     * through the same generation-service contract.
     */
    public ConnDevArtifactType generateObjectClassArtifact(ConnDevGenerateArtifactDefinitionType input, boolean skipCache) {
        var artifactSpec = input.getArtifact();
        var objectClass = artifactSpec.getObjectClass();
        var classification = ConnectorDevelopmentArtifacts.classify(artifactSpec);
        var body = repairContextBody(input);
        var content = switch (classification) {
            case NATIVE_SCHEMA_DEFINITION -> generateObjectClassScript(artifactSpec,
                    "native-schema", "native schema script", body, skipCache);
            case CONNID_SCHEMA_DEFINITION -> generateObjectClassScript(artifactSpec,
                    "connid", "ConnID mapping script", body, skipCache);
            case SEARCH_ALL_DEFINITION -> generateObjectClassScript(artifactSpec,
                    "search/" + ConnDevJsonMapper.toServiceIntent(artifactSpec.getIntent()),
                    "search script", body, skipCache);
            case SEARCH_BY_ID_DEFINITION -> generateObjectClassScript(artifactSpec,
                    "search/" + ConnDevJsonMapper.toServiceIntent(artifactSpec.getIntent()),
                    "search by ID script", body, skipCache);
            case SEARCH_FILTER_DEFINITION -> generateObjectClassScript(artifactSpec,
                    "search/" + ConnDevJsonMapper.toServiceIntent(artifactSpec.getIntent()),
                    "search filter script", body, skipCache);
            case CREATE -> generateObjectClassScript(artifactSpec, "create", "Create script", body, skipCache);
            case UPDATE ->  generateObjectClassScript(artifactSpec, "update", "Update script", body, skipCache);
            case DELETE -> generateObjectClassScript(artifactSpec, "delete", "Delete script", body, skipCache);
            case RELATIONSHIP_SCHEMA_DEFINITION -> generateRelation(artifactSpec, input.getRelation(), body, skipCache);
            default -> throw new IllegalStateException("Unexpected script type: " + classification);
        };
        content = content.replace("${objectClass}", objectClass);
        return artifactSpec.content(content);
    }

    private ObjectNode repairContextBody(ConnDevGenerateArtifactDefinitionType input) {
        var body = JSON_FACTORY.objectNode();
        var artifact = input.getArtifact();
        body.set("currentScript", JSON_FACTORY.textNode(
                artifact != null && artifact.getContent() != null ? artifact.getContent() : ""));
        var errors = JSON_FACTORY.arrayNode();
        input.getMidpointError().forEach(errors::add);
        body.set("midpointErrors", errors);
        var preferredEndpoints = JSON_FACTORY.arrayNode();
        for (var endpoint : input.getEndpoint()) {
            var jsonEndpoint = JSON_FACTORY.objectNode();
            jsonEndpoint.set("method", JSON_FACTORY.textNode(ConnDevJsonMapper.toValue(endpoint.getOperation())));
            jsonEndpoint.set("path", JSON_FACTORY.textNode(endpoint.getUri()));
            preferredEndpoints.add(jsonEndpoint);
        }
        body.set("preferredEndpoints", preferredEndpoints);
        return body;
    }

    private String generateRelation(ConnDevArtifactType artifactSpec, List<ConnDevRelationInfoType> relation, ObjectNode body, boolean skipCache) {
        try(var job = client().postJob("codegen/{sessionId}/relations/" + artifactSpec.getObjectClass(), body, null, skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), json -> json.get("code").asText());
        } catch (IOException e) {
            throw new SystemException("Couldn't generate relation for objectClass " + artifactSpec.getObjectClass(), e);
        }
    }

    private String generateObjectClassScript(ConnDevArtifactType artifactSpec, String endpointSuffix, String scriptDescription, ObjectNode body, boolean skipCache) {
        var apiType = "connid".equals(endpointSuffix) ? null : apiType();
        try(var job = client().postJob("codegen/{sessionId}/classes/"+ artifactSpec.getObjectClass() + "/" + endpointSuffix, body, apiType, skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), json -> json.get("code").asText());
        } catch (IOException e) {
            throw new SystemException("Couldn't generate " + scriptDescription + " for objectClass " + artifactSpec.getObjectClass(), e);
        }
    }

    public abstract List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass, boolean skipCache);
    public abstract List<ConnDevHttpEndpointType> discoverConnectivityEndpoints(boolean skipCache);

    public void populateConnectivityEndpoints(List<ConnDevHttpEndpointType> endpoints) throws CommonException {
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_SUGGESTED_ENDPOINT)
                .replaceRealValues(endpoints.stream().limit(MAX_SUGGESTED_CONNECTIVITY_ENDPOINTS).toList())
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    public ConnDevArtifactType getArtifactContent(ConnDevArtifactType type) throws IOException {
        var ret = type.clone();
        ret.content(editableConnector().readFile(type.getFilename()));
        return ret;
    }

    public void updateConfigurationOverride() throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException,
            ObjectAlreadyExistsException, SubscriptionComplianceException {
        if (skipConfigurationPropsUpgrade) {
            return;
        }

        var props = new Properties();
        updateConfigurationOverride(props);

        try (var stream = new ByteArrayOutputStream()) {
            props.store(stream, null);
            var propString = stream.toString(StandardCharsets.UTF_8);
            editableConnector().saveFile(CONFIGURATION_OVERRIDE, propString);
        } catch (IOException e) {
            throw new SystemException("Couldn't write connector configuration override (" + CONFIGURATION_OVERRIDE + ")", e);
        }
        var connRef = development.getConnector().getConnectorRef();
        if (connRef != null && deleteConnectorSchema) {
            var delta = PrismContext.get().deltaFor(ConnectorType.class)
                            .item(ConnectorType.F_SCHEMA).replace()
                            .<ConnectorType>asObjectDelta(connRef.getOid());
            beans.modelService.executeChanges( List.of(delta), null, task, result);

        }
    }

    protected void updateConfigurationOverride(Properties props) {
        var enabledAuths = new HashSet<SupportedAuthorization>();
        for (var auth : development.getConnector().getAuth()) {
            enabledAuths.add(SupportedAuthorization.forAuthorizationType(auth.getType()));
        }
        for (SupportedAuthorization auth : SupportedAuthorization.values()) {
            if (!enabledAuths.contains(auth)) {
                for (var confProp : auth.attributesFor(development.getApplication().getIntegrationType())) {
                    props.setProperty(confProp.getLocalPart(), "ignore");
                }
            }
        }
    }

    public abstract void processDocumentation(boolean skipCache) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            PolicyViolationException, ObjectAlreadyExistsException, SubscriptionComplianceException;

    public void ensureDocumentationIsProcessed() throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException,
            ObjectAlreadyExistsException, SubscriptionComplianceException {
        if (development.getProcessedDocumentation().isEmpty()) {
            processDocumentation(false);
            reload();
        }
    }

    public String connectorDisplayName() {
        var ret = development.getConnector().getDisplayName();
        if (ret == null) {
            ret = development.getApplication().getApplicationName().plus(" Connector");
        }
        return ret.getOrig();
    }

    public boolean isOnline() {
        return false;
    }


    public abstract List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered, boolean skipCache);

    public void updateRelations(List<ConnDevRelationInfoType> relations) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException,
            ObjectAlreadyExistsException, SubscriptionComplianceException {
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_DETECTED_SCHEMA, ConnDevSchemaType.F_RELATION).replaceRealValues(relations)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    protected abstract void restoreSession(ServiceClient.RestorationClient client) throws IOException;

    protected void synchronizeSession(ServiceClient.RestorationClient client) throws IOException {
        // FIXME: Implement session synchronization here
        // ensureDocumentationIsUploaded(client);
    }

    protected void restoreObjectClasses(ServiceClient.RestorationClient client) throws IOException {
        var app = developmentObject().getApplication();
        if (app == null) return;
        var schema = app.getDetectedSchema();
        if (schema == null) return;

        var appClasses = schema.getObjectClass();
        if (appClasses == null || appClasses.isEmpty()) return;
        var text = ConnDevJsonMapper.mapObjectClassesToJson(appClasses).toPrettyString();

        client.put("digester/{sessionId}/classes", () ->
                EntityBuilder.create()
                        .setContentType(ContentType.APPLICATION_JSON)
                        .setText(text)
                        .build());
    }

    protected void restoreEndpoints(ServiceClient.RestorationClient client) throws IOException {
        var app = developmentObject().getApplication();
        if (app == null) return;
        var schema = app.getDetectedSchema();
        if (schema == null) return;

        for (var appOc : schema.getObjectClass()) {
            var name = appOc.getName();
            var endpoints = appOc.getEndpoint();
            if (endpoints == null || endpoints.isEmpty()) continue;

            client.put("digester/{sessionId}/classes/" + name + "/endpoints", () ->
                    EntityBuilder.create()
                            .setContentType(ContentType.APPLICATION_JSON)
                            .setText(ConnDevJsonMapper.mapEndpointsToJson(endpoints).toPrettyString())
                            .build());
        }
    }

    protected void restoreAttributes(ServiceClient.RestorationClient client) throws IOException {
        var connector = developmentObject().getConnector();
        if (connector == null) return;

        for (var connectorOc : connector.getObjectClass()) {
            var name = connectorOc.getName();
            var attributes = connectorOc.getAttribute();
            if (attributes == null || attributes.isEmpty()) continue;

            client.put("digester/{sessionId}/classes/" + name + "/attributes", () ->
                    EntityBuilder.create()
                            .setContentType(ContentType.APPLICATION_JSON)
                            .setText(ConnDevJsonMapper.mapAttributesToJson(attributes).toPrettyString())
                            .build());
        }
    }

    protected void restoreMetadata(ServiceClient.RestorationClient client) throws IOException {
        var app = developmentObject().getApplication();
        if (app == null) return;

        var infoMetadata = JSON_FACTORY.objectNode();

        if (app.getApplicationName() != null) {
            infoMetadata.set("name", JSON_FACTORY.textNode(app.getApplicationName().getOrig()));
        }
        if (app.getVersion() != null) {
            infoMetadata.set("applicationVersion", JSON_FACTORY.textNode(app.getVersion()));
        }
        if (app.getApiVersion() != null) {
            infoMetadata.set("apiVersion", JSON_FACTORY.textNode(app.getApiVersion()));
        }
        if (app.getIntegrationType() != null) {
            var apiTypeArray = JSON_FACTORY.arrayNode();
            apiTypeArray.add(app.getIntegrationType().value());
            infoMetadata.set("apiType", apiTypeArray);
        }
        if (app.getBaseApiEndpoint() != null) {
            var endpointEntry = JSON_FACTORY.objectNode();
            endpointEntry.set("uri", JSON_FACTORY.textNode(app.getBaseApiEndpoint()));
            endpointEntry.set("type", JSON_FACTORY.textNode("constant"));
            var endpointsArray = JSON_FACTORY.arrayNode();
            endpointsArray.add(endpointEntry);
            var availability = JSON_FACTORY.objectNode();
            availability.set("baseApiEndpoint", endpointsArray);
            var availabilityKey = app.getIntegrationType() == ConnDevIntegrationType.SCIM
                    ? "scimAvailability" : "restAvailability";
            infoMetadata.set(availabilityKey, availability);
        }

        var body = JSON_FACTORY.objectNode();
        body.set("infoMetadata", infoMetadata);
        var bodyText = body.toPrettyString();

        client.put("digester/{sessionId}/metadata", () ->
                EntityBuilder.create()
                        .setContentType(ContentType.APPLICATION_JSON)
                        .setText(bodyText)
                        .build());
    }

    protected void restoreRelations(ServiceClient.RestorationClient client) throws IOException {
        var app = developmentObject().getApplication();
        if (app == null) return;
        var schema = app.getDetectedSchema();
        if (schema == null) return;

        var relations = schema.getRelation();
        if (relations == null || relations.isEmpty()) return;

        client.put("digester/{sessionId}/relations", () ->
                EntityBuilder.create()
                        .setContentType(ContentType.APPLICATION_JSON)
                        .setText(ConnDevJsonMapper.mapRelationsToJson(relations).toPrettyString())
                        .build());
    }

    protected void restoreCodegenArtifacts(ServiceClient.RestorationClient client) throws IOException {
        var connector = developmentObject().getConnector();
        if (connector == null) return;

        for (var oc : connector.getObjectClass()) {
            var name = oc.getName();
            putCodegenArtifact(client, "codegen/{sessionId}/classes/" + name + "/native-schema", oc.getNativeSchemaScript());
            putCodegenArtifact(client, "codegen/{sessionId}/classes/" + name + "/connid", oc.getConnidSchemaScript());
            putCodegenArtifact(client, "codegen/{sessionId}/classes/" + name + "/create", oc.getCreateScript());
            putCodegenArtifact(client, "codegen/{sessionId}/classes/" + name + "/update", oc.getUpdateScript());
            putCodegenArtifact(client, "codegen/{sessionId}/classes/" + name + "/delete", oc.getDeleteScript());
            var searchAll = oc.getSearchAllOperation();
            if (searchAll != null && searchAll.getFilename() != null && searchAll.getIntent() != null) {
                putCodegenArtifact(client, "codegen/{sessionId}/classes/" + name + "/search/" + searchAll.getIntent().value(), searchAll);
            }
        }

        for (var relation : connector.getRelation()) {
            putCodegenArtifact(client, "codegen/{sessionId}/relations/" + relation.getName(), relation.getSchemaScript());
        }

        putCodegenArtifact(client, "codegen/{sessionId}/authorization", connector.getAuthenticationScript());
    }

    protected void putCodegenArtifact(ServiceClient.RestorationClient client, String path, ConnDevArtifactType artifact) throws IOException {
        if (artifact == null || artifact.getFilename() == null) return;
        var content = getArtifactContent(artifact).getContent();
        if (content == null) return;
        var body = JSON_FACTORY.objectNode();
        body.set("code", JSON_FACTORY.textNode(content));
        var bodyText = body.toPrettyString();
        client.put(path, () -> EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(bodyText)
                .build());
    }

    protected List<ProcessedDocumentation> getProcessedDocumentation() {
        return developmentObject().getProcessedDocumentation().stream()
                .map(ProcessedDocumentation::new).toList();
    }

    public void ensureDocumentationIsUploaded(ServiceClient.RestorationClient client) {
        try {
            for (var documentation : getProcessedDocumentation()) {
                client.putDocumentationIfMissing(
                        "session/{sessionId}/documentation/" + documentation.uuid(), () -> {
                            try {
                                var body = new String(documentation.asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                                return EntityBuilder.create()
                                        .setText(body)
                                        .setContentType(ContentType.create(documentation.contentType(), StandardCharsets.UTF_8))
                                        .build();
                            } catch (IOException e) {
                                throw new SystemException("Couldn't build documentation upload body", e);
                            }
                        });
            }
        } catch (Exception e) {
            throw new SystemException("Couldn't upload documentation", e);
        }
    }

    private String filenameFrom(ProcessedDocumentation documentation) {
        /*
        var suffix = switch (documentation.contentType()) {
            case "application/yaml" -> "yml";
            case "application/json" -> "json";
            default -> "txt";
        };
        */
        return documentation.uri();
    }

    /**
     * Connector-agnostic dev-schema refresh: refreshes the testing resource, and if it exposes the
     * shared {@code conndev_ObjectClass} (SCIM/SQL/... in development mode), reads the development
     * object classes ({@link #devDocumentationObjectClasses()}) and turns their objects into
     * {@link ProcessedDocumentation}. Classic REST has no schema standard, so it simply does not
     * expose {@code conndev_ObjectClass} and is skipped — no connector-type branching needed.
     */
    public void refreshConnDevDocumentation() throws CommonException {
        var testing = developmentObject().getTesting();
        if (testing == null || testing.getTestingResource() == null) {
            return;
        }
        var testingResourceOid = testing.getTestingResource().getOid();

        ResourceUtils.deleteSchema(testingResourceOid, beans.modelService, task, result);
        beans.provisioningService.testResource(testingResourceOid, task, result);

        if (!exposesConnDevObjectClass(testingResourceOid)) {
            return; // classic REST, or development mode off — nothing to discover
        }

        var objectClasses = devDocumentationObjectClasses();
        var shadowDocs = objectClasses.stream()
                .flatMap(objectClass -> loadShadowsAsDocumentation(testingResourceOid, objectClass).stream())
                .toList();

        // Push the freshly built dev-shadow documentation to the generation service and pull the
        // processed result back (see synchronizeDocumentation). Offline/base does nothing and keeps
        // the docs as built; REST/SCIM overrides it to POST each doc and pull the processed items.
        var newDocs = synchronizeDocumentation(shadowDocs).stream()
                .map(ProcessedDocumentation::toBean)
                .toList();

        var mergedDocs = new ArrayList<>(
                developmentObject().getProcessedDocumentation().stream()
                        .filter(d -> objectClasses.stream().noneMatch(c -> d.getUri().startsWith(c)))
                        .map(ProcessedDocumentationType::clone)
                        .toList());
        mergedDocs.addAll(newDocs);

        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_PROCESSED_DOCUMENTATION)
                .replaceRealValues(mergedDocs)
                .<ConnectorDevelopmentType>asObjectDelta(developmentObject().getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
    }

    private String sessionId() {
        return developmentObject().getOid();
    }

    protected ServiceClient client() {
        return beans.client(sessionId(), this::restoreSession, this::synchronizeSession, result);
    }

    protected String apiType() {
        var app = developmentObject().getApplication();
        var integrationType = app != null ? app.getIntegrationType() : null;
        return integrationType != null ? integrationType.value() : null;
    }

    /**
     * Pushes each freshly built dev-shadow documentation to the connector-generation service via
     * {@code POST session/{sessionId}/documentation/{docId}} and pulls the processed result back into
     * midPoint. The service processes each upload with the LLM (chunking it into {@code DocumentationItem}s)
     * as an asynchronous job, and the jobs run in parallel, so all docs are submitted first and only then
     * are the resulting jobs harvested (submit-all-then-wait) instead of blocking on each doc in turn.
     * Backends without a generation service (offline) override this to skip synchronization entirely.
     */
    protected List<ProcessedDocumentation> synchronizeDocumentation(List<DevShadowDocument> documentation) throws CommonException {
        if (documentation.isEmpty()) {
            return List.of();
        }

        var sync = client().synchronizationClient();
        try {
            // Submit every upload first so the (parallel) processing jobs run concurrently on the service.
            for (var doc : documentation) {
                sync.postDocumentation(
                        doc.uuid(),
                        new ByteArrayInputStream(doc.content().getBytes(StandardCharsets.UTF_8)),
                        ContentType.create(doc.contentType(), StandardCharsets.UTF_8),
                        doc.uri());
            }

            // Harvest: wait for each upload to finish processing (HEAD 204), then pull the processed
            // content back from the service and only now materialize it as a ProcessedDocumentation,
            // keeping the original uri/uuid so the merge in the caller works.
            var synced = new ArrayList<ProcessedDocumentation>();
            for (var doc : documentation) {
                sync.awaitDocumentation(doc.uuid(), SLEEP_TIME, canRun());
                var content = extractDocumentationContent(sync.getDocumentation(doc.uuid()));
                var processed = new ProcessedDocumentation(doc.uuid(), doc.uri()).contentType(doc.contentType());
                processed.write(content);
                synced.add(processed);
            }
            return synced;
        } catch (IOException e) {
            throw new SystemException("Couldn't synchronize documentation with the generation service", e);
        }
    }

    /**
     * Extracts the documentation content from a {@code GET documentation/{docId}} bundle. The bundle is
     * {@code {docId, chunks:[{content, ...}]}}; a conndev schema is preserved as a single item, so there
     * is normally one chunk whose {@code content} is the original schema JSON. Multiple chunks are joined
     * (best effort); a bundle with no chunk content falls back to the raw bundle JSON.
     */
    private String extractDocumentationContent(String bundleJson) throws IOException {
        var chunks = MAPPER.readTree(bundleJson).get("chunks");
        if (chunks == null || !chunks.isArray() || chunks.isEmpty()) {
            return bundleJson;
        }
        var contents = new StringBuilder();
        for (var chunk : chunks) {
            var content = chunk.get("content");
            if (content != null && !content.isNull()) {
                if (contents.length() > 0) {
                    contents.append("\n");
                }
                contents.append(content.asText());
            }
        }
        return contents.length() > 0 ? contents.toString() : bundleJson;
    }

    public List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(
            List<ConnDevBasicObjectClassInfoType> connectorDiscovered, boolean includeUnrelated, boolean skipCache) {
        try (var job = client().postJob("digester/{sessionId}/classes", apiType(), skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ArrayList<ConnDevBasicObjectClassInfoType>();
                var jsonClasses = o.get("objectClasses");
                for (var jsonClass : jsonClasses) {
                    var objClass = ConnDevJsonMapper.mapObjectClassFromJson(jsonClass);
                    if (objClass.isRelevant() || includeUnrelated) {
                        ret.add(objClass);
                    }
                }
                return ret;
            });
        } catch (IOException e) {
            throw new SystemException("Couldn't discover object classes from documentation", e);
        }
    }

    public List<ConnDevAttributeInfoType> discoverObjectClassAttributes(String objectClass, boolean skipCache) {
        try (var job = client().postJob("digester/{sessionId}/classes/" + objectClass + "/attributes", apiType(), skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ArrayList<ConnDevAttributeInfoType>();
                var jsonAttributes = (ObjectNode) o.get("attributes");
                for (var entry : jsonAttributes.properties()) {
                    ret.add(ConnDevJsonMapper.mapAttributeFromJson(entry.getKey(), entry.getValue()));
                }
                return ret;
            });
        } catch (IOException e) {
            throw new SystemException("Couldn't discover attributes for object class " + objectClass, e);
        }
    }

    /**
     * Development object classes whose objects are forwarded as documentation: the shared
     * {@code conndev_ObjectClass} for every connector; backends may add their own raw exports
     * (e.g. SCIM adds {@code conndev_ScimSchema}/{@code conndev_ScimResource}).
     */
    protected List<String> devDocumentationObjectClasses() {
        return List.of(CONNDEV_OBJECT_CLASS);
    }

    private boolean exposesConnDevObjectClass(String resourceOid) {
        try {
            var resource = beans.modelService.getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();
            var schema = Resource.of(resource).getCompleteSchema();
            return schema != null
                    && schema.findObjectClassDefinition(new QName(SchemaConstants.NS_RI, CONNDEV_OBJECT_CLASS)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Loads shadows of a given {@code conndev_*} object class from a testing resource and forwards each
     * as a {@link DevShadowDocument}, faithfully: the shadow content is only structurally unwrapped from
     * prism serialization ({@link ConnDevShadowUnwrapper}), never interpreted — the connector owns the
     * schema-mapping content (single source), midPoint reads only the name (for uri/uuid). The result is
     * an in-memory carrier; it becomes a persisted {@link ProcessedDocumentation} only in
     * {@link #synchronizeDocumentation}. Connector-agnostic core: any connector exposing
     * {@code conndev_ObjectClass} (SCIM, SQL, ...) works unchanged, including future fields.
     */
    protected List<DevShadowDocument> loadShadowsAsDocumentation(String resourceOid, String objectClassLocalName) {
        try {
            var objectClass = new QName(SchemaConstants.NS_RI, objectClassLocalName);
            var query = PrismContext.get().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
                    .build();

            // Associations must not be fetched: the embedded reference values have no OID/identifiers,
            // so the validity checker would fail them with "No effective operation policy".
            var options = GetOperationOptionsBuilder.create()
                    .item(ShadowType.F_ASSOCIATIONS).dontRetrieve()
                    .build();
            var shadows = beans.provisioningService.searchObjects(ShadowType.class, query, options, task, result);
            if (shadows.isEmpty()) {
                return List.of();
            }

            var serializer = PrismContext.get().jsonSerializer();
            var mapper = new ObjectMapper();
            var writer = mapper.writerWithDefaultPrettyPrinter();
            var unwrapper = new ConnDevShadowUnwrapper();
            var docs = new ArrayList<DevShadowDocument>();
            for (var shadow : shadows) {
                var attrs = shadow.findContainer(ShadowType.F_ATTRIBUTES);
                if (attrs == null || attrs.isEmpty()) {
                    continue;
                }

                var json = serializer.serialize(attrs.getValue());
                var attributesContainer = mapper.readTree(json).get("attributes");
                if (attributesContainer == null) {
                    continue;
                }

                var document = unwrapper.unwrap(attributesContainer);
                var content = writer.writeValueAsString(document);

                var name = resolveDocName(shadow, document);
                var uri = objectClassLocalName + "_" + name + ".json";
                var uuid = UUID.nameUUIDFromBytes(uri.getBytes(StandardCharsets.UTF_8)).toString();
                docs.add(new DevShadowDocument(uuid, uri, CONNDEV_CONTENT_TYPE, content));
            }
            return docs;
        } catch (Exception e) {
            throw new SystemException("Could not load shadow documentation for " + objectClassLocalName, e);
        }
    }

    /**
     * Human-readable name for a dev shadow: the discovered object class name (the unwrapped
     * {@code icfs:name}), then the shadow's own name (__NAME__), finally the OID. This is the only
     * piece of the forwarded content midPoint reads — everything else is passed through untouched.
     */
    protected String resolveDocName(PrismObject<ShadowType> shadow, JsonNode document) {
        var name = document.path("name");
        if (name.isArray() && !name.isEmpty()) {
            name = name.get(0);
        }
        if (name.isTextual() && !name.asText().isBlank()) {
            return name.asText();
        }
        if (shadow.getName() != null && shadow.getName().getOrig() != null) {
            return shadow.getName().getOrig();
        }
        return shadow.getOid();
    }

}
