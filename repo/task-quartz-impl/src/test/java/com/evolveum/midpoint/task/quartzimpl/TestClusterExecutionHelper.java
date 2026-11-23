/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.RestAuthenticationMethod;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.ClusterExecutionOptions;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Tests outgoing cluster HTTP communication, including credential handling
 * during redirects and continued communication when a remote node is unreachable.
 */
@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@TestPropertySource(properties = {
        "midpoint.taskManager.clustered=true",
        "midpoint.nodeId=cluster-http-test-local",
        "midpoint.taskManager.jdbcJobStore=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestClusterExecutionHelper extends AbstractTaskManagerTest {

    private static final String CLUSTER_CHALLENGE = "Cluster realm=\"midpoint\"";
    private static final String NODE_ONE_OID = "9fc47d55-ed79-409f-90d4-46811dd89101";
    private static final String NODE_TWO_OID = "9fc47d55-ed79-409f-90d4-46811dd89102";
    private static final Set<String> TEST_NODE_OIDS = Set.of(NODE_ONE_OID, NODE_TWO_OID);

    @Autowired private ClusterExecutionHelper clusterExecutionHelper;
    @Autowired private Protector protector;

    @PostConstruct
    public void initialize() throws Exception {
        super.initialize();
    }

    @Test
    public void test100ClusterCredentialIsNotForwardedToRedirectDestination() throws Exception {
        AtomicInteger initialRequests = new AtomicInteger();
        AtomicReference<String> initialAuthorization = new AtomicReference<>();
        AtomicInteger redirectTargetRequests = new AtomicInteger();
        AtomicBoolean redirectTargetAuthorizationPresent = new AtomicBoolean();

        HttpServer redirectTarget = startServer("/capture", exchange -> {
            redirectTargetRequests.incrementAndGet();
            redirectTargetAuthorizationPresent.set(exchange.getRequestHeaders().getFirst("Authorization") != null);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        HttpServer originalDestination = null;

        try {
            originalDestination = startServer("/ws/cluster/redirect", exchange -> {
                initialRequests.incrementAndGet();
                initialAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                exchange.getResponseHeaders().set("Location", serverUrl(redirectTarget) + "/capture");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            });
            NodeType node = createRemoteNode("redirect-test-node", originalDestination);
            AtomicInteger responseStatus = new AtomicInteger();

            clusterExecutionHelper.execute(
                    node,
                    (client, ignoredNode, ignoredResult) -> {
                        client.path("/redirect");
                        try (Response response = client.get()) {
                            responseStatus.set(response.getStatus());
                        }
                    },
                    new ClusterExecutionOptions().tryAllNodes(),
                    "redirect credential test",
                    createOperationResult());

            assertThat(responseStatus.get())
                    .as("CXF does not follow cluster redirects by default")
                    .isEqualTo(Response.Status.FOUND.getStatusCode());
            assertThat(initialRequests.get()).isEqualTo(1);
            assertThat(initialAuthorization.get()).isNotNull();
            assertThat(matchesLocalClusterCredential(initialAuthorization.get())).isTrue();
            assertThat(redirectTargetRequests.get()).isZero();
            assertThat(redirectTargetAuthorizationPresent.get()).isFalse();
        } finally {
            if (originalDestination != null) {
                originalDestination.stop(0);
            }
            redirectTarget.stop(0);
        }
    }

    @Test
    public void test110UnreachablePeerDoesNotPreventHealthyPeerBroadcast() throws Exception {
        AtomicInteger peerOneRequests = new AtomicInteger();
        AtomicReference<String> peerOneAuthorization = new AtomicReference<>();
        AtomicInteger peerTwoRequests = new AtomicInteger();
        AtomicReference<String> peerTwoAuthorization = new AtomicReference<>();

        HttpServer peerOne = startPeer(peerOneRequests, peerOneAuthorization);
        HttpServer peerTwo = null;
        boolean peerOneStopped = false;
        boolean peerTwoStopped = false;
        OperationResult result = createOperationResult();

        try {
            peerTwo = startPeer(peerTwoRequests, peerTwoAuthorization);
            deleteNodeIfPresent(NODE_ONE_OID, result);
            deleteNodeIfPresent(NODE_TWO_OID, result);
            repositoryService.addObject(
                    createRemoteNode(NODE_ONE_OID, "broadcast-test-node-1", peerOne).asPrismObject(), null, result);
            repositoryService.addObject(
                    createRemoteNode(NODE_TWO_OID, "broadcast-test-node-2", peerTwo).asPrismObject(), null, result);

            List<String> discoveredOrder = new ArrayList<>();
            clusterExecutionHelper.execute(
                    (client, node, ignoredResult) -> {
                        if (TEST_NODE_OIDS.contains(node.getOid())) {
                            discoveredOrder.add(node.getOid());
                        }
                    },
                    new ClusterExecutionOptions().tryAllNodes(),
                    "determine test peer order",
                    result);
            assertThat(discoveredOrder).containsExactlyInAnyOrder(NODE_ONE_OID, NODE_TWO_OID);

            String unreachableNodeOid = discoveredOrder.get(0);
            String healthyNodeOid = discoveredOrder.get(1);
            if (NODE_ONE_OID.equals(unreachableNodeOid)) {
                peerOne.stop(0);
                peerOneStopped = true;
            } else {
                peerTwo.stop(0);
                peerTwoStopped = true;
            }

            AtomicBoolean unreachableAttemptFailed = new AtomicBoolean();
            AtomicInteger healthyResponseStatus = new AtomicInteger(-1);
            List<String> attemptedOrder = new ArrayList<>();
            clusterExecutionHelper.execute(
                    (client, node, ignoredResult) -> {
                        if (!TEST_NODE_OIDS.contains(node.getOid())) {
                            return;
                        }
                        attemptedOrder.add(node.getOid());
                        client.path("/broadcast");
                        try (Response response = client.get()) {
                            if (healthyNodeOid.equals(node.getOid())) {
                                healthyResponseStatus.set(response.getStatus());
                            }
                        } catch (RuntimeException e) {
                            if (unreachableNodeOid.equals(node.getOid())) {
                                unreachableAttemptFailed.set(true);
                            }
                            throw e;
                        }
                    },
                    new ClusterExecutionOptions().tryAllNodes(),
                    "unreachable peer broadcast test",
                    result);

            assertThat(attemptedOrder).containsExactly(unreachableNodeOid, healthyNodeOid);
            assertThat(unreachableAttemptFailed.get()).isTrue();
            assertThat(healthyResponseStatus.get()).isEqualTo(Response.Status.OK.getStatusCode());
            if (NODE_ONE_OID.equals(healthyNodeOid)) {
                assertHealthyRequest(peerOneRequests, peerOneAuthorization);
            } else {
                assertHealthyRequest(peerTwoRequests, peerTwoAuthorization);
            }
        } finally {
            if (!peerOneStopped) {
                peerOne.stop(0);
            }
            if (peerTwo != null && !peerTwoStopped) {
                peerTwo.stop(0);
            }
            try {
                deleteNodeIfPresent(NODE_ONE_OID, result);
            } finally {
                deleteNodeIfPresent(NODE_TWO_OID, result);
            }
        }
    }

    @Test
    public void test120SuccessfulRequestIsNotRetried() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = startServer("/ws/cluster/success", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        try {
            AtomicInteger status = new AtomicInteger();
            executeOnNode(createRemoteNode("successful-request-node", server), client -> {
                client.path("/success");
                try (Response response = clusterExecutionHelper.executeWithAuthenticationRetry(client, client::get)) {
                    status.set(response.getStatus());
                }
            });

            assertThat(status.get()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(requests.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test130RotatedSecretRetriesOnceAndPreservesRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        List<String> methods = new CopyOnWriteArrayList<>();
        List<String> requestUris = new CopyOnWriteArrayList<>();
        List<String> requestBodies = new CopyOnWriteArrayList<>();
        List<String> authorizations = new CopyOnWriteArrayList<>();
        String originalSecret = getLocalNodeSecret();
        String rotatedSecret = "rotated-" + UUID.randomUUID();
        HttpServer server = startServer("/ws/cluster/rotation", exchange -> {
            int requestNumber = requests.incrementAndGet();
            methods.add(exchange.getRequestMethod());
            requestUris.add(exchange.getRequestURI().toString());
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), UTF_8));
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (requestNumber == 1) {
                exchange.getResponseHeaders().set("WWW-Authenticate", CLUSTER_CHALLENGE);
                exchange.sendResponseHeaders(401, -1);
            } else {
                exchange.sendResponseHeaders(200, -1);
            }
            exchange.close();
        });

        try {
            AtomicInteger status = new AtomicInteger();
            executeOnNode(createRemoteNode("secret-rotation-node", server), client -> {
                client.path("/rotation").query("marker", "preserved");
                replaceLocalNodeSecret(rotatedSecret);
                try (Response response = clusterExecutionHelper.executeWithAuthenticationRetry(
                        client,
                        () -> client.post("preserved-body"))) {
                    status.set(response.getStatus());
                }
            });

            assertThat(status.get()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(requests.get()).isEqualTo(2);
            assertThat(methods).containsExactly("POST", "POST");
            assertThat(requestUris).containsExactly(
                    "/ws/cluster/rotation?marker=preserved",
                    "/ws/cluster/rotation?marker=preserved");
            assertThat(requestBodies).containsExactly("preserved-body", "preserved-body");
            assertThat(authorizations).hasSize(2);
            assertThat(sameCredential(authorizations.get(0), clusterAuthorization(originalSecret))).isTrue();
            assertThat(sameCredential(authorizations.get(1), clusterAuthorization(rotatedSecret))).isTrue();
        } finally {
            server.stop(0);
            replaceLocalNodeSecret(originalSecret);
        }
    }

    @Test
    public void test140UnchangedSecretDoesNotRetryUnauthorizedResponse() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = startUnauthorizedServer("/ws/cluster/unchanged", requests, CLUSTER_CHALLENGE);

        try {
            AtomicInteger status = new AtomicInteger();
            executeOnNode(createRemoteNode("unchanged-secret-node", server), client -> {
                client.path("/unchanged");
                try (Response response = clusterExecutionHelper.executeWithAuthenticationRetry(client, client::get)) {
                    status.set(response.getStatus());
                }
            });

            assertThat(status.get()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
            assertThat(requests.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test150SecondUnauthorizedResponseIsNotRetried() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        String originalSecret = getLocalNodeSecret();
        String rotatedSecret = "rotated-" + UUID.randomUUID();
        HttpServer server = startUnauthorizedServer("/ws/cluster/retry-limit", requests, CLUSTER_CHALLENGE);

        try {
            AtomicInteger status = new AtomicInteger();
            executeOnNode(createRemoteNode("retry-limit-node", server), client -> {
                client.path("/retry-limit");
                replaceLocalNodeSecret(rotatedSecret);
                try (Response response = clusterExecutionHelper.executeWithAuthenticationRetry(client, client::get)) {
                    status.set(response.getStatus());
                }
            });

            assertThat(status.get()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
            assertThat(requests.get()).isEqualTo(2);
        } finally {
            server.stop(0);
            replaceLocalNodeSecret(originalSecret);
        }
    }

    @Test
    public void test160NonClusterUnauthorizedResponseIsNotRetried() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        String originalSecret = getLocalNodeSecret();
        String rotatedSecret = "rotated-" + UUID.randomUUID();
        HttpServer server = startUnauthorizedServer("/ws/cluster/non-cluster", requests, "Basic realm=\"midpoint\"");

        try {
            AtomicInteger status = new AtomicInteger();
            executeOnNode(createRemoteNode("non-cluster-challenge-node", server), client -> {
                client.path("/non-cluster");
                replaceLocalNodeSecret(rotatedSecret);
                try (Response response = clusterExecutionHelper.executeWithAuthenticationRetry(client, client::get)) {
                    status.set(response.getStatus());
                }
            });

            assertThat(status.get()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
            assertThat(requests.get()).isEqualTo(1);
        } finally {
            server.stop(0);
            replaceLocalNodeSecret(originalSecret);
        }
    }

    @Test
    public void test170NonAuthenticationHttpFailuresAreNotRetried() throws Exception {
        AtomicInteger forbiddenRequests = new AtomicInteger();
        AtomicInteger serverErrorRequests = new AtomicInteger();
        String originalSecret = getLocalNodeSecret();
        HttpServer server = startServer("/ws/cluster/forbidden", exchange -> {
            forbiddenRequests.incrementAndGet();
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
        });
        server.createContext("/ws/cluster/server-error", exchange -> {
            serverErrorRequests.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        try {
            NodeType node = createRemoteNode("http-failure-node", server);
            AtomicInteger forbiddenStatus = new AtomicInteger();
            executeOnNode(node, client -> {
                client.path("/forbidden");
                replaceLocalNodeSecret("rotated-" + UUID.randomUUID());
                try (Response response = clusterExecutionHelper.executeWithAuthenticationRetry(client, client::get)) {
                    forbiddenStatus.set(response.getStatus());
                }
            });
            AtomicInteger serverErrorStatus = new AtomicInteger();
            executeOnNode(node, client -> {
                client.path("/server-error");
                replaceLocalNodeSecret("rotated-" + UUID.randomUUID());
                try (Response response = clusterExecutionHelper.executeWithAuthenticationRetry(client, client::get)) {
                    serverErrorStatus.set(response.getStatus());
                }
            });

            assertThat(forbiddenStatus.get()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
            assertThat(serverErrorStatus.get()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertThat(forbiddenRequests.get()).isEqualTo(1);
            assertThat(serverErrorRequests.get()).isEqualTo(1);
        } finally {
            server.stop(0);
            replaceLocalNodeSecret(originalSecret);
        }
    }

    @Test
    public void test180ConnectionFailureIsNotRetried() throws Exception {
        HttpServer server = startServer("/ws/cluster/unreachable", exchange -> {
            throw new AssertionError("The stopped server must not receive a request");
        });
        NodeType node = createRemoteNode("connection-failure-node", server);
        server.stop(0);
        AtomicInteger attempts = new AtomicInteger();
        String originalSecret = getLocalNodeSecret();

        try {
            assertThatThrownBy(() -> executeOnNode(node, client -> {
                client.path("/unreachable");
                replaceLocalNodeSecret("rotated-" + UUID.randomUUID());
                clusterExecutionHelper.executeWithAuthenticationRetry(client, () -> {
                    attempts.incrementAndGet();
                    return client.get();
                });
            })).isInstanceOf(RuntimeException.class);
            assertThat(attempts.get()).isEqualTo(1);
        } finally {
            replaceLocalNodeSecret(originalSecret);
        }
    }

    private HttpServer startPeer(AtomicInteger requests, AtomicReference<String> authorization) throws Exception {
        return startServer("/ws/cluster/broadcast", exchange -> {
            requests.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
    }

    private static HttpServer startUnauthorizedServer(String path, AtomicInteger requests, String challenge) throws Exception {
        return startServer(path, exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().set("WWW-Authenticate", challenge);
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
    }

    private static HttpServer startServer(String path, HttpHandler handler) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, handler);
        server.start();
        return server;
    }

    private static String serverUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static NodeType createRemoteNode(String identifier, HttpServer server) {
        return createRemoteNode(null, identifier, server);
    }

    private static NodeType createRemoteNode(String oid, String identifier, HttpServer server) {
        NodeType node = new NodeType()
                .name(new PolyStringType(identifier))
                .nodeIdentifier(identifier)
                .operationalState(NodeOperationalStateType.STARTING)
                .clustered(true)
                .lastCheckInTime(XmlTypeConverter.createXMLGregorianCalendar())
                .url(serverUrl(server));
        if (oid != null) {
            node.setOid(oid);
        }
        node.getIpAddress().add("127.0.0.1");
        return node;
    }

    private void executeOnNode(NodeType node, Consumer<WebClient> code) throws Exception {
        clusterExecutionHelper.execute(
                node,
                (client, ignoredNode, ignoredResult) -> code.accept(client),
                new ClusterExecutionOptions().tryAllNodes(),
                "cluster authentication retry test",
                createOperationResult());
    }

    private String getLocalNodeSecret() throws Exception {
        return protector.decryptString(taskManager.getLocalNode().getSecret());
    }

    private void replaceLocalNodeSecret(String secret) {
        try {
            repositoryService.modifyObject(
                    NodeType.class,
                    taskManager.getLocalNodeOid(),
                    prismContext.deltaFor(NodeType.class)
                            .item(NodeType.F_SECRET).replace(protector.encryptString(secret))
                            .asItemDeltas(),
                    createOperationResult());
        } catch (Exception e) {
            throw new AssertionError("Couldn't replace the local node secret for the test", e);
        }
    }

    private static String clusterAuthorization(String secret) {
        return RestAuthenticationMethod.CLUSTER.getMethod() + " "
                + Base64.getEncoder().encodeToString(secret.getBytes(UTF_8));
    }

    private static boolean sameCredential(String actual, String expected) {
        return actual != null && expected != null
                && MessageDigest.isEqual(actual.getBytes(UTF_8), expected.getBytes(UTF_8));
    }

    private void assertHealthyRequest(AtomicInteger requests, AtomicReference<String> authorization) throws Exception {
        assertThat(requests.get()).isEqualTo(1);
        assertThat(authorization.get()).isNotNull();
        assertThat(matchesLocalClusterCredential(authorization.get())).isTrue();
    }

    private boolean matchesLocalClusterCredential(String authorization) throws Exception {
        String prefix = RestAuthenticationMethod.CLUSTER.getMethod() + " ";
        if (!authorization.startsWith(prefix)) {
            return false;
        }
        byte[] suppliedCredential;
        try {
            suppliedCredential = Base64.getDecoder().decode(authorization.substring(prefix.length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] expectedCredential = protector.decryptString(taskManager.getLocalNode().getSecret()).getBytes(UTF_8);
        return MessageDigest.isEqual(expectedCredential, suppliedCredential);
    }

    private void deleteNodeIfPresent(String oid, OperationResult result) throws Exception {
        try {
            repositoryService.deleteObject(NodeType.class, oid, result);
        } catch (ObjectNotFoundException ignored) {
            // The synthetic node is already absent.
        }
    }
}
