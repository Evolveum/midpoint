/*
 * Copyright (c) 2010-2019 Evolveum
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
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.*;

/**
 * Created by honchar.
 */
public class OrgStructurePanelStorage implements PageStorage, DebugDumpable, OrgTreeStateStorage {

    private static final long serialVersionUID = 1L;

    private SelectableBean<OrgType> selectedItem;                //selected tree item on the Org. structure page
    private TreeStateSet<SelectableBean<OrgType>> expandedItems; //expanded tree items on the Org. structure page
    private SelectableBean<OrgType> collapsedItem = null;                 //selected tab id on the Org. structure page
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
    public Set<SelectableBean<OrgType>> getExpandedItems() {
        return getSelectedTabStorage().getExpandedItems();
    }

    @Override
    public void setExpandedItems(TreeStateSet<SelectableBean<OrgType>> expandedItems) {
        getSelectedTabStorage().setExpandedItems(expandedItems != null ? expandedItems.clone() : null);
    }


    @Override
    public SelectableBean<OrgType> getSelectedItem() {
        return getSelectedTabStorage().getSelectedItem();
    }

    @Override
    public void setSelectedItem(SelectableBean<OrgType> selectedItem) {
        getSelectedTabStorage().setSelectedItem(selectedItem);
    }

    @Override
    public SelectableBean<OrgType> getCollapsedItem() {
        return getSelectedTabStorage().getCollapsedItem();
    }

    @Override
    public void setCollapsedItem(SelectableBean<OrgType> collapsedItem) {
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
