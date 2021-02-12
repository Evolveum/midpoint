/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.page.self.PageAssignmentShoppingCart;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Created by honchar.
 */
public class RoleCatalogStorage implements PageStorage, OrgTreeStateStorage {

    public static final String F_ORG_SEARCH_SCOPE = "orgSearchScope";

    /**
     * DTO used for search in {@link PageAssignmentShoppingCart}
     */
    private Map<Integer, Search> roleCatalogSearchMap = new HashMap<>();

    /**
     * <
     * Paging DTO used in table on page {@link PageAssignmentShoppingCart}
     */

    private TreeSelectableBean<OrgType> selectedItem;                //selected tree item on the Org. structure page
    private String selectedOid;
    private TreeStateSet<TreeSelectableBean<OrgType>> expandedItems; //expanded tree items on the Org. structure page
    private int selectedTabId = 0;                 //selected tab id on the Org. structure page
    private TreeStateSet<TreeSelectableBean<OrgType>> collapsedItems = null;                 //collapsed tree item
    private List<AssignmentEditorDto> assignmentShoppingCart;   //  a list of assignments in the shopping cart
    private AssignmentViewType viewType = null;      //the current view type
    private int defaultTabIndex = -1;
    private List<String> targetUserOidsList = new ArrayList<>();
    private UserType assignmentsUserOwner = null;
    private List<ConflictDto> conflictsList;
    private String requestDescription = "";
    private ObjectPaging roleCatalogPaging;
    private int assignmentRequestLimit = -1;
    private QName selectedRelation = null;
    private SearchBoxScopeType orgSearchScope = SearchBoxScopeType.ONE_LEVEL;

    public Search getSearch() {
        return roleCatalogSearchMap.get(getDefaultTabIndex() < 0 ? 0 : getDefaultTabIndex());
    }

    public void setSearch(Search roleCatalogSearch) {
        int selectedTab = getDefaultTabIndex() < 0 ? 0 : getDefaultTabIndex();
        if (!roleCatalogSearchMap.containsKey(selectedTab)) {
            roleCatalogSearchMap.put(selectedTab, roleCatalogSearch);
        } else {
            roleCatalogSearchMap.replace(selectedTab, roleCatalogSearch);
        }
    }

    @Override
    public ObjectPaging getPaging() {
        return roleCatalogPaging;
    }

    @Override
    public void setPaging(ObjectPaging roleCatalogPaging) {
        this.roleCatalogPaging = roleCatalogPaging;
    }

    public String getRequestDescription() {
        return requestDescription;
    }

    public void setRequestDescription(String requestDescription) {
        this.requestDescription = requestDescription;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("RoleCatalogStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "roleCatalogSearchMap", roleCatalogSearchMap, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "roleCatalogPaging", roleCatalogPaging, indent + 1);
        return sb.toString();
    }

    @Override
    public TreeSelectableBean<OrgType> getSelectedItem() {
        return selectedItem;
    }

    @Override
    public void setSelectedItem(TreeSelectableBean<OrgType> selectedItem) {
        this.selectedItem = selectedItem;
    }

    @Override
    public TreeStateSet<TreeSelectableBean<OrgType>> getExpandedItems() {
        return expandedItems;
    }

    @Override
    public void setExpandedItems(TreeStateSet<TreeSelectableBean<OrgType>> expandedItems) {
        this.expandedItems = expandedItems;
    }

    @Override
    public int getSelectedTabId() {
        return selectedTabId;
    }

    @Override
    public void setSelectedTabId(int selectedTabId) {
        this.selectedTabId = selectedTabId;
    }

    @Override
    public Set<TreeSelectableBean<OrgType>> getCollapsedItems() {
        return collapsedItems;
    }

    @Override
    public void setCollapsedItems(TreeStateSet<TreeSelectableBean<OrgType>> collapsedItems) {
        this.collapsedItems = collapsedItems;
    }

    public List<ConflictDto> getConflictsList() {
        return conflictsList == null ? new ArrayList<>() : conflictsList;
    }

    public void setConflictsList(List<ConflictDto> conflictsList) {
        this.conflictsList = conflictsList;
    }

    public List<AssignmentEditorDto> getAssignmentShoppingCart() {
        if (assignmentShoppingCart == null) {
            assignmentShoppingCart = new ArrayList<>();
        }
        return assignmentShoppingCart;
    }

    public void setAssignmentShoppingCart(List<AssignmentEditorDto> assignmentShoppingCart) {
        this.assignmentShoppingCart = assignmentShoppingCart;
    }

    public AssignmentViewType getViewType() {
        return viewType;
    }

    public void setViewType(AssignmentViewType viewType) {
        this.viewType = viewType;
    }

    public int getDefaultTabIndex() {
        return defaultTabIndex;
    }

    public void setDefaultTabIndex(int defaultTabIndex) {
        this.defaultTabIndex = defaultTabIndex;
    }

    public String getSelectedOid() {
        return selectedOid;
    }

    public void setSelectedOid(String selectedOid) {
        this.selectedOid = selectedOid;
    }

    public List<String> getTargetUserOidsList() {
        return targetUserOidsList;
    }

    public void setTargetUserOidsList(List<String> targetUserOidsList) {
        this.targetUserOidsList = targetUserOidsList;
    }

    public UserType getAssignmentsUserOwner() {
        return assignmentsUserOwner;
    }

    public void setAssignmentsUserOwner(UserType assignmentsUserOwner) {
        this.assignmentsUserOwner = assignmentsUserOwner;
    }

    public boolean isSelfRequest() {
        return getTargetUserOidsList() == null || getTargetUserOidsList().size() == 0;
    }

    public boolean isMultiUserRequest() {
        return getTargetUserOidsList() != null && getTargetUserOidsList().size() > 1;
    }

    public int getAssignmentRequestLimit() {
        return assignmentRequestLimit;
    }

    public void setAssignmentRequestLimit(int assignmentRequestLimit) {
        this.assignmentRequestLimit = assignmentRequestLimit;
    }

    public QName getSelectedRelation() {
        return selectedRelation;
    }

    public void setSelectedRelation(QName selectedRelation) {
        this.selectedRelation = selectedRelation;
    }

    public SearchBoxScopeType getOrgSearchScope() {
        return orgSearchScope;
    }

    public void setOrgSearchScope(SearchBoxScopeType orgSearchScope) {
        this.orgSearchScope = orgSearchScope;
    }
}
