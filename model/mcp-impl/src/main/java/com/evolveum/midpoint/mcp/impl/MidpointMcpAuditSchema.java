/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.evolveum.midpoint.mcp.api.MidpointMcpSchemaAttribute;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.commons.lang3.StringUtils;

/**
 * Static MCP audit path vocabulary (not object schema). Used for validation, ordering, and filter translation.
 */
final class MidpointMcpAuditSchema {

    private static final Pattern CUSTOM_COLUMN_PATH = Pattern.compile("^customColumn\\.(.+)$");

    private MidpointMcpAuditSchema() {}

    static Map<String, MidpointMcpSchemaAttribute> attributesByPath() {
        Map<String, MidpointMcpSchemaAttribute> map = new LinkedHashMap<>();
        put(map, "timestamp", "datetime");
        put(map, "eventType", "string");
        put(map, "eventStage", "string");
        put(map, "outcome", "string");
        put(map, "initiator.oid", "string");
        put(map, "initiator.name", "string");
        put(map, "target.oid", "string");
        put(map, "target.name", "string");
        put(map, "target.type", "string");
        put(map, "channel", "string");
        put(map, "task.oid", "string");
        put(map, "task.name", "string");
        put(map, "node", "string");
        put(map, "message", "string");
        put(map, "delta", "string");
        put(map, "result", "string");
        put(map, "sessionIdentifier", "string");
        put(map, "requestIdentifier", "string");
        return map;
    }

    private static void put(Map<String, MidpointMcpSchemaAttribute> map, String path, String type) {
        MidpointMcpSchemaAttribute a = new MidpointMcpSchemaAttribute();
        a.setPath(path);
        a.setType(type);
        map.put(path, a);
    }

    /** Returns custom column property name or null if path is not {@code customColumn.<name>}. */
    static String customColumnKey(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        var m = CUSTOM_COLUMN_PATH.matcher(path.trim());
        return m.matches() ? m.group(1) : null;
    }

    static boolean isKnownPath(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        String p = path.trim();
        if (attributesByPath().containsKey(p)) {
            return true;
        }
        return customColumnKey(p) != null;
    }

    /**
     * Item paths for {@code orderBy}; only single-segment Prism paths (audit top-level items).
     */
    static ItemPath orderByItemPath(String mcpPath) {
        if (StringUtils.isBlank(mcpPath)) {
            throw new IllegalArgumentException("orderBy.path is required");
        }
        String p = mcpPath.trim();
        return switch (p) {
            case "timestamp" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_TIMESTAMP;
            case "eventType" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_EVENT_TYPE;
            case "eventStage" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_EVENT_STAGE;
            case "outcome" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_OUTCOME;
            case "channel" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_CHANNEL;
            case "message" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_MESSAGE;
            case "node" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_NODE_IDENTIFIER;
            case "task.name" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_TASK_IDENTIFIER;
            case "task.oid" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_TASK_OID;
            case "sessionIdentifier" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_SESSION_IDENTIFIER;
            case "requestIdentifier" -> com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_REQUEST_IDENTIFIER;
            default -> throw new IllegalArgumentException(
                    "orderBy path '" + p + "' is not supported for audit (use timestamp, eventType, eventStage, outcome, "
                            + "channel, message, node, task.name, task.oid, sessionIdentifier, requestIdentifier)");
        };
    }

    static void validateOrderByPaths(List<com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedOrderBySpec> orderBy) {
        if (orderBy == null || orderBy.isEmpty()) {
            return;
        }
        for (var ob : orderBy) {
            if (ob == null || StringUtils.isBlank(ob.getPath())) {
                throw new IllegalArgumentException("orderBy.path is required for each entry");
            }
            orderByItemPath(ob.getPath().trim());
        }
    }

    static String normalizeOp(String op) {
        if (StringUtils.isBlank(op)) {
            throw new IllegalArgumentException("filter.op is required");
        }
        return op.trim().toLowerCase(Locale.ROOT);
    }
}
