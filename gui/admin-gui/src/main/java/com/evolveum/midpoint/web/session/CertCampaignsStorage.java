/*
 * Copyright (c) 2010-2018 Evolveum
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
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignsSearchDto;

/**
 *  @author shood
 *  @author mederly
 * */
public class CertCampaignsStorage implements PageStorage {
	private static final long serialVersionUID = 1L;

    private CertCampaignsSearchDto campaignsSearch;
    private ObjectPaging campaignsPaging;
    private Search search;

    public CertCampaignsSearchDto getCampaignsSearch() {
        return campaignsSearch;
    }

    public void setCampaignsSearch(CertCampaignsSearchDto campaignsSearch) {
        this.campaignsSearch = campaignsSearch;
    }

    @Override
    public ObjectPaging getPaging() {
        return campaignsPaging;
    }

    @Override
    public void setPaging(ObjectPaging tasksPaging) {
        this.campaignsPaging = tasksPaging;
    }

    @Override
    public Search getSearch() {
		return search;
	}

    @Override
    public void setSearch(Search search) {
    	this.search = search;
    }

    @Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("CertCampaignsStorage\n");
		DebugUtil.debugDumpWithLabelLn(sb, "campaignsSearch", campaignsSearch, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "campaignsPaging", campaignsPaging, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "search", search, indent+1);
		return sb.toString();
	}
}
