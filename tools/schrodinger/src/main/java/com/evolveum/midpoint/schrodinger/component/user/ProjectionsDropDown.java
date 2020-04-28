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
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/2/2018.
 */
public class ProjectionsDropDown<T> extends TableHeaderDropDownMenu<T> {

    public ProjectionsDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T enable() {
        return clickByDataResourceKey("pageAdminFocus.button.enable");
    }

    public T disable() {
        return clickByDataResourceKey("pageAdminFocus.button.disable");
    }

    public T unlink() {
        return clickByDataResourceKey("pageAdminFocus.button.unlink");
    }

    public T unlock() {
        return clickByDataResourceKey("pageAdminFocus.button.unlock");
    }

//    public FocusSetProjectionModal<T> addProjection() {
//        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
//                "        Add projection")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
//
//        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Choose object"));
//
//        return new FocusSetProjectionModal<>(this.getParent(), actualModal);
//    }

    public ConfirmationModal<ProjectionsDropDown<T>> delete() {
        $(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Delete")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm deletion"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this, actualModal);
    }
}
