/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.Channel;
import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.MatchStatus;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ListResponse;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure.JsonRequest;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.auth.AuthenticationProvider;

import com.evolveum.midpoint.util.exception.CommunicationException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Client {

    private static final Trace LOGGER = TraceManager.getTrace(Client.class);

    private final ApacheApiRequest apachePutRequest;
    private final ApacheApiRequest apachePostRequest;
    private final ApacheApiRequest apacheGetRequest;
    private final ApacheApiRequest apacheDeleteRequest;

    private final HttpBuilder httpClientSuper;

    String urlPrefix;
    String username;
    String password;

    AuthenticationProvider authenticationProvider;

    ListResponse listResponse;

    public int getResponseCode() {
        return responseCode;
    }

    private void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getEntity() {
        return entity;
    }

    private void setEntity(String entity) {
        this.entity = entity;
    }

    int responseCode;
    String entity;
    String message;

    public Client(String urlPrefix, String username, String password) {
        this.urlPrefix = urlPrefix;
        this.username = username;
        this.password = password;
        this.authenticationProvider = new AuthenticationProvider(username, password);

        apachePutRequest = new ApachePutRequest(this.authenticationProvider);
        apachePostRequest = new ApachePostRequest(this.authenticationProvider);
        apacheGetRequest = new ApacheGetRequest(this.authenticationProvider);
        apacheDeleteRequest = new ApacheDeleteRequest(this.authenticationProvider);
        httpClientSuper = new HttpBuilder(this.authenticationProvider);
    }

    public void peoplePut(@NotNull JsonRequest jsonRequest) throws CommunicationException {

        String sorLabel = jsonRequest.getSorLabel();
        String sorId = jsonRequest.getSorId();
        String objectToSend = jsonRequest.getObjectToSend();

        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());

        try {
            LOGGER.info("Invoking peoplePut with url = `{}`, urlSuffix = `{}`, object:\n{}",
                    url, urlSuffix, objectToSend); // TODO trace
            apachePutRequest.doRequest(url.toString(), urlSuffix.toString(), objectToSend);
            setResponses(apachePutRequest);
        } catch (IOException e) {
            throw new CommunicationException("Couldn't invoke ID Match service: " + e.getMessage(), e);
        }
    }

    public void listPeople(String sorLabel) {

        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        try {
            apacheGetRequest.doRequest(url.toString(), sorLabel, "");
            setResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void peopleById(String sorLabel, String sorId) {

        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);

        try {
            apacheGetRequest.doRequest(url.toString(), urlSuffix.toString(), "");
            setResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listMatchRequest(MatchStatus matchStatus) {

        StringBuilder urlSuffix = new StringBuilder(matchStatus.getUrl());

        try {
            apacheGetRequest.doRequest(urlPrefix, urlSuffix.toString(), "");
            setResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void getMatchRequest(String id) {

        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_GET_MATCH_REQUEST_MATCH_ID.getUrl());

        try {
            LOGGER.info("Getting match request {}", id);
            apacheGetRequest.doRequest(url.toString(), id, "");
            setResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void searchMatchRequestsByReferenceId(String refId) {
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_GET_MATCH_REQUEST_REFERENCE_ID.getUrl());

        try {
            apacheGetRequest.doRequest(url.toString(), refId, "");
            setResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void peoplePost(String sorLabel, String sorId, String object) {

        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        try {
            apachePostRequest.doRequest(url.toString(), urlSuffix.toString(), object);
            setResponses(apachePostRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deletePeople(String sorLabel, String sorId) {

        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        try {
            apacheDeleteRequest.doRequest(url.toString(), urlSuffix.toString(), "");
            setResponses(apacheDeleteRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setResponses(ApacheApiRequest apacheApiRequest) {
        if (!apacheApiRequest.listResponse().isEmpty()) {
            listResponse = apacheApiRequest.listResponse().get(0);
            LOGGER.info("Response code: {}\nMessage: {}\n Entity:\n{}",
                    listResponse.getResponseCode(), listResponse.getMessage(), listResponse.getEntity());
            setResponseCode(listResponse.getResponseCode());
            setEntity(listResponse.getEntity());
            setMessage(listResponse.getMessage());
        } else {
            LOGGER.info("No response messages received"); // TODO trace
        }
    }

    public void close() {
        httpClientSuper.clientClose();
    }

    public String getMessage() {
        return message;
    }

    private void setMessage(String message) {
        this.message = message;
    }

}
