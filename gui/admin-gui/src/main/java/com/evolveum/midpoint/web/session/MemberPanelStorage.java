/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;


import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;

/**
 * Created by honchar
 */
public class MemberPanelStorage implements PageStorage{
    private static final long serialVersionUID = 1L;

    private ObjectPaging orgMemberPanelPaging;

    private Search search;

    @Override
    public void setSearch(Search search) {
        this.search = search;
    }

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public ObjectPaging getPaging() {
        return orgMemberPanelPaging;
    }

    @Override
    public void setPaging(ObjectPaging orgMemberPanelPaging) {
        this.orgMemberPanelPaging = orgMemberPanelPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("MemberPanelStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "search", search, indent+1);
        DebugUtil.dumpObjectSizeEstimate(sb, "searchSize", search, indent + 1);
        return sb.toString();
    }
}
