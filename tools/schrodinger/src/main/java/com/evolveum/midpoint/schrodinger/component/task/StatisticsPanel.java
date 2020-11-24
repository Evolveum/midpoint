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

    public String getProvisioningOperationsOtherValue() {
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

    public String getStatisticValueByFieldName(String fieldName) {
        if (getParentElement().$(Schrodinger.byDataId(fieldName)).exists()) {
            return getParentElement().$(Schrodinger.byDataId(fieldName)).text();
        }
        return null;
    }
}
