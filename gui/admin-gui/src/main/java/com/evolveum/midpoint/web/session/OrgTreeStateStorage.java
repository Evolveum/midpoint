package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.Set;

/**
 * Created by honchar
 */
public interface OrgTreeStateStorage {
    Set<SelectableBean<OrgType>> getExpandedItems();

    void setExpandedItems(TreeStateSet<SelectableBean<OrgType>> expandedItems);

    SelectableBean<OrgType> getSelectedItem();

    void setSelectedItem(SelectableBean<OrgType> selectedItem);

    int getSelectedTabId();

    void setSelectedTabId(int selectedTabId);

    SelectableBean<OrgType> getCollapsedItem();

    void setCollapsedItem(SelectableBean<OrgType> collapsedItem);

    boolean isInverse();

    void setInverse(boolean inverse);
}
