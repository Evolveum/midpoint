/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.Set;

/**
 * Created by honchar
 */
public interface OrgTreeStateStorage {
    Set<TreeSelectableBean<OrgType>> getExpandedItems();

    void setExpandedItems(TreeStateSet<TreeSelectableBean<OrgType>> expandedItems);

    TreeSelectableBean<OrgType> getSelectedItem();

    void setSelectedItem(TreeSelectableBean<OrgType> selectedItem);

    int getSelectedTabId();

    void setSelectedTabId(int selectedTabId);

    Set<TreeSelectableBean<OrgType>> getCollapsedItems();

    void setCollapsedItems(TreeStateSet<TreeSelectableBean<OrgType>> collapsedItems);
}
