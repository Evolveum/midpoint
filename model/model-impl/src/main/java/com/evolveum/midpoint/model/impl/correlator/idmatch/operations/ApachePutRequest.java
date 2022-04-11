/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

class ApachePutRequest extends AbstractRequest {

    ApachePutRequest(AuthenticationProvider authenticationProvider) {
        super(authenticationProvider);
    }

    @Override
    protected HttpRequestBase createRequest(String urlPrefix, String urlSuffix, String jsonString)
            throws UnsupportedEncodingException {
        HttpPut request = new HttpPut(urlPrefix + urlSuffix);
        request.addHeader("content-type", "application/json");
        request.setEntity(new StringEntity(jsonString));
        return request;
    }
}
