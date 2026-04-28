/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedFilterSpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedQuerySpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpSchemaAttribute;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates {@link MidpointMcpAdvancedQuerySpec} against flattened MCP schema paths and builds an MQL filter string.
 */
final class MidpointMcpAdvancedQueryTranslator {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private MidpointMcpAdvancedQueryTranslator() {}

    static String translateToMqlFilter(MidpointMcpAdvancedQuerySpec spec, Map<String, MidpointMcpSchemaAttribute> schemaByPath) {
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
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (MidpointMcpAdvancedFilterSpec f : filters) {
            if (f == null) {
                throw new IllegalArgumentException("advancedQuery.filters contains a null entry");
            }
            parts.add(translateOneFilter(f, schemaByPath));
        }
        String sep = " and ";
        if ("or".equals(combine)) {
            sep = " or ";
            List<String> wrapped = new ArrayList<>();
            for (String p : parts) {
                wrapped.add("(" + p + ")");
            }
            return String.join(sep, wrapped);
        }
        return String.join(sep, parts);
    }

    static Map<String, MidpointMcpSchemaAttribute> schemaMapForValidation(PrismObjectDefinition<?> objDef) {
        List<MidpointMcpSchemaAttribute> flat = MidpointMcpSchemaFlattener.flatten(objDef, Integer.MAX_VALUE);
        Map<String, MidpointMcpSchemaAttribute> map = new LinkedHashMap<>();
        for (MidpointMcpSchemaAttribute a : flat) {
            map.put(a.getPath(), a);
        }
        return map;
    }

    /**
     * Resolves a dot path from the MCP schema to an {@link ItemPath} for sorting.
     */
    static ItemPath dotPathToItemPath(PrismObjectDefinition<?> objDef, String dotPath) {
        if (StringUtils.isBlank(dotPath)) {
            throw new IllegalArgumentException("orderBy.path is required");
        }
        String trimmed = dotPath.trim();
        String[] segments = trimmed.split("\\.");
        if (segments.length == 0) {
            throw new IllegalArgumentException("orderBy.path is empty");
        }
        List<QName> names = new ArrayList<>();
        ComplexTypeDefinition ctd = objDef.getComplexTypeDefinition();
        if (ctd == null) {
            throw new IllegalArgumentException("Cannot resolve path '" + trimmed + "': no complex type on object definition");
        }
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i].trim();
            if (seg.isEmpty()) {
                throw new IllegalArgumentException("Invalid path '" + trimmed + "'");
            }
            ItemDefinition<?> def = findChildByLocalPart(ctd, seg);
            names.add(def.getItemName());
            if (i < segments.length - 1) {
                if (!(def instanceof PrismContainerDefinition<?> cont)) {
                    throw new IllegalArgumentException("Path '" + trimmed + "': '" + seg + "' is not a container");
                }
                ctd = cont.getComplexTypeDefinition();
                if (ctd == null) {
                    throw new IllegalArgumentException("Path '" + trimmed + "': missing complex type under '" + seg + "'");
                }
            }
        }
        return ItemPath.create(names);
    }

    private static ItemDefinition<?> findChildByLocalPart(ComplexTypeDefinition ctd, String local) {
        for (ItemDefinition<?> d : ctd.getDefinitions()) {
            if (local.equals(d.getItemName().getLocalPart())) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown item '" + local + "' for type " + ctd.getTypeName());
    }

    private static String translateOneFilter(MidpointMcpAdvancedFilterSpec f, Map<String, MidpointMcpSchemaAttribute> schemaByPath) {
        String path = f.getPath();
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("filter.path is required");
        }
        path = path.trim();
        MidpointMcpSchemaAttribute attr = schemaByPath.get(path);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown path '" + path + "' for this object type (use midpoint_describe_object_type_schema)");
        }
        String op = f.getOp();
        if (StringUtils.isBlank(op)) {
            throw new IllegalArgumentException("filter.op is required for path '" + path + "'");
        }
        op = op.trim().toLowerCase(Locale.ROOT);
        String type = attr.getType();
        if (type == null) {
            type = "string";
        }
        String mqlPath = dotPathToMqlPath(path);
        return switch (op) {
            case "exists" -> {
                if (f.getValue() != null) {
                    throw new IllegalArgumentException("filter.value must not be set for op 'exists' (path '" + path + "')");
                }
                yield mqlPath + " exists";
            }
            case "eq", "neq", "startswith", "contains", "gt", "gte", "lt", "lte", "in" -> {
                assertOpAllowedForType(path, op, type);
                yield buildValueFilter(mqlPath, op, f.getValue(), type, attr);
            }
            default -> throw new IllegalArgumentException("Unsupported filter.op '" + f.getOp() + "' for path '" + path + "'");
        };
    }

    private static void assertOpAllowedForType(String path, String op, String type) {
        switch (type) {
            case "string" -> {
                if (!List.of("eq", "neq", "startswith", "contains", "gt", "gte", "lt", "lte", "in").contains(op)) {
                    throw new IllegalArgumentException("Op '" + op + "' is not allowed for string path '" + path + "'");
                }
            }
            case "integer", "datetime" -> {
                if (!List.of("eq", "neq", "gt", "gte", "lt", "lte", "in").contains(op)) {
                    throw new IllegalArgumentException("Op '" + op + "' is not allowed for " + type + " path '" + path + "'");
                }
            }
            case "boolean" -> {
                if (!List.of("eq", "neq").contains(op)) {
                    throw new IllegalArgumentException("Op '" + op + "' is not allowed for boolean path '" + path + "'");
                }
            }
            case "reference" -> {
                if (!List.of("eq", "in").contains(op)) {
                    throw new IllegalArgumentException(
                            "Op '" + op + "' is not supported for reference path '" + path + "'; use 'exists', 'eq' (target OID), or 'in' (OID list)");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported schema type '" + type + "' for path '" + path + "'");
        }
    }

    private static String buildValueFilter(
            String mqlPath, String op, Object value, String type, MidpointMcpSchemaAttribute attr) {

        if ("reference".equals(type)) {
            return buildReferenceFilter(mqlPath, op, value);
        }
        if ("in".equals(op)) {
            List<?> list = expectList(value, "in", mqlPath);
            if (list.isEmpty()) {
                throw new IllegalArgumentException("filter.value for 'in' must be a non-empty array (path '" + mqlPath + "')");
            }
            if ("integer".equals(type)) {
                List<String> parts = new ArrayList<>();
                for (Object o : list) {
                    parts.add(formatInteger(o, mqlPath));
                }
                return mqlPath + " = (" + String.join(", ", parts) + ")";
            }
            if ("datetime".equals(type)) {
                List<String> parts = new ArrayList<>();
                for (Object o : list) {
                    parts.add(mqlStringLiteral(coerceStringForEnum(o, attr, mqlPath)));
                }
                return mqlPath + " = (" + String.join(", ", parts) + ")";
            }
            List<String> strings = new ArrayList<>();
            for (Object o : list) {
                strings.add(coerceStringForEnum(o, attr, mqlPath));
            }
            return mqlPath + " = " + parenthesizedStrings(strings);
        }
        if (value == null) {
            throw new IllegalArgumentException("filter.value is required for op '" + op + "' on path '" + mqlPath + "'");
        }
        if ("exists".equals(op)) {
            throw new IllegalStateException();
        }
        validateEnumIfNeeded(value, attr, mqlPath);

        return switch (op) {
            case "startswith" -> {
                String s = coerceStringForEnum(value, attr, mqlPath);
                yield mqlPath + " startsWith " + mqlStringLiteral(s);
            }
            case "contains" -> {
                String s = coerceStringForEnum(value, attr, mqlPath);
                yield mqlPath + " contains " + mqlStringLiteral(s);
            }
            case "eq" -> mqlPath + " = " + formatScalarValue(value, type, attr, mqlPath);
            case "neq" -> mqlPath + " != " + formatScalarValue(value, type, attr, mqlPath);
            case "gt" -> mqlPath + " > " + formatScalarValue(value, type, attr, mqlPath);
            case "gte" -> mqlPath + " >= " + formatScalarValue(value, type, attr, mqlPath);
            case "lt" -> mqlPath + " < " + formatScalarValue(value, type, attr, mqlPath);
            case "lte" -> mqlPath + " <= " + formatScalarValue(value, type, attr, mqlPath);
            default -> throw new IllegalArgumentException("Unexpected op '" + op + "'");
        };
    }

    private static String buildReferenceFilter(String mqlPath, String op, Object value) {
        if ("eq".equals(op)) {
            String oid = expectOidString(value, mqlPath);
            return mqlPath + " matches (oid = " + mqlStringLiteral(oid) + ")";
        }
        List<?> list = expectList(value, "in", mqlPath);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("filter.value for 'in' must be a non-empty OID array (path '" + mqlPath + "')");
        }
        List<String> parts = new ArrayList<>();
        for (Object o : list) {
            String oid = expectOidString(o, mqlPath);
            parts.add(mqlPath + " matches (oid = " + mqlStringLiteral(oid) + ")");
        }
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return "(" + String.join(" or ", parts) + ")";
    }

    private static String expectOidString(Object value, String mqlPath) {
        if (!(value instanceof String s) || StringUtils.isBlank(s)) {
            throw new IllegalArgumentException("Reference filter on '" + mqlPath + "' requires a string OID value");
        }
        s = s.trim();
        if (!UUID_PATTERN.matcher(s).matches()) {
            throw new IllegalArgumentException("Reference OID must be a UUID string for path '" + mqlPath + "'");
        }
        return s;
    }

    private static List<?> expectList(Object value, String op, String mqlPath) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("filter.value for op '" + op + "' on path '" + mqlPath + "' must be a JSON array");
    }

    private static void validateEnumIfNeeded(Object value, MidpointMcpSchemaAttribute attr, String mqlPath) {
        List<String> allowed = attr.getEnum();
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        String s = String.valueOf(value).trim();
        if (!allowed.contains(s)) {
            throw new IllegalArgumentException(
                    "Value '" + s + "' is not allowed for path '" + mqlPath + "'; allowed: " + String.join(", ", allowed));
        }
    }

    private static String coerceStringForEnum(Object value, MidpointMcpSchemaAttribute attr, String mqlPath) {
        if (value == null) {
            throw new IllegalArgumentException("filter.value is required for path '" + mqlPath + "'");
        }
        String s = value instanceof String str ? str : String.valueOf(value);
        validateEnumIfNeeded(s, attr, mqlPath);
        return s;
    }

    private static String formatScalarValue(Object value, String type, MidpointMcpSchemaAttribute attr, String mqlPath) {
        return switch (type) {
            case "string" -> mqlStringLiteral(coerceStringForEnum(value, attr, mqlPath));
            case "integer" -> formatInteger(value, mqlPath);
            case "datetime" -> mqlStringLiteral(coerceStringForEnum(value, attr, mqlPath));
            case "boolean" -> {
                if (value instanceof Boolean b) {
                    yield b ? "true" : "false";
                }
                if ("true".equalsIgnoreCase(String.valueOf(value).trim())) {
                    yield "true";
                }
                if ("false".equalsIgnoreCase(String.valueOf(value).trim())) {
                    yield "false";
                }
                throw new IllegalArgumentException("Boolean value expected for path '" + mqlPath + "'");
            }
            default -> mqlStringLiteral(coerceStringForEnum(value, attr, mqlPath));
        };
    }

    private static String formatInteger(Object value, String mqlPath) {
        if (value instanceof Number n) {
            long v = n.longValue();
            if (v != n.doubleValue()) {
                throw new IllegalArgumentException("Integer value expected for path '" + mqlPath + "'");
            }
            return Long.toString(v);
        }
        if (value instanceof String s) {
            try {
                return Long.toString(Long.parseLong(s.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Integer value expected for path '" + mqlPath + "'");
            }
        }
        throw new IllegalArgumentException("Integer value expected for path '" + mqlPath + "'");
    }

    private static String parenthesizedStrings(List<String> values) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(mqlStringLiteral(values.get(i)));
        }
        sb.append(")");
        return sb.toString();
    }

    static String dotPathToMqlPath(String dotPath) {
        return dotPath.trim().replace('.', '/');
    }

    static String mqlStringLiteral(String raw) {
        if (raw.chars().anyMatch(ch -> ch < 0x20)) {
            throw new IllegalArgumentException("String values must not contain control characters");
        }
        String escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}
