/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class ApacheDeleteRequest extends AbstractRequest {

    ApacheDeleteRequest(AuthenticationProvider authenticationProvider) {
        super(authenticationProvider);
    }

    @Override
    protected HttpRequestBase createRequest(String urlPrefix, String urlSuffix, String jsonString) {
        return new HttpDelete(urlPrefix + urlSuffix);
    }
}
