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
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.page.self.PageAssignmentShoppingCart;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by honchar.
 */
public class RoleCatalogStorage implements PageStorage, OrgTreeStateStorage {
    /**
     * DTO used for search in {@link PageAssignmentShoppingCart}
     */
    private Map<Integer, Search> roleCatalogSearchMap = new HashMap<>();

    /**
     * Paging DTO used in table on page {@link PageAssignmentShoppingCart}
     */

    private SelectableBean<OrgType> selectedItem;                //selected tree item on the Org. structure page
    private String selectedOid;
    private TreeStateSet<SelectableBean<OrgType>> expandedItems; //expanded tree items on the Org. structure page
    private int selectedTabId = 0;                 //selected tab id on the Org. structure page
    private SelectableBean<OrgType> collapsedItem = null;                 //collapsed tree item
    private List<AssignmentEditorDto> assignmentShoppingCart;   //  a list of assignments in the shopping cart
    private AssignmentViewType viewType = null;      //the current view type
    private int defaultTabIndex = -1;
    private List<UserType> targetUserList = new ArrayList<>();
    private UserType assignmentsUserOwner = null;
    private List<ConflictDto> conflictsList;
    private String requestDescription = "";
    private ObjectPaging roleCatalogPaging;
    private int assignmentRequestLimit = -1;

    public Search getSearch() {
        return roleCatalogSearchMap.get(getDefaultTabIndex() < 0 ? 0 : getDefaultTabIndex());
    }

    public void setSearch(Search roleCatalogSearch) {
        int selectedTab = getDefaultTabIndex() < 0 ? 0 : getDefaultTabIndex();
        if (!roleCatalogSearchMap.containsKey(selectedTab)){
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
        DebugUtil.debugDumpWithLabelLn(sb, "roleCatalogSearchMap", roleCatalogSearchMap, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "roleCatalogPaging", roleCatalogPaging, indent + 1);
        return sb.toString();
    }

    @Override
    public SelectableBean<OrgType> getSelectedItem() {
        return selectedItem;
    }

    @Override
    public void setSelectedItem(SelectableBean<OrgType> selectedItem) {
        this.selectedItem = selectedItem;
    }

    @Override
    public TreeStateSet<SelectableBean<OrgType>> getExpandedItems() {
        return expandedItems;
    }

    @Override
    public void setExpandedItems(TreeStateSet<SelectableBean<OrgType>> expandedItems) {
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
    public SelectableBean<OrgType> getCollapsedItem() {
        return collapsedItem;
    }

    @Override
    public void setCollapsedItem(SelectableBean<OrgType> collapsedItem) {
        this.collapsedItem = collapsedItem;
    }

    public List<ConflictDto> getConflictsList() {
        return conflictsList == null ? new ArrayList<>() : conflictsList;
    }

    public void setConflictsList(List<ConflictDto> conflictsList) {
        this.conflictsList = conflictsList;
    }

    public List<AssignmentEditorDto> getAssignmentShoppingCart() {
        if (assignmentShoppingCart == null){
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

    public List<UserType> getTargetUserList() {
        return targetUserList;
    }

    public void setTargetUserList(List<UserType> targetUserList) {
        this.targetUserList = targetUserList;
    }

    public UserType getAssignmentsUserOwner() {
        return assignmentsUserOwner;
    }

    public void setAssignmentsUserOwner(UserType assignmentsUserOwner) {
        this.assignmentsUserOwner = assignmentsUserOwner;
    }

    public boolean isSelfRequest(){
        return getTargetUserList() == null || getTargetUserList().size() == 0;
    }

    public boolean isMultiUserRequest(){
        return getTargetUserList() != null && getTargetUserList().size() > 1;
    }

    public int getAssignmentRequestLimit() {
        return assignmentRequestLimit;
    }

    public void setAssignmentRequestLimit(int assignmentRequestLimit) {
        this.assignmentRequestLimit = assignmentRequestLimit;
    }
}
