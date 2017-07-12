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

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;

/**
 * @author shood
 */
public class ResourcesStorage implements PageStorage {
	private static final long serialVersionUID = 1L;

	/**
     * DTO used for search purposes in {@link com.evolveum.midpoint.web.page.admin.resources.PageResources}
     */
    private Search resourceSearch;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.resources.PageResources}
     */
    private ObjectPaging resourcePaging;

    /**
     * DTO used for search in {@link com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts}
     */
//    private AccountContentSearchDto accountContentSearch;
    
    private ResourceContentSearchDto resourceContentSearch;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts}
     */
    private ObjectPaging accountContentPaging;

    @Override
    public Search getSearch() {
        return resourceSearch;
    }

    @Override
    public void setSearch(Search resourceSearch) {
        this.resourceSearch = resourceSearch;
    }

    @Override
    public ObjectPaging getPaging() {
        return resourcePaging;
    }

    @Override
    public void setPaging(ObjectPaging resourcePaging) {
        this.resourcePaging = resourcePaging;
    }

    
    public ObjectPaging getAccountContentPaging() {
        return accountContentPaging;
    }

    public void setAccountContentPaging(ObjectPaging accountContentPaging) {
        this.accountContentPaging = accountContentPaging;
    }
    
    public ResourceContentSearchDto getResourceContentSearch() {
		return resourceContentSearch;
	}
    
    public void setResourceContentSearch(ResourceContentSearchDto resourceContentSearch) {
		this.resourceContentSearch = resourceContentSearch;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ResourcesStorage\n");
		DebugUtil.debugDumpWithLabelLn(sb, "resourceSearch", resourceSearch, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "resourcePaging", resourcePaging, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "resourceContentSearch", resourceContentSearch, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "accountContentPaging", accountContentPaging, indent+1);
		return sb.toString();
	}
}
