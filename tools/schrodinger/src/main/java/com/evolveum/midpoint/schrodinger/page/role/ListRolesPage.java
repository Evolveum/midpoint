/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.role;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListRolesPage extends AssignmentHolderObjectListPage<RolesPageTable> {

    @Override
    public RolesPageTable table() {
        return new RolesPageTable(this, getTableBoxElement());
    }

    @Override
    protected String getTableAdditionalClass(){
        return ConstantsUtil.OBJECT_ROLE_BOX_COLOR;
    }

    @Override
    public RolePage newRole() {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@" + Schrodinger.DATA_S_ID + "='mainButton']"));
        String expanded = mainButton.getAttribute("aria-haspopup");
        if (Boolean.getBoolean(expanded)) {
            return newRole("New role");
        }
        mainButton.click();
        return new RolePage();
    }

    public RolePage newRole(String title) {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@" + Schrodinger.DATA_S_ID + "='mainButton']"));
        if (!Boolean.getBoolean(mainButton.getAttribute("aria-expanded"))) {
            mainButton.click();
        }
        $(Schrodinger.byElementAttributeValue("div", "title", title))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new RolePage();
    }
}
