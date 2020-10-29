/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.user;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListUsersPage extends AssignmentHolderObjectListPage<UsersPageTable, UserPage> {

    @Override
    public UsersPageTable table() {
        return new UsersPageTable(this, getTableBoxElement());
    }

    @Override
    public UserPage getObjectDetailsPage() {
        return new UserPage();
    }

    @Override
    protected String getTableAdditionalClass(){
        return ConstantsUtil.OBJECT_USER_BOX_COLOR;
    }

    @Override
    public UserPage newUser() {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@" + Schrodinger.DATA_S_ID + "='mainButton']"));
        String expanded = mainButton.getAttribute("aria-haspopup");
        if (Boolean.getBoolean(expanded)) {
            return newObjectCollection("New user");
        }
        mainButton.click();
        return new UserPage();
    }
}
