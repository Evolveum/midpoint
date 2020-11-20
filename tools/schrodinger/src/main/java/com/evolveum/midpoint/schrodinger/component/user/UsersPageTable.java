/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersPageTable extends AssignmentHolderObjectListTable<ListUsersPage, UserPage> {

    public UsersPageTable(ListUsersPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    protected TableHeaderDropDownMenu<UsersPageTable> clickHeaderActionDropDown() {

        $(Schrodinger.bySelfOrAncestorElementAttributeValue("button", "data-toggle", "dropdown", "class", "sortableLabel"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement dropDown = $(Schrodinger.byDataId("ul", "dropDownMenu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new TableHeaderDropDownMenu<UsersPageTable>(this, dropDown);

    }

    public ConfirmationModal<UsersPageTable> enableUser() {
        return enableUser(null, null);
    }

    public ConfirmationModal<UsersPageTable> enableUser(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageUsers.menu.enable");
    }

    public ConfirmationModal<UsersPageTable> disableUser() {
        return disableUser(null, null);
    }

    public ConfirmationModal<UsersPageTable> disableUser(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageUsers.menu.disable");
    }

    public ConfirmationModal<UsersPageTable> reconcileUser() {
        return reconcileUser(null, null);
    }

    public ConfirmationModal<UsersPageTable> reconcileUser(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageUsers.menu.reconcile");
    }

    public ConfirmationModal<UsersPageTable> unlockUser() {
        return unlockUser(null, null);
    }

    public ConfirmationModal<UsersPageTable> unlockUser(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageUsers.menu.unlock");
    }

    public ConfirmationModal<UsersPageTable> deleteUser() {
        return deleteUser(null, null);
    }

    public ConfirmationModal<UsersPageTable> deleteUser(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageUsers.menu.delete");
    }

    public ConfirmationModal<UsersPageTable> mergeUser() {
        return mergeUser(null, null);
    }

    public ConfirmationModal<UsersPageTable> mergeUser(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageUsers.menu.merge");
    }

    @Override
    public UserPage getObjectDetailsPage(){
        return new UserPage();
    }

}
