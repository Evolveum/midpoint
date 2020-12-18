/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.FocusTableWithChoosableElements;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.util.aspect.MidpointInterceptor;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/11/2018.
 */
public class FocusSetAssignmentsModal<T> extends ModalBox<T> {
    public FocusSetAssignmentsModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public FocusSetAssignmentsModal<T> selectType(String option) {
        SelenideElement tabElement = $(Schrodinger.byElementValue("a", "class", "tab-label", option))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        String classActive = tabElement.attr("class");

        tabElement.click();
        if (!classActive.contains("active")) {
            $(Schrodinger.byElementValue("a", "class", "tab-label", option))
                    .waitUntil(Condition.attribute("class", classActive + " active"), MidPoint.TIMEOUT_DEFAULT_2_S).exists();
        }


        return this;
    }

    public FocusSetAssignmentsModal<T> selectKind(String option) {
        $(By.name("mainPopup:content:popupBody:kindContainer:kind:input"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).selectOption(option);

        return this;
    }

    public FocusSetAssignmentsModal<T> selectIntent(String option) {
        $(By.name("mainPopup:content:popupBody:intentContainer:intent:input"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).selectOption(option);

        return this;
    }

    public FocusTableWithChoosableElements<FocusSetAssignmentsModal<T>> table() {
        SelenideElement resourcesBox = getParentElement().$x(".//div[@class='box boxed-table']")
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new FocusTableWithChoosableElements<FocusSetAssignmentsModal<T>>(this, resourcesBox){



        };
    }

    public T clickAdd() {

        $(Schrodinger.byDataResourceKey("userBrowserDialog.button.addButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        getParentElement().waitWhile(Condition.exist, MidPoint.TIMEOUT_LONG_1_M);
        return this.getParent();
    }

}
