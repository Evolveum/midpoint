/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;

import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class ConnectorDevelopmentServiceImpl implements ConnectorDevelopmentService {

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

        @Override
        public StatusInfo<PrismContainer<ConnDevDocumentationSourceType>> discoverDocumentation(ConnectorDevelopmentType type) {
            return null;
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
        public StatusInfo<ConnectorType> updateSupportedAuthTypes(PrismContainer<ConnDevAuthInfoType> basicInfo) {
            return null;
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
        public String getArtifactContent(ConnDevArtifactType type) {
            return "";
        }

        @Override
        public void saveNativeSchemaScript(ConnDevArtifactType type, String updated) {

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
        public void saveSearchAll(ConnDevArtifactType script, String body) {

        }

        @Override
        public void saveArtifact(String objectClass, ConnDevArtifactType endpoint) {

        }

        @Override
        public StatusInfo<ConnDevArtifactType> generateGet(String objectClass, ConnDevHttpEndpointType endpoint) {
            return null;
        }

        @Override
        public void testGet(String objectClass, ConnDevArtifactType script) {

        }

        @Override
        public void saveGet(ConnDevArtifactType script, String body) {

        }
    }
}
