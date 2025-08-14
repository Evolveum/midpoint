/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.Arrays;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.common.rest.MidpointXmlProvider;
import com.evolveum.midpoint.common.rest.MidpointYamlProvider;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationConfigurationType;

/**
 * A client for the remote Smart integration service (the real one, accessible via HTTP).
 */
public class DefaultServiceClientImpl implements ServiceClient {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultServiceClientImpl.class);

    /** @see #getPath(Method) */
    private static final String URL_PREFIX = "/api/v1/";
    private static final String METHOD_SUGGEST_OBJECT_TYPES = "objectType/suggestObjectType"; // TODO This should be plural!
    private static final String METHOD_SUGGEST_FOCUS_TYPE = "focusType/suggestFocusType";
    private static final String METHOD_MATCH_SCHEMA = "matching/matchSchema";
    private static final String METHOD_SUGGEST_MAPPING = "mapping/suggestMapping";

    /** The client used to access the remote service. */
    private final WebClient webClient;

    /** Timeout for receiving answer from the Python service. Later it will be configurable. */
    private static final long RECEIVE_TIMEOUT = 300_000;

    // TODO decide if we use these providers or not.
    @Autowired private MidpointXmlProvider<?> xmlProvider;
    @Autowired private MidpointJsonProvider<?> jsonProvider;
    @Autowired private MidpointYamlProvider<?> yamlProvider;

    DefaultServiceClientImpl(@Nullable SmartIntegrationConfigurationType configurationBean) throws ConfigurationException {
        // FIXME temporary hack to force CXF to use HTTP/1.1 (remove it eventually, because it influences all HTTP communication).
        System.setProperty("org.apache.cxf.transport.http.forceVersion", "1.1");
        webClient = WebClient.create(
                getServiceUrl(configurationBean),
                Arrays.asList(xmlProvider, jsonProvider, yamlProvider),
                true);

        var conduit = WebClient.getConfig(webClient).getHttpConduit();
        var policy = new HTTPClientPolicy();
        policy.setReceiveTimeout(RECEIVE_TIMEOUT);
        conduit.setClient(policy);
    }

    private static String getServiceUrl(@Nullable SmartIntegrationConfigurationType configurationBean)
            throws ConfigurationException {
        var urlOverride = getServiceUrlOverride();
        if (urlOverride != null) {
            return urlOverride;
        }
        return MiscUtil.configNonNull(
                configurationBean != null ? configurationBean.getServiceUrl() : null,
                "Smart integration service URL is not configured. "
                        + "Please set it in the configuration or via system property "
                        + MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE);
    }

    private static String getServiceUrlOverride() {
        return System.getProperty(MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE);
    }

    @VisibleForTesting
    public static boolean hasServiceUrlOverride() {
        return getServiceUrlOverride() != null;
    }

    /** A generic method that calls a remote service. Treats serialization/parsing of the exchanged data. */
    public <REQ, RESP> RESP invoke(Method method, REQ request, Class<RESP> responseClass)
            throws SchemaException {
        // FIXME this is a temporary hack to work around limitations of our JSON serializer/deserializer.
        //  So we serialize/deserialize the data ourselves.
        var requestText = PrismContext.get().jsonSerializer().serializeRealValueContent(request);
        // TEMPORARILY "info" logging for request and response
        LOGGER.info("Calling {} with request (class: {}):\n{}", method, request.getClass().getName(), requestText);
        webClient.reset();
        webClient.type(MediaType.APPLICATION_JSON);
        webClient.accept(MediaType.APPLICATION_JSON);
        webClient.path(getPath(method));
        try (var response = webClient.post(requestText)) {
            var statusType = response.getStatusInfo();
            var responseText = response.readEntity(String.class);
            LOGGER.info("Response (status: {}, expected class: {}):\n{}",
                    statusType.getStatusCode(), responseClass, responseText);
            if (statusType.getFamily() == Response.Status.Family.SUCCESSFUL) {
                // Another hack: we don't have "parseRealValueContent" method that would parse the response.
                // So we wrap it in a JSON object that will look like a regularly serialized Item.
                var wrappedResponseText = "{ \"wrapper\": " + responseText + " }";
                return PrismContext.get().parserFor(wrappedResponseText).parseRealValue(responseClass);
            } else {
                throw new SystemException("Service call (%s) failed with status: %d %s".formatted(
                        method, statusType.getStatusCode(), statusType.getReasonPhrase()));
            }
        }
    }

    private static String getPath(Method method) {
        return switch (method) {
            case SUGGEST_OBJECT_TYPES -> URL_PREFIX + METHOD_SUGGEST_OBJECT_TYPES;
            case SUGGEST_FOCUS_TYPE -> URL_PREFIX + METHOD_SUGGEST_FOCUS_TYPE;
            case MATCH_SCHEMA -> URL_PREFIX + METHOD_MATCH_SCHEMA;
            case SUGGEST_MAPPING -> URL_PREFIX + METHOD_SUGGEST_MAPPING;
        };
    }

    @Override
    public void close() {
        webClient.close();
    }
}
