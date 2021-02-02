/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.report;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.CasesTab;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.cases.CasePage;
import com.evolveum.midpoint.schrodinger.page.report.AuditLogViewerDetailsPage;
import com.evolveum.midpoint.schrodinger.page.report.AuditLogViewerPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class AuditRecordTable<T> extends TableWithPageRedirect<T> {

    public AuditRecordTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public AuditLogViewerDetailsPage clickByName(String name) {
        return new AuditLogViewerDetailsPage();
    }

    @Override
    public AuditRecordTable<T> selectCheckboxByName(String name) {
        return null;
    }

    @Override
    protected TableHeaderDropDownMenu<AuditRecordTable<T>> clickHeaderActionDropDown() {
        return null;
    }

    public void checkInitiator(int row, String name) {
        checkTextInColumn(row, 2, name);
    }

    public void checkEventType(int row, String name) {
        checkTextInColumn(row, 4, name);
    }

    public void checkOutcome(int row, String name) {
        checkTextInColumn(row, 8, name);
    }

    public void checkTextInColumn(int row, int column, String name) {
        $(By.cssSelector(".box.boxed-table"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        getCell(row, column).shouldHave(Condition.text(name));
    }

    public SelenideElement getCell(int row, int column) {
        SelenideElement tbody = getParentElement().$(Schrodinger.byElementAttributeValue("tbody", "data-s-id", "body")).waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        ElementsCollection rowsElement = tbody.findAll(By.tagName("tr"));
        SelenideElement rowElement = rowsElement.get(row > 0 ? (row-1) : row);
        ElementsCollection columnsElement = rowElement.findAll(By.tagName("td"));
        return columnsElement.get(column > 0 ? (column-1) : column);
    }
}
