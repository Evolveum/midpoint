/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.ColumnTypeConfigContext;

public class CertificationColumnTypeConfigContext extends ColumnTypeConfigContext {

    //this flag means that we want to view all items, not only those assigned to logged-in user, usually true for administrator user
    boolean viewAllItems;

    //this flag means that we want to view only items that are not decided yet
    boolean notDecidedOnly;

    PageBase pageBase;

    public CertificationColumnTypeConfigContext() {
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
