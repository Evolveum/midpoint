/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
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

    private HttpServer startPeer(AtomicInteger requests, AtomicReference<String> authorization) throws Exception {
        return startServer("/ws/cluster/broadcast", exchange -> {
            requests.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
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
