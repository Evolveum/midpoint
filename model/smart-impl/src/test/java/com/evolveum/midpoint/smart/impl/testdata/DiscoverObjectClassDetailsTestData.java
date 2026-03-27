/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.testdata;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.IOException;
import java.io.InputStream;

import com.evolveum.midpoint.smart.impl.TestDiscoverObjectClassDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Utility for defining atomic WireMock stubs and preparing mock data for {@link TestDiscoverObjectClassDetails}.
 */
public class DiscoverObjectClassDetailsTestData {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MOCK_PATH_PREFIX = "/codegen/mock/openproject/";
    private static final String ENDPOINTS_PATH_PATTERN = MOCK_PATH_PREFIX + "endpoints/%s.json";
    private static final String SCHEMA_PATH_PATTERN = MOCK_PATH_PREFIX + "schema/%s.json";
    private static final String TEMPLATE_PATH = MOCK_PATH_PREFIX + "responseJobFinishedTemplate.json";

    // --- WireMock Stub Definitions ---

    public static void stubSessionHeadSuccess(WireMockServer wireMockServer, String sessionId) {
        wireMockServer.stubFor(head(urlMatching("/session/" + sessionId))
                .willReturn(aResponse().withStatus(204)));
    }

    public static void stubSessionHeadNotFound(WireMockServer wireMockServer, String sessionId) {
        wireMockServer.stubFor(head(urlMatching("/session/" + sessionId))
                .willReturn(aResponse().withStatus(404)));
    }

    public static void stubSessionPostSuccess(WireMockServer wireMockServer, String sessionId) {
        wireMockServer.stubFor(post(urlMatching("/session/" + sessionId))
                .willReturn(aResponse().withStatus(201)));
    }

    public static void stubEndpointsJobSuccess(WireMockServer wireMockServer, String sessionId, String objectClass, String jobId) {
        wireMockServer.stubFor(post(urlMatching("/digester/" + sessionId + "/classes/" + objectClass + "/endpoints"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"jobId\": \"" + jobId + "\" }")));
    }

    public static void stubEndpointsPollSuccess(WireMockServer wireMockServer, String sessionId, String objectClass, String jobId) throws IOException {
        wireMockServer.stubFor(get(urlPathEqualTo("/digester/" + sessionId + "/classes/" + objectClass + "/endpoints"))
                .withQueryParam("jobId", equalTo(jobId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getEndpointsJson(objectClass))));
    }

    public static void stubAttributesJobSuccess(WireMockServer wireMockServer, String sessionId, String objectClass, String jobId) {
        wireMockServer.stubFor(post(urlMatching("/digester/" + sessionId + "/classes/" + objectClass + "/attributes"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"jobId\": \"" + jobId + "\" }")));
    }

    public static void stubAttributesPollSuccess(WireMockServer wireMockServer, String sessionId, String objectClass, String jobId) throws IOException {
        wireMockServer.stubFor(get(urlPathEqualTo("/digester/" + sessionId + "/classes/" + objectClass + "/attributes"))
                .withQueryParam("jobId", equalTo(jobId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getAttributesJson(objectClass))));
    }

    public static void stubAttributesPollFailure(WireMockServer wireMockServer, String sessionId, String objectClass, String jobId) {
        wireMockServer.stubFor(get(urlPathEqualTo("/digester/" + sessionId + "/classes/" + objectClass + "/attributes"))
                .withQueryParam("jobId", equalTo(jobId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"jobId\": \"" + jobId + "\", \"status\": \"failed\" }")));
    }

    // --- Mock Data Generation ---

    private static String getEndpointsJson(String objectClass) throws IOException {
        JsonNode root = readTree(String.format(ENDPOINTS_PATH_PATTERN, objectClass));
        ObjectNode response = (ObjectNode) readTree(TEMPLATE_PATH);

        ObjectNode resultNode = MAPPER.createObjectNode();
        resultNode.set("endpoints", root.get("endpoints"));
        response.set("result", resultNode);

        return response.toString();
    }

    private static String getAttributesJson(String objectClass) throws IOException {
        JsonNode root = readTree(String.format(SCHEMA_PATH_PATTERN, objectClass));
        ObjectNode response = (ObjectNode) readTree(TEMPLATE_PATH);

        ObjectNode resultNode = MAPPER.createObjectNode();
        resultNode.set("attributes", root.get("attributes"));
        response.set("result", resultNode);

        return response.toString();
    }

    private static JsonNode readTree(String path) throws IOException {
        try (InputStream is = getStream(path)) {
            return MAPPER.readTree(is);
        }
    }

    private static InputStream getStream(String path) throws IOException {
        InputStream is = DiscoverObjectClassDetailsTestData.class.getResourceAsStream(path);
        if (is == null) {
            throw new IOException("Resource not found: " + path);
        }
        return is;
    }
}
