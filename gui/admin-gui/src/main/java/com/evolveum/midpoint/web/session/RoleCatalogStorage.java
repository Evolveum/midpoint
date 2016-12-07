/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class RoleCatalogStorage implements PageStorage{
    /**
     * DTO used for search in {@link com.evolveum.midpoint.web.page.self.PageAssignmentShoppingKart}
     */
    private Search roleCatalogSearch;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.self.PageAssignmentShoppingKart}
     */

    private SelectableBean<OrgType> selectedItem;                //selected tree item on the Org. structure page
    private String selectedOid;
    private TreeStateSet<SelectableBean<OrgType>> expandedItems; //expanded tree items on the Org. structure page
    private int selectedTabId = 0;                 //selected tab id on the Org. structure page
    private SelectableBean<OrgType> collapsedItem = null;                 //collapsed tree item
    private List<AssignmentEditorDto> assignmentShoppingCart;   //  a list of assignments in the shopping cart
    private AssignmentViewType viewType = AssignmentViewType.ROLE_CATALOG_VIEW;      //the current view type

    private ObjectPaging roleCatalogPaging;

    public Search getSearch() {
        return roleCatalogSearch;
    }

    public void setSearch(Search roleCatalog) {
        this.roleCatalogSearch = roleCatalog;
    }

    @Override
    public ObjectPaging getPaging() {
        return roleCatalogPaging;
    }

    @Override
    public void setPaging(ObjectPaging roleCatalogPaging) {
        this.roleCatalogPaging = roleCatalogPaging;
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
        DebugUtil.debugDumpWithLabelLn(sb, "roleCatalogSearch", roleCatalogSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "roleCatalogPaging", roleCatalogPaging, indent + 1);
        return sb.toString();
    }

    public SelectableBean<OrgType> getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(SelectableBean<OrgType> selectedItem) {
        this.selectedItem = selectedItem;
    }

    public TreeStateSet<SelectableBean<OrgType>> getExpandedItems() {
        return expandedItems;
    }

    public void setExpandedItems(TreeStateSet<SelectableBean<OrgType>> expandedItems) {
        this.expandedItems = expandedItems;
    }

    public int getSelectedTabId() {
        return selectedTabId;
    }

    public void setSelectedTabId(int selectedTabId) {
        this.selectedTabId = selectedTabId;
    }

    public SelectableBean<OrgType> getCollapsedItem() {
        return collapsedItem;
    }

    public void setCollapsedItem(SelectableBean<OrgType> collapsedItem) {
        this.collapsedItem = collapsedItem;
    }


    public List<AssignmentEditorDto> getAssignmentShoppingCart() {
        return assignmentShoppingCart == null ? new ArrayList<AssignmentEditorDto>() : assignmentShoppingCart;
    }

    public void setAssignmentShoppingCart(List<AssignmentEditorDto> assignmentShoppingCart) {
        this.assignmentShoppingCart = assignmentShoppingCart;
    }

    public AssignmentViewType getViewType() {
        if (viewType == null){
            viewType = AssignmentViewType.ROLE_TYPE;
        }
        return viewType;
    }

    public void setViewType(AssignmentViewType viewType) {
        this.viewType = viewType;
    }

    public String getSelectedOid() {
        return selectedOid;
    }

    public void setSelectedOid(String selectedOid) {
        this.selectedOid = selectedOid;
    }
}
