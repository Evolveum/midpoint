/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests mapping of the structured conndev log events (parsed from raw log lines) to the GUI
 * {@link OperationLogEntry} rows provided by {@link OperationResultLogProvider}.
 */
public class OperationResultLogProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testEmptyInput() {
        assertNull(OperationResultLogProvider.fromLines(null));
        assertNull(OperationResultLogProvider.fromLines(List.of()));
        assertNull(OperationResultLogProvider.fromLines(List.of("2026-01-01 00:00:00 plain log line without marker")));
    }

    @Test
    public void testEventMapping() throws Exception {
        List<String> lines = new ArrayList<>();
        // out-of-order on purpose, the parser/entries are ordered by timestamp
        lines.add(line("Retrying after failure", payload(
                "v", 1, "seq", 0, "ts", 3000L, "severity", "WARN", "thread", "worker-2",
                "event", "MESSAGE", "message", "Retrying after failure")));
        lines.add(line("t2 operation start", payload(
                "v", 1, "id", "t2", "seq", 1, "ts", 2000L, "severity", "INFO", "thread", "worker-1",
                "event", "OPERATION", "operation", "create", "objectClass", "urn:scim:Users",
                "message", "Creating user", "location", "ScimRestConnector")));
        lines.add(line("t1 operation start", payload(
                "v", 1, "id", "t1", "seq", 1, "ts", 1000L, "severity", "INFO", "thread", "worker-1",
                "event", "OPERATION", "operation", "search", "objectClass", "urn:scim:Users",
                "message", "Searching Users", "location", "search(ScimRestConnector.java:42)")));
        lines.add(line("t1 request", payload(
                "v", 1, "id", "t1", "seq", 2, "ts", 1100L, "severity", "INFO", "thread", "worker-1",
                "event", "PROTOCOL", "message", "Sending request", "location", "send(ScimRestClient.java:88)",
                "protocol", Map.of("type", "http", "kind", "request", "method", "GET",
                        "uri", "https://rest.example.com/v1/Users"))));
        lines.add(line("t1 request body", payload(
                "v", 1, "id", "t1", "seq", 3, "ts", 1101L, "severity", "DEBUG", "thread", "worker-1",
                "event", "PROTOCOL", "message", "Request body",
                "protocol", Map.of("type", "http", "kind", "request-body", "body", "filter=userName+eq+largo"))));
        lines.add(line("t2 sql", payload(
                "v", 1, "id", "t2", "seq", 2, "ts", 2100L, "severity", "INFO", "thread", "worker-1",
                "event", "PROTOCOL", "message", "Executing SQL",
                "protocol", Map.of("type", "sql", "kind", "query", "sql", "INSERT INTO users (name) VALUES (?)",
                        "params", Map.of("name", "Largo")))));
        lines.add(line("t1 response", payload(
                "v", 1, "id", "t1", "seq", 4, "ts", 1400L, "severity", "INFO", "thread", "worker-1",
                "event", "PROTOCOL", "message", "Received response",
                "protocol", Map.of("type", "http", "kind", "response", "status", 200))));
        lines.add(line("t1 response body", payload(
                "v", 1, "id", "t1", "seq", 5, "ts", 1401L, "severity", "DEBUG", "thread", "worker-1",
                "event", "PROTOCOL", "message", "Response body",
                "protocol", Map.of("type", "http", "kind", "response-body", "body", "[{\"userName\":\"largo\"}]"))));
        lines.add(line("t2 error", payload(
                "v", 1, "id", "t2", "seq", 3, "ts", 2200L, "severity", "ERROR", "thread", "worker-1",
                "event", "ERROR", "message", "Create failed",
                "error", Map.of("message", "Duplicate key",
                        "stacktrace", List.of(
                                "java.lang.Exception: Duplicate key",
                                " at com.evolveum.polygon.connector.sql.SqlConnector.insert(SqlConnector.java:120)")))));
        lines.add(line("t1 result", payload(
                "v", 1, "id", "t1", "seq", 6, "ts", 1500L, "severity", "INFO", "thread", "worker-1",
                "event", "RESULT", "message", "Found 3 entries",
                "result", Map.of("ok", true, "value", 3))));
        lines.add("2026-01-01 00:00:00 plain log line without marker");

        OperationResultLogProvider provider = OperationResultLogProvider.fromLines(lines);

        assertNotNull(provider);
        assertTrue(provider.isDebugModeEnabled());
        List<OperationLogEntry> entries = provider.getOperationLogEntries();
        // t1: start + request + response + result (body companions merged), t2: start + sql + error, message: 1
        assertEquals(entries.size(), 8);

        OperationLogEntry t1Start = entries.get(0);
        assertEquals(t1Start.getTraceId(), "t1");
        assertEquals(t1Start.getLevel(), OperationLogLevel.INFO);
        assertEquals(t1Start.getMessage(), "Searching Users");
        assertEquals(t1Start.getMethod(), "search");
        assertEquals(t1Start.getSourceFile(), "ScimRestConnector.java");
        assertEquals(t1Start.getLineNumber(), Integer.valueOf(42));
        assertNull(t1Start.getProtocol());

        OperationLogEntry t1Request = entries.get(1);
        HttpProtocol t1RequestProtocol = (HttpProtocol) t1Request.getProtocol();
        assertNotNull(t1Request);
        assertNotNull(t1RequestProtocol.request());
        assertEquals(t1RequestProtocol.request().method(), HttpMethod.GET);
        assertEquals(t1RequestProtocol.request().url(), "https://rest.example.com/v1/Users");
        // body merged from the companion request-body event
        assertEquals(t1RequestProtocol.request().body(), "filter=userName+eq+largo");
        assertNull(t1RequestProtocol.response());
        assertEquals(t1Request.getMethod(), "send");
        assertEquals(t1Request.getSourceFile(), "ScimRestClient.java");
        assertEquals(t1Request.getLineNumber(), Integer.valueOf(88));

        OperationLogEntry t1Response = entries.get(2);
        HttpProtocol t1ResponseProtocol = (HttpProtocol) t1Response.getProtocol();
        assertNull(t1ResponseProtocol.request());
        assertNotNull(t1ResponseProtocol.response());
        assertEquals(t1ResponseProtocol.response().statusCode(), 200);
        assertEquals(t1ResponseProtocol.response().body(), "[{\"userName\":\"largo\"}]");

        OperationLogEntry t1Result = entries.get(3);
        assertEquals(t1Result.getLevel(), OperationLogLevel.INFO);
        assertEquals(t1Result.getMessage(), "Found 3 entries");

        OperationLogEntry t2Start = entries.get(4);
        assertEquals(t2Start.getTraceId(), "t2");
        assertEquals(t2Start.getMessage(), "Creating user");
        // plain location kept as fully qualified class
        assertEquals(t2Start.getFullyQualifiedClass(), "ScimRestConnector");

        OperationLogEntry t2Sql = entries.get(5);
        SqlProtocol sqlProtocol = (SqlProtocol) t2Sql.getProtocol();
        assertTrue(sqlProtocol.query().contains("INSERT INTO users (name) VALUES (?)"));
        assertTrue(sqlProtocol.query().contains("name = Largo"));

        OperationLogEntry t2Error = entries.get(6);
        assertEquals(t2Error.getLevel(), OperationLogLevel.ERROR);
        assertEquals(t2Error.getMessage(), "Duplicate key");
        assertNotNull(t2Error.getStacktrace());
        assertTrue(t2Error.getStacktrace().contains("SqlConnector.java:120"));
        assertTrue(t2Error.getStacktrace().contains("\n"));

        OperationLogEntry message = entries.get(7);
        assertNull(message.getTraceId());
        assertEquals(message.getLevel(), OperationLogLevel.WARN);
        assertEquals(message.getMessage(), "Retrying after failure");
    }

    private static Map<String, Object> payload(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    private static String line(String message, Map<String, Object> payload) {
        try {
            return message + " conndev-log/v1 " + MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
