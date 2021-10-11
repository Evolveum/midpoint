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

import java.util.*;

/**
 * Created by honchar.
 */
public class OrgStructurePanelStorage implements PageStorage, DebugDumpable, OrgTreeStateStorage {

    private static final long serialVersionUID = 1L;

    private TreeSelectableBean<OrgType> selectedItem;                //selected tree item on the Org. structure page
    private TreeStateSet<TreeSelectableBean<OrgType>> expandedItems; //expanded tree items on the Org. structure page
    private TreeSelectableBean<OrgType> collapsedItem = null;                 //selected tab id on the Org. structure page
    private boolean inverse = false;
    private int selectedTabId = 0;                 //selected tab id on the Org. structure page
    private Map<Integer, OrgTabPanelStorage> orgTabStorageMap = new HashMap<>();

    private Search membersPanelSearch;
    private ObjectPaging membersPanelPaging;

    @Override
    public ObjectPaging getPaging() {
        return getSelectedTabStorage() != null ? getSelectedTabStorage().getPaging() : null;
    }

    @Override
    public void setPaging(ObjectPaging membersPanelPaging) {
        getSelectedTabStorage().setPaging(membersPanelPaging);
    }

    @Override
    public Search getSearch() {
        return getSelectedTabStorage() != null ? getSelectedTabStorage().getSearch() : null;
    }

    @Override
    public void setSearch(Search membersPanelSearch) {
        getSelectedTabStorage().setSearch(membersPanelSearch);
    }

    @Override
    public Set<TreeSelectableBean<OrgType>> getExpandedItems() {
        return getSelectedTabStorage().getExpandedItems();
    }

    @Override
    public void setExpandedItems(TreeStateSet<TreeSelectableBean<OrgType>> expandedItems) {
        getSelectedTabStorage().setExpandedItems(expandedItems != null ? expandedItems.clone() : null);
    }


    @Override
    public TreeSelectableBean<OrgType> getSelectedItem() {
        return getSelectedTabStorage().getSelectedItem();
    }

    @Override
    public void setSelectedItem(TreeSelectableBean<OrgType> selectedItem) {
        getSelectedTabStorage().setSelectedItem(selectedItem);
    }

    @Override
    public TreeSelectableBean<OrgType> getCollapsedItem() {
        return getSelectedTabStorage().getCollapsedItem();
    }

    @Override
    public void setCollapsedItem(TreeSelectableBean<OrgType> collapsedItem) {
        getSelectedTabStorage().setCollapsedItem(collapsedItem);
    }

    @Override
    public boolean isInverse(){
        return getSelectedTabStorage().isInverse();
    }

    @Override
    public void setInverse(boolean inverse){
        getSelectedTabStorage().setInverse(inverse);
    }

    @Override
    public int getSelectedTabId() {
        return selectedTabId;
    }

    @Override
    public void setSelectedTabId(int selectedTabId) {
        this.selectedTabId = selectedTabId;
    }

    public OrgTabPanelStorage getSelectedTabStorage(){
        if (!orgTabStorageMap.containsKey(selectedTabId)){
            orgTabStorageMap.put(selectedTabId, new OrgTabPanelStorage());
        }
        return orgTabStorageMap.get(selectedTabId);
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("OrgStructurePanelStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "selectedItem", selectedItem, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "expandedItems", expandedItems, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "collapsedItem", collapsedItem, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "selectedTabId", selectedTabId, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "orgTabStorageMap", orgTabStorageMap, indent+1);
        return sb.toString();
    }

}
