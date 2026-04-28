/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.mcp.api.MidpointMcpSchemaAttribute;

/**
 * MCP-oriented synthetic schema paths for {@code cases} (in addition to flattened {@link CaseType}).
 */
final class MidpointMcpCaseSchema {

    private MidpointMcpCaseSchema() {}

    /**
     * Paths merged into {@code midpoint_describe_object_type_schema} output for {@code cases}.
     */
    static List<MidpointMcpSchemaAttribute> syntheticDescribeAttributes() {
        List<MidpointMcpSchemaAttribute> out = new ArrayList<>();
        out.add(stringAttr("request.type"));
        out.add(stringAttr("request.summary"));
        out.add(stringAttr("currentStep.name"));
        out.add(stringAttr("workItems.state"));
        out.add(stringAttr("workItems.assignee.name"));
        out.add(stringAttr("workItems.candidates.name"));
        out.add(stringAttr("decisionHistory.actor.name"));
        out.add(stringAttr("decisionHistory.decision"));
        out.add(stringAttr("decisionHistory.timestamp"));
        out.add(stringAttr("created"));
        out.add(stringAttr("closed"));
        return out;
    }

    private static MidpointMcpSchemaAttribute stringAttr(String path) {
        MidpointMcpSchemaAttribute a = new MidpointMcpSchemaAttribute();
        a.setPath(path);
        a.setType("string");
        return a;
    }
}
