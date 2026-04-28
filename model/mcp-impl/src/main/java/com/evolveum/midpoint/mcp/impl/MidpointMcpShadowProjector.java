/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.binding.TypeSafeEnum;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.ShadowAssociation;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

/**
 * Normalized shadow {@code attributes} and {@code associations} for MCP responses (stable JSON, not raw connector dumps).
 */
final class MidpointMcpShadowProjector {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MidpointMcpShadowProjector() {}

    static void putShadowPayload(Map<String, Object> values, ShadowType shadow) {
        Map<String, Object> attrs = projectAttributes(shadow);
        if (!attrs.isEmpty()) {
            values.put("attributes", attrs);
        }
        Map<String, List<Map<String, Object>>> assoc = projectAssociations(shadow);
        if (!assoc.isEmpty()) {
            values.put("associations", assoc);
        }
    }

    static String explainShadowSummary(ShadowType shadow, String source, boolean fetched) {
        String name = shadow.getName() != null ? shadow.getName().getOrig() : shadow.getOid();
        StringBuilder sb = new StringBuilder(256);
        sb.append("Shadow '")
                .append(name)
                .append("' — data source: ")
                .append(source)
                .append(fetched ? " (live resource involved)" : " (repository only)")
                .append(". ");
        if (shadow.getResourceRef() != null && StringUtils.isNotBlank(shadow.getResourceRef().getOid())) {
            sb.append("Resource OID ").append(shadow.getResourceRef().getOid()).append(". ");
        }
        if (shadow.getKind() != null) {
            sb.append("Kind ").append(shadow.getKind().value()).append(". ");
        }
        if (StringUtils.isNotBlank(shadow.getIntent())) {
            sb.append("Intent ").append(shadow.getIntent()).append(". ");
        }
        if (shadow.getObjectClass() != null) {
            sb.append("Object class ").append(shadow.getObjectClass().toString()).append(". ");
        }
        if (shadow.getSynchronizationSituation() != null) {
            sb.append("Sync situation ")
                    .append(shadow.getSynchronizationSituation().value())
                    .append(". ");
        }
        Map<String, List<Map<String, Object>>> assoc = projectAssociations(shadow);
        if (!assoc.isEmpty()) {
            sb.append("Associations: ");
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> e : assoc.entrySet()) {
                parts.add(e.getKey() + "(" + e.getValue().size() + ")");
            }
            sb.append(String.join(", ", parts)).append(". ");
        }
        return sb.toString().trim();
    }

    private static Map<String, Object> projectAttributes(ShadowType shadow) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (var attr : ShadowUtil.getAttributes(shadow.asPrismObject())) {
            if (!(attr instanceof ShadowSimpleAttribute<?> simple)) {
                continue;
            }
            String local = simple.getElementName().getLocalPart();
            if (isSensitiveAttributeName(local)) {
                continue;
            }
            Collection<?> real = simple.getRealValues();
            if (real == null || real.isEmpty()) {
                continue;
            }
            Object jsonVal;
            if (real.size() == 1) {
                jsonVal = simplify(real.iterator().next());
            } else {
                List<Object> list = new ArrayList<>(real.size());
                for (Object v : real) {
                    list.add(simplify(v));
                }
                jsonVal = list;
            }
            out.putIfAbsent(local, jsonVal);
        }
        return out;
    }

    private static boolean isSensitiveAttributeName(String local) {
        if (local == null) {
            return true;
        }
        String l = local.toLowerCase();
        return l.contains("password") || l.contains("secret") || "userpassword".equals(l);
    }

    private static Object simplify(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof PolyStringType p) {
            return p.getOrig();
        }
        if (v instanceof PolyString ps) {
            return ps.getOrig();
        }
        if (v instanceof TypeSafeEnum ts) {
            return ts.value();
        }
        if (v instanceof Enum<?> e) {
            return e.name();
        }
        if (v instanceof XMLGregorianCalendar cal) {
            return cal.toXMLFormat();
        }
        if (v instanceof QName q) {
            return q.toString();
        }
        if (v instanceof byte[]) {
            return null;
        }
        if (v instanceof Number || v instanceof Boolean || v instanceof String) {
            return v;
        }
        try {
            return OBJECT_MAPPER.convertValue(v, Object.class);
        } catch (IllegalArgumentException e) {
            return String.valueOf(v);
        }
    }

    private static Map<String, List<Map<String, Object>>> projectAssociations(ShadowType shadow) {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        for (ShadowAssociation association : ShadowUtil.getAssociations(shadow)) {
            QName name = association.getElementName();
            if (name == null) {
                continue;
            }
            String key = name.getLocalPart();
            List<Map<String, Object>> entries = new ArrayList<>();
            for (ShadowAssociationValue val : association.getAssociationValues()) {
                Map<String, Object> one = new LinkedHashMap<>();
                ObjectReferenceType ref = val.getSingleObjectRefRelaxed();
                if (ref != null) {
                    if (StringUtils.isNotBlank(ref.getOid())) {
                        one.put("identifier", ref.getOid());
                    }
                    if (ref.getTargetName() != null && StringUtils.isNotBlank(ref.getTargetName().getOrig())) {
                        one.put("displayName", ref.getTargetName().getOrig());
                    }
                }
                for (ShadowSimpleAttribute<?> a : val.getAttributes()) {
                    String ln = a.getElementName().getLocalPart();
                    if ("name".equalsIgnoreCase(ln) || "uid".equalsIgnoreCase(ln) || "dn".equalsIgnoreCase(ln)) {
                        Collection<?> real = a.getRealValues();
                        if (real != null && !real.isEmpty()) {
                            String id = String.valueOf(simplify(real.iterator().next()));
                            one.putIfAbsent("identifier", id);
                            one.putIfAbsent("displayName", id);
                        }
                    }
                }
                if (!one.isEmpty()) {
                    entries.add(one);
                }
            }
            if (!entries.isEmpty()) {
                out.put(key, entries);
            }
        }
        return out;
    }

    static ShadowKindType parseShadowKindEnum(String raw) {
        if (StringUtils.isBlank(raw)) {
            throw new IllegalArgumentException("shadowKind is blank");
        }
        try {
            return ShadowKindType.fromValue(raw.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            try {
                return ShadowKindType.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException("Invalid shadowKind: use e.g. account, entitlement, generic");
            }
        }
    }

    /**
     * Accepts JAXB {@code {namespaceURI}localPart} form or {@code namespaceURI#localPart}.
     */
    static QName parseObjectClass(String raw) {
        if (StringUtils.isBlank(raw)) {
            throw new IllegalArgumentException("objectClass is blank");
        }
        String s = raw.trim();
        if (s.startsWith("{")) {
            return QName.valueOf(s);
        }
        int hash = s.lastIndexOf('#');
        if (hash > 0 && hash < s.length() - 1) {
            return new QName(s.substring(0, hash), s.substring(hash + 1));
        }
        throw new IllegalArgumentException(
                "objectClass must be in {namespaceURI}localPart or namespaceURI#localPart form");
    }
}
