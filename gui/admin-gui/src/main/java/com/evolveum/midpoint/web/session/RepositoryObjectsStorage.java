/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Storage for the repository objects table and its search panel
 * on {@link com.evolveum.midpoint.web.page.admin.configuration.PageDebugList}.
 */
public class RepositoryObjectsStorage implements PageStorage {

    @Serial private static final long serialVersionUID = 1L;

    private Search search;

    private ObjectPaging paging;

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
        return paging;
    }

    @Override
    public void setPaging(ObjectPaging paging) {
        this.paging = paging;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("RepositoryObjectsStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "search", search, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "paging", paging, indent + 1);
        return sb.toString();
    }
}
