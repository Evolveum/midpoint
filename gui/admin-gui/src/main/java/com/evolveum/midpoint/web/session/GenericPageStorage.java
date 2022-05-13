/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Created by Viliam Repan (lazyman).
 */
public class GenericPageStorage implements PageStorage {

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
        sb.append("GenericPageStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "search", search, indent + 1);
        DebugUtil.dumpObjectSizeEstimate(sb, "searchSize", search, indent + 1);
        return sb.toString();
    }
}
