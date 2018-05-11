package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/10/2018.
 */
public class UsersTableDropDown<T> extends DropDown<T> {
    public UsersTableDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickEnable() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Enable")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ConfirmationModal<>(this, actualModal);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickDisable() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Disable")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ConfirmationModal<>(this, actualModal);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickReconcile() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Reconcile")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ConfirmationModal<>(this, actualModal);
    }


    public ConfirmationModal<UsersTableDropDown<T>> clickUnlock() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Unlock")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ConfirmationModal<>(this, actualModal);
    }


    public ConfirmationModal<UsersTableDropDown<T>> clickDelete() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Delete")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ConfirmationModal<>(this, actualModal);
    }

    public ConfirmationModal<UsersTableDropDown<T>> clickMerge() {

        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Merge")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ConfirmationModal<>(this, actualModal);
    }


}
