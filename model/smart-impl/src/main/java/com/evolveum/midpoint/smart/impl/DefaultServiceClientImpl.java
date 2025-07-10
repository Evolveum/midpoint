/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.Arrays;
import javax.xml.namespace.QName;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.common.rest.MidpointXmlProvider;
import com.evolveum.midpoint.common.rest.MidpointYamlProvider;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDelineation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SuggestFocusTypeRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SuggestFocusTypeResponseType;

/**
 * A client for the remote Smart integration service.
 *
 * TODO implement
 */
class DefaultServiceClientImpl implements ServiceClient {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultServiceClientImpl.class);

    /** TODO decide if we use this client or not. */
    private final WebClient webClient;

    // TODO decide if we use these providers or not.
    @Autowired private MidpointXmlProvider<?> xmlProvider;
    @Autowired private MidpointJsonProvider<?> jsonProvider;
    @Autowired private MidpointYamlProvider<?> yamlProvider;

    DefaultServiceClientImpl(@Nullable SmartIntegrationConfigurationType configurationBean) throws ConfigurationException {
        webClient = WebClient.create(
                getServiceUrl(configurationBean),
                Arrays.asList(xmlProvider, jsonProvider, yamlProvider),
                true);
    }

    private static String getServiceUrl(@Nullable SmartIntegrationConfigurationType configurationBean)
            throws ConfigurationException {
        var urlOverride = System.getProperty(MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE);
        if (urlOverride != null) {
            return urlOverride;
        }
        return MiscUtil.configNonNull(
                configurationBean != null ? configurationBean.getServiceUrl() : null,
                "Smart integration service URL is not configured. "
                        + "Please set it in the configuration or via system property "
                        + MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE);
    }

    /** Calls the `suggestFocusType` method on the remote service. */
    @Override
    public QName suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation,
            Task task,
            OperationResult result) throws SchemaException {
        SuggestFocusTypeRequestType request = new SuggestFocusTypeRequestType()
                .kind(typeIdentification.getKind().value())
                .intent(typeIdentification.getIntent())
                .objectClassName(objectClassDef.getObjectClassLocalName());
                // TODO attributes
        var response = callService("suggestFocusType", request, SuggestFocusTypeResponseType.class);
        var typeName = response.getFocusTypeName();
        LOGGER.trace("Type suggested by the service: {}", typeName);
        return new QName(
                SchemaConstants.NS_C,
                MiscUtil.requireNonNull(typeName, "No returned type name from the service"));
    }

    /** A generic method that calls a remote service. Treats serialization/parsing of the exchanged data. */
    private <REQ, RESP> RESP callService(String method, REQ request, Class<RESP> responseClass) throws SchemaException {
        // FIXME this is a temporary hack to work around limitations of our JSON serializer/deserializer.
        //  So we serialize/deserialize the data ourselves.
        var requestText = PrismContext.get().jsonSerializer().serializeRealValue(request, new QName("request"));
        LOGGER.trace("Calling {} with request (class: {}):\n{}", method, request.getClass().getName(), requestText);
        webClient.type(MediaType.APPLICATION_JSON);
        webClient.accept(MediaType.APPLICATION_JSON);
        webClient.path("/" + method);
        try (var response = webClient.post(requestText)) {
            var statusType = response.getStatusInfo();
            var responseText = response.readEntity(String.class);
            LOGGER.trace("Response (status: {}, expected class: {}):\n{}", statusType, responseClass, responseText);
            if (statusType.getFamily() == Response.Status.Family.SUCCESSFUL) {
                return PrismContext.get().parserFor(responseText).parseRealValue(responseClass);
            } else {
                throw new SystemException("Service call (%s) failed with status: %d %s".formatted(
                        method, statusType.getStatusCode(), statusType.getReasonPhrase()));
            }
        }
    }

    @Override
    public void close() {
        webClient.close();
    }
}
