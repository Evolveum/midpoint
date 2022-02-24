/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.ServicePath;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.PersonRequest;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ServerResponse;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class Client {

    private static final Trace LOGGER = TraceManager.getTrace(Client.class);

    private final String urlPrefix;

    private final AuthenticationProvider authenticationProvider;

    private ServerResponse lastServerResponse;

    public Client(String urlPrefix, String username, String password) {
        this.urlPrefix = urlPrefix;
        this.authenticationProvider = new AuthenticationProvider(username, password);
    }

    public void putPerson(@NotNull PersonRequest personRequest) throws CommunicationException, SecurityViolationException {
        lastServerResponse = createPutRequest()
                .executeAndClose(
                        urlPrefix + ServicePath.PEOPLE.getUrl(),
                        personRequest.getSorLabel() + "/" + personRequest.getSorId(),
                        personRequest.getObjectToSend());
    }

    public void getPerson(@NotNull PersonRequest personRequest) throws CommunicationException, SecurityViolationException {
        lastServerResponse = createGetRequest()
                .executeAndClose(
                        urlPrefix + ServicePath.PEOPLE.getUrl(),
                        personRequest.getSorLabel() + "/" + personRequest.getSorId(),
                        "");
    }

    public void getMatchRequest(String id) throws CommunicationException, SecurityViolationException {
        LOGGER.info("Getting match request {}", id);
        lastServerResponse = createGetRequest()
                .executeAndClose(
                        urlPrefix + ServicePath.MATCH_REQUESTS.getUrl(),
                        id,
                        "");
    }

    private @NotNull ApachePutRequest createPutRequest() {
        return new ApachePutRequest(authenticationProvider);
    }

    private @NotNull ApacheGetRequest createGetRequest() {
        return new ApacheGetRequest(authenticationProvider);
    }

    private ServerResponse getLastServerResponseRequired() {
        return Objects.requireNonNull(lastServerResponse, "No last server response");
    }

    public int getResponseCode() {
        return getLastServerResponseRequired()
                .getResponseCode();
    }

    public String getEntity() {
        return getLastServerResponseRequired()
                .getEntity();
    }

    public String getMessage() {
        return getLastServerResponseRequired()
                .getMessage();
    }
}
