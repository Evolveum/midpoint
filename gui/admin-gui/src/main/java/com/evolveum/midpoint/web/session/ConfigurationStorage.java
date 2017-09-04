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
import com.evolveum.midpoint.web.page.admin.configuration.dto.AccountDetailsSearchDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;

/**
 * @author lazyman
 */
public class ConfigurationStorage implements PageStorage {
	private static final long serialVersionUID = 1L;

	private DebugSearchDto debugSearchDto;
    private AccountDetailsSearchDto accountSearchDto;

    private ObjectPaging debugSearchPaging;
    private ObjectPaging accountDetailsPaging;

    @Override
    public Search getSearch() {
        return debugSearchDto.getSearch();
    }

    @Override
    public void setSearch(Search search) {
    	debugSearchDto.setSearch(search);
    }

    public void setDebugSearchDto(DebugSearchDto debugSearchDto) {
        this.debugSearchDto = debugSearchDto;
    }

    public DebugSearchDto getDebugSearchDto() {
		return debugSearchDto;
	}

    public AccountDetailsSearchDto getAccountSearchDto() {
        if(accountSearchDto == null){
            accountSearchDto = new AccountDetailsSearchDto();
        }

        return accountSearchDto;
    }

    public void setAccountSearchDto(AccountDetailsSearchDto accountSearchDto) {
        this.accountSearchDto = accountSearchDto;
    }

    @Override
    public ObjectPaging getPaging() {
        return debugSearchPaging;
    }

    @Override
    public void setPaging(ObjectPaging debugSearchPaging) {
        this.debugSearchPaging = debugSearchPaging;
    }

    public ObjectPaging getAccountDetailsPaging() {
        return accountDetailsPaging;
    }

    public void setAccountDetailsPaging(ObjectPaging accountDetailsPaging) {
        this.accountDetailsPaging = accountDetailsPaging;
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ConfigurationStorage\n");
		DebugUtil.debugDumpWithLabelLn(sb, "debugSearchDto", debugSearchDto, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "accountSearchDto", accountSearchDto, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "debugSearchPaging", debugSearchPaging, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "accountDetailsPaging", accountDetailsPaging, indent+1);
		return sb.toString();
	}
}
