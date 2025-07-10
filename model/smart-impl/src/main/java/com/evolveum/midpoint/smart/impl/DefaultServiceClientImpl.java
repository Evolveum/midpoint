/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.Arrays;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.common.rest.MidpointXmlProvider;
import com.evolveum.midpoint.common.rest.MidpointYamlProvider;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDelineation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationConfigurationType;

/**
 * A client for the remote Smart integration service.
 *
 * TODO implement
 */
class DefaultServiceClientImpl implements ServiceClient {

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
            OperationResult result) {
        throw new UnsupportedOperationException("TODO call the remote service using webClient (if present)");
    }

    @Override
    public void close() {
        webClient.close();
    }
}
