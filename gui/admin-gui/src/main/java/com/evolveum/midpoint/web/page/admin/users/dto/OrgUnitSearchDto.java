/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 *  @author shood
 * */
public class OrgUnitSearchDto implements Serializable, DebugDumpable {

    public static final String F_SEARCH_TEXT = "text";

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String debugDump() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("OrgUnitSearchDto\n");
        DebugUtil.debugDumpWithLabel(sb, "text", text, indent+1);
        return sb.toString();
    }
}
