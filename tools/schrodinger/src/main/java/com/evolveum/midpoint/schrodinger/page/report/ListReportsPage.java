/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.report;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.component.report.ReportTable;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListReportsPage extends AssignmentHolderObjectListPage<ReportTable, ReportPage> {

    @Override
    public ReportTable table() {
        return new ReportTable(this, getTableBoxElement());
    }

    @Override
    public ReportPage getObjectDetailsPage() {
        return new ReportPage();
    }

    public ReportPage newReport() {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@" + Schrodinger.DATA_S_ID + "='mainButton']"));
        mainButton.click();
        return new ReportPage();
    }
}
