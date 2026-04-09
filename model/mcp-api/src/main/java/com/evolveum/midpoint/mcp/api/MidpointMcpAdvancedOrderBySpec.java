/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * Sort instruction for {@link MidpointMcpAdvancedQuerySpec}; path uses MCP dot notation.
 */
public class MidpointMcpAdvancedOrderBySpec {

    private String path;
    /** {@code asc} or {@code desc}; default ascending when omitted. */
    private String direction;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
