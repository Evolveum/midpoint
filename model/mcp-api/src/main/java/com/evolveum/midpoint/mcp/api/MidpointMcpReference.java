/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

public class MidpointMcpReference {

    private String oid;
    private String type;
    private String name;
    private String relation;

    public MidpointMcpReference() {
    }

    public MidpointMcpReference(String oid, String type, String name, String relation) {
        this.oid = oid;
        this.type = type;
        this.name = name;
        this.relation = relation;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }
}
