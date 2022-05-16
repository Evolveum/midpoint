/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.session.PageStorage;

/**
 * @author skublik
 */
public class WorkItemsStorage implements PageStorage{

    private static final long serialVersionUID = 1L;

    private ObjectPaging workItemsPaging;

    private Search search;

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public void setSearch(Search search) {
        this.search = search;
    }

    @Override
    public ObjectPaging getPaging() {
        return workItemsPaging;
    }

    @Override
    public void setPaging(ObjectPaging workItemsPaging) {
        this.workItemsPaging = workItemsPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("WorkItemsStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "workItemsPaging", workItemsPaging, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "search", search, indent+1);
        return sb.toString();
    }

}
