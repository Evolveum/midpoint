/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgUnitSearchDto;

/**
 * @author lazyman
 */
public class UsersStorage implements PageStorage, DebugDumpable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * DTO used for search in {@link com.evolveum.midpoint.web.page.admin.users.PageUsers}
     */
    private Search usersSearch;

    /**
     * DTO used for search purposes in {@link com.evolveum.midpoint.web.page.admin.users in OrgUnitBrowser}
     */
    private OrgUnitSearchDto orgUnitSearch;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.users in OrgUnitBrowser}
     */
    private ObjectPaging orgUnitPaging;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.users.PageUsers}
     */
    private ObjectPaging usersPaging;

    public OrgUnitSearchDto getOrgUnitSearch() {
        return orgUnitSearch;
    }

    public void setOrgUnitSearch(OrgUnitSearchDto orgUnitSearch) {
        this.orgUnitSearch = orgUnitSearch;
    }

    public ObjectPaging getOrgUnitPaging() {
        return orgUnitPaging;
    }

    public void setOrgUnitPaging(ObjectPaging orgUnitPaging) {
        this.orgUnitPaging = orgUnitPaging;
    }


    @Override
    public ObjectPaging getPaging() {
        return usersPaging;
    }

    @Override
    public void setPaging(ObjectPaging usersPaging) {
        this.usersPaging = usersPaging;
    }

    @Override
    public Search getSearch() {
        return usersSearch;
    }

    @Override
    public void setSearch(Search usersSearch) {
        this.usersSearch = usersSearch;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("UsersStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "usersSearch", usersSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "orgUnitSearch", orgUnitSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "orgUnitPaging", orgUnitPaging, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "usersPaging", usersPaging, indent+1);
        return sb.toString();
    }
}
