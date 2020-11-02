/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate Honchar.
 */
public class CasesListTable extends AssignmentHolderObjectListTable<CasesPage, CasePage> {

    public CasesListTable(CasesPage parent, SelenideElement parentElement){
        super(parent, parentElement);
    }

    @Override
    protected TableHeaderDropDownMenu<CasesListTable> clickHeaderActionDropDown() {
        return null;
    }

    @Override
    public CasePage getObjectDetailsPage(){
        $(Schrodinger.byDataId("mainPanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new CasePage();
    }

    public ConfirmationModal<CasesListTable> stopCase() {
        return stopCase(null, null);
    }

    public ConfirmationModal<CasesListTable> stopCaseByName(String nameValue) {
        return stopCase("ObjectType.name", nameValue);
    }

    public ConfirmationModal<CasesListTable> stopCase(String columnTitleKey, String rowValue) {
        return clickButtonMenuItemWithConfirmation(columnTitleKey, rowValue, "fa.fa-stop");
    }

    public ConfirmationModal<CasesListTable> deleteCase() {
        return stopCase(null, null);
    }

    public ConfirmationModal<CasesListTable> deleteCaseByName(String nameValue) {
        return stopCase("ObjectType.name", nameValue);
    }

    public ConfirmationModal<CasesListTable> deleteCase(String columnTitleKey, String rowValue) {
        return clickButtonMenuItemWithConfirmation(columnTitleKey, rowValue, "fa.fa-minus");
    }
}
