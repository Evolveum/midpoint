/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.service;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListServicesPage extends AssignmentHolderObjectListPage<ServicesPageTable, ServicePage> {

    @Override
    public ServicesPageTable table() {
        return new ServicesPageTable(this, getTableBoxElement());
    }

    @Override
    public ServicePage getObjectDetailsPage() {
        return new ServicePage();
    }

    @Override
    protected String getTableAdditionalClass(){
        return ConstantsUtil.OBJECT_SERVICE_BOX_COLOR;
    }

    @Override
    public ServicePage newService() {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@" + Schrodinger.DATA_S_ID + "='mainButton']"));
        String expanded = mainButton.getAttribute("aria-haspopup");
        if (Boolean.getBoolean(expanded)) {
            return newObjectCollection("New service");
        }
        mainButton.click();
        return new ServicePage();
    }
}
