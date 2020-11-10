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

    public String getProvisioningOperationsFailValue() {
        return getStatisticValueByFieldName("Provisioning.GetFailure");
    }

    /* continue for fields
     * <td><span wicket:id="Provisioning.SearchSuccess"/></td>
                            <td><span wicket:id="Provisioning.SearchFailure"/></td>
                            <td><span wicket:id="Provisioning.CreateSuccess"/></td>
                            <td><span wicket:id="Provisioning.CreateFailure"/></td>
                            <td><span wicket:id="Provisioning.UpdateSuccess"/></td>
                            <td><span wicket:id="Provisioning.UpdateFailure"/></td>
                            <td><span wicket:id="Provisioning.DeleteSuccess"/></td>
                            <td><span wicket:id="Provisioning.DeleteFailure"/></td>
                            <td><span wicket:id="Provisioning.SyncSuccess"/></td>
                            <td><span wicket:id="Provisioning.SyncFailure"/></td>
                            <td><span wicket:id="Provisioning.ScriptSuccess"/></td>
                            <td><span wicket:id="Provisioning.ScriptFailure"/></td>
                            <td><span wicket:id="Provisioning.OtherSuccess"/></td>
                            <td><span wicket:id="Provisioning.OtherFailure"/></td>
                            <td><span wicket:id="Provisioning.TotalOperationsCount"/></td>
                            <td><span wicket:id="Provisioning.AverageTime"/></td>
                            <td><span wicket:id="Provisioning.MinTime"/></td>
                            <td><span wicket:id="Provisioning.MaxTime"/></td>
                            <td><span wicket:id="Provisioning.TotalTime"/></td>
     *
     */

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
