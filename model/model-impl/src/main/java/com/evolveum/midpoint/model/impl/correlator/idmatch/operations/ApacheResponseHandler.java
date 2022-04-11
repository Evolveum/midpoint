/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
