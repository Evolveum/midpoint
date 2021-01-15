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

import org.openqa.selenium.By;
import org.testng.Assert;

import static com.codeborne.selenide.Selenide.$;
/**
 * Created by honchar
 */
public class DelegationDetailsPanel<T> extends Component<T> {

    public DelegationDetailsPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public DelegationDetailsPanel<T> expandDetailsPanel(String linkText) {
        $(By.linkText(linkText)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        $(Schrodinger.byDataId("delegationDescription")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
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

    public boolean isDescriptionEnabled() {
        return $(Schrodinger.byDataId("delegationDescription")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isEnabled();
    }

    public DateTimePanel<DelegationDetailsPanel<T>> getValidFromPanel() {
        return new DateTimePanel<>(this,
                $(Schrodinger.byDataId("delegationValidFrom")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public boolean isValidFromPanelEnabled () {
        getValidFromPanel().findDate().shouldBe(Condition.enabled);
        if (!getValidFromPanel().findDate().isEnabled()) {
            return false;
        }
        getValidFromPanel().findHours().shouldBe(Condition.enabled);
        if (!getValidFromPanel().findHours().isEnabled()) {
            return false;
        }
        getValidFromPanel().findMinutes().shouldBe(Condition.enabled);
        if (!getValidFromPanel().findMinutes().isEnabled()) {
            return false;
        }
        return true;
    }

    public boolean isValidFromPanelDisabled () {
        getValidFromPanel().findDate().shouldBe(Condition.disabled);
        if (getValidFromPanel().findDate().isEnabled()) {
            return false;
        }
        getValidFromPanel().findHours().shouldBe(Condition.disabled);
        if (getValidFromPanel().findHours().isEnabled()) {
            return false;
        }
        getValidFromPanel().findMinutes().shouldBe(Condition.disabled);
        if (getValidFromPanel().findMinutes().isEnabled()) {
            return false;
        }
        return true;
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

    public boolean isAssignmentPrivilegesSelected() {
        if ($(Schrodinger.byDataId("assignmentPrivilegesCheckbox")).exists()) {
            return $(Schrodinger.byDataId("assignmentPrivilegesCheckbox")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                    .isSelected();
        }
        return false;
    }

    public DelegationDetailsPanel<T> clickAssignmentLimitationsCheckbox(boolean value) {
        Utils.setOptionCheckedById("allowTransitive", value);
        return this;
    }

    public boolean isAssignmentLimitationsSelected() {
        return $(Schrodinger.byDataId("allowTransitive")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isSelected();
    }

    public DelegationDetailsPanel<T> clickApprovalWorkItemsCheckbox(boolean value) {
        Utils.setOptionCheckedById("approvalWorkItems", value);
        return this;
    }

    public boolean isApprovalWorkItemsSelected() {
        return $(Schrodinger.byDataId("approvalWorkItems")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isSelected();
    }

    public DelegationDetailsPanel<T> clickCertificationWorkItemsCheckbox(boolean value) {
        Utils.setOptionCheckedById("certificationWorkItems", value);
        return this;
    }

    public boolean isCertificationWorkItemsSelected() {
        return $(Schrodinger.byDataId("certificationWorkItems")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .isSelected();
    }

    public DelegationDetailsPanel<T> assertApprovalWorkItemsSelected() {
        Assert.assertTrue(isApprovalWorkItemsSelected(), "Workflow approvals (for approval work items) checkbox is not selected but should be.");
        return this;
    }

    public DelegationDetailsPanel<T> assertApprovalWorkItemsNotSelected() {
        Assert.assertTrue(isApprovalWorkItemsSelected(), "Workflow approvals (for approval work items) checkbox is selected but shouldn't be.");
        return this;
    }

    public DelegationDetailsPanel<T> assertAssignmentLimitationsSelected() {
        Assert.assertTrue(isAssignmentLimitationsSelected(), "Assignment limitations checkbox is not selected but should be.");
        return this;
    }

    public DelegationDetailsPanel<T> assertAssignmentLimitationsNotSelected() {
        Assert.assertTrue(isAssignmentLimitationsSelected(), "Assignment limitations checkbox is selected but shouldn't be.");
        return this;
    }

    public DelegationDetailsPanel<T> assertAssignmentPrivilegesSelected() {
        Assert.assertTrue(isAssignmentPrivilegesSelected(),"Assignment privileges checkbox is not selected but should be.");
        return this;
    }

    public DelegationDetailsPanel<T> assertAssignmentPrivilegesNotSelected() {
        Assert.assertFalse(isAssignmentPrivilegesSelected(),"Assignment privileges checkbox is selected but shouldn't be.");
        return this;
    }

    public DelegationDetailsPanel<T> assertCertificationWorkItemsSelected() {
        Assert.assertTrue(isCertificationWorkItemsSelected(), "Workflow approvals (for certification work items) checkbox is not selected but should be.");
        return this;
    }

    public DelegationDetailsPanel<T> assertCertificationWorkItemsNotSelected() {
        Assert.assertFalse(isCertificationWorkItemsSelected(), "Workflow approvals (for certification work items) checkbox is selected but shouldn't be.");
        return this;
    }
}
