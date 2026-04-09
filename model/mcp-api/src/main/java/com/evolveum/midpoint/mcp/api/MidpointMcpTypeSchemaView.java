/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattened schema for a midPoint REST collection type (from the schema registry), including extension items.
 */
public class MidpointMcpTypeSchemaView {

    /** REST collection name, e.g. {@code users}. */
    private String type;
    private List<MidpointMcpSchemaAttribute> attributes = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<MidpointMcpSchemaAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<MidpointMcpSchemaAttribute> attributes) {
        this.attributes = attributes != null ? attributes : new ArrayList<>();
    }
}
