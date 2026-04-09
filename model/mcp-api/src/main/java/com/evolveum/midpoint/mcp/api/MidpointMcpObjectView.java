/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explain-tool payload: flat dot-path keys (aligned with {@code midpoint_describe_object_type_schema}) to JSON values.
 */
public class MidpointMcpObjectView {

    /** For shadows: {@code repository} or {@code resource}. */
    private String source;

    /** For shadows: whether live resource data was fetched. */
    private Boolean fetched;

    private Map<String, Object> values = new LinkedHashMap<>();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getFetched() {
        return fetched;
    }

    public void setFetched(Boolean fetched) {
        this.fetched = fetched;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values != null ? values : new LinkedHashMap<>();
    }
}
