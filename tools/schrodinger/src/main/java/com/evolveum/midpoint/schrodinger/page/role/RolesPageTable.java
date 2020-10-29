/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.page.role;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.component.task.TasksPageTable;
import com.evolveum.midpoint.schrodinger.component.user.UsersTableDropDown;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by honchar
 */
public class RolesPageTable extends AssignmentHolderObjectListTable<ListRolesPage, RolePage> {

    public RolesPageTable(ListRolesPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public TableHeaderDropDownMenu<RolesPageTable> clickHeaderActionDropDown() {
        //todo implement if needed or move implementation in AssignmentHolderObjectListTable
        return null;
    }

    @Override
    public RolePage getObjectDetailsPage(){
        return new RolePage();
    }

}
