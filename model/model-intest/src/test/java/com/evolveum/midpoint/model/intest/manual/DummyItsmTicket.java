/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class DummyItsmTicket implements DebugDumpable {

    private String identifier;
    private DummyItsmTicketStatus status;
    private String body;

    public DummyItsmTicket(String identifier) {
        this.identifier = identifier;
        this.status = DummyItsmTicketStatus.OPEN;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public DummyItsmTicketStatus getStatus() {
        return status;
    }

    public void setStatus(DummyItsmTicketStatus status) {
        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(DummyItsmTicket.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "identifier", identifier, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "status", status, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "body", body, indent + 1);
        return sb.toString();
    }

}
