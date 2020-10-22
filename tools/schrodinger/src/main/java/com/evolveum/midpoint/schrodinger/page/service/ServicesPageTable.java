/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.page.service;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;

/**
 * Created by honchar
 */
public class ServicesPageTable extends AssignmentHolderObjectListTable<ListServicesPage, ServicePage> {

    public ServicesPageTable(ListServicesPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public TableHeaderDropDownMenu<ServicesPageTable> clickHeaderActionDropDown() {
        //todo implement if needed or move implementation in AssignmentHolderObjectListTable
        return null;
    }

    @Override
    public ServicePage getObjectDetailsPage(){
        return new ServicePage();
    }

}
