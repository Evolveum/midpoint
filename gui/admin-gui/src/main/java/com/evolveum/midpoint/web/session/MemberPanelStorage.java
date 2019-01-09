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
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_4.SearchBoxScopeType;

import javax.xml.namespace.QName;
/**
 * Created by honchar
 */
public class MemberPanelStorage implements PageStorage{
    private static final long serialVersionUID = 1L;

    private ObjectPaging orgMemberPanelPaging;

    private Search search;
    private SearchBoxScopeType orgSearchScope;
    private Boolean isIndirect = false;
    private QName relation = null;
    private ObjectTypes type = null;

    public SearchBoxScopeType getOrgSearchScope() {
        return orgSearchScope;
    }

    public void setOrgSearchScope(SearchBoxScopeType orgSearchScope) {
        this.orgSearchScope = orgSearchScope;
    }

    public Boolean getIndirect() {
        return isIndirect;
    }

    public void setIndirect(Boolean indirect) {
        isIndirect = indirect;
    }

    public QName getRelation() {
        return relation;
    }

    public void setRelation(QName relation) {
        this.relation = relation;
    }

    public ObjectTypes getType() {
        return type;
    }

    public void setType(ObjectTypes type) {
        this.type = type;
    }

    @Override
    public void setSearch(Search search) {
        this.search = search;
    }

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public ObjectPaging getPaging() {
        return orgMemberPanelPaging;
    }

    @Override
    public void setPaging(ObjectPaging orgMemberPanelPaging) {
        this.orgMemberPanelPaging = orgMemberPanelPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("MemberPanelStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "search", search, indent+1);
        return sb.toString();
    }


}
