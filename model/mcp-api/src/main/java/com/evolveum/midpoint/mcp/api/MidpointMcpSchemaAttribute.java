/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One flattened attribute path for an MCP object-type schema (property or reference).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MidpointMcpSchemaAttribute {

    /** Dot-separated path, e.g. {@code activation.effectiveStatus} or {@code extension.employeeBadge}. */
    private String path;
    /** Simplified type: {@code string}, {@code integer}, {@code boolean}, {@code datetime}, {@code reference}. */
    private String type;
    /** For references: MCP REST collection or union (e.g. {@code roles|orgs|services}) or {@code objects}. */
    private String target;
    /** When the Prism definition exposes allowed values (e.g. XSD enumeration), serialized as JSON {@code "enum"}. */
    private List<String> enumValues;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @JsonProperty("enum")
    public List<String> getEnum() {
        return enumValues;
    }

    public void setEnum(List<String> enumValues) {
        this.enumValues = enumValues;
    }
}
