/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAttributeInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevBasicObjectClassInfoType;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Best-effort parsing of the development-mode schema model exported by low-code connector
 * frameworks through the shared {@code conndev_ObjectClass} dev object class (the unwrapped
 * shadow content), into midPoint's connector development types. The content is owned by the
 * connector side (framework-specific fields), so parsing is lenient: unknown fields are
 * ignored and missing ones fall back to sensible defaults. Returns {@code null} (object
 * class) or an empty list (attributes) when nothing recognizable is found.
 */
public final class DevSchemaObjectClassParser {

    private DevSchemaObjectClassParser() {
    }

    public static ConnDevBasicObjectClassInfoType toBasicObjectClassInfo(String name, JsonNode content) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // embedded/abstract are always set explicitly - the generated Boolean getters
        // unbox null, so a missing property would turn into a NullPointerException downstream.
        var objectClass = new ConnDevBasicObjectClassInfoType()
                .name(name)
                .relevant(true)
                .embedded(content.path("embedded").asBoolean(false))
                ._abstract(content.path("abstract").asBoolean(false));
        var description = text(content.path("description"));
        if (description != null) {
            objectClass.description(description);
        }
        var superclass = text(content.path("superclass"));
        if (superclass != null) {
            objectClass.superclass(superclass);
        }
        return objectClass;
    }

    public static List<ConnDevAttributeInfoType> toAttributes(JsonNode content) {
        var attributes = content.path("attributes");
        if (!attributes.isArray()) {
            return List.of();
        }
        var result = new ArrayList<ConnDevAttributeInfoType>();
        for (var attribute : attributes) {
            var name = text(attribute.path("name"));
            if (name == null) {
                continue;
            }
            var autoIncrement = attribute.path("autoIncrement").asBoolean(false);
            var info = new ConnDevAttributeInfoType()
                    .name(name)
                    .mandatory(
                            attribute.path("required").asBoolean(false)
                                    || attribute.path("primaryKey").asBoolean(false)
                                    || attribute.path("notNull").asBoolean(false))
                    .updatable(attribute.path("updateable").asBoolean(!autoIncrement))
                    .creatable(attribute.path("creatable").asBoolean(!autoIncrement))
                    .readable(attribute.path("readable").asBoolean(true))
                    .multivalue(
                            attribute.path("multiValued").asBoolean(attribute.path("multivalue").asBoolean(false)))
                    .returnedByDefault(attribute.path("returnedByDefault").asBoolean(true));
            var type = text(attribute.path("type"));
            if (type == null) {
                type = text(attribute.path("jsonType"));
            }
            if (type != null) {
                info.type(type);
            }
            var description = text(attribute.path("description"));
            if (description != null) {
                info.description(description);
            }
            result.add(info);
        }
        return result;
    }

    /**
     * Text value of a node; prism-serialized strings (PolyString) are arrays, so the first
     * textual array element is taken. Returns {@code null} when absent or empty.
     */
    private static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        var target = node;
        if (node.isArray() && !node.isEmpty()) {
            target = node.get(0);
        }
        if (!target.isTextual()) {
            return null;
        }
        var value = target.asText();
        return value.isBlank() ? null : value;
    }
}
