/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

class ApacheGetRequest extends AbstractRequest {

    ApacheGetRequest(AuthenticationProvider authenticationProvider) {
        super(authenticationProvider);
    }

    @Override
    protected HttpRequestBase createRequest(String urlPrefix, String urlSuffix, String jsonString) {
        return new HttpGet(urlPrefix + urlSuffix);
    }
}
