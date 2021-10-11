/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class MemberTableDropDown<T> extends TableHeaderDropDownMenu<T> {
    public MemberTableDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public FocusSetAssignmentsModal<T> assign(){
        getParentElement().$x(".//schrodinger[@"+ Schrodinger.DATA_S_RESOURCE_KEY +"='abstractRoleMemberPanel.menu.assign']").parent()
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement modalElement = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Select object(s)"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new FocusSetAssignmentsModal<T>((T) this.getParent(), modalElement);
    }

    public ConfirmationModal<T> recompute(){
        getParentElement().$x(".//schrodinger[@"+ Schrodinger.DATA_S_RESOURCE_KEY +"='abstractRoleMemberPanel.menu.recompute']").parent()
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Confirm action"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ConfirmationModal<>(this.getParent(), actualModal);
    }
}
