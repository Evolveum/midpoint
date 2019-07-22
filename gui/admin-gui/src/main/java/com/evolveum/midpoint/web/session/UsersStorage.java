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

package com.evolveum.midpoint.web.session;

import java.util.Set;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgUnitSearchDto;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

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
