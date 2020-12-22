/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;


import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import javax.xml.namespace.QName;
/**
 * Created by honchar
 */
public class MemberPanelStorage implements PageStorage{
    private static final long serialVersionUID = 1L;

    public static final String F_RELATION = "relation";
    public static final String F_ORG_SEARCH_SCOPE = "orgSearchScope";
    public static final String F_INDIRECT = "indirect";
    public static final String F_TENANT = "tenant";
    public static final String F_PROJECT = "project";

    private ObjectPaging orgMemberPanelPaging;

    private Search search;
    private SearchBoxScopeType orgSearchScope;
    private Boolean isIndirect = false;
    private QName relation = null;
    private ObjectReferenceType tenant = new ObjectReferenceType();
    private ObjectReferenceType project = new ObjectReferenceType();

    public MemberPanelStorage(){
        resetTenantRef();
        resetProjectRef();
    }

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

    public ObjectReferenceType getTenant() {
        return tenant;
    }

    public void setTenant(ObjectReferenceType tenant) {
        this.tenant = tenant;
    }

    public boolean isTenantEmpty() {
        return getTenant().getOid() == null || getTenant().getOid() == null || getTenant().asReferenceValue().isEmpty();
    }

    public void resetTenantRef(){
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(OrgType.COMPLEX_TYPE);
        this.tenant = ref;
    }

    public ObjectReferenceType getProject() {
        return project;
    }

    public void setProject(ObjectReferenceType project) {
        this.project = project;
    }

    public boolean isProjectEmpty() {
        return getProject() == null || getProject().getOid() == null || getProject().asReferenceValue().isEmpty();
    }

    public void resetProjectRef(){
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(OrgType.COMPLEX_TYPE);
        this.project = ref;
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
