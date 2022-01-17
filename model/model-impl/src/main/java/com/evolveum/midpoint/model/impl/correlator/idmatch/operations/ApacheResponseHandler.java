package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.ResponseType;
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

    String EXISTING = "EXISTING IDENTITIES";
    String CREATED = "CREATED";
    String ACCEPTED = "ACCEPTED";
    String NOT_FOUND = "NOT FOUND";
    String MULTIPLE_CHOICES = "MULTIPLE CHOICES RESPONSE";
    String CONFLICT = "DENIAL OF PROCESSING DUE TO DATA CONFLICTS";
    String UNAUTHORIZED = "UNAUTHORIZED";
    String UNEXPECTED_HEADER = "UNEXPECTED HEADER FOUND";
    String BAD_REQUEST = "BAD REQUEST";
    String NOT_IMPLEMENTED = "NOT IMPLEMENTED YET";

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
            listResponses.add(new ListResponse(message, EntityUtils.toString(entity), Integer.toString(status)));
        }
        return listResponses;
    }


}

