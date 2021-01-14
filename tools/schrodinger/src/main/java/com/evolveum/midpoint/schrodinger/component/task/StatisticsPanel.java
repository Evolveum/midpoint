/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import java.util.Objects;

/**
 * @author honchar
 */
public class StatisticsPanel<T> extends Component<T> {

    public StatisticsPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String getProvisioningOperationsResourceValue() {
        return getStatisticValueByFieldName("Provisioning.Resource");
    }

    public String getProvisioningOperationsObjectClassValue() {
        return getStatisticValueByFieldName("Provisioning.ObjectClass");
    }

    public String getProvisioningOperationsGetOkValue() {
        return getStatisticValueByFieldName("Provisioning.GetSuccess");
    }

    public String getProvisioningOperationsGetFailValue() {
        return getStatisticValueByFieldName("Provisioning.GetFailure");
    }

    public String getProvisioningOperationsSearchOkValue() {
        return getStatisticValueByFieldName("Provisioning.SearchSuccess");
    }

    public String getProvisioningOperationsSearchFailValue() {
        return getStatisticValueByFieldName("Provisioning.SearchFailure");
    }

    public String getProvisioningOperationsCreateOkValue() {
        return getStatisticValueByFieldName("Provisioning.CreateSuccess");
    }

    public String getProvisioningOperationsCreateFailValue() {
        return getStatisticValueByFieldName("Provisioning.CreateFailure");
    }

    public String getProvisioningOperationsUpdateOkValue() {
        return getStatisticValueByFieldName("Provisioning.UpdateSuccess");
    }

    public String getProvisioningOperationsUpdateFailValue() {
        return getStatisticValueByFieldName("Provisioning.UpdateFailure");
    }

    public String getProvisioningOperationsDeleteOkValue() {
        return getStatisticValueByFieldName("Provisioning.DeleteSuccess");
    }

    public String getProvisioningOperationsDeleteFailValue() {
        return getStatisticValueByFieldName("Provisioning.DeleteFailure");
    }

    public String getProvisioningOperationsSyncOkValue() {
        return getStatisticValueByFieldName("Provisioning.SyncSuccess");
    }

    public String getProvisioningOperationsSyncFailValue() {
        return getStatisticValueByFieldName("Provisioning.SyncFailure");
    }

    public String getProvisioningOperationsScriptOkValue() {
        return getStatisticValueByFieldName("Provisioning.ScriptSuccess");
    }

    public String getProvisioningOperationsScriptFailValue() {
        return getStatisticValueByFieldName("Provisioning.ScriptFailure");
    }

    public String getProvisioningOperationsOtherOkValue() {
        return getStatisticValueByFieldName("Provisioning.OtherSuccess");
    }

    public String getProvisioningOperationsOtherFailValue() {
        return getStatisticValueByFieldName("Provisioning.OtherFailure");
    }

    public String getProvisioningOperationsAllOperationsValue() {
        return getStatisticValueByFieldName("Provisioning.TotalOperationsCount");
    }

    public String getProvisioningOperationsAvgTimeValue() {
        return getStatisticValueByFieldName("Provisioning.AverageTime");
    }

    public String getProvisioningOperationsMinValue() {
        return getStatisticValueByFieldName("Provisioning.MinTime");
    }

    public String getProvisioningOperationsMaxValue() {
        return getStatisticValueByFieldName("Provisioning.MaxTime");
    }

    public String getProvisioningOperationsTotalTimeValue() {
        return getStatisticValueByFieldName("Provisioning.TotalTime");
    }

    public String getMappingsEvaluationContainingObjectValue() {
        return getStatisticValueByFieldName("Mappings.Object");
    }

    public String getMappingsEvaluationInvocationsCountValue() {
        return getStatisticValueByFieldName("Mappings.Count");
    }

    public String getMappingsEvaluationAvgTimeValue() {
        return getStatisticValueByFieldName("Mappings.AverageTime");
    }

    public String getMappingsEvaluationMinValue() {
        return getStatisticValueByFieldName("Mappings.MinTime");
    }

    public String getMappingsEvaluationMaxValue() {
        return getStatisticValueByFieldName("Mappings.MaxTime");
    }

    public String getMappingsEvaluationTotalTimeValue() {
        return getStatisticValueByFieldName("Mappings.TotalTime");
    }

    public String getNotificationsTransportValue() {
        return getStatisticValueByFieldName("Notifications.Transport");
    }

    public String getNotificationsSuccessfulValue() {
        return getStatisticValueByFieldName("Notifications.CountSuccess");
    }

    public String getNotificationsFailedValue() {
        return getStatisticValueByFieldName("Notifications.CountFailure");
    }

    public String getNotificationsAvgTimeValue() {
        return getStatisticValueByFieldName("Notifications.AverageTime");
    }

    public String getNotificationsMinValue() {
        return getStatisticValueByFieldName("Notifications.MinTime");
    }

    public String getNotificationsMaxValue() {
        return getStatisticValueByFieldName("Notifications.MaxTime");
    }

    public String getNotificationsTotalTimeValue() {
        return getStatisticValueByFieldName("Notifications.TotalTime");
    }

    public StatisticsPanel<T> assertProvisioningOperationsResourceValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsResourceValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations resource value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsObjectClassValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsObjectClassValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations object class value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsGetOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsGetOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Get OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsGetFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsGetFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Get Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsSearchOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsSearchOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Search OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsSearchFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsSearchFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Search Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsCreateOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsCreateOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Create OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsCreateFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsCreateFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Create Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsUpdateOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsUpdateOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Update OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsUpdateFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsUpdateFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Update Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsDeleteOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsDeleteOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Delete OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsDeleteFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsDeleteFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Delete Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsSyncOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsSyncOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Sync OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsSyncFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsSyncFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Sync Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsScriptOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsScriptOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Script OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsScriptFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsScriptFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Script Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsOtherOkValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsOtherOkValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Other OK' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsOtherFailValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsOtherFailValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Other Fail' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsAllOperationsValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsAllOperationsValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'All operations' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsAvgTimeValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsAvgTimeValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Avg Time' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsMinValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsMinValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Min Value' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsMaxValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsMaxValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Max Value' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertProvisioningOperationsTotalTimeValueMatch(String expectedValue) {
        String realValue = getProvisioningOperationsTotalTimeValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Provisioning operations 'Total Time' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertMappingsEvaluationContainingObjectValueMatch(String expectedValue) {
        String realValue = getMappingsEvaluationContainingObjectValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Mappings evaluation 'Containing object' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertMappingsEvaluationInvocationsCountValueMatch(String expectedValue) {
        String realValue = getMappingsEvaluationInvocationsCountValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Mappings evaluation 'Invocations count' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertMappingsEvaluationAvgTimeValueMatch(String expectedValue) {
        String realValue = getMappingsEvaluationAvgTimeValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Mappings evaluation 'Avg Time' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertMappingsEvaluationMinValueMatch(String expectedValue) {
        String realValue = getMappingsEvaluationMinValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Mappings evaluation 'Min' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertMappingsEvaluationMaxValueMatch(String expectedValue) {
        String realValue = getMappingsEvaluationMaxValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Mappings evaluation 'Max' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertMappingsEvaluationTotalTimeValueMatch(String expectedValue) {
        String realValue = getMappingsEvaluationTotalTimeValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Mappings evaluation 'Total Time' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertNotificationsTransportValueMatch(String expectedValue) {
        String realValue = getNotificationsTransportValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Notifications 'Transport' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertNotificationsSuccessfulValueMatch(String expectedValue) {
        String realValue = getNotificationsSuccessfulValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Notifications 'Successful' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertNotificationsFailedValueMatch(String expectedValue) {
        String realValue = getNotificationsFailedValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Notifications 'Failed' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertNotificationsAvgTimeValueMatch(String expectedValue) {
        String realValue = getNotificationsAvgTimeValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Notifications 'Avg Time' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertNotificationsMinValueMatch(String expectedValue) {
        String realValue = getNotificationsMinValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Notifications 'Min' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertNotificationsMaxValueMatch(String expectedValue) {
        String realValue = getNotificationsMaxValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Notifications 'Max' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public StatisticsPanel<T> assertNotificationsTotalTimeValueMatch(String expectedValue) {
        String realValue = getNotificationsTotalTimeValue();
        if (!Objects.equals(expectedValue, realValue)) {
            throw new AssertionError("Notifications 'Total Time' value doesn't match, expected: " + expectedValue +
                    ", real: " + realValue);
        }
        return this;
    }

    public String getStatisticValueByFieldName(String fieldName) {
        if (getParentElement().$(Schrodinger.byDataId(fieldName)).exists()) {
            return getParentElement().$(Schrodinger.byDataId(fieldName)).text();
        }
        return null;
    }
}
