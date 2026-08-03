/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Faithfully unwraps a prism-serialized {@code conndev_*} shadow attributes container into plain JSON
 * for the connector-generation service. The transformation is purely structural — it strips prism
 * serialization artifacts and forwards everything else untouched:
 * <ul>
 *     <li>{@code @...} metadata entries (e.g. {@code @ns}) are dropped,</li>
 *     <li>namespace prefixes of keys are stripped ({@code icfs:name} → {@code name}),</li>
 *     <li>embedded prism objects ({@code { "t:type": "c:ShadowType", "object": { ..., "attributes":
 *         {...} } }}) are replaced by their {@code object.attributes} content, recursively.</li>
 * </ul>
 * There is deliberately <b>no field allow-list</b>: the content of {@code conndev_ObjectClass} is owned
 * by the connector side (single source of the schema mapping), so midPoint must not interpret it —
 * SQL/SCIM specifics and any future fields ({@code locator}, native types, ...) flow through unchanged.
 */
public class ConnDevShadowUnwrapper {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    public JsonNode unwrap(JsonNode node) {
        if (node == null || node.isNull()) {
            return NODES.nullNode();
        }
        if (node.isArray()) {
            var out = NODES.arrayNode();
            node.forEach(value -> out.add(unwrap(value)));
            return out;
        }
        if (node.isObject()) {
            var embedded = embeddedAttributes(node);
            if (embedded != null) {
                return unwrap(embedded);
            }
            var out = NODES.objectNode();
            node.fields().forEachRemaining(entry -> {
                if (entry.getKey().startsWith("@")) {
                    return;
                }
                out.set(localName(entry.getKey()), unwrap(entry.getValue()));
            });
            return out;
        }
        return node;
    }

    /**
     * Detects the prism wrapper of an embedded object value and returns the connector data inside
     * ({@code object.attributes}); the wrapper itself (type hint, shadow scaffolding) is serialization
     * noise. Returns {@code null} when the node is a regular object.
     */
    private static JsonNode embeddedAttributes(JsonNode node) {
        var object = node.get("object");
        if (object != null && object.isObject() && object.has("attributes")) {
            return object.get("attributes");
        }
        return null;
    }

    private static String localName(String field) {
        int colon = field.indexOf(':');
        return colon >= 0 ? field.substring(colon + 1) : field;
    }
}
