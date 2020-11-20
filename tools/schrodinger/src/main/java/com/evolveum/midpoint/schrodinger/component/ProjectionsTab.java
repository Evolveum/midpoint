/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetProjectionModal;
import com.evolveum.midpoint.schrodinger.component.user.ProjectionsDropDown;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.FocusPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProjectionsTab<P extends AssignmentHolderDetailsPage> extends TabWithTableAndPrismView<FocusPage> {

    public ProjectionsTab(FocusPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ProjectionsDropDown<ProjectionsTab<P>> clickHeaderActionDropDown() {

        $(By.tagName("thead"))
                .$(Schrodinger.byDataId("inlineMenuPanel"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();

        SelenideElement dropDownMenu = $(Schrodinger.byElementAttributeValue("ul", "class", "dropdown-menu pull-right"));

        return new ProjectionsDropDown<ProjectionsTab<P>>(this, dropDownMenu);
    }


    public AbstractTableWithPrismView<ProjectionsTab<P>> table() {

        SelenideElement tableBox = $(Schrodinger.byDataId("div", "itemsTable"));

        return new AbstractTableWithPrismView<ProjectionsTab<P>>(this, tableBox) {
            @Override
            public PrismFormWithActionButtons<AbstractTableWithPrismView<ProjectionsTab<P>>> clickByName(String name) {

                $(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                SelenideElement prismElement = $(Schrodinger.byDataId("div", "itemDetails"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return new PrismFormWithActionButtons<>(this, prismElement);
            }

            @Override
            public AbstractTableWithPrismView<ProjectionsTab<P>> selectCheckboxByName(String name) {

                $(Schrodinger.byFollowingSiblingEnclosedValue("td", "class", "check", "data-s-id", "3", name))
                        .$(By.tagName("input"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }

            @Override
            public AbstractTableWithPrismView<ProjectionsTab<P>> removeByName(String name) {
                selectCheckboxByName(name);
                clickHeaderActionDropDown().delete();
                return this;
            }
        };
    }

//    public AbstractTable<ProjectionsTab> table() {
//
//        SelenideElement tableBox = $(By.cssSelector(".box.projection"));
//
//        return new AbstractTable<ProjectionsTab>(this, tableBox) {
//            @Override
//            public PrismForm<AbstractTable<ProjectionsTab>> clickByName(String name) {
//
//                $(Schrodinger.byElementValue("span", "data-s-id", "name", name))
//                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
//
//                SelenideElement prismElement = $(By.cssSelector(".container-fluid.prism-object"))
//                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
//
//                return new PrismForm<>(this, prismElement);
//            }
//
//            @Override
//            public AbstractTable<ProjectionsTab> selectCheckboxByName(String name) {
//
//                $(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("input", "type", "checkbox", "data-s-id", "3", name))
//                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
//
//                return this;
//            }
//        };
//    }

    public FocusSetProjectionModal<ProjectionsTab<P>> clickAddProjection() {
        $(Schrodinger.byElementAttributeValue("i", "class", "fa fa-plus "))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return new FocusSetProjectionModal<ProjectionsTab<P>>(this, Utils.getModalWindowSelenideElement());
    }

    public boolean projectionExists(String assignmentName){
        SelenideElement assignmentSummaryDisplayName = table()
                .clickByName(assignmentName)
                .getParentElement()
                .$(Schrodinger.byDataId("displayName"));
        return assignmentName.equals(assignmentSummaryDisplayName.getText());
    }

    @Override
    protected String getPrismViewPanelId() {
        return "itemDetails";
    }

}
