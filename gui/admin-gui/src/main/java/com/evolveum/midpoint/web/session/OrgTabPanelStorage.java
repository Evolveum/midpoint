/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.Set;

/**
 * Created by honchar
 */
public class OrgTabPanelStorage implements PageStorage, DebugDumpable, OrgTreeStateStorage {

    private static final long serialVersionUID = 1L;

    private TreeSelectableBean<OrgType> selectedItem;                //selected tree item on the Org. structure page
    private TreeStateSet<TreeSelectableBean<OrgType>> expandedItems; //expanded tree items on the Org. structure page
    private TreeSelectableBean<OrgType> collapsedItem = null;                 //selected tab id on the Org. structure page
    private boolean inverse = false;
    private int selectedTabId = 0;                 //selected tab id on the Org. structure page

    private Search membersPanelSearch;
    private ObjectPaging membersPanelPaging;

    @Override
    public ObjectPaging getPaging() {
        return membersPanelPaging;
    }

    @Override
    public void setPaging(ObjectPaging membersPanelPaging) {
        this.membersPanelPaging = membersPanelPaging;
    }

    @Override
    public Search getSearch() {
        return membersPanelSearch;
    }

    @Override
    public void setSearch(Search membersPanelSearch) {
        this.membersPanelSearch = membersPanelSearch;
    }

    @Override
    public Set<TreeSelectableBean<OrgType>> getExpandedItems() {
        return expandedItems;
    }

    @Override
    public void setExpandedItems(TreeStateSet<TreeSelectableBean<OrgType>> expandedItems) {
        this.expandedItems = expandedItems != null ? expandedItems.clone() : null;
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
    public TreeSelectableBean<OrgType> getCollapsedItem() {
        return collapsedItem;
    }

    @Override
    public void setCollapsedItem(TreeSelectableBean<OrgType> collapsedItem) {
        this.collapsedItem = collapsedItem;
    }

    @Override
    public boolean isInverse(){
        return inverse;
    }

    @Override
    public void setInverse(boolean inverse){
        this.inverse = inverse;
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
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("OrgTabPanelStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "selectedItem", selectedItem, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "expandedItems", expandedItems, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "collapsedItem", collapsedItem, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "inverse", inverse, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "selectedTabId", selectedTabId, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "membersPanelSearch", membersPanelSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "membersPanelPaging", membersPanelPaging, indent+1);
        return sb.toString();
    }



}
