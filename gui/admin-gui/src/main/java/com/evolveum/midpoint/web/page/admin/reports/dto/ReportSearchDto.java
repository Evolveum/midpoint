/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		sb.append("DebugSearchDto\n");
		DebugUtil.debugDumpWithLabelLn(sb, "text", text, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "parent", parent, indent+1);
		return sb.toString();
	}
}
