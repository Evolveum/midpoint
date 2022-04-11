/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
