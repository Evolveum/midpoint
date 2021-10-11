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

/**
 * @author shood
 */
public class RolesStorage implements PageStorage {
    private static final long serialVersionUID = 1L;

    /**
     * DTO used for search in {@link com.evolveum.midpoint.web.page.admin.roles.PageRoles}
     */
    private Search rolesSearch;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.roles.PageRoles}
     */
    private ObjectPaging rolesPaging;

    @Override
    public Search getSearch() {
        return rolesSearch;
    }

    @Override
    public void setSearch(Search rolesSearch) {
        this.rolesSearch = rolesSearch;
    }

    @Override
    public ObjectPaging getPaging() {
        return rolesPaging;
    }

    @Override
    public void setPaging(ObjectPaging rolesPaging) {
        this.rolesPaging = rolesPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("RolesStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "rolesSearch", rolesSearch, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "rolesPaging", rolesPaging, indent+1);
        return sb.toString();
    }
}
