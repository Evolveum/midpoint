/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * One filter in {@link MidpointMcpAdvancedQuerySpec#getFilters()}.
 * {@code value} is JSON-driven: string, number, boolean, or array of strings (for {@code in}).
 */
public class MidpointMcpAdvancedFilterSpec {

    private String path;
    /**
     * One of: {@code eq}, {@code neq}, {@code startsWith}, {@code contains}, {@code gt}, {@code gte},
     * {@code lt}, {@code lte}, {@code exists}, {@code in}.
     */
    private String op;
    /** Omitted for {@code exists}. */
    private Object value;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
