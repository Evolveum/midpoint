/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.authentication.impl.module.authentication.NodeAuthenticationTokenImpl;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.rest.impl.ClusterRestController;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.CacheInvalidationDispatcher;
import com.evolveum.midpoint.repo.api.CacheInvalidationEventSpecification;
import com.evolveum.midpoint.repo.api.CacheInvalidationListener;
import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationDispatcher;
import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * End-to-end checks that public resources and application endpoints retain
 * their expected HTTP security behavior.
 */
public class TestSecurityFilterChainInitialization extends RestServiceInitializer {

    private static final String TEST_NODE_OID = "58d99ed2-9b42-4f37-8b7b-2a393eecf701";
    private static final String TEST_NODE_SECRET = "ClusterSecuritySecret1";
    private static final String REMOVED_NODE_OID = "18ca67c2-d98d-4fd3-a462-2379ae18ef3b";
    private static final String DOWN_NODE_OID = "59083a64-12bd-4397-983a-e1b798fc7050";
    private static final String CHANGED_ADDRESS_NODE_OID = "22bbb817-181a-42a4-bc1b-438e9603e3f3";
    private static final List<String> SYNTHETIC_NODE_OIDS =
            List.of(TEST_NODE_OID, REMOVED_NODE_OID, DOWN_NODE_OID, CHANGED_ADDRESS_NODE_OID);
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
    @Autowired private CacheInvalidationDispatcher cacheInvalidationDispatcher;
    @Autowired private ClusterwideCacheInvalidationDispatcher clusterwideCacheInvalidationDispatcher;
    @Autowired private ClusterRestController clusterRestController;
    @Autowired private MidpointConfiguration midpointConfiguration;

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);
        removeSyntheticClusterNodes(result);
        getRepositoryService().addObject(createTestNode(TEST_NODE_OID, "cluster-security-test-node", TEST_NODE_SECRET)
                .asPrismObject(), null, result);
    }

    @AfterClass(alwaysRun = true)
    public void removeSyntheticClusterNodes() {
        OperationResult result = new OperationResult("removeSyntheticClusterNodes");
        removeSyntheticClusterNodes(result);
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

    @Test
    public void test242RemovedNodeCredentialIsRejected() throws Exception {
        String secret = "RemovedNodeSecret1";
        OperationResult result = new OperationResult("test242RemovedNodeCredentialIsRejected");

        try {
            replaceDefaultNodeWithSyntheticNode(
                    REMOVED_NODE_OID, "removed-cluster-security-test-node", secret, result);
            assertClusterCredentialAccepted(secret);

            getRepositoryService().deleteObject(NodeType.class, REMOVED_NODE_OID, result);

            assertClusterRequestRejected("/ws/cluster", clusterHeader(secret), "Cluster");
        } finally {
            removeSyntheticNodeAndRestoreDefault(REMOVED_NODE_OID, result);
        }
    }

    @Test
    public void test244DownNodeCredentialIsRejected() throws Exception {
        String secret = "DownNodeSecret1";
        OperationResult result = new OperationResult("test244DownNodeCredentialIsRejected");

        try {
            replaceDefaultNodeWithSyntheticNode(
                    DOWN_NODE_OID, "down-cluster-security-test-node", secret, result);
            assertClusterCredentialAccepted(secret);

            getRepositoryService().modifyObject(
                    NodeType.class,
                    DOWN_NODE_OID,
                    getPrismContext().deltaFor(NodeType.class)
                            .item(NodeType.F_OPERATIONAL_STATE)
                            .replace(NodeOperationalStateType.DOWN)
                            .asItemDeltas(),
                    result);

            assertClusterRequestRejected("/ws/cluster", clusterHeader(secret), "Cluster");
        } finally {
            removeSyntheticNodeAndRestoreDefault(DOWN_NODE_OID, result);
        }
    }

    @Test
    public void test246ChangedSourceAddressRejectsExistingCredential() throws Exception {
        String secret = "ChangedAddressNodeSecret1";
        OperationResult result = new OperationResult("test246ChangedSourceAddressRejectsExistingCredential");

        try {
            replaceDefaultNodeWithSyntheticNode(
                    CHANGED_ADDRESS_NODE_OID, "changed-address-cluster-security-test-node", secret, result);
            assertClusterCredentialAccepted(secret);

            getRepositoryService().modifyObject(
                    NodeType.class,
                    CHANGED_ADDRESS_NODE_OID,
                    getPrismContext().deltaFor(NodeType.class)
                            .item(NodeType.F_IP_ADDRESS)
                            .replace("192.0.2.1")
                            .asItemDeltas(),
                    result);

            assertClusterRequestRejected("/ws/cluster", clusterHeader(secret), "Cluster");
        } finally {
            removeSyntheticNodeAndRestoreDefault(CHANGED_ADDRESS_NODE_OID, result);
        }
    }

    @Test
    public void test250ReceivedClusterInvalidationIsDispatchedLocallyWithoutRebroadcast() {
        String oid = "62a83cb4-cd27-4741-bf3d-8902d58c34ea";
        RecordingInvalidationListener listener = new RecordingInvalidationListener(oid);
        clusterwideCacheInvalidationDispatcher.registerListener(listener);
        cacheInvalidationDispatcher.registerListener(listener);

        try {
            WebClient client = prepareClusterClient(
                    "/ws/cluster", validClusterHeader(), "/event/invalidation/roles/" + oid);
            try (Response response = client.post(null)) {
                assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            }

            assertThat(listener.clusterwideInvocations).isEqualTo(1);
            assertThat(listener.localInvocations).isEqualTo(1);
            assertThat(listener.localType).isEqualTo(RoleType.class);
            assertThat(listener.localOid).isEqualTo(oid);
            assertThat(listener.clusterwide).isFalse();
            assertThat(listener.localContext).isNotNull();
            assertThat(listener.localContext.isFromRemoteNode()).isTrue();
        } finally {
            cacheInvalidationDispatcher.unregisterListener(listener);
            clusterwideCacheInvalidationDispatcher.unregisterListener(listener);
        }
    }

    @Test
    public void test260ReportFileResponseIsStreamed() throws Exception {
        byte[] expectedContent = "MID-13003 streamed report".getBytes(StandardCharsets.UTF_8);
        Path exportDirectory = Path.of(midpointConfiguration.getMidpointHome(), "export");
        Files.createDirectories(exportDirectory);
        Path reportFile = Files.createTempFile(exportDirectory, "mid-13003-", ".txt");
        Files.write(reportFile, expectedContent);

        try {
            var authentication = new NodeAuthenticationTokenImpl(
                    getRepositoryService().getObject(
                            NodeType.class, TEST_NODE_OID, null, new OperationResult("getTestNode")),
                    "127.0.0.1",
                    List.of());
            authentication.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            var response = clusterRestController.getReportFile(reportFile.getFileName().toString());

            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(response.getHeaders().getContentType())
                    .isEqualTo(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            assertThat(response.getBody()).isInstanceOf(Resource.class);

            Resource resource = (Resource) response.getBody();
            var output = new MockHttpOutputMessage();
            new ResourceHttpMessageConverter().write(
                    resource, org.springframework.http.MediaType.APPLICATION_OCTET_STREAM, output);

            assertThat(output.getBodyAsBytes()).containsExactly(expectedContent);
            Files.delete(reportFile);
            assertThat(reportFile).doesNotExist();
        } finally {
            SecurityContextHolder.clearContext();
            Files.deleteIfExists(reportFile);
        }
    }

    private static class RecordingInvalidationListener implements CacheInvalidationListener, ClusterwideCacheInvalidationListener {

        private final String expectedOid;
        private volatile int clusterwideInvocations;
        private volatile int localInvocations;
        private volatile Class<? extends ObjectType> localType;
        private volatile String localOid;
        private volatile CacheInvalidationContext localContext;
        private volatile boolean clusterwide;

        private RecordingInvalidationListener(String expectedOid) {
            this.expectedOid = expectedOid;
        }

        @Override
        public Collection<CacheInvalidationEventSpecification> getEventSpecifications() {
            return CacheInvalidationEventSpecification.ALL_AVAILABLE_EVENTS;
        }

        @Override
        public <O extends ObjectType> void invalidate(Class<O> type, String oid, CacheInvalidationContext context) {
            if (expectedOid.equals(oid)) {
                localInvocations++;
                localType = type;
                localOid = oid;
                localContext = context;
            }
        }

        @Override
        public <O extends ObjectType> void invalidate(
                Class<O> type, String oid, boolean clusterwide, CacheInvalidationContext context) {
            if (expectedOid.equals(oid)) {
                clusterwideInvocations++;
                this.clusterwide = clusterwide;
            }
        }
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

    private void assertClusterCredentialAccepted(String secret) {
        try (Response response = prepareClusterClient(
                "/ws/cluster", clusterHeader(secret), "/scheduler/information").get()) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        }
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

    private void addSyntheticNode(String oid, String identifier, String secret, OperationResult result) throws Exception {
        deleteNodeIfPresent(oid, result);
        getRepositoryService().addObject(createTestNode(oid, identifier, secret).asPrismObject(), null, result);
    }

    private void replaceDefaultNodeWithSyntheticNode(
            String oid, String identifier, String secret, OperationResult result) throws Exception {
        deleteNodeIfPresent(TEST_NODE_OID, result);
        addSyntheticNode(oid, identifier, secret, result);
    }

    private void removeSyntheticNodeAndRestoreDefault(String oid, OperationResult result) throws Exception {
        try {
            deleteNodeIfPresent(oid, result);
        } finally {
            addSyntheticNode(TEST_NODE_OID, "cluster-security-test-node", TEST_NODE_SECRET, result);
        }
    }

    private void deleteNodeIfPresent(String oid, OperationResult result) {
        OperationResult cleanupResult = new OperationResult(result.getOperation() + ".deleteNodeIfPresent");
        try {
            getRepositoryService().deleteObject(NodeType.class, oid, cleanupResult);
        } catch (ObjectNotFoundException ignored) {
            // The fixture is already absent, which is the desired state.
        }
    }

    private void removeSyntheticClusterNodes(OperationResult result) {
        for (String oid : SYNTHETIC_NODE_OIDS) {
            deleteNodeIfPresent(oid, result);
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
