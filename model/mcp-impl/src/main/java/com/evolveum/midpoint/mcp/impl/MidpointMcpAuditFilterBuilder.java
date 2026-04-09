/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedFilterSpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedQuerySpec;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordCustomColumnPropertyType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds {@link ObjectFilter} for {@link AuditEventRecordType} from MCP advanced query (audit path vocabulary).
 */
final class MidpointMcpAuditFilterBuilder {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private MidpointMcpAuditFilterBuilder() {}

    static ObjectFilter buildAdvancedFilter(PrismContext prismContext, MidpointMcpAdvancedQuerySpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("advancedQuery is required");
        }
        String combine = spec.getCombine();
        if (StringUtils.isBlank(combine)) {
            combine = "and";
        } else {
            combine = combine.trim().toLowerCase(Locale.ROOT);
            if (!combine.equals("and") && !combine.equals("or")) {
                throw new IllegalArgumentException("advancedQuery.combine must be 'and' or 'or'");
            }
        }
        List<MidpointMcpAdvancedFilterSpec> filters = spec.getFilters();
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        List<ObjectFilter> parts = new ArrayList<>();
        for (MidpointMcpAdvancedFilterSpec f : filters) {
            if (f == null) {
                throw new IllegalArgumentException("advancedQuery.filters contains a null entry");
            }
            parts.add(buildOneFilter(prismContext, f));
        }
        if ("or".equals(combine)) {
            return prismContext.queryFactory().createOr(parts);
        }
        return prismContext.queryFactory().createAnd(parts);
    }

    private static ObjectFilter buildOneFilter(PrismContext pc, MidpointMcpAdvancedFilterSpec f) {
        String path = f.getPath();
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("filter.path is required");
        }
        path = path.trim();
        if (!MidpointMcpAuditSchema.isKnownPath(path)) {
            throw new IllegalArgumentException("Unknown audit filter path '" + path + "'");
        }
        String op = MidpointMcpAuditSchema.normalizeOp(f.getOp());
        String customKey = MidpointMcpAuditSchema.customColumnKey(path);
        if (customKey != null) {
            return buildCustomColumnFilter(pc, customKey, op, f.getValue());
        }
        return switch (path) {
            case "timestamp" -> buildTimestamp(pc, op, f.getValue());
            case "eventType" -> buildEventType(pc, op, f.getValue());
            case "eventStage" -> buildEventStage(pc, op, f.getValue());
            case "outcome" -> buildOutcome(pc, op, f.getValue());
            case "initiator.oid" -> buildInitiatorOid(pc, op, f.getValue());
            case "initiator.name" -> buildInitiatorName(pc, op, f.getValue());
            case "target.oid" -> buildTargetOid(pc, op, f.getValue());
            case "target.name" -> buildTargetName(pc, op, f.getValue());
            case "target.type" -> buildTargetType(pc, op, f.getValue());
            case "channel" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_CHANNEL);
            case "task.oid" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_TASK_OID);
            case "task.name" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_TASK_IDENTIFIER);
            case "node" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_NODE_IDENTIFIER);
            case "message" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_MESSAGE);
            case "sessionIdentifier" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_SESSION_IDENTIFIER);
            case "requestIdentifier" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_REQUEST_IDENTIFIER);
            case "delta" -> buildDeltaExists(pc, op, f.getValue());
            case "result" -> buildStringItem(pc, op, f.getValue(), AuditEventRecordType.F_RESULT);
            default -> throw new IllegalArgumentException("Unsupported audit path '" + path + "'");
        };
    }

    private static ObjectFilter buildCustomColumnFilter(PrismContext pc, String key, String op, Object value) {
        assertStringOpsOnly(key, op, List.of("eq", "neq", "contains", "startsWith", "in"));
        var item = AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY;
        return switch (op) {
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .eq(new AuditEventRecordCustomColumnPropertyType().name(key).value(expectString(value, op, key)))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(item)
                    .eq(new AuditEventRecordCustomColumnPropertyType().name(key).value(expectString(value, op, key)))
                    .build()
                    .getFilter();
            case "contains" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .contains(new AuditEventRecordCustomColumnPropertyType().name(key).value(expectString(value, op, key)))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "startswith" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .startsWith(new AuditEventRecordCustomColumnPropertyType().name(key).value(expectString(value, op, key)))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, key);
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(item)
                            .eq(new AuditEventRecordCustomColumnPropertyType().name(key).value(String.valueOf(o)))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalArgumentException("Unsupported op '" + op + "' for custom column '" + key + "'");
        };
    }

    private static ObjectFilter buildTimestamp(PrismContext pc, String op, Object value) {
        assertOps(op, List.of("eq", "neq", "gt", "gte", "lt", "lte", "exists", "in"), "timestamp");
        var item = AuditEventRecordType.F_TIMESTAMP;
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, "timestamp");
                yield pc.queryFor(AuditEventRecordType.class).not().item(item).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .eq(parseDateTime(value, "timestamp"))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(item)
                    .eq(parseDateTime(value, "timestamp"))
                    .build()
                    .getFilter();
            case "gt" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .gt(parseDateTime(value, "timestamp"))
                    .build()
                    .getFilter();
            case "gte" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .ge(parseDateTime(value, "timestamp"))
                    .build()
                    .getFilter();
            case "lt" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .lt(parseDateTime(value, "timestamp"))
                    .build()
                    .getFilter();
            case "lte" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .le(parseDateTime(value, "timestamp"))
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, "timestamp");
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(item)
                            .eq(parseDateTime(o, "timestamp"))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static ObjectFilter buildEventType(PrismContext pc, String op, Object value) {
        assertOps(op, List.of("eq", "neq", "exists", "in"), "eventType");
        var item = AuditEventRecordType.F_EVENT_TYPE;
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, "eventType");
                yield pc.queryFor(AuditEventRecordType.class).not().item(item).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .eq(parseEventType(value))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(item)
                    .eq(parseEventType(value))
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, "eventType");
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(item)
                            .eq(parseEventType(o))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static ObjectFilter buildEventStage(PrismContext pc, String op, Object value) {
        assertOps(op, List.of("eq", "neq", "exists", "in"), "eventStage");
        var item = AuditEventRecordType.F_EVENT_STAGE;
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, "eventStage");
                yield pc.queryFor(AuditEventRecordType.class).not().item(item).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .eq(parseEventStage(value))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(item)
                    .eq(parseEventStage(value))
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, "eventStage");
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(item)
                            .eq(parseEventStage(o))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static ObjectFilter buildOutcome(PrismContext pc, String op, Object value) {
        assertOps(op, List.of("eq", "neq", "exists", "in"), "outcome");
        var item = AuditEventRecordType.F_OUTCOME;
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, "outcome");
                yield pc.queryFor(AuditEventRecordType.class).not().item(item).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .eq(parseOutcome(value))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(item)
                    .eq(parseOutcome(value))
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, "outcome");
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(item)
                            .eq(parseOutcome(o))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static ObjectFilter buildInitiatorOid(PrismContext pc, String op, Object value) {
        return buildRefOid(pc, op, value, AuditEventRecordType.F_INITIATOR_REF, "initiator.oid");
    }

    private static ObjectFilter buildTargetOid(PrismContext pc, String op, Object value) {
        return buildRefOid(pc, op, value, AuditEventRecordType.F_TARGET_REF, "target.oid");
    }

    private static ObjectFilter buildRefOid(PrismContext pc, String op, Object value, ItemName refItem, String pathLabel) {
        assertOps(op, List.of("eq", "neq", "exists", "in"), pathLabel);
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, pathLabel);
                yield pc.queryFor(AuditEventRecordType.class).not().item(refItem).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(refItem)
                    .ref(expectUuid(value, pathLabel))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(refItem)
                    .ref(expectUuid(value, pathLabel))
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, pathLabel);
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(refItem)
                            .ref(expectUuid(o, pathLabel))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static ObjectFilter buildInitiatorName(PrismContext pc, String op, Object value) {
        return buildRefTargetName(pc, op, value, AuditEventRecordType.F_INITIATOR_REF, "initiator.name");
    }

    private static ObjectFilter buildTargetName(PrismContext pc, String op, Object value) {
        return buildRefTargetName(pc, op, value, AuditEventRecordType.F_TARGET_REF, "target.name");
    }

    private static ObjectFilter buildRefTargetName(PrismContext pc, String op, Object value, ItemName refItem, String pathLabel) {
        assertStringOpsOnly(pathLabel, op, List.of("eq", "neq", "contains", "startsWith", "exists", "in"));
        var tn = ObjectReferenceType.F_TARGET_NAME;
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, pathLabel);
                yield pc.queryFor(AuditEventRecordType.class).not().item(refItem, tn).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(refItem, tn)
                    .eq(PolyString.fromOrig(expectString(value, op, pathLabel)))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(refItem, tn)
                    .eq(PolyString.fromOrig(expectString(value, op, pathLabel)))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "contains" -> pc.queryFor(AuditEventRecordType.class)
                    .item(refItem, tn)
                    .contains(PolyString.fromOrig(expectString(value, op, pathLabel)))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "startswith" -> pc.queryFor(AuditEventRecordType.class)
                    .item(refItem, tn)
                    .startsWith(PolyString.fromOrig(expectString(value, op, pathLabel)))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, pathLabel);
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(refItem, tn)
                            .eq(PolyString.fromOrig(String.valueOf(o)))
                            .matchingCaseIgnore()
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static ObjectFilter buildTargetType(PrismContext pc, String op, Object value) {
        assertOps(op, List.of("eq", "neq", "exists", "in"), "target.type");
        var ref = AuditEventRecordType.F_TARGET_REF;
        var typeItem = ObjectReferenceType.F_TYPE;
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, "target.type");
                yield pc.queryFor(AuditEventRecordType.class).not().item(ref, typeItem).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(ref, typeItem)
                    .eq(resolveTargetTypeQName(value))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(ref, typeItem)
                    .eq(resolveTargetTypeQName(value))
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, "target.type");
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(ref, typeItem)
                            .eq(resolveTargetTypeQName(o))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static javax.xml.namespace.QName resolveTargetTypeQName(Object value) {
        String s = expectString(value, "eq", "target.type").trim().toLowerCase(Locale.ROOT);
        try {
            return ObjectTypes.getTypeQNameFromRestType(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown target.type alias '" + value + "'; use MCP REST collection names such as users, roles, orgs");
        }
    }

    private static ObjectFilter buildStringItem(PrismContext pc, String op, Object value, ItemName item) {
        String label = item.getLocalPart();
        assertStringOpsOnly(label, op, List.of("eq", "neq", "contains", "startsWith", "gt", "gte", "lt", "lte", "exists", "in"));
        return switch (op) {
            case "exists" -> {
                requireNoValue(op, value, label);
                yield pc.queryFor(AuditEventRecordType.class).not().item(item).isNull().build().getFilter();
            }
            case "eq" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .eq(expectString(value, op, label))
                    .build()
                    .getFilter();
            case "neq" -> pc.queryFor(AuditEventRecordType.class)
                    .not()
                    .item(item)
                    .eq(expectString(value, op, label))
                    .build()
                    .getFilter();
            case "contains" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .contains(expectString(value, op, label))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "startswith" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .startsWith(expectString(value, op, label))
                    .matchingCaseIgnore()
                    .build()
                    .getFilter();
            case "gt" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .gt(expectString(value, op, label))
                    .build()
                    .getFilter();
            case "gte" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .ge(expectString(value, op, label))
                    .build()
                    .getFilter();
            case "lt" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .lt(expectString(value, op, label))
                    .build()
                    .getFilter();
            case "lte" -> pc.queryFor(AuditEventRecordType.class)
                    .item(item)
                    .le(expectString(value, op, label))
                    .build()
                    .getFilter();
            case "in" -> {
                List<?> list = expectNonEmptyList(value, op, label);
                List<ObjectFilter> ors = new ArrayList<>();
                for (Object o : list) {
                    ors.add(pc.queryFor(AuditEventRecordType.class)
                            .item(item)
                            .eq(String.valueOf(o))
                            .build()
                            .getFilter());
                }
                yield pc.queryFactory().createOr(ors);
            }
            default -> throw new IllegalStateException();
        };
    }

    private static ObjectFilter buildDeltaExists(PrismContext pc, String op, Object value) {
        assertOps(op, List.of("exists"), "delta");
        requireNoValue(op, value, "delta");
        return pc.queryFor(AuditEventRecordType.class)
                .exists(AuditEventRecordType.F_DELTA)
                .build()
                .getFilter();
    }

    private static void assertOps(String op, List<String> allowed, String path) {
        if (!allowed.contains(op)) {
            throw new IllegalArgumentException("Unsupported op '" + op + "' for path '" + path + "'");
        }
    }

    private static void assertStringOpsOnly(String path, String op, List<String> allowed) {
        assertOps(op, allowed, path);
    }

    private static void requireNoValue(String op, Object value, String path) {
        if (value != null) {
            throw new IllegalArgumentException("filter.value must not be set for op '" + op + "' on path '" + path + "'");
        }
    }

    private static String expectString(Object value, String op, String path) {
        if (value == null) {
            throw new IllegalArgumentException("filter.value is required for op '" + op + "' on path '" + path + "'");
        }
        return value instanceof String s ? s : String.valueOf(value);
    }

    private static List<?> expectNonEmptyList(Object value, String op, String path) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("filter.value for op '" + op + "' on path '" + path + "' must be a JSON array");
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("filter.value for op '" + op + "' on path '" + path + "' must be a non-empty array");
        }
        return list;
    }

    private static String expectUuid(Object value, String path) {
        String s = expectString(value, "eq", path).trim();
        if (!UUID_PATTERN.matcher(s).matches()) {
            throw new IllegalArgumentException("Value for path '" + path + "' must be a UUID string");
        }
        return s;
    }

    private static XMLGregorianCalendar parseDateTime(Object value, String path) {
        String s = expectString(value, "eq", path).trim();
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(s);
        } catch (DatatypeConfigurationException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid datetime for path '" + path + "': " + s);
        }
    }

    private static AuditEventTypeType parseEventType(Object value) {
        String raw = expectString(value, "eq", "eventType").trim();
        String norm = raw.replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return AuditEventTypeType.valueOf(norm);
        } catch (IllegalArgumentException ignored) {
            for (AuditEventTypeType t : AuditEventTypeType.values()) {
                if (t.value().equalsIgnoreCase(raw) || t.name().equalsIgnoreCase(raw)) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("Unknown eventType '" + raw + "'");
    }

    private static AuditEventStageType parseEventStage(Object value) {
        String raw = expectString(value, "eq", "eventStage").trim();
        try {
            return AuditEventStageType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            for (AuditEventStageType t : AuditEventStageType.values()) {
                if (t.value().equalsIgnoreCase(raw) || t.name().equalsIgnoreCase(raw)) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("Unknown eventStage '" + raw + "'");
    }

    private static OperationResultStatusType parseOutcome(Object value) {
        String raw = expectString(value, "eq", "outcome").trim();
        String alias = raw.toUpperCase(Locale.ROOT);
        if ("FAILURE".equals(alias)) {
            return OperationResultStatusType.FATAL_ERROR;
        }
        if ("PARTIAL_ERROR".equals(alias)) {
            return OperationResultStatusType.PARTIAL_ERROR;
        }
        try {
            return OperationResultStatusType.valueOf(alias);
        } catch (IllegalArgumentException ignored) {
            for (OperationResultStatusType t : OperationResultStatusType.values()) {
                if (t.value().equalsIgnoreCase(raw) || t.name().equalsIgnoreCase(raw)) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("Unknown outcome '" + raw + "'");
    }

    /** Human-oriented summary of advanced filters for {@link com.evolveum.midpoint.mcp.api.MidpointMcpAuditSearchResult#setTranslatedQuery}. */
    static String describeAdvancedFilter(ObjectFilter filter) {
        if (filter == null) {
            return null;
        }
        return filter.toString();
    }

    static ObjectQuery buildSimpleTextQuery(PrismContext pc, String queryText) {
        if (StringUtils.isBlank(queryText)) {
            return null;
        }
        String q = queryText.trim();
        List<String> tokens = new ArrayList<>();
        for (String t : q.split("\\s+")) {
            if (StringUtils.isNotBlank(t)) {
                tokens.add(t.trim());
            }
        }
        if (tokens.isEmpty()) {
            return null;
        }
        List<ObjectFilter> tokenFilters = new ArrayList<>();
        for (String token : tokens) {
            List<ObjectFilter> ors = new ArrayList<>();
            ors.add(pc.queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_MESSAGE)
                    .contains(token)
                    .matchingCaseIgnore()
                    .build()
                    .getFilter());
            ors.add(pc.queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_TASK_IDENTIFIER)
                    .contains(token)
                    .matchingCaseIgnore()
                    .build()
                    .getFilter());
            if (UUID_PATTERN.matcher(token).matches()) {
                ors.add(pc.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_TARGET_REF)
                        .ref(token)
                        .build()
                        .getFilter());
                ors.add(pc.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_INITIATOR_REF)
                        .ref(token)
                        .build()
                        .getFilter());
            }
            tokenFilters.add(pc.queryFactory().createOr(ors));
        }
        ObjectFilter combined = pc.queryFactory().createAnd(tokenFilters);
        return pc.queryFactory().createQuery(combined);
    }
}
