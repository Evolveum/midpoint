/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.evolveum.midpoint.util.exception.SecurityViolationException;

import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ServerResponse;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public abstract class AbstractRequest {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRequest.class);

    private final AuthenticationProvider authenticationProvider;
    private CloseableHttpClient httpClient;

    AbstractRequest(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    ServerResponse executeAndClose(String urlPrefix, String urlSuffix, String jsonString)
            throws CommunicationException, SecurityViolationException {
        try {
            logRequest(urlPrefix, urlSuffix, jsonString);

            HttpRequestBase request = createRequest(urlPrefix, urlSuffix, jsonString);
            var response = createHttpClient()
                    .execute(request, new ApacheResponseHandler());

            logResponse(response);
            processCommonExceptions(response);
            return response;
        } catch (IOException e) {
            throw new CommunicationException("Couldn't invoke ID Match service: " + e.getMessage(), e);
        } finally {
            closeHttpClient();
        }
    }

    protected abstract HttpRequestBase createRequest(String urlPrefix, String urlSuffix, String jsonString) throws UnsupportedEncodingException;

    private HttpClient createHttpClient() {
        httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(authenticationProvider.provider())
                .build();
        return httpClient;
    }

    private void closeHttpClient() {
        try {
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logRequest(String urlPrefix, String urlSuffix, String jsonString) {
        LOGGER.debug("Executing {} on {}{} with:\n{}", getClass().getSimpleName(), urlPrefix, urlSuffix, jsonString);
    }

    private void logResponse(ServerResponse response) {
        LOGGER.debug("Got response:\n{}", response.debugDumpLazily(1));
    }

    private void processCommonExceptions(ServerResponse response) throws SecurityViolationException {
        int code = response.getResponseCode();

        if (code == HttpStatus.SC_UNAUTHORIZED
                || code == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED
                || code == HttpStatus.SC_FORBIDDEN) {
            throw new SecurityViolationException(response.getExceptionMessage());
        }
        if (code >= 400 && code < 600) {
            throw new SystemException(response.getExceptionMessage());
        }
        // Other unrecognized codes are treated in the upper layers.
    }
}
