/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ServerResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class ApacheResponseHandler implements ResponseHandler<ServerResponse> {

    public ServerResponse handleResponse(HttpResponse httpResponse) throws IOException {

        HttpEntity entity = httpResponse.getEntity();

        return new ServerResponse(
                httpResponse.getStatusLine().getStatusCode(), httpResponse.getStatusLine().getReasonPhrase(),
                entity != null ? EntityUtils.toString(entity) : null
        );
    }
}
