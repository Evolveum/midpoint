/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class MemberTable<T> extends AssignmentHolderObjectListTable<T, AssignmentHolderDetailsPage> {

    public MemberTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public AssignmentHolderDetailsPage getObjectDetailsPage() {
        return new AssignmentHolderDetailsPage() {};
    }

    @Override
    protected TableHeaderDropDownMenu<MemberTable<T>> clickHeaderActionDropDown() {
        SelenideElement dropDownButton = $(Schrodinger.bySelfOrAncestorElementAttributeValue("button", "data-toggle", "dropdown",
                "class", "sortableLabel"));
        dropDownButton.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement dropDown = dropDownButton.parent().$x(".//ul[@"+Schrodinger.DATA_S_ID+"='dropDownMenu']")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new TableHeaderDropDownMenu<MemberTable<T>>(this, dropDown);
    }

    public FocusSetAssignmentsModal<MemberTable<T>> assign(){
        return assign(null, null);
    }

    public FocusSetAssignmentsModal<MemberTable<T>> assign(String columnTitleKey, String rowValue){
        return clickMenuItemWithFocusSetAssignmentsModal(columnTitleKey, rowValue, "abstractRoleMemberPanel.menu.assign");
    }

    public ConfirmationModal<MemberTable<T>> recompute(){
        return recompute(null, null);
    }

    public ConfirmationModal<MemberTable<T>> recompute(String columnTitleKey, String rowValue){
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "abstractRoleMemberPanel.menu.recompute");
    }

}
