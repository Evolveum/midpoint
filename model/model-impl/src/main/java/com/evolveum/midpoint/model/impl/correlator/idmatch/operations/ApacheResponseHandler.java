/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ListResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApacheResponseHandler implements ResponseHandler<List<ListResponse>> {

    private static final String EXISTING = "EXISTING IDENTITIES";
    private static final String CREATED = "CREATED";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String NOT_FOUND = "NOT FOUND";
    private static final String MULTIPLE_CHOICES = "MULTIPLE CHOICES RESPONSE";
    private static final String CONFLICT = "DENIAL OF PROCESSING DUE TO DATA CONFLICTS";
    private static final String UNAUTHORIZED = "UNAUTHORIZED";
    private static final String UNEXPECTED_HEADER = "UNEXPECTED HEADER FOUND";
    private static final String BAD_REQUEST = "BAD REQUEST";
    private static final String NOT_IMPLEMENTED = "NOT IMPLEMENTED YET";

    public List<ListResponse> handleResponse(HttpResponse httpResponse) throws IOException {

        int status = httpResponse.getStatusLine().getStatusCode();

        final HttpEntity entity = httpResponse.getEntity();
        String message;

        List<ListResponse> listResponses = new ArrayList<>();

        if (status == HttpStatus.SC_OK) {
            message = EXISTING;
        } else if (status == HttpStatus.SC_CREATED) {
            message = CREATED;
        } else if (status == HttpStatus.SC_ACCEPTED) {
            message = ACCEPTED;
        } else if (status == HttpStatus.SC_BAD_REQUEST) {
            throw new IllegalStateException(BAD_REQUEST);
        } else if (status == HttpStatus.SC_NOT_FOUND) {
            message = NOT_FOUND;
        } else if (status == HttpStatus.SC_MULTIPLE_CHOICES) {
            message = MULTIPLE_CHOICES;
        } else if (status == HttpStatus.SC_CONFLICT) {
            message = CONFLICT;
        } else if (status == HttpStatus.SC_UNAUTHORIZED) {
            throw new IllegalStateException(UNAUTHORIZED + EntityUtils.toString(entity));
        } else if (status == 250) {
            throw new IllegalStateException(UNEXPECTED_HEADER + EntityUtils.toString(entity));
        } else {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED + httpResponse);
        }

        if (entity != null) {
            listResponses.add(new ListResponse(message, EntityUtils.toString(entity), status));
        }
        return listResponses;
    }
}
