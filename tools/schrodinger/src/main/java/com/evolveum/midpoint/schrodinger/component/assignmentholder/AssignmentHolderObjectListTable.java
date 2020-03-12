/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.assignmentholder;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public abstract class AssignmentHolderObjectListTable<P extends AssignmentHolderObjectListPage, PD extends AssignmentHolderDetailsPage> extends TableWithPageRedirect<P> {

    public AssignmentHolderObjectListTable(P parent, SelenideElement parentElement){
        super(parent, parentElement);
    }


    @Override
    public TableWithPageRedirect<P> selectCheckboxByName(String name) {

        //TODO implement
        return null;
    }

    @Override
    public PD clickByName(String name) {

        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
//        Selenide.sleep(2000);
        return getObjectDetailsPage();
    }

    public PD clickByPartialName(String name) {

        getParentElement()
                .$(Schrodinger.byDataId("tableContainer"))
                .$(By.partialLinkText(name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return getObjectDetailsPage();
    }

    @Override
    public Search<? extends AssignmentHolderObjectListTable<P, PD>> search() {
        SelenideElement searchElement = getParentElement().$(By.cssSelector(".form-inline.pull-right.search-form"));

        return new Search<>(this, searchElement);
    }

    @Override
    public AssignmentHolderObjectListTable<P, PD> selectAll() {

        $(Schrodinger.bySelfOrAncestorElementAttributeValue("input", "type", "checkbox", "data-s-id", "topToolbars"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

    public SelenideElement getToolbarButton(String iconCssClass){
        SelenideElement buttonToolbar = getButtonToolbar();
        SelenideElement buttonElement = null;
        ElementsCollection toolbarButtonsList = buttonToolbar
                .findAll(By.tagName("button"));
        for (SelenideElement button : toolbarButtonsList) {
            if (button.$(Schrodinger.byElementAttributeValue("i", "class", iconCssClass)).exists()) {
                buttonElement = button;
            }
        }
        return buttonElement;
    }

    public PD newObjectButtonClickPerformed(String iconCssClass){
        getToolbarButton(iconCssClass)
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        Selenide.sleep(2000);
        return getObjectDetailsPage();
    }

    public abstract PD getObjectDetailsPage();

}
