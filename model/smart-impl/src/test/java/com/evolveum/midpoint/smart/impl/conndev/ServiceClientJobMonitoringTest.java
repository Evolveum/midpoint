/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.util.exception.SystemException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the job monitoring in {@link ServiceClient}: transient gateway errors
 * (HTTP 502/504) must be tolerated for the configured window instead of failing the job on
 * the first occurrence, and error reports must contain the HTTP status code and the response
 * message.
 */
public class ServiceClientJobMonitoringTest {

    private static final String SESSION_ID = "00000000-0000-0000-0000-000000000000";
    private static final String JOB_FINISHED_JSON = "{\"status\":\"finished\",\"result\":{\"name\":\"x\"}}";
    private static final String JOB_RUNNING_JSON = "{\"status\":\"running\"}";

    /** A scripted response: either a status code with a body, or a dropped connection (no response at all). */
    private record ScriptedResponse(int code, String body, boolean dropConnection) {
        static ScriptedResponse of(int code, String body) {
            return new ScriptedResponse(code, body, false);
        }

        static ScriptedResponse drop() {
            return new ScriptedResponse(0, null, true);
        }
    }

    private static HttpServer server;
    private static CloseableHttpClient http;
    private static String apiBase;

    private static final Deque<ScriptedResponse> jobStatusResponses = new ArrayDeque<>();
    private static final Deque<ScriptedResponse> documentationResponses = new ArrayDeque<>();
    private static ScriptedResponse jobStatusDefault = ScriptedResponse.of(200, JOB_FINISHED_JSON);
    private static ScriptedResponse documentationDefault = ScriptedResponse.of(204, null);

    @BeforeClass
    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            // Drain the request body so the connection stays usable
            exchange.getRequestBody().readAllBytes();
            var path = exchange.getRequestURI().getPath();
            var method = exchange.getRequestMethod();
            if (path.startsWith("/api/v1/session/" + SESSION_ID + "/documentation/") && "HEAD".equals(method)) {
                respond(exchange, next(documentationResponses, documentationDefault));
            } else if (path.equals("/api/v1/session/" + SESSION_ID) && "HEAD".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            } else if (path.startsWith("/api/v1/digester/") && "POST".equals(method)) {
                respondJson(exchange, 200, "{\"jobId\":\"job-1\"}");
            } else if (path.startsWith("/api/v1/digester/") && "GET".equals(method)) {
                respond(exchange, next(jobStatusResponses, jobStatusDefault));
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });
        server.start();
        apiBase = "http://localhost:" + server.getAddress().getPort() + "/api/v1/";
        http = HttpClients.createDefault();
    }

    @AfterClass
    public static void stopServer() throws IOException {
        server.stop(0);
        http.close();
    }

    @BeforeMethod
    public void resetScripts() {
        jobStatusResponses.clear();
        documentationResponses.clear();
        jobStatusDefault = ScriptedResponse.of(200, JOB_FINISHED_JSON);
        documentationDefault = ScriptedResponse.of(204, null);
    }

    private static ScriptedResponse next(Deque<ScriptedResponse> scripted, ScriptedResponse fallback) {
        return scripted.isEmpty() ? fallback : scripted.pollFirst();
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, ScriptedResponse response) throws IOException {
        if (response.dropConnection()) {
            // Close without sending any response: the client sees a connection-level error
            exchange.close();
            return;
        }
        var body = response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.code(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    private static void respondJson(com.sun.net.httpserver.HttpExchange exchange, int code, String body) throws IOException {
        respond(exchange, ScriptedResponse.of(code, body));
    }

    private ServiceClient client(long transientErrorTimeoutMs) {
        return new ServiceClient(apiBase, null, SESSION_ID, null, null, http, transientErrorTimeoutMs);
    }

    @Test
    public void transientGatewayErrorsAreToleratedAndJobSucceeds() throws Exception {
        jobStatusResponses.addLast(ScriptedResponse.of(502, "Bad Gateway"));
        jobStatusResponses.addLast(ScriptedResponse.of(504, "Gateway Timeout"));
        jobStatusResponses.addLast(ScriptedResponse.of(200, JOB_FINISHED_JSON));

        var client = client(1_000);
        var job = client.postJob("digester/{sessionId}/classes", "rest", false);
        var name = job.waitAndProcess(50, () -> true, o -> o.get("name").asText());
        assertThat(name).isEqualTo("x");
    }

    @Test
    public void transientErrorWindowResetsOnSuccessfulPoll() throws Exception {
        // Without the reset, the unbroken 502 sequence (t0..t200) would exceed the 150ms window
        jobStatusResponses.addLast(ScriptedResponse.of(502, "Bad Gateway"));
        jobStatusResponses.addLast(ScriptedResponse.of(502, "Bad Gateway"));
        jobStatusResponses.addLast(ScriptedResponse.of(200, JOB_RUNNING_JSON));
        jobStatusResponses.addLast(ScriptedResponse.of(502, "Bad Gateway"));
        jobStatusResponses.addLast(ScriptedResponse.of(502, "Bad Gateway"));
        jobStatusResponses.addLast(ScriptedResponse.of(200, JOB_FINISHED_JSON));

        var client = client(150);
        var job = client.postJob("digester/{sessionId}/classes", "rest", false);
        var result = job.waitAndProcess(50, () -> true, ObjectNode.class::cast);
        assertThat(result.get("name").asText()).isEqualTo("x");
    }

    @Test
    public void persistentGatewayErrorFailsAfterTimeoutWithStatusAndBody() throws Exception {
        jobStatusDefault = ScriptedResponse.of(502, "upstream unavailable");

        var start = System.currentTimeMillis();
        assertThatThrownBy(() -> {
            var client = client(300);
            var job = client.postJob("digester/{sessionId}/classes", "rest", false);
            job.waitAndProcess(50, () -> true, ObjectNode.class::cast);
        }).isInstanceOf(SystemException.class)
                .hasMessageContaining("The connector generation service reported a failure")
                .hasMessageContaining("HTTP 502")
                .hasMessageContaining("upstream unavailable");
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(300);
    }

    @Test
    public void nonTransientStatusFailsImmediatelyWithStatusAndBody() throws Exception {
        jobStatusDefault = ScriptedResponse.of(422, "{\"message\":\"bad request\"}");

        var start = System.currentTimeMillis();
        assertThatThrownBy(() -> {
            var client = client(60_000);
            var job = client.postJob("digester/{sessionId}/classes", "rest", false);
            job.waitAndProcess(50, () -> true, ObjectNode.class::cast);
        }).isInstanceOf(SystemException.class)
                .hasMessageContaining("The connector generation service reported a failure")
                .hasMessageContaining("HTTP 422")
                .hasMessageContaining("bad request");
        assertThat(System.currentTimeMillis() - start).isLessThan(3_000);
    }

    @Test
    public void serviceSideErrorsAreStillReported() {
        jobStatusDefault = ScriptedResponse.of(200, "{\"status\":\"failed\",\"errors\":[\"boom happened\"]}");

        assertThatThrownBy(() -> {
            var client = client(300);
            var job = client.postJob("digester/{sessionId}/classes", "rest", false);
            job.waitAndProcess(50, () -> true, ObjectNode.class::cast);
        }).isInstanceOf(SystemException.class)
                .hasMessageContaining("The connector generation service reported a failure")
                .hasMessageContaining("boom happened");
    }

    @Test
    public void connectionErrorDuringPollingFailsAndReportsTheMessage() throws Exception {
        // The classic client transparently retries idempotent GETs on connection-level errors, so
        // the default response must also drop the connection for the retry attempts to fail too.
        jobStatusResponses.addLast(ScriptedResponse.drop());
        jobStatusDefault = ScriptedResponse.drop();

        var client = client(60_000);
        var job = client.postJob("digester/{sessionId}/classes", "rest", false);
        assertThatThrownBy(() -> job.waitAndProcess(50, () -> true, ObjectNode.class::cast))
                .isInstanceOf(SystemException.class)
                .hasMessageStartingWith("The connector generation service reported a failure");
        // No HTTP response was received, so only the exception message is recorded
        assertThat(httpFailureMessage(job)).isNotNull();
        assertThat(httpFailureCode(job)).isZero();
    }

    @Test
    public void documentationAwaitToleratesTransientGatewayErrors() throws Exception {
        documentationResponses.addLast(ScriptedResponse.of(502, "Bad Gateway"));
        documentationResponses.addLast(ScriptedResponse.of(204, null));

        var client = client(1_000);
        client.synchronizationClient().awaitDocumentation("doc-1", 50, () -> true);
    }

    @Test
    public void documentationAwaitFailsOnPersistentGatewayError() throws Exception {
        documentationDefault = ScriptedResponse.of(504, "Gateway Timeout");

        var start = System.currentTimeMillis();
        assertThatThrownBy(() -> {
            var client = client(300);
            client.synchronizationClient().awaitDocumentation("doc-1", 50, () -> true);
        }).isInstanceOf(IOException.class)
                .hasMessageContaining("doc-1")
                .hasMessageContaining("HTTP 504");
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(300);
    }

    private static String httpFailureMessage(ServiceClient.Job job) throws Exception {
        var field = ServiceClient.Job.class.getDeclaredField("httpFailureMessage");
        field.setAccessible(true);
        return (String) field.get(job);
    }

    private static int httpFailureCode(ServiceClient.Job job) throws Exception {
        var field = ServiceClient.Job.class.getDeclaredField("httpFailureCode");
        field.setAccessible(true);
        return (int) field.get(job);
    }
}
