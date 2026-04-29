package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.ucf.api.EditableConnector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.smart.impl.mappings.ConnDevJsonMapper;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.function.BooleanSupplier;

public abstract class ConnectorDevelopmentBackend {

    private static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;
    private static final String CONNECTOR_MANIFEST = "connector.manifest.json";
    private static final String CONFIGURATION_OVERRIDE = "configurationOverride.properties";
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
            case DUMMY -> new OfflineBackend(beans, connDev, task, result);
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

    protected void reload() throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        development = beans.modelService.getObject(ConnectorDevelopmentType.class, development.getOid(), null, task, result).asObjectable();
    }

    public void populateApplicationAuthInfo(List<ConnDevAuthInfoType> authInfo) throws CommonException {
        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_AUTH)
                .addRealValues( authInfo.stream().map(ConnDevAuthInfoType::clone).toList())
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
        var modelArtifact = artifact.clone().content(null);

        var delta = deltaBuilder
                .item(itemPath).replace(modelArtifact)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
        recomputeConnectorManifest();
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
                    .relevant(v.getRelevant());
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

    public abstract ConnDevApplicationInfoType discoverBasicInformation();
    public abstract List<ConnDevAuthInfoType> discoverAuthorizationInformation();
    public abstract List<ConnDevDocumentationSourceType> discoverDocumentation();
    public abstract ConnDevArtifactType generateArtifact(ConnDevGenerateArtifactDefinitionType artifactSpec);
    public abstract ConnDevArtifactType generateObjectClassArtifact(ConnDevGenerateArtifactDefinitionType artifactSpec);
    public abstract List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(List<ConnDevBasicObjectClassInfoType> connectorDiscovered, boolean includeUnrelated);
    public abstract List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass);
    public abstract List<ConnDevAttributeInfoType> discoverObjectClassAttributes(String objectClass);

    public ConnDevArtifactType getArtifactContent(ConnDevArtifactType type) throws IOException {
        var ret = type.clone();
        ret.content(editableConnector().readFile(type.getFilename()));
        return ret;
    }

    public void updateConfigurationOverride() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
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
            throw new RuntimeException(e);
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

    public abstract void processDocumentation() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException;

    public void ensureDocumentationIsProcessed() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        if (development.getProcessedDocumentation().isEmpty()) {
            processDocumentation();
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


    public abstract List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered);

    public void updateRelations(List<ConnDevRelationInfoType> relations) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
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
                                        .setContentType(ContentType.APPLICATION_JSON)
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

}
