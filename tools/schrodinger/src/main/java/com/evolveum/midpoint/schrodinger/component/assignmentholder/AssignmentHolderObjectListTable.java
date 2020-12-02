/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.assignmentholder;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.modal.ExportPopupPanel;

import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.search.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by honchar
 */
public abstract class AssignmentHolderObjectListTable<P, PD extends AssignmentHolderDetailsPage> extends TableWithPageRedirect<P> {

    public AssignmentHolderObjectListTable(P parent, SelenideElement parentElement){
        super(parent, parentElement);
    }


    @Override
    public AssignmentHolderObjectListTable<P, PD> selectCheckboxByName(String name) {
        rowByColumnLabel(getNameColumnLabel(), name).clickCheckBox();
        return this;
    }

    @Override
    public PD clickByName(String name) {

        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(getDetailsPageLoadingTimeToWait());
        return getObjectDetailsPage();
    }

    public long getDetailsPageLoadingTimeToWait() {
        return MidPoint.TIMEOUT_DEFAULT_2_S;
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
        return getButtonToolbar().$(By.cssSelector(iconCssClass));
    }

    public PD newObjectButtonClickPerformed(String iconCssClass){
        getToolbarButton(iconCssClass)
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        Selenide.sleep(2000);
        return getObjectDetailsPage();
    }

    public ExportPopupPanel<P> clickExportButton() {
        getToolbarButton(".fa.fa-download")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        Selenide.sleep(2000);
        return new ExportPopupPanel<>(getParent(), Utils.getModalWindowSelenideElement());
    }

    public PD newObjectCollectionButtonClickPerformed(String mainButtonIconCssClass, String objCollectionButtonIconCssClass){
        SelenideElement mainButtonElement = getToolbarButton(mainButtonIconCssClass)
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        mainButtonElement.click();
        if (mainButtonElement.exists()) {
            mainButtonElement.parent().parent()
                    .$(By.cssSelector(".dropdown-menu.auto-width"))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                    .$(By.cssSelector(objCollectionButtonIconCssClass))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                    .click();
            Selenide.sleep(2000);
        }
        return getObjectDetailsPage();
    }

    public int countDropdownButtonChildrenButtons(String mainButtonIconCssClass) {
        SelenideElement mainButtonElement = getToolbarButton(mainButtonIconCssClass)
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        mainButtonElement.click();
        if (mainButtonElement.exists()) {
            ElementsCollection childrenButtonCollection = mainButtonElement.parent().parent()
                    .$(By.cssSelector(".dropdown-menu.auto-width"))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                    .findAll(By.tagName("i"));
            return childrenButtonCollection != null ? childrenButtonCollection.size() : 0;
        }
        return 0;
    }

    public abstract PD getObjectDetailsPage();

    protected String getNameColumnLabel() {
        return "Name";
    }

}
