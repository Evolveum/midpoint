/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/10/2018.
 */
public class UsersTableDropDown<T> extends TableHeaderDropDownMenu<T> {
    public UsersTableDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickEnable() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Enable")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickDisable() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Disable")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickReconcile() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Reconcile")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }


    public ConfirmationModal<UsersTableDropDown<T>> clickUnlock() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Unlock")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }


    public ConfirmationModal<UsersTableDropDown<T>> clickDelete() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Delete")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickMerge() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Merge")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }


}
