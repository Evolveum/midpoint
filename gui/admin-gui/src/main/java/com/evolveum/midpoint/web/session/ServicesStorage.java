/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;

public class ServicesStorage implements PageStorage{
    private static final long serialVersionUID = 1L;

    private Search servicesSearch;
    private ObjectPaging servicesPaging;

    @Override
    public Search getSearch() {
        return servicesSearch;
    }

    @Override
    public void setSearch(Search search) {
        this.servicesSearch = search;
    }

    @Override
    public void setPaging(ObjectPaging paging) {
        this.servicesPaging = paging;

    }

    @Override
    public ObjectPaging getPaging() {
        return servicesPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ServicesStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "servicesSearch", servicesSearch, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "servicesPaging", servicesPaging, indent+1);
        return sb.toString();
    }

}
