/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One search hit: same {@link #values} shape as {@link MidpointMcpObjectView} for the same {@code returnAttributes}.
 */
public class MidpointMcpSearchItem {

    private Map<String, Object> values = new LinkedHashMap<>();

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values != null ? values : new LinkedHashMap<>();
    }
}
