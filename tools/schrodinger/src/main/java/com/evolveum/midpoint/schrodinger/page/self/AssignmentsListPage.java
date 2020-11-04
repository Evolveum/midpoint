/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.self;

import com.codeborne.selenide.Condition;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.ObjectBrowserModal;
import com.evolveum.midpoint.schrodinger.component.modal.ObjectBrowserModalTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class AssignmentsListPage extends BasicPage {

    /**
     * if request runs successfully, the user is redirected to RequestRolePage
     * if some error occurs, the user stays on the same page
     * @return
     */
    public BasicPage clickRequestButton() {
        $(Schrodinger.byDataId("request")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        if (feedback().isSuccess()) {
            return new RequestRolePage();
        } else {
            return this;
        }
    }

    public RequestRolePage clickCancelButton() {
        $(Schrodinger.byDataId("back")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new RequestRolePage();
    }

    public AssignmentsListPage setTargetUser(String... userNames) {
        if (userNames == null) {
            return this;
        }
        $(Schrodinger.byDataId("userSelectionButton")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        ObjectBrowserModal<RequestRoleTab> userSelectionModal = new ObjectBrowserModal(this, Utils.getModalWindowSelenideElement());
        ObjectBrowserModalTable<RequestRoleTab, ObjectBrowserModal<RequestRoleTab>> table = userSelectionModal.table();
        if (userSelectionModal != null) {
            for (String userName : userNames) {
                table.search()
                        .byName()
                        .inputValue(userName)
                        .updateSearch()
                        .and()
                        .selectCheckboxByName(userName);
            }
            userSelectionModal.clickAddButton();
        }
        return this;
    }

    public AssignmentsListPage setRequestComment(String comment) {
        $(Schrodinger.byDataId("description")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .setValue(comment);
        return this;
    }

}
