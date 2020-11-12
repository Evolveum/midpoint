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
import com.evolveum.midpoint.schrodinger.component.DateTimePanel;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

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

    public DateTimePanel<DelegationDetailsPanel<T>> getValidFromPanel() {
        return new DateTimePanel<>(this,
                $(Schrodinger.byDataId("delegationValidFrom")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public DelegationDetailsPanel<T> setValidFromValue(String date, String hours, String minutes, DateTimePanel.AmOrPmChoice amOrPmChoice) {
        DateTimePanel<DelegationDetailsPanel<T>> validFromPanel = new DateTimePanel<>(this,
                $(Schrodinger.byDataId("delegationValidFrom")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
        validFromPanel.setDateTimeValue(date, hours, minutes, amOrPmChoice);
        return this;
    }

    public DateTimePanel<DelegationDetailsPanel<T>> getValidToPanel() {
        return new DateTimePanel<>(this,
                $(Schrodinger.byDataId("delegationValidTo")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public DelegationDetailsPanel<T> setValidToValue(String date, String hours, String minutes, DateTimePanel.AmOrPmChoice amOrPmChoice) {
        DateTimePanel<DelegationDetailsPanel<T>> validFromPanel = new DateTimePanel<>(this,
                $(Schrodinger.byDataId("delegationValidTo")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
        validFromPanel.setDateTimeValue(date, hours, minutes, amOrPmChoice);
        return this;
    }

    public DelegationDetailsPanel<T> setAssignmentPrivilegesCheckboxValue(boolean value) {
        Utils.setOptionCheckedById("assignmentPrivilegesCheckbox", value);
        return this;
    }

    public boolean isAssignmentPrivileges() {
        return $(Schrodinger.byDataId("assignmentPrivilegesCheckbox")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isSelected();
    }

    public DelegationDetailsPanel<T> clickAssignmentLimitationsCheckbox(boolean value) {
        Utils.setOptionCheckedById("allowTransitive", value);
        return this;
    }

    public boolean isAssignmentLimitations() {
        return $(Schrodinger.byDataId("allowTransitive")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isSelected();
    }

    public DelegationDetailsPanel<T> clickApprovalWorkItemsCheckbox(boolean value) {
        Utils.setOptionCheckedById("approvalWorkItems", value);
        return this;
    }

    public boolean isApprovalWorkItems() {
        return $(Schrodinger.byDataId("approvalWorkItems")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isSelected();
    }

    public DelegationDetailsPanel<T> clickCertificationWorkItemsCheckbox(boolean value) {
        Utils.setOptionCheckedById("certificationWorkItems", value);
        return this;
    }

    public boolean isCertificationWorkItems() {
        return $(Schrodinger.byDataId("certificationWorkItems")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isSelected();
    }
}
