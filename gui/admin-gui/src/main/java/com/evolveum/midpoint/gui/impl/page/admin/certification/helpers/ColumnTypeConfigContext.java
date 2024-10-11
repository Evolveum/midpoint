/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.page.PageBase;

public class ColumnTypeConfigContext {

    //this flag means that we want to view all items, not only those assigned to logged-in user, usually true for administrator user
    boolean viewAllItems;

    //this flag means that we want to view only items that are not decided yet
    boolean notDecidedOnly;

    PageBase pageBase;

    public ColumnTypeConfigContext() {
    }

    public boolean isViewAllItems() {
        return viewAllItems;
    }

    public void setViewAllItems(boolean viewAllItems) {
        this.viewAllItems = viewAllItems;
    }

    public boolean notDecidedOnly() {
        return notDecidedOnly;
    }

    public void setNotDecidedOnly(boolean notDecidedOnly) {
        this.notDecidedOnly = notDecidedOnly;
    }

    public PageBase getPageBase() {
        return pageBase;
    }

    public void setPageBase(PageBase pageBase) {
        this.pageBase = pageBase;
    }
}
