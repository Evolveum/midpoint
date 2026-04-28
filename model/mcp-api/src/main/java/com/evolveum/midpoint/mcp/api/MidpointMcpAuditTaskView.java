/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * Task context. {@code name} is the persisted task identifier string ({@code taskIdentifier}), not a resolved
 * display name.
 */
public class MidpointMcpAuditTaskView {

    private String oid;
    private String name;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
