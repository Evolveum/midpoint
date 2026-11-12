/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * End-to-end checks that public resources and application endpoints retain
 * their expected HTTP security behavior.
 */
public class TestSecurityFilterChainInitialization extends RestServiceInitializer {

    private static final String TEST_NODE_OID = "58d99ed2-9b42-4f37-8b7b-2a393eecf701";
    private static final String TEST_NODE_SECRET = "ClusterSecuritySecret1";
    private static final List<String> LOOPBACK_ADDRESSES = List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");

    private static final String[] PUBLIC_STATIC_RESOURCE_PATHS = {
            "/css/overlay.css",
            "/js/overlay.js",
            "/img/logo.jpg",
            "/fonts/font-evosome.woff",
            "/static/midpoint.css",
            "/favicon.ico",
    };

    @LocalServerPort private int serverPort;

    @Autowired private Protector protector;

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);
        deleteNodeIfPresent(TEST_NODE_OID, result);
        getRepositoryService().addObject(createTestNode(TEST_NODE_OID, "cluster-security-test-node", TEST_NODE_SECRET)
                .asPrismObject(), null, result);
    }

    @AfterClass(alwaysRun = true)
    public void removeSyntheticClusterNodes() {
        OperationResult result = new OperationResult("removeSyntheticClusterNodes");
        deleteNodeIfPresent(TEST_NODE_OID, result);
    }

    @DataProvider(name = "clusterPrefixes")
    public Object[][] clusterPrefixes() {
        return new Object[][] {
                { "/ws/cluster" },
                { "/rest/cluster" },
                { "/api/cluster" }
        };
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

    @Test
    public void test100PublicStaticResourcesRemainAccessibleWithInvalidSession() {
        for (String path : PUBLIC_STATIC_RESOURCE_PATHS) {
            WebClient client = prepareRootClient(null, null);
            client.header("Cookie", "JSESSIONID=invalid-session");
            client.path(path);

            Response response = client.get();

            assertThat(response.getStatus())
                    .as("status for %s", path)
                    .isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.getHeaderString("Location")).as("redirect for %s", path).isNull();
        }
    }

    @Test
    public void test110StaticWebResourceDoesNotRequireAuthentication() {
        String path = "/static-web/nonexistent.css";
        WebClient client = prepareRootClient(null, null);
        client.header("Cookie", "JSESSIONID=invalid-session");
        client.path(path);

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getHeaderString("Location")).isNull();
    }

    @Test
    public void test115WicketResourceDoesNotRequireAuthentication() {
        String path = "/wicket/resource/nonexistent";
        WebClient client = prepareRootClient(null, null);
        client.header("Cookie", "JSESSIONID=invalid-session");
        client.path(path);

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getHeaderString("Location")).isNull();
    }

    @Test
    public void test120LoginStillUsesNormalPermitAllHandling() {
        WebClient client = prepareRootClient(null, null);
        WebClient.getConfig(client).getRequestContext().put(Message.MAINTAIN_SESSION, true);
        client.path("/login");

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.FOUND.getStatusCode());
        URI location = response.getLocation();
        assertThat(location).isNotNull();

        URI resolvedLocation = client.getCurrentURI().resolve(location);
        assertThat(resolvedLocation.getPath()).isEqualTo("/login");
        assertThat(resolvedLocation.getQuery()).as("Wicket render URL query").isNotBlank();
        response.close();

        client.to(resolvedLocation.toString(), true);
        Response renderedLoginPage = client.get();

        assertThat(renderedLoginPage.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(renderedLoginPage.getHeaderString("Location")).isNull();
        renderedLoginPage.close();
    }

    @Test
    public void test130ProtectedGuiRequestRedirectsToLogin() {
        WebClient client = prepareRootClient(null, null);
        client.path("/admin/users");

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.FOUND.getStatusCode());
        assertThat(response.getHeaderString("Location")).contains("/login");
    }

    @Test
    public void test140ActuatorHealthUsesConfiguredBasicAuthentication() {
        WebClient anonymous = prepareRootClient(null, null);
        anonymous.header("Cookie", "JSESSIONID=invalid-session");
        anonymous.path("/actuator/health");

        Response anonymousResponse = anonymous.get();

        assertThat(anonymousResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
        assertThat(anonymousResponse.getHeaderString("Location")).isNull();
        assertThat(anonymousResponse.getHeaderString("WWW-Authenticate")).containsIgnoringCase("Basic");

        WebClient administrator = prepareRootClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
        administrator.path("/actuator/health");

        Response administratorResponse = administrator.get();

        assertThat(administratorResponse.getStatus())
                .isIn(Response.Status.OK.getStatusCode(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        assertThat(administratorResponse.getHeaderString("Location")).isNull();
    }

    @Test
    public void test150RestRequestStillUsesDynamicAuthenticationAndAuthorization() {
        WebClient anonymous = prepareRootClient(null, null);
        anonymous.path("/ws/rest/users");
        assertThat(anonymous.get().getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        WebClient unauthorized = prepareRootClient(USER_NOBODY_USERNAME, USER_NOBODY_PASSWORD);
        unauthorized.path("/ws/rest/users");
        assertThat(unauthorized.get().getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        WebClient administrator = prepareRootClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
        administrator.path("/ws/rest/users");
        assertThat(administrator.get().getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test(dataProvider = "clusterPrefixes")
    public void test200ValidClusterAuthenticationProtectsAllPrefixes(String prefix) {
        try (Response response = prepareClusterClient(prefix, validClusterHeader(), "/scheduler/information").get()) {
            assertThat(response.getStatus()).as("status for %s", prefix)
                    .isEqualTo(Response.Status.OK.getStatusCode());
        }
    }

    @Test(dataProvider = "clusterPrefixes")
    public void test210MissingClusterAuthorizationHeaderIsRejected(String prefix) {
        assertClusterRequestRejected(prefix, null, "Basic");
    }

    @Test(dataProvider = "clusterPrefixes")
    public void test220IncorrectClusterSecretIsRejected(String prefix) {
        assertClusterRequestRejected(prefix, clusterHeader("incorrect-cluster-secret"), "Cluster");
    }

    @Test(dataProvider = "clusterPrefixes")
    public void test230MalformedClusterAuthenticationHeaderIsRejected(String prefix) {
        assertClusterRequestRejected(prefix, "Cluster", "Cluster");
    }

    @Test(dataProvider = "clusterPrefixes")
    public void test240InvalidBase64ClusterCredentialIsRejected(String prefix) {
        assertClusterRequestRejected(prefix, "Cluster ***not-base64***", "Cluster");
    }

    private WebClient prepareRootClient(String username, String password) {
        WebClient client = WebClient.create(rootUrl());
        client.accept(getAcceptHeader());
        createAuthorizationHeader(client, username, password);
        return client;
    }

    private WebClient prepareClusterClient(String prefix, String authorization, String path) {
        WebClient client = WebClient.create(rootUrl());
        client.accept(getAcceptHeader());
        if (authorization != null) {
            client.header("Authorization", authorization);
        }
        client.path(prefix + path);
        return client;
    }

    private void assertClusterRequestRejected(String prefix, String authorization, String expectedChallengeScheme) {
        try (Response response = prepareClusterClient(prefix, authorization, "/scheduler/information").get()) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
            assertThat(response.getHeaderString("Location")).as("authentication failure redirect").isNull();
            assertThat(response.getCookies()).as("cookies created by authentication failure").doesNotContainKey("JSESSIONID");
            assertThat(response.getHeaderString("WWW-Authenticate"))
                    .as("authentication challenge")
                    .startsWithIgnoringCase(expectedChallengeScheme + " ");

            String contentType = response.getHeaderString("Content-Type");
            assertThat(contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("text/html"))
                    .as("authentication failure is not an HTML response")
                    .isTrue();

            String body = response.hasEntity() ? new String(response.readEntity(byte[].class), StandardCharsets.UTF_8) : "";
            assertThat(body)
                    .as("authentication failure body")
                    .doesNotContainIgnoringCase("<!doctype html", "<html", "wicket");
        }
    }

    private NodeType createTestNode(String oid, String identifier, String secret) throws Exception {
        NodeType node = new NodeType();
        node.setOid(oid);
        node.setName(new PolyStringType(identifier));
        node.setNodeIdentifier(identifier);
        node.setOperationalState(NodeOperationalStateType.UP);
        node.setClustered(true);
        node.setLastCheckInTime(XmlTypeConverter.createXMLGregorianCalendar());
        node.getIpAddress().addAll(LOOPBACK_ADDRESSES);
        node.setSecret(protector.encryptString(secret));
        node.setSecretUpdateTimestamp(XmlTypeConverter.createXMLGregorianCalendar());
        return node;
    }

    private void deleteNodeIfPresent(String oid, OperationResult result) {
        OperationResult cleanupResult = new OperationResult(result.getOperation() + ".deleteNodeIfPresent");
        try {
            getRepositoryService().deleteObject(NodeType.class, oid, cleanupResult);
        } catch (ObjectNotFoundException ignored) {
            // The fixture is already absent, which is the desired state.
        }
    }

    private String validClusterHeader() {
        return clusterHeader(TEST_NODE_SECRET);
    }

    private String clusterHeader(String secret) {
        return "Cluster " + Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String rootUrl() {
        return "http://localhost:" + serverPort;
    }

}
