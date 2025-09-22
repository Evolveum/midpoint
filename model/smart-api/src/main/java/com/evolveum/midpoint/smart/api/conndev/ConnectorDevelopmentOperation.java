package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts.KnownArtifactType.*;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public interface ConnectorDevelopmentOperation {

    ConnectorDevelopmentType getObject();

    // AI only
    String submitDiscoverDocumentation(Task task, OperationResult result);

    // AI only
    String submitProcessDocumentation(Task task, OperationResult result);


    // AI only
    StatusInfo<ConnectorDevelopmentType> processDocumentation(PrismContainer<ConnDevDocumentationSourceType> sources);

    String submitDiscoverBasicInformation(Task task, OperationResult result);

    // Midpoint local (AI optional in background)
    void basicConnectorInfoUpdated(ConnectorDevelopmentType  updated);

    // AI optional
    StatusInfo<PrismContainer<ConnDevAuthInfoType>>  selectBaseApiInformation(String basicInfo);

    String submitCreateConnector(Task task, OperationResult result);

    // Midpoint local (+ download framework)

    String submitDiscoverObjectClasses(Task task, OperationResult result);

    String submitDiscoverObjectClassDetails(String user, Task testTask, OperationResult testOperationResult);

    String submitGenerateArtifact(ConnDevArtifactType artifact, Task testTask, OperationResult testOperationResult);

    default String submitGenerateNativeSchema(String objectClass, Task task, OperationResult result) {
        return submitGenerateArtifact(NATIVE_SCHEMA_DEFINITION.create(objectClass), task, result);
    }

    default String submitGenerateConnIdSchema(String objectClass, Task testTask, OperationResult testOperationResult) {
        return submitGenerateArtifact(CONNID_SCHEMA_DEFINITION.create(objectClass), testTask, testOperationResult);
    }

    default String submitGenerateAuthenticationScript(Task task, OperationResult result) {
        return submitGenerateArtifact(AUTHENTICATION_CUSTOMIZATION.create(), task, result);
    }

    default String submitGenerateSearchScript(String objectClass, List<ConnDevHttpEndpointType> endpoints, Task task, OperationResult result) {
        return submitGenerateArtifact(SEARCH_ALL_DEFINITION.create(objectClass),
                definition -> {
                    if (endpoints != null && !endpoints.isEmpty()) {
                        for (var endpoint : endpoints) {
                            // Somehow clone triggers NPE in serializer
                            definition.endpoint(new ConnDevHttpEndpointType()
                                    .uri(endpoint.getUri())
                                    .name(endpoint.getName())
                                    .operation(endpoint.getOperation())
                                    .requestContentType(endpoint.getRequestContentType())
                                    .responseContentType(endpoint.getResponseContentType())
                            );
                        }
                    }
                }, task, result);
    }

    default String submitGenerateRelationScript(ConnDevRelationInfoType relation, Task task, OperationResult result) {
        return submitGenerateArtifact(RELATIONSHIP_SCHEMA_DEFINITION.create(relation.getName()),
                definition -> definition.relation(relation.clone()), task, result);
    }

    String submitGenerateArtifact(ConnDevArtifactType artifact, Consumer<ConnDevGenerateArtifactDefinitionType> customizer, Task task, OperationResult result);
    // FIXME: Also add operation results
    // Midpoint local
    ResourceType testConnection(ConnectorConfigurationType type);

    // AI optional
    StatusInfo<PrismContainer<ConnDevBasicObjectClassInfoType>> discoverObjectClasses();

    // AI optional
    StatusInfo<PrismContainer<ConnDevAttributeInfoType>> generateAttributes(ConnDevBasicObjectClassInfoType type);

    // AI required for REST, could be local for SCIM partially
    StatusInfo<ConnDevArtifactType> generateNativeSchemaScript(PrismContainer<ConnDevAttributeInfoType> type);

    // local
    String getArtifactContent(ConnDevArtifactType type, Task task, OperationResult result) throws IOException;
    // local
    BareResourceSchema testSchema(ConnDevArtifactType type);

    // AI only
    StatusInfo<PrismContainer<ConnDevHttpEndpointType>> getSearchEndpoints(String objectClass);

    // AI + local skeleton
    StatusInfo<ConnDevArtifactType> generateSearchAll(String objectClass, ConnDevHttpEndpointType endpoint);

    // local
    void testSearchAll(String objectClass, ConnDevArtifactType script);


    void saveArtifact(ConnDevArtifactType endpoint, Task task, OperationResult result) throws IOException, CommonException;


    default void  saveNativeSchemaScript(ConnDevArtifactType artifact, Task task, OperationResult result) throws IOException, CommonException {
        saveArtifact(artifact, task, result);
        resetResourceSchema(task, result);
    }

    default void saveRelationScript(ConnDevArtifactType endpoint, Task task, OperationResult result) throws IOException, CommonException {
        saveArtifact(endpoint, task, result);
    }


    default void saveAuthenticationScript(ConnDevArtifactType artifact, Task task, OperationResult result) throws IOException, CommonException {
        saveArtifact(artifact, task, result);
    }

    default void saveConnIdSchemaScript(ConnDevArtifactType artifact, Task task, OperationResult result) throws IOException, CommonException {
        saveArtifact(artifact, task, result);
        resetResourceSchema(task, result);
    }
    void resetResourceSchema(Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException;

    default void saveSearchAllScript(ConnDevArtifactType artifact, Task task, OperationResult result) throws IOException, CommonException {
        saveArtifact(artifact, task, result);
    }

    List<ConnDevHttpEndpointType> suggestedEndpointsFor(String objectClass, ConnectorDevelopmentArtifacts.KnownArtifactType knownArtifactType);

    void authenticationSelectionUpdated(Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException;
}
