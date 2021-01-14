/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 *  @author shood
 * */
public class ReportSearchDto implements Serializable, DebugDumpable {
    private static final long serialVersionUID = 1L;

    public static final String F_SEARCH_TEXT = "text";
    public static final String F_PARENT = "parent";

    private String text;
    private Boolean parent = false;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean isParent() {
        return parent;
    }

    public void setParent(Boolean parent) {
        this.parent = parent;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ReportSearchDto\n");
        DebugUtil.debugDumpWithLabelLn(sb, "text", text, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "parent", parent, indent+1);
        return sb.toString();
    }
}
