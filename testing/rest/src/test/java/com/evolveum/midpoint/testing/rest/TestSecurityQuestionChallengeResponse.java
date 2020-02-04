/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.rest;

import static org.testng.AssertJUnit.*;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestSecurityQuestionChallengeResponse extends RestServiceInitializer {

    private static final Trace LOGGER = TraceManager.getTrace(TestSecurityQuestionChallengeResponse.class);

    @Test
    public void testChallengeResponse() throws Exception {
        Response response = getUserAdministrator("SecQ");

        String challengeBase64 = assertAndGetChallenge(response);
        String usernameChallenge = null;
        try {
             usernameChallenge = new String(Base64Utility.decode(challengeBase64));
             LOGGER.info("Username challenge: " +usernameChallenge);
        } catch (Base64Exception e) {
            fail("Failed to decode base64 username challenge");
        }

        String secQusernameChallenge = usernameChallenge.replace("username", "administrator");
        LOGGER.info("Username response: " +secQusernameChallenge);

        response = getUserAdministrator("SecQ " + Base64Utility.encode(secQusernameChallenge.getBytes()));
        challengeBase64 = assertAndGetChallenge(response);
        String answerChallenge;
        try {
            answerChallenge = new String(Base64Utility.decode(challengeBase64));
            LOGGER.info("Answer challenge: " +answerChallenge);
        } catch (Base64Exception e) {
            fail("Failed to decode base64 username challenge");
        }
        String secQAnswerChallenge = "{"
                + "\"user\" : \"administrator\","
                + "\"answer\" : ["
                    + "{ "
                        + "\"qid\" : \"http://midpoint.evolveum.com/xml/ns/public/security/question-2#q001\","
                        + "\"qans\" : \"5ecr3t\""
                    + "},"
                    + "{ "
                            + "\"qid\" : \"http://midpoint.evolveum.com/xml/ns/public/security/question-2#q002\","
                            + "\"qans\" : \"black\""
                        + "}"
                + "]"
                + "}";

        LOGGER.info("Answer response: " +secQAnswerChallenge);

        response = getUserAdministrator("SecQ " + Base64Utility.encode(secQAnswerChallenge.getBytes()));

        assertEquals("Unexpected status code. Expected 200 but got " + response.getStatus(), 200, response.getStatus());
        UserType user = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", user);
        LOGGER.info("Returned entity: {}", user.asPrismObject().debugDump());


    }

    private String assertAndGetChallenge(Response response){
        assertEquals("Unexpected status code. Expected 401 but was "+ response.getStatus(), 401, response.getStatus());
        assertNotNull("Headers null. Something very strange happened", response.getHeaders());

        List<Object> wwwAuthenticateHeaders = response.getHeaders().get("WWW-Authenticate");
        assertNotNull("WWW-Authenticate headers null. Something very strange happened", wwwAuthenticateHeaders);
        LOGGER.info("WWW-Authenticate header: " + wwwAuthenticateHeaders);
        assertEquals("Expected WWW-Authenticate header, but the actual size is " + wwwAuthenticateHeaders.size(), 1, wwwAuthenticateHeaders.size());
        String secQHeader = (String) wwwAuthenticateHeaders.iterator().next();

        String[] headerSplitted = secQHeader.split(" ");
        assertEquals("Expected the challenge in the SecQ but haven't got one.", 2, headerSplitted.length);
        String challengeBase64 = headerSplitted[1];
        assertNotNull("Unexpected null challenge in the SecQ header", challengeBase64);
        return challengeBase64;
    }

    private Response getUserAdministrator(String authorizationHeader){
        WebClient client = WebClient.create(ENDPOINT_ADDRESS, Arrays.asList(getProvider()));
        ClientConfiguration clientConfig = WebClient.getConfig(client);

        clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        client.accept(getAcceptHeader());
        client.type(getContentType());

        client.authorization(authorizationHeader);

        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        Response response = client.get();
        return response;
    }

    @Override
    protected String getAcceptHeader() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected String getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return jsonProvider;
    }

}
