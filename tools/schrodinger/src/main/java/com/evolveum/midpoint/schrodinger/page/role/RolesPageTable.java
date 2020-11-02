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
        $(Schrodinger.bySelfOrAncestorElementAttributeValue("button", "data-toggle", "dropdown", "class", "sortableLabel"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement dropDown = $(Schrodinger.byDataId("ul", "dropDownMenu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new TableHeaderDropDownMenu<RolesPageTable>(this, dropDown);
    }

    @Override
    public RolePage getObjectDetailsPage(){
        return new RolePage();
    }

    public ConfirmationModal<RolesPageTable> enableRole() {
        return enableRole(null, null);
    }

    public ConfirmationModal<RolesPageTable> enableRoleByName(String nameValue) {
        return enableRole("ObjectType.name", nameValue);
    }

    public ConfirmationModal<RolesPageTable> enableRole(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "FocusListInlineMenuHelper.menu.enable");
    }

    public ConfirmationModal<RolesPageTable> disableRole() {
        return disableRole(null, null);
    }

    public ConfirmationModal<RolesPageTable> disableRoleByName(String nameValue) {
        return disableRole("ObjectType.name", nameValue);
    }

    public ConfirmationModal<RolesPageTable> disableRole(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "FocusListInlineMenuHelper.menu.disable");
    }

    public ConfirmationModal<RolesPageTable> reconcileRole() {
        return reconcileRole(null, null);
    }

    public ConfirmationModal<RolesPageTable> reconcileRoleByName(String nameValue) {
        return reconcileRole("ObjectType.name", nameValue);
    }

    public ConfirmationModal<RolesPageTable> reconcileRole(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "FocusListInlineMenuHelper.menu.reconcile");
    }

    public ConfirmationModal<RolesPageTable> deleteRole() {
        return deleteRole(null, null);
    }

    public ConfirmationModal<RolesPageTable> deleteRoleByName(String nameValue) {
        return deleteRole("ObjectType.name", nameValue);
    }

    public ConfirmationModal<RolesPageTable> deleteRole(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "FocusListInlineMenuHelper.menu.delete");
    }
}
