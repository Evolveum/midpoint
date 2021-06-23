/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;


import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Created by honchar
 */
public class MemberPanelStorage implements PageStorage{
    private static final long serialVersionUID = 1L;


    public static final String F_TENANT = "tenant";
    public static final String F_PROJECT = "project";

    public static final String F_RELATION_ITEM = "relationSearchItem";
    public static final String F_ORG_SEARCH_SCOPE_ITEM = "scopeSearchItem";
    public static final String F_INDIRECT_ITEM = "indirectSearchItem";
    public static final String F_OBJECT_TYPE_ITEM = "objectTypeSearchItem";

    private ObjectPaging orgMemberPanelPaging;

    private Search search;

    private ScopeSearchItemConfigurationType scopeSearchItem;

    private IndirectSearchItemConfigurationType indirectSearchItem;

    private RelationSearchItemConfigurationType relationSearchItem;

    private ObjectReferenceType tenant = new ObjectReferenceType();
    private UserInterfaceFeatureType tenantSearchItem;

    private ObjectReferenceType project = new ObjectReferenceType();
    private UserInterfaceFeatureType projectSearchItem;

    private ObjectTypeSearchItemConfigurationType objectTypeSearchItem;

    public MemberPanelStorage(){
        resetTenantRef();
        resetProjectRef();
    }

    public ScopeSearchItemConfigurationType getScopeSearchItem() {
        return scopeSearchItem;
    }

    public void setScopeSearchItem(ScopeSearchItemConfigurationType scopeSearchItem) {
        this.scopeSearchItem = scopeSearchItem;
    }

    public IndirectSearchItemConfigurationType getIndirectSearchItem() {
        return indirectSearchItem;
    }

    public void setIndirectSearchItem(IndirectSearchItemConfigurationType indirectSearchItem) {
        this.indirectSearchItem = indirectSearchItem;
    }

    public RelationSearchItemConfigurationType getRelationSearchItem() {
        return relationSearchItem;
    }

    public QName getDefaultRelation() {
        return relationSearchItem.getDefaultValue();
    }

    public List<QName> getSupportedRelations() {
        return relationSearchItem.getSupportedRelations();
    }

    public void setRelationSearchItem(RelationSearchItemConfigurationType relationSearchItem) {
        this.relationSearchItem = relationSearchItem;
    }

    public UserInterfaceFeatureType getTenantSearchItem() {
        return tenantSearchItem;
    }

    public void setTenantSearchItem(UserInterfaceFeatureType tenantSearchItem) {
        this.tenantSearchItem = tenantSearchItem;
    }

    public UserInterfaceFeatureType getProjectSearchItem() {
        return projectSearchItem;
    }

    public void setProjectSearchItem(UserInterfaceFeatureType projectSearchItem) {
        this.projectSearchItem = projectSearchItem;
    }

    public ObjectTypeSearchItemConfigurationType getObjectTypeSearchItem() {
        return objectTypeSearchItem;
    }

    public void setObjectTypeSearchItem(ObjectTypeSearchItemConfigurationType objectTypeSearchItem) {
        this.objectTypeSearchItem = objectTypeSearchItem;
    }

    public QName getDefaultObjectType() {
        return objectTypeSearchItem.getDefaultValue();
    }

    public List<QName> getSupportedObjectTypes() {
        return objectTypeSearchItem.getSupportedTypes();
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

    public boolean isRelationVisible() {
        return isSearchItemVisible(relationSearchItem);
    }

    public boolean isIndirectVisible() {
        return isSearchItemVisible(indirectSearchItem);
    }

    public boolean isIndirect() {
        return BooleanUtils.isTrue(indirectSearchItem.isIndirect());
    }

    public boolean isSearchScopeVisible() {
        return isSearchItemVisible(scopeSearchItem);
    }

    public boolean isSearchScope(SearchBoxScopeType scope) {
        return scope == scopeSearchItem.getDefaultValue();
    }

    public boolean isTenantVisible() {
        return isSearchItemVisible(tenantSearchItem);
    }

    public boolean isProjectVisible() {
        return isSearchItemVisible(projectSearchItem);
    }

    private boolean isSearchItemVisible(UserInterfaceFeatureType feature) {
        if (feature == null) {
            return true;
        }
        return CompiledGuiProfile.isVisible(feature.getVisibility(), null);
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
