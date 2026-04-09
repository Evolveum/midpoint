/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditExplainResult;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditInitiatorView;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditRecordSummary;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditTargetView;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditTaskView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;

final class MidpointMcpAuditNormalizer {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointMcpAuditNormalizer.class);

    private static final int MAX_DELTA_OPS = 50;
    private static final int MAX_RESULT_CHARS = 8000;

    private MidpointMcpAuditNormalizer() {}

    static MidpointMcpAuditRecordSummary toSummary(AuditEventRecordType r) {
        MidpointMcpAuditRecordSummary s = new MidpointMcpAuditRecordSummary();
        s.setId(r.getEventIdentifier());
        s.setTimestamp(formatTs(r.getTimestamp()));
        if (r.getEventType() != null) {
            s.setEventType(r.getEventType().name());
        }
        if (r.getEventStage() != null) {
            s.setEventStage(r.getEventStage().name());
        }
        if (r.getOutcome() != null) {
            s.setOutcome(r.getOutcome().name());
        }
        s.setInitiator(initiatorView(r.getInitiatorRef()));
        s.setTarget(targetView(r.getTargetRef()));
        s.setChannel(r.getChannel());
        s.setTask(taskView(r));
        s.setNode(r.getNodeIdentifier());
        s.setMessage(r.getMessage());
        s.setSummary(buildSummary(r));
        return s;
    }

    static MidpointMcpAuditExplainResult toExplain(
            AuditEventRecordType r, boolean includeDelta, boolean includeResult, PrismContext prismContext) {
        MidpointMcpAuditExplainResult e = new MidpointMcpAuditExplainResult();
        e.setId(r.getEventIdentifier());
        e.setTimestamp(formatTs(r.getTimestamp()));
        if (r.getEventType() != null) {
            e.setEventType(r.getEventType().name());
        }
        if (r.getEventStage() != null) {
            e.setEventStage(r.getEventStage().name());
        }
        if (r.getOutcome() != null) {
            e.setOutcome(r.getOutcome().name());
        }
        e.setInitiator(initiatorView(r.getInitiatorRef()));
        e.setTarget(targetView(r.getTargetRef()));
        e.setChannel(r.getChannel());
        e.setTask(taskView(r));
        e.setNode(r.getNodeIdentifier());
        e.setMessage(r.getMessage());
        String summary = buildSummary(r);
        e.setSummary(summary);
        e.setExplanation(buildExplanation(r, summary));
        if (includeDelta) {
            if (prismContext != null) {
                try {
                    AuditEventRecord.adopt(r);
                } catch (SchemaException ex) {
                    LOGGER.debug("Could not adopt audit record {} deltas for MCP explain: {}", r.getEventIdentifier(), ex.getMessage());
                }
            }
            e.setDelta(deltaPayload(r));
        }
        if (includeResult) {
            e.setResult(resultPayload(r));
        }
        return e;
    }

    private static MidpointMcpAuditInitiatorView initiatorView(ObjectReferenceType ref) {
        if (ref == null || StringUtils.isBlank(ref.getOid())) {
            return null;
        }
        MidpointMcpAuditInitiatorView v = new MidpointMcpAuditInitiatorView();
        v.setOid(ref.getOid());
        v.setName(refName(ref));
        return v;
    }

    private static MidpointMcpAuditTargetView targetView(ObjectReferenceType ref) {
        if (ref == null || StringUtils.isBlank(ref.getOid())) {
            return null;
        }
        MidpointMcpAuditTargetView v = new MidpointMcpAuditTargetView();
        v.setOid(ref.getOid());
        v.setName(refName(ref));
        v.setType(restTypeFromRef(ref));
        return v;
    }

    private static String refName(ObjectReferenceType ref) {
        if (ref.getTargetName() != null && ref.getTargetName().getOrig() != null) {
            return ref.getTargetName().getOrig();
        }
        return ref.getDescription();
    }

    private static String restTypeFromRef(ObjectReferenceType ref) {
        if (ref.getType() == null) {
            return null;
        }
        try {
            Class<? extends ObjectType> clazz = ObjectTypes.getObjectTypeClass(ref.getType());
            return ObjectTypes.getRestTypeFromClass(clazz);
        } catch (Exception e) {
            return ref.getType().getLocalPart();
        }
    }

    private static MidpointMcpAuditTaskView taskView(AuditEventRecordType r) {
        if (StringUtils.isBlank(r.getTaskOID()) && StringUtils.isBlank(r.getTaskIdentifier())) {
            return null;
        }
        MidpointMcpAuditTaskView t = new MidpointMcpAuditTaskView();
        t.setOid(r.getTaskOID());
        t.setName(r.getTaskIdentifier());
        return t;
    }

    private static String formatTs(XMLGregorianCalendar cal) {
        if (cal == null) {
            return null;
        }
        try {
            Instant i = cal.toGregorianCalendar().toInstant();
            return DateTimeFormatter.ISO_INSTANT.format(i);
        } catch (Exception e) {
            return cal.toXMLFormat();
        }
    }

    private static String buildSummary(AuditEventRecordType r) {
        String who = refNameOrUnknown(r.getInitiatorRef());
        String target = refNameOrUnknown(r.getTargetRef());
        String verb = eventVerb(r.getEventType());
        String stage = r.getEventStage() != null ? r.getEventStage().name().toLowerCase(Locale.ROOT) : "unknown stage";
        String outcome = r.getOutcome() != null ? r.getOutcome().name().toLowerCase(Locale.ROOT) : "unknown outcome";
        return String.format(Locale.ROOT, "%s %s %s during %s (%s)", who, verb, target, stage, outcome);
    }

    private static String refNameOrUnknown(ObjectReferenceType ref) {
        if (ref == null) {
            return "unknown actor";
        }
        String n = refName(ref);
        return StringUtils.isNotBlank(n) ? n : (StringUtils.isNotBlank(ref.getOid()) ? ref.getOid() : "unknown");
    }

    private static String eventVerb(AuditEventTypeType type) {
        if (type == null) {
            return "performed an operation on";
        }
        return switch (type) {
            case ADD_OBJECT -> "added";
            case MODIFY_OBJECT -> "modified";
            case DELETE_OBJECT -> "deleted";
            case GET_OBJECT -> "read";
            default -> "acted on";
        };
    }

    private static String buildExplanation(AuditEventRecordType r, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("This audit record shows ");
        if (r.getEventStage() == AuditEventStageType.EXECUTION) {
            sb.append("an execution-stage ");
        } else if (r.getEventStage() == AuditEventStageType.REQUEST) {
            sb.append("a request-stage ");
        } else if (r.getEventStage() != null) {
            sb.append(r.getEventStage().name().toLowerCase(Locale.ROOT)).append("-stage ");
        } else {
            sb.append("an ");
        }
        if (r.getEventType() != null) {
            sb.append(r.getEventType().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        } else {
            sb.append("event");
        }
        sb.append(" involving target ").append(refNameOrUnknown(r.getTargetRef()));
        sb.append(" initiated by ").append(refNameOrUnknown(r.getInitiatorRef()));
        if (StringUtils.isNotBlank(r.getChannel())) {
            sb.append(" over channel ").append(r.getChannel());
        }
        sb.append(". Outcome: ")
                .append(r.getOutcome() != null ? r.getOutcome().name() : "unknown")
                .append(". ");
        sb.append(summary);
        return sb.toString();
    }

    private static Map<String, Object> deltaPayload(AuditEventRecordType r) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<ObjectDeltaOperationType> deltas = r.getDelta();
        if (deltas == null || deltas.isEmpty()) {
            map.put("operations", List.of());
            map.put("truncated", false);
            return map;
        }
        boolean truncated = deltas.size() > MAX_DELTA_OPS;
        List<Map<String, Object>> ops = new ArrayList<>();
        int n = Math.min(deltas.size(), MAX_DELTA_OPS);
        for (int i = 0; i < n; i++) {
            ObjectDeltaOperationType d = deltas.get(i);
            Map<String, Object> one = new LinkedHashMap<>();
            if (d.getObjectDelta() != null && d.getObjectDelta().getObjectType() != null) {
                one.put("objectType", d.getObjectDelta().getObjectType().getLocalPart());
            }
            if (d.getObjectDelta() != null && StringUtils.isNotBlank(d.getObjectDelta().getOid())) {
                one.put("oid", d.getObjectDelta().getOid());
            }
            if (d.getObjectDelta() != null && d.getObjectDelta().getChangeType() != null) {
                one.put("changeType", String.valueOf(d.getObjectDelta().getChangeType()));
            }
            if (StringUtils.isNotBlank(d.getResourceOid())) {
                one.put("resourceOid", d.getResourceOid());
            }
            if (d.getObjectDelta() != null) {
                MidpointMcpAuditDeltaDetailBuilder.AttributeChangesResult detail =
                        MidpointMcpAuditDeltaDetailBuilder.buildAttributeChanges(d.getObjectDelta());
                one.put("attributeChanges", detail.rows());
                one.put("attributeChangesTruncated", detail.truncated());
            }
            ops.add(one);
        }
        map.put("operations", ops);
        map.put("truncated", truncated);
        return map;
    }

    private static Map<String, Object> resultPayload(AuditEventRecordType r) {
        Map<String, Object> map = new LinkedHashMap<>();
        String res = r.getResult();
        if (res == null) {
            map.put("text", null);
            map.put("truncated", false);
            return map;
        }
        boolean truncated = res.length() > MAX_RESULT_CHARS;
        map.put("text", truncated ? res.substring(0, MAX_RESULT_CHARS) : res);
        map.put("truncated", truncated);
        return map;
    }
}
