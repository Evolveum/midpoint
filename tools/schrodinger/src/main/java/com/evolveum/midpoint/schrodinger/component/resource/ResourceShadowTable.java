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
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/25/2018.
 */
public class ResourceShadowTable<T> extends TableWithPageRedirect<T> {
    public ResourceShadowTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public AccountPage clickByName(String name) {
        SelenideElement link = getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name));
        link.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        link.waitWhile(Condition.exist, MidPoint.TIMEOUT_LONG_1_M);

        return new AccountPage();
    }

    @Override
    public ResourceShadowTable<T> selectCheckboxByName(String name) {

        SelenideElement check = $(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("input", "type", "checkbox", "data-s-id", "3", name));
        check.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        check.waitUntil(Condition.selected, MidPoint.TIMEOUT_MEDIUM_6_S);

        return this;
    }

    public UserPage clickOnOwnerByName(String name) {

        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return new UserPage();
    }

    @Override
    protected TableHeaderDropDownMenu<ResourceShadowTable<T>> clickHeaderActionDropDown() {
        $(Schrodinger.byElementAttributeValue("button", "data-toggle", "dropdown"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement cog = $(Schrodinger.byElementAttributeValue("ul","role","menu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new TableHeaderDropDownMenu<>(this, cog);
    }

    @Override
    public Search<? extends ResourceShadowTable<T>> search() {
        return (Search<? extends ResourceShadowTable<T>>) super.search();
    }

    public ResourceShadowTable<T> clickEnable() {
        return clickEnable(null, null);
    }

    public ResourceShadowTable<T> clickEnable(String columnTitleKey, String rowValue) {
        clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageContentAccounts.menu.enableAccount");
        return this;
    }

    public ResourceShadowTable<T> clickDisable() {
        return clickDisable(null, null);
    }

    public ResourceShadowTable<T> clickDisable(String columnTitleKey, String rowValue) {
        clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageContentAccounts.menu.disableAccount");
        return this;
    }

    public ConfirmationModal<ResourceShadowTable<T>> clickDelete() {
        return clickDelete(null, null);
    }

    public ConfirmationModal<ResourceShadowTable<T>> clickDelete(String columnTitleKey, String rowValue) {
        return clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageContentAccounts.menu.deleteAccount");
    }

    public ResourceShadowTable<T> clickImport() {
        return clickImport(null, null);
    }

    public ResourceShadowTable<T> clickImport(String columnTitleKey, String rowValue) {
        clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageContentAccounts.menu.importAccount");
        return this;
    }

    public ResourceShadowTable<T> clickRemoveOwner() {
        return clickRemoveOwner(null, null);
    }

    public ResourceShadowTable<T> clickRemoveOwner(String columnTitleKey, String rowValue) {
        clickMenuItemWithConfirmation(columnTitleKey, rowValue, "pageContentAccounts.menu.removeOwner");
        return this;
    }
}
