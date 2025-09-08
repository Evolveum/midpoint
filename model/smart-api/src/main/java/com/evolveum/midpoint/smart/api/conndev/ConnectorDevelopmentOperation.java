package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.IOException;
import java.util.List;

public interface ConnectorDevelopmentOperation {

    ConnectorDevelopmentType getObject();

    // AI only
    String submitDiscoverDocumentation(Task task, OperationResult result);

    // AI only
    StatusInfo<ConnectorDevelopmentType> processDocumentation(PrismContainer<ConnDevDocumentationSourceType> sources);

    String submitDiscoverBasicInformation(Task task, OperationResult result);

    // Midpoint local (AI optional in background)
    void basicConnectorInfoUpdated(ConnectorDevelopmentType  updated);

    // AI optional
    StatusInfo<PrismContainer<ConnDevAuthInfoType>>  selectBaseApiInformation(String basicInfo);

    String submitCreateConnector(Task task, OperationResult result);

    // Midpoint local (+ download framework)
    String generateAuthenticationScript(Task task, OperationResult result);

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
    String getArtifactContent(ConnDevArtifactType type);

    // local
    void saveNativeSchemaScript(ConnDevArtifactType type, String updated);

    // local
    BareResourceSchema testSchema(ConnDevArtifactType type);

    // AI only
    StatusInfo<PrismContainer<ConnDevHttpEndpointType>> getSearchEndpoints(String objectClass);

    // AI + local skeleton
    StatusInfo<ConnDevArtifactType> generateSearchAll(String objectClass, ConnDevHttpEndpointType endpoint);

    // local
    void testSearchAll(String objectClass, ConnDevArtifactType script);

    // local
    void saveSearchAll(ConnDevArtifactType script, String body);

    // local
    void saveArtifact(String objectClass, ConnDevArtifactType endpoint);

    StatusInfo<ConnDevArtifactType> generateGet(String objectClass, ConnDevHttpEndpointType endpoint);

    void testGet(String objectClass, ConnDevArtifactType script);

    void saveGet(ConnDevArtifactType script, String body);

    void saveAuthenticationScript(ConnDevArtifactType artifact, Task task, OperationResult result) throws IOException;
}
