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
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

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
    public MemberTableDropDown<MemberTable<T>> clickHeaderActionDropDown() {
        SelenideElement dropDownButton = $(Schrodinger.bySelfOrAncestorElementAttributeValue("button", "data-toggle", "dropdown",
                "class", "sortableLabel"));
        dropDownButton.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement dropDown = dropDownButton.parent().$x(".//ul[@"+Schrodinger.DATA_S_ID+"='dropDownMenu']")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new MemberTableDropDown<MemberTable<T>>(this, dropDown);
    }
}
