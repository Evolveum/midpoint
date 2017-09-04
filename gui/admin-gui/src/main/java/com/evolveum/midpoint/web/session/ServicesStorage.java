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
