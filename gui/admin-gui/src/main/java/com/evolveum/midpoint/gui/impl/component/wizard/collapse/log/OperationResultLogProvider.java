/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evolveum.polygon.conndev.devtools.log.ConndevLogFormat;
import com.evolveum.polygon.conndev.devtools.log.DetailEvent;
import com.evolveum.polygon.conndev.devtools.log.LogSeverity;
import com.evolveum.polygon.conndev.devtools.log.OperationLogParser;
import com.evolveum.polygon.conndev.devtools.log.OperationTrace;
import com.evolveum.polygon.conndev.devtools.log.Outcome;
import com.evolveum.polygon.conndev.devtools.log.ProtocolEvent;
import com.evolveum.polygon.conndev.devtools.log.ProtocolPayload;

/**
 * {@link OperationLogProvider} backed by the structured information parsed (via conndev devtools,
 * see {@link OperationLogParser}) from the raw ConnId log lines collected in an operation result's
 * log segments - one {@link OperationLogEntry} row per structured event: the operation start, each
 * detail, each protocol event (HTTP request/response or SQL query) and the end outcome.
 *
 * <p>Entries are computed once at construction time (on the server thread) and cached, so the
 * provider stays a plain serializable value for Wicket page serialization.
 */
public final class OperationResultLogProvider implements OperationLogProvider {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Matches the caller-location form {@code method(File.java:42)}. */
    private static final Pattern LOCATION_WITH_METHOD = Pattern.compile("^(.*?)\\((.+):(\\d+)\\)$");

    private final List<OperationLogEntry> entries;

    private OperationResultLogProvider(List<OperationLogEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    /**
     * Parses the given raw log lines and builds the provider.
     *
     * @param lines the raw log lines collected from the operation result's log segments
     * @return the provider with the parsed entries ordered by timestamp, or {@code null} when the
     *         lines carry no structured log events
     */
    public static OperationResultLogProvider fromLines(Collection<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        List<OperationLogEntry> entries = new ArrayList<>();
        for (OperationTrace trace : OperationLogParser.parse(lines)) {
            toEntries(trace, entries);
        }
        if (entries.isEmpty()) {
            return null;
        }
        entries.sort(Comparator.comparingLong(entry -> entry.getTimestamp() == null ? 0L : entry.getTimestamp().toEpochMilli()));
        return new OperationResultLogProvider(entries);
    }

    @Override
    public List<OperationLogEntry> getOperationLogEntries() {
        return entries;
    }

    @Override
    public boolean isDebugModeEnabled() {
        // entries only exist when the task was run with connector log capture enabled
        return true;
    }

    // ========================================================================
    // Mapping: OperationTrace (conndev) -> OperationLogEntry (GUI)
    // ========================================================================

    private static void toEntries(OperationTrace trace, List<OperationLogEntry> entries) {
        if (trace == null) {
            return;
        }
        if (trace.id() != null) {
            // operation entry: start row (standalone message traces carry only their detail row)
            entries.add(entry(trace.id(), trace.startTs(), OperationLogLevel.INFO, trace.firstMessage(), trace.location(), null, null));
        }
        for (DetailEvent detail : trace.details()) {
            entries.add(entry(trace.id(), detail.ts(), toLevel(detail.severity()), detail.message(), detail.location(), null, null));
        }
        toProtocolEntries(trace, entries);
        if (trace.outcome() != null) {
            entries.add(outcomeEntry(trace));
        }
    }

    /** Row for the terminating result/error event of the trace. */
    private static OperationLogEntry outcomeEntry(OperationTrace trace) {
        Outcome outcome = trace.outcome();
        String stacktrace = !outcome.ok() && outcome.stacktrace() != null && !outcome.stacktrace().isEmpty()
                ? String.join("\n", outcome.stacktrace())
                : null;
        return entry(
                trace.id(),
                trace.endTs() == null ? trace.startTs() : trace.endTs(),
                outcome.ok() ? OperationLogLevel.INFO : OperationLogLevel.ERROR,
                outcome.message() != null ? outcome.message() : trace.firstMessage(),
                trace.location(),
                null,
                stacktrace);
    }

    /**
     * Rows for the trace's protocol events. HTTP {@code request-body}/{@code response-body}
     * companion payloads are merged into the preceding request/response row instead of becoming
     * rows of their own.
     */
    private static void toProtocolEntries(OperationTrace trace, List<OperationLogEntry> entries) {
        List<ProtocolEvent> events = trace.protocolEvents();
        for (int i = 0; i < events.size(); i++) {
            ProtocolEvent event = events.get(i);
            ProtocolPayload payload = event.protocol();
            if (payload == null || isBodyCompanion(events, i)) {
                continue;
            }
            entries.add(entry(
                    trace.id(),
                    event.ts(),
                    toLevel(event.severity()),
                    event.message(),
                    event.location(),
                    toProtocol(events, i),
                    null));
        }
    }

    /** Whether the event at {@code index} is a body companion already merged into its header event. */
    private static boolean isBodyCompanion(List<ProtocolEvent> events, int index) {
        ProtocolPayload payload = events.get(index).protocol();
        if (payload == null || !ConndevLogFormat.PROTOCOL_HTTP.equals(payload.type())) {
            return false;
        }
        String headerKind = ConndevLogFormat.HTTP_REQUEST_BODY.equals(payload.kind())
                ? ConndevLogFormat.HTTP_REQUEST
                : ConndevLogFormat.HTTP_RESPONSE_BODY.equals(payload.kind())
                        ? ConndevLogFormat.HTTP_RESPONSE
                        : null;
        if (headerKind == null) {
            return false;
        }
        for (int i = index - 1; i >= 0; i--) {
            ProtocolPayload previous = events.get(i).protocol();
            if (previous == null || !ConndevLogFormat.PROTOCOL_HTTP.equals(previous.type())) {
                continue;
            }
            return headerKind.equals(previous.kind());
        }
        return false;
    }

    /** Builds the GUI protocol detail for the protocol event at {@code index}, merging its body companion. */
    private static OperationLogProtocol toProtocol(List<ProtocolEvent> events, int index) {
        ProtocolPayload payload = events.get(index).protocol();
        if (ConndevLogFormat.PROTOCOL_HTTP.equals(payload.type())) {
            if (ConndevLogFormat.HTTP_REQUEST.equals(payload.kind())) {
                return new HttpProtocol(
                        new HttpRequest(toHttpMethod(payload.method()), payload.uri(), mergedBody(events, index, payload)),
                        null);
            }
            if (ConndevLogFormat.HTTP_RESPONSE.equals(payload.kind())) {
                return new HttpProtocol(null, new HttpResponse(
                        payload.status() == null ? 0 : payload.status(),
                        mergedBody(events, index, payload)));
            }
            return null;
        }
        if (ConndevLogFormat.PROTOCOL_SQL.equals(payload.type())) {
            return new SqlProtocol(toSqlQuery(payload));
        }
        return null;
    }

    /** The header event's own body, or the body of its companion {@code *-body} event when present. */
    private static String mergedBody(List<ProtocolEvent> events, int index, ProtocolPayload header) {
        String ownBody = header.body();
        for (int i = index + 1; i < events.size(); i++) {
            ProtocolPayload payload = events.get(i).protocol();
            if (payload == null || !ConndevLogFormat.PROTOCOL_HTTP.equals(payload.type())) {
                continue;
            }
            boolean companion = (ConndevLogFormat.HTTP_REQUEST.equals(header.kind())
                    && ConndevLogFormat.HTTP_REQUEST_BODY.equals(payload.kind()))
                    || (ConndevLogFormat.HTTP_RESPONSE.equals(header.kind())
                            && ConndevLogFormat.HTTP_RESPONSE_BODY.equals(payload.kind()));
            if (companion) {
                return payload.body() != null ? payload.body() : ownBody;
            }
            if (ConndevLogFormat.HTTP_REQUEST.equals(payload.kind()) || ConndevLogFormat.HTTP_RESPONSE.equals(payload.kind())) {
                break;
            }
        }
        return ownBody;
    }

    /** SQL query text, with bound parameters appended as comment lines when present. */
    private static String toSqlQuery(ProtocolPayload payload) {
        String query = payload.sql();
        Map<String, Object> params = payload.params();
        if (query == null || params == null || params.isEmpty()) {
            return query;
        }
        StringBuilder sb = new StringBuilder(query);
        sb.append("\n-- params:");
        for (Map.Entry<String, Object> param : params.entrySet()) {
            sb.append("\n--   ").append(param.getKey()).append(" = ").append(param.getValue());
        }
        return sb.toString();
    }

    /**
     * One GUI row from the common event fields. The caller-location string is applied best effort
     * (see {@link #applyLocation}); protocol and stacktrace are attached when non-null.
     */
    private static OperationLogEntry entry(
            String traceId, long timestamp, OperationLogLevel level, String message, String location,
            OperationLogProtocol protocol, String stacktrace) {
        OperationLogEntry.Builder builder = OperationLogEntry.builder()
                .traceId(traceId)
                .timestamp(Instant.ofEpochMilli(timestamp))
                .level(level)
                .message(message);
        if (protocol != null) {
            builder.protocol(protocol);
        }
        if (stacktrace != null) {
            builder.stacktrace(stacktrace);
        }
        applyLocation(builder, location);
        return builder.build();
    }

    private static HttpMethod toHttpMethod(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        try {
            return HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static OperationLogLevel toLevel(LogSeverity severity) {
        if (severity == null) {
            return OperationLogLevel.INFO;
        }
        return switch (severity) {
            case TRACE -> OperationLogLevel.TRACE;
            case DEBUG -> OperationLogLevel.DEBUG;
            case INFO -> OperationLogLevel.INFO;
            case WARN -> OperationLogLevel.WARN;
            case ERROR -> OperationLogLevel.ERROR;
        };
    }

    /**
     * Applies the caller-location string to the entry builder. The wire format carries the location
     * as a plain string; the {@code method(File.java:42)} form is split into method/source file/line,
     * anything else is kept as the fully qualified class (displayed as the "Class"/source label).
     */
    private static void applyLocation(OperationLogEntry.Builder builder, String location) {
        if (location == null || location.isBlank()) {
            return;
        }
        Matcher matcher = LOCATION_WITH_METHOD.matcher(location.trim());
        if (matcher.matches()) {
            builder.method(matcher.group(1).trim())
                    .sourceFile(matcher.group(2).trim())
                    .lineNumber(Integer.valueOf(matcher.group(3)));
        } else {
            builder.fullyQualifiedClass(location.trim());
        }
    }
}
