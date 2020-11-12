/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;
/**
 * Created by honchar
 */
public class DelegationDetailsPanel<T> extends Component<T> {

    public DelegationDetailsPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public DelegationDetailsPanel<T> clickHeaderCheckbox(){
        SelenideElement checkBox = $(Schrodinger.byDataId("headerRow")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .find(Schrodinger.byDataId("selected"));
        String checkedAttr = checkBox.getAttribute("checked");
        checkBox.click();
        if (checkedAttr == null || !checkedAttr.equals("checked")) {
            checkBox.waitUntil(Condition.attribute("checked", "checked"), MidPoint.TIMEOUT_DEFAULT_2_S);
        } else {
            checkBox.waitUntil(Condition.attribute("checked", null), MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        return this;
    }

    public String getDescriptionValue() {
        return $(Schrodinger.byDataId("delegationDescription")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .getText();
    }

    public DelegationDetailsPanel<T> setDescriptionValue(String descriptionValue) {
        $(Schrodinger.byDataId("delegationDescription")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .setValue(descriptionValue);
        return this;
    }

    public String getValidFromValue() {
        return "";
    }

    public DelegationDetailsPanel<T> setValidFromValue() {
        return this;
    }

    public String getValidToValue() {
        return "";
    }

    public DelegationDetailsPanel<T> setValidToValue() {
        return this;
    }

    public DelegationDetailsPanel<T> clickAssignmentPrivilegesCheckbox() {

        return this;
    }

    public DelegationDetailsPanel<T> clickAssignmentLimitationsCheckbox() {
        return this;
    }

    public DelegationDetailsPanel<T> clickApprovalWorkItemsCheckbox() {
        return this;
    }

    public DelegationDetailsPanel<T> clickCertificationWorkItemsCheckbox() {
        return this;
    }
}
