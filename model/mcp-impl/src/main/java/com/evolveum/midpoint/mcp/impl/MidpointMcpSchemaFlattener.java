/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.mcp.api.MidpointMcpSchemaAttribute;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Flattens {@link PrismObjectDefinition} into dot-path attributes (cycle-safe on complex types).
 */
final class MidpointMcpSchemaFlattener {

    /** REST collections exposed by MCP tools; used to label reference targets. */
    private static final Set<String> MCP_REST_TYPES = Set.of(
            "users",
            "roles",
            "orgs",
            "archetypes",
            "connectors",
            "resources",
            "services",
            "shadows",
            "tasks",
            "nodes",
            "cases");

    /**
     * Skip entire subtrees when this item name is a container (or any definition). Omits engine/policy/diagnostic
     * branches and SCIM-style structured address trees from the default schema.
     */
    private static final Set<String> SUBTREE_BLOCKED_LOCAL_NAMES = Set.of(
            "operationExecution",
            "fetchResult",
            "metadata",
            "lensContext",
            "trigger",
            "policyException",
            "policyStatement",
            "adminGuiConfiguration",
            "physicalAddress",
            "modelContext");

    /** Leaf or reference paths to omit (exact match). */
    private static final Set<String> EMIT_BLOCKED_EXACT_PATHS = Set.of(
            "policySituation",
            "triggeredPolicyRule",
            "diagnosticInformation",
            "effectiveMarkRef",
            "iteration",
            "iterationToken",
            "delegatedRef",
            "roleInfluenceRef",
            "personaRef",
            "jpegPhoto");

    /**
     * If path equals the string, or starts with it followed by '.', omit. Assignment/inducement noise and SCIM
     * sub-fields (email/phone) use the same rule.
     */
    private static final List<String> EMIT_BLOCKED_PATH_PREFIXES = List.of(
            "assignment.identifier",
            "assignment.documentation",
            "assignment.policySituation",
            "assignment.triggeredPolicyRule",
            "assignment.effectiveMarkRef",
            "assignment.iteration",
            "assignment.iterationToken",
            "inducement.identifier",
            "inducement.documentation",
            "inducement.policySituation",
            "inducement.triggeredPolicyRule",
            "inducement.effectiveMarkRef",
            "inducement.iteration",
            "inducement.iterationToken",
            "email.type",
            "email.primary",
            "phoneNumber.type",
            "approvalContext.deltasToApprove",
            "approvalContext.resultingDeltas",
            "approvalContext.policyRules");

    private MidpointMcpSchemaFlattener() {}

    static List<MidpointMcpSchemaAttribute> flatten(PrismObjectDefinition<?> objectDefinition, int maxPathDepth) {
        LinkedHashMap<String, MidpointMcpSchemaAttribute> byPath = new LinkedHashMap<>();
        Deque<String> complexTypeStack = new ArrayDeque<>();
        for (ItemDefinition<?> def : objectDefinition.getDefinitions()) {
            walk(def, "", complexTypeStack, byPath, maxPathDepth);
        }
        return new ArrayList<>(byPath.values());
    }

    private static void walk(
            ItemDefinition<?> def,
            String pathPrefix,
            Deque<String> complexTypeStack,
            Map<String, MidpointMcpSchemaAttribute> byPath,
            int maxPathDepth) {

        String local = def.getItemName().getLocalPart();
        if (SUBTREE_BLOCKED_LOCAL_NAMES.contains(local)) {
            return;
        }
        String path = pathPrefix.isEmpty() ? local : pathPrefix + "." + local;

        if (def instanceof PrismPropertyDefinition<?> prop) {
            if (pathDepth(path) <= maxPathDepth && shouldEmitPath(path)) {
                putOnce(
                        byPath,
                        path,
                        simplifiedPropertyType(prop.getTypeName()),
                        null,
                        enumValuesForProperty(prop));
            }
        } else if (def instanceof PrismReferenceDefinition ref) {
            if (pathDepth(path) <= maxPathDepth && shouldEmitPath(path)) {
                MidpointMcpSchemaAttribute attr = new MidpointMcpSchemaAttribute();
                attr.setPath(path);
                attr.setType("reference");
                attr.setTarget(referenceTarget(ref.getTargetTypeName()));
                byPath.putIfAbsent(path, attr);
            }
        } else if (def instanceof PrismContainerDefinition<?> cont) {
            ComplexTypeDefinition ctd = cont.getComplexTypeDefinition();
            if (ctd == null) {
                return;
            }
            String typeKey = qNameToUri(ctd.getTypeName());
            if (typeKey != null && complexTypeStack.contains(typeKey)) {
                return;
            }
            if (typeKey != null) {
                complexTypeStack.push(typeKey);
            }
            try {
                for (ItemDefinition<?> sub : ctd.getDefinitions()) {
                    walk(sub, path, complexTypeStack, byPath, maxPathDepth);
                }
            } finally {
                if (typeKey != null) {
                    complexTypeStack.pop();
                }
            }
        }
    }

    private static void putOnce(
            Map<String, MidpointMcpSchemaAttribute> byPath,
            String path,
            String type,
            String target,
            List<String> enumValues) {
        byPath.putIfAbsent(path, attribute(path, type, target, enumValues));
    }

    private static MidpointMcpSchemaAttribute attribute(
            String path, String type, String target, List<String> enumValues) {
        MidpointMcpSchemaAttribute a = new MidpointMcpSchemaAttribute();
        a.setPath(path);
        a.setType(type);
        a.setTarget(target);
        if (enumValues != null && !enumValues.isEmpty()) {
            a.setEnum(enumValues);
        }
        return a;
    }

    /**
     * Prism enumeration / value-policy allowed values, when present on the property definition.
     */
    private static List<String> enumValuesForProperty(PrismPropertyDefinition<?> prop) {
        Collection<? extends DisplayableValue<?>> allowed = prop.getAllowedValues();
        if (allowed == null || allowed.isEmpty()) {
            return null;
        }
        List<String> out = new ArrayList<>(allowed.size());
        for (DisplayableValue<?> dv : allowed) {
            if (dv == null || dv.getValue() == null) {
                continue;
            }
            Object v = dv.getValue();
            out.add(v instanceof Enum<?> e ? e.name() : String.valueOf(v));
        }
        return out.isEmpty() ? null : List.copyOf(out);
    }

    private static int pathDepth(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        int n = 1;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '.') {
                n++;
            }
        }
        return n;
    }

    private static boolean shouldEmitPath(String path) {
        if (EMIT_BLOCKED_EXACT_PATHS.contains(path)) {
            return false;
        }
        for (String prefix : EMIT_BLOCKED_PATH_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + ".")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Maps XSD / Prism value types to a small vocabulary. Non-numeric non-boolean non-date types become
     * {@code string} (including decimal/double).
     */
    private static String simplifiedPropertyType(QName type) {
        if (type == null) {
            return "string";
        }
        if (QNameUtil.match(type, SchemaConstants.T_POLY_STRING_TYPE)) {
            return "string";
        }
        String ns = type.getNamespaceURI();
        String lp = type.getLocalPart();
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(ns)) {
            return switch (lp) {
                case "int", "integer", "long", "short", "byte",
                        "unsignedInt", "unsignedLong", "unsignedShort", "unsignedByte" -> "integer";
                case "boolean" -> "boolean";
                case "dateTime", "date", "time" -> "datetime";
                default -> "string";
            };
        }
        return "string";
    }

    private static String referenceTarget(QName targetTypeName) {
        if (targetTypeName == null) {
            return "objects";
        }
        Class<? extends ObjectType> clazz = ObjectTypes.getObjectTypeClassIfKnown(targetTypeName);
        if (clazz == null) {
            return "objects";
        }
        ObjectTypes ot = ObjectTypes.getObjectTypeIfKnown(clazz);
        if (ot != null) {
            String rest = ot.getRestType();
            if (MCP_REST_TYPES.contains(rest)) {
                return rest;
            }
            if (AbstractRoleType.class.isAssignableFrom(clazz)) {
                return "roles|orgs|services";
            }
        }
        return "objects";
    }

    private static String qNameToUri(QName q) {
        return q != null ? QNameUtil.qNameToUri(q) : null;
    }
}
