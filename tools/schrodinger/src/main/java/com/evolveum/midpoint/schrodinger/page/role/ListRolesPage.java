/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.role;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListRolesPage extends AssignmentHolderObjectListPage<RolesPageTable, RolePage> {

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
            return newObjectCollection("New role");
        }
        mainButton.click();
        return new RolePage();
    }

    public RolePage getObjectDetailsPage() {
        return new RolePage();
    }
}
