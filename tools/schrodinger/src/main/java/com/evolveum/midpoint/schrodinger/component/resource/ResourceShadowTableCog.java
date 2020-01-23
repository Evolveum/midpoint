/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/25/2018.
 */
public class ResourceShadowTableCog<T> extends DropDown<T> {
    public ResourceShadowTableCog(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }


    public T clickEnable() {
        $(Schrodinger.byDataResourceKey("pageContentAccounts.menu.enableAccounts"))
                .parent().waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

    public T clickDisable() {
        $(Schrodinger.byDataResourceKey("pageContentAccounts.menu.disableAccounts"))
                .parent().waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

    public ConfirmationModal<T> clickDelete() {
        $(Schrodinger.byDataResourceKey("pageContentAccounts.menu.deleteAccount"))
                .waitUntil(Condition.exist, MidPoint.TIMEOUT_DEFAULT_2_S).parent().click();

        SelenideElement modalBox = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm deletion"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this.getParent(), modalBox);
    }

    public T clickImport() {
        $(Schrodinger.byDataResourceKey("pageContentAccounts.menu.importAccounts"))
                .parent().waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

    public T clickRemoveOwner() {
        $(Schrodinger.byDataResourceKey("pageContentAccounts.menu.removeOwners"))
                .parent().waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

}
