package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.ucf.api.EditableConnector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public abstract class ConnectorDevelopmentBackend {

    private static final String CONNECTOR_MANIFEST = "connector.manifest.json";
    private static final String CONFIGURATION_OVERRIDE = "configurationOverride.properties";
    protected final Task task;
    protected final OperationResult result;

    ConnDevBeans beans;
    private ConnectorDevelopmentType development;
    private EditableConnector editableConnector;
    protected boolean deleteConnectorSchema = false;
    protected boolean skipConfigurationPropsUpgrade = false;

    public ConnectorDevelopmentBackend(ConnDevBeans beans, ConnectorDevelopmentType development, Task task, OperationResult result) {
        this.beans = beans;
        this.development = development;
        this.task = task;
        this.result = result;
    }


    public static ConnectorDevelopmentBackend backendFor(String connectorDevelopmentOid, Task task, OperationResult result) throws CommonException {
        var beans = ConnDevBeans.get();
        var connDev = beans.modelService.getObject(ConnectorDevelopmentType.class, connectorDevelopmentOid, null, task, result);
        // FIXME: Select offline mode if system is configured to be offline.
        return backendFor(connDev.asObjectable(), task, result);
    }

    private static ConnectorDevelopmentBackend backendFor(ConnDevIntegrationType integrationType, ConnectorDevelopmentType connDev, ConnDevBeans beans, Task task, OperationResult result) {
        return switch (integrationType) {
            case REST -> new RestBackend(beans, connDev, task, result);
            case SCIM -> throw new UnsupportedOperationException();
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
            .item(ConnectorDevelopmentType.F_APPLICATION).replace(type.clone())
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
        var itemPath = itemPathFor(artifact);
        saveConnectorFile(artifact.getFilename(), artifact.getContent());
        var modelArtifact = artifact.clone().content(null);

        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(itemPath).replace(modelArtifact)
                .<ConnectorDevelopmentType>asObjectDelta(development.getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
        recomputeConnectorManifest();
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

    private ItemPath itemPathFor(ConnDevArtifactType artifact) {
        ItemPath path = ConnectorDevelopmentType.F_CONNECTOR;
        if (artifact.getObjectClass() != null) {
            path = path.append(ConnDevConnectorType.F_OBJECT_CLASS);
            var objClass = development.getConnector().getObjectClass().stream().filter(o -> o.getName().equals(artifact.getObjectClass())).findFirst().orElse(null);
            if (objClass == null) {
                throw new UnsupportedOperationException("No connector class found for object class " + artifact.getObjectClass());
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
        List<ConnDevObjectClassInfoType> applicationClasses = discovered.stream().map(v -> new ConnDevObjectClassInfoType()
                .name(v.getName())
                .description(v.getDescription())
                .embedded(v.getEmbedded())
                ._abstract(v.isAbstract())
                .superclass(v.getSuperclass())
                .relevant(v.getRelevant())
        ).toList();
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
    public abstract ConnDevArtifactType generateArtifact(ConnDevArtifactType artifactSpec);
    public abstract ConnDevArtifactType generateObjectClassArtifact(ConnDevArtifactType artifactSpec);
    public abstract List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(List<ConnDevBasicObjectClassInfoType> connectorDiscovered);
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
        if(development.getProcessedDocumentation().isEmpty()) {
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
}
