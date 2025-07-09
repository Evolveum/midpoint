/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.common.rest.MidpointXmlProvider;
import com.evolveum.midpoint.common.rest.MidpointYamlProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDelineation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

/**
 * A client for the remote Smart integration service.
 *
 * TODO implement
 */
class ServiceClient implements AutoCloseable {

    /**
     * TODO decide if we use this client or not.
     */
    @Nullable private final WebClient webClient;

    // TODO decide if we use these providers or not.
    @Autowired private MidpointXmlProvider<?> xmlProvider;
    @Autowired private MidpointJsonProvider<?> jsonProvider;
    @Autowired private MidpointYamlProvider<?> yamlProvider;

    ServiceClient(@Nullable SmartIntegrationConfigurationType configurationBean) {
        var url = configurationBean != null ? configurationBean.getServiceUrl() : null;
        if (url != null) {
            webClient = WebClient.create(url, Arrays.asList(xmlProvider, jsonProvider, yamlProvider), true);
        } else {
            webClient = null;
        }
    }

    /** Calls the `suggestFocusType` method on the remote service. */
    QName suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation,
            Task task,
            OperationResult result) {
        if (webClient == null) {
            return UserType.COMPLEX_TYPE; // just for testing purposes, should not happen in production
        }
        throw new UnsupportedOperationException("TODO call the remote service using webClient (if present)");
    }

    @Override
    public void close() {
        if (webClient != null) {
            webClient.close();
        }
    }
}
