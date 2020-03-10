/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersPageTable extends AssignmentHolderObjectListTable<ListUsersPage, UserPage> {

    public UsersPageTable(ListUsersPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public UsersTableDropDown<UsersPageTable> clickActionDropDown() {

        $(Schrodinger.bySelfOrAncestorElementAttributeValue("button", "data-toggle", "dropdown", "class", "sortableLabel"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement dropDown = $(Schrodinger.byElementAttributeValue("ul", "class", "dropdown-menu pull-right"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new UsersTableDropDown<>(this, dropDown);

    }

    public ConfirmationModal<UsersPageTable> clickEnable() {

        $(Schrodinger.bySelfOrAncestorElementAttributeValue("i", "class", "fa fa-user fa-fw", "data-s-id", "topToolbars"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }

    @Override
    public UserPage getObjectDetailsPage(){
        return new UserPage();
    }

}
