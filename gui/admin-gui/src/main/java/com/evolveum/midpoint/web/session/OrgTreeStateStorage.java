package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.Set;

/**
 * Created by honchar
 */
public interface OrgTreeStateStorage {
    public Set<SelectableBean<OrgType>> getExpandedItems();

    public void setExpandedItems(TreeStateSet<SelectableBean<OrgType>> expandedItems);

    public SelectableBean<OrgType> getSelectedItem();

    public void setSelectedItem(SelectableBean<OrgType> selectedItem);

    public int getSelectedTabId();

    public void setSelectedTabId(int selectedTabId);

    public SelectableBean<OrgType> getCollapsedItem();

    public void setCollapsedItem(SelectableBean<OrgType> collapsedItem);


}
