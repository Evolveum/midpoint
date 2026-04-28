/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/** Effective timestamp bounds applied to the audit query (ISO-8601, UTC). */
public class MidpointMcpAuditEffectiveWindow {

    private String from;
    private String to;

    public MidpointMcpAuditEffectiveWindow() {
    }

    public MidpointMcpAuditEffectiveWindow(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
