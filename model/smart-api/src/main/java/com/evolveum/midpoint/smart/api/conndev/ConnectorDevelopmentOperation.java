package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public interface ConnectorDevelopmentOperation {

    ConnectorDevelopmentType getObject();


    StatusInfo<PrismContainer<ConnDevDocumentationSourceType>> discoverDocumentation(ConnectorDevelopmentType type);

    StatusInfo<ConnectorDevelopmentType> processDocumentation(PrismContainer<ConnDevDocumentationSourceType> sources);

    ConnectorDevelopmentType updateBasicInformation(ConnDevApplicationInfoType basicInfo);

    StatusInfo<PrismContainer<ConnDevAuthInfoType>>  selectBaseApiInformation(String basicInfo);

    StatusInfo<ConnectorType> updateSupportedAuthTypes(PrismContainer<ConnDevAuthInfoType> basicInfo);

    StatusInfo<ResourceType> testConnection(ConnectorConfigurationType type);

    StatusInfo<PrismContainer<ConnDevBasicObjectClassInfoType>> discoverObjectClasses(ResourceType resourceType);

    StatusInfo<ConnDevArtifactType> generateNativeSchemaScript(ConnDevBasicObjectClassInfoType type, ResourceType resourceType);

    ConnectorDevelopmentType saveNativeSchemaScript(ConnDevArtifactType type);

    StatusInfo<PrismContainer<ConnDevHttpEndpointType>> getSearchEndpoints(String objectClass);

    StatusInfo<ConnDevArtifactType> generateSearchAll(String objectClass, ConnDevHttpEndpointType endpoint);

    void testSearchAll(String objectClass, ConnDevArtifactType script);

    void saveArtifact(String objectClass, ConnDevArtifactType endpoint);

}
