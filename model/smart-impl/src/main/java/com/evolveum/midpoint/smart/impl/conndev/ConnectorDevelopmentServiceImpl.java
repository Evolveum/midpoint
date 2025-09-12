/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;

import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.smart.impl.StatusInfoImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
public class ConnectorDevelopmentServiceImpl implements ConnectorDevelopmentService {

    /** Auto cleanup time for background tasks created by the service. Will be shorter, probably. */
    private static final Duration AUTO_CLEANUP_TIME = XmlTypeConverter.createDuration("P1D");

    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private TaskManager taskManager;

    private static ConnectorDevelopmentServiceImpl instance;

    @Override
    public ConnectorDevelopmentOperation startFromNew(ConnDevApplicationInfoType basicInfo, OperationResult result) {
        return null;
    }

    @Override
    public ConnectorDevelopmentOperation continueFrom(ConnectorDevelopmentType type) {
        return new OperationWrapper(type);
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
            return submitTask("Creating editable connector for " + stateObject.getOid(),
                    new WorkDefinitionsType().createConnector(new ConnDevCreateConnectorWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                            .baseTemplateUrl(connectorTemplateFor(stateObject.getConnector().getIntegrationType()))
                    ), task, result);
        }

        public String submitDiscoverBasicInformation(Task task, OperationResult result) {
            return submitTask("Discover Basic Information for " + stateObject.getOid(),
                    new WorkDefinitionsType().discoverGlobalInformation(new ConnDevDiscoverGlobalInformationWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        public String submitDiscoverDocumentation(Task task, OperationResult result) {
            return submitTask("Discovering documentation for " + stateObject.getOid(),
                    new WorkDefinitionsType().discoverDocumentation(new ConnDevDiscoverDocumentationWorkDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        @Override
        public String submitDiscoverObjectClasses(Task task, OperationResult result) {
            return submitTask("Discovering object classes for for " + stateObject.getOid(),
                    new WorkDefinitionsType().discoverObjectClassInformation(new ConnDevDiscoverObjectClassInformationDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                    ), task, result);
        }

        @Override
        public String submitDiscoverObjectClassDetails(String objectClass, Task task, OperationResult result) {
            return submitTask("Discovering '" + objectClass +"'object classe details for " + stateObject.getOid(),
                    new WorkDefinitionsType().discoverObjectClassDetails(new ConnDevDiscoverObjectClassDetailsDefinitionType()
                            .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                            .objectClass(objectClass)
                    ), task, result);
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
        public String submitGenerateArtifact(ConnDevArtifactType artifact, Task task, OperationResult result) {
            return submitTask("Generating script",
                    new WorkDefinitionsType().generateConnectorArtifact(
                            new ConnDevGenerateArtifactDefinitionType()
                                    .connectorDevelopmentRef(stateObject.getOid(), ConnectorDevelopmentType.COMPLEX_TYPE)
                                    .artifact(artifact.clone())
                    ), task, result);
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
        }

        public void comfirmApplicationInformation(Task task, OperationResult result) {
            ConnectorDevelopmentBackend.backendFor(stateObject, task, result).suggestConnectorCoordinates();
        }

        @Override
        public List<ConnDevHttpEndpointType> suggestedEndpointsFor(String user, ConnectorDevelopmentArtifacts.KnownArtifactType knownArtifactType) {
            var obj = stateObject.getApplication().getDetectedSchema().getObjectClass().stream()
                    .filter(o -> o.getName().equals(user)).findFirst().orElse(null);

            var use = switch (knownArtifactType.scriptIntent) {
                case ALL -> ConnDevHttpEndpointIntentType.GET_ALL;
                default -> throw new IllegalArgumentException();
            };

            return obj.getEndpoint().stream().filter(e -> e.getSuggestedUse().contains(use)).toList();
        }
    }

    private String submitTask(String name, WorkDefinitionsType work, Task task, OperationResult result) {
        try {
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(work),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name(name)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            return oid;
        } catch (Exception e) {
            throw new SystemException(e);
        }

    }

    private String connectorTemplateFor(ConnDevIntegrationType integrationType) {
        // FIXME: Dispatch to IntegrationType specific handler
        return "file:///home/tony/.m2/repository/com/evolveum/polygon/scimrest/connector-sample-scimdev-noclass/0.1-SNAPSHOT/connector-sample-scimdev-noclass-0.1-SNAPSHOT.jar";
    }

    private static @NotNull Collection<SelectorOptions<GetOperationOptions>> taskRetrievalOptions() {
        return GetOperationOptionsBuilder.create()
                .noFetch()
                .item(TaskType.F_RESULT).retrieve()
                .build();
    }

    private @NotNull TaskType getTask(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return taskManager
                .getObject(TaskType.class, oid, taskRetrievalOptions(), result)
                .asObjectable();
    }

    @Override
    public StatusInfo<ConnDevCreateConnectorResultType> getCreateConnectorStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return new StatusInfoImpl<>(
                getTask(token,result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevCreateConnectorResultType.class);
    }

    @Override
    public StatusInfoImpl<ConnDevDiscoverGlobalInformationResultType> getDiscoverBasicInformationStatus(String token, Task testTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return new StatusInfoImpl<>(
                getTask(token,result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverGlobalInformationResultType.class
                );
    }

    @Override
    public StatusInfo<ConnDevDiscoverDocumentationResultType> getDiscoverDocumentationStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return new StatusInfoImpl<>(
                getTask(token,result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverDocumentationResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevGenerateArtifactResultType> getGenerateArtifactStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return new StatusInfoImpl<>(
                getTask(token,result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevGenerateArtifactResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevDiscoverObjectClassInformationResultType> getDiscoverObjectClassInformationStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return new StatusInfoImpl<>(
                getTask(token,result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverObjectClassInformationResultType.class
        );
    }

    @Override
    public StatusInfo<ConnDevDiscoverObjectClassDetailsResultType> getDiscoverObjectClassDetailsStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return new StatusInfoImpl<>(
                getTask(token,result),
                ConnDevCreateConnectorWorkStateType.F_RESULT,
                ConnDevDiscoverObjectClassDetailsResultType.class
        );
    }
}
