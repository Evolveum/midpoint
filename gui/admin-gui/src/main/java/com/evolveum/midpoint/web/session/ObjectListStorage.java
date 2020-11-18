/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;

/**
 * Created by honchar.
 */
public class ObjectListStorage implements PageStorage, DebugDumpable {

    private static final long serialVersionUID = 1L;

    private Search objectListSearch;
    private ObjectPaging objectListTablePaging;

    @Override
    public ObjectPaging getPaging() {
        return objectListTablePaging;
    }

    @Override
    public void setPaging(ObjectPaging objectListTablePaging) {
        this.objectListTablePaging = objectListTablePaging;
    }

    @Override
    public Search getSearch() {
        return objectListSearch;
    }

    @Override
    public void setSearch(Search objectListSearch) {
        this.objectListSearch = objectListSearch;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ObjectListStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "objectListSearch", objectListSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectListTablePaging", objectListTablePaging, indent+1);
        return sb.toString();
    }
}
