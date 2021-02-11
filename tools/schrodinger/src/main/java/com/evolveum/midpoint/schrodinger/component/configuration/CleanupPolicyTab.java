package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.component.common.PrismContainerPanel;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

public class CleanupPolicyTab extends TabWithContainerWrapper<SystemPage> {

    public CleanupPolicyTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public CleanupPolicyTab auditRecordsCleanupInterval(String interval) {
        setCleanupIntervalValueByContainerResourceKey("Audit records", interval);
        return this;
    }

    public CleanupPolicyTab auditRecordsMaxRecordsToKeep(String maxRecordsToKeep) {
        setMaxRecordsToKeepValueByContainerResourceKey("Audit records", maxRecordsToKeep);
        return this;
    }

    public String getAuditRecordsCleanupInterval() {
        return getCleanupIntervalValueByContainerResourceKey("Audit records");
    }

    public String getAuditRecordsMaxRecordsToKeep() {
        return getMaxRecordsToKeepValueByContainerResourceKey("Audit records");
    }

    public CleanupPolicyTab closedCertificationCampaignsCleanupInterval(String interval) {
        setCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.closedCertificationCampaigns", interval);
        return this;
    }

    public CleanupPolicyTab closedCertificationMaxRecordsToKeep(String maxRecordsToKeep) {
        setMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.closedCertificationCampaigns", maxRecordsToKeep);
        return this;
    }

    public String getClosedCertificationCampaignsCleanupInterval() {
        return getCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.closedCertificationCampaigns");
    }

    public String getClosedCertificationCampaignsMaxRecordsToKeep() {
        return getMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.closedCertificationCampaigns");
    }

    public CleanupPolicyTab closedTasksCleanupInterval(String interval) {
        setCleanupIntervalValueByContainerResourceKey("Closed tasks", interval);
        return this;
    }

    public CleanupPolicyTab closedTasksMaxRecordsToKeep(String maxRecordsToKeep) {
        setMaxRecordsToKeepValueByContainerResourceKey("Closed tasks", maxRecordsToKeep);
        return this;
    }

    public String getClosedTasksCleanupInterval() {
        return getCleanupIntervalValueByContainerResourceKey("Closed tasks");
    }

    public String getClosedTasksMaxRecordsToKeep() {
        return getMaxRecordsToKeepValueByContainerResourceKey("Closed tasks");
    }

    public CleanupPolicyTab closedCasesCleanupInterval(String interval) {
        setCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.closedCases", interval);
        return this;
    }

    public CleanupPolicyTab closedCasesMaxRecordsToKeep(String maxRecordsToKeep) {
        setMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.closedCases", maxRecordsToKeep);
        return this;
    }

    public String getClosedCasesCleanupInterval() {
        return getCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.closedCases");
    }

    public String getClosedCasesMaxRecordsToKeep() {
        return getMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.closedCases");
    }

    public CleanupPolicyTab outputReportsCleanupInterval(String interval) {
        setCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.outputReports", interval);
        return this;
    }

    public CleanupPolicyTab outputReportsMaxRecordsToKeep(String maxRecordsToKeep) {
        setMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.outputReports", maxRecordsToKeep);
        return this;
    }

    public String getOutputReportsCleanupInterval() {
        return getCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.outputReports");
    }

    public String getOutputReportsMaxRecordsToKeep() {
        return getMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.outputReports");
    }

    public CleanupPolicyTab objectResultsCleanupInterval(String interval) {
        setCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.objectResults", interval);
        return this;
    }

    public CleanupPolicyTab objectResultsMaxRecordsToKeep(String maxRecordsToKeep) {
        setMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.objectResults", maxRecordsToKeep);
        return this;
    }

    public String getObjectResultsCleanupInterval() {
        return getCleanupIntervalValueByContainerResourceKey("CleanupPoliciesType.objectResults");
    }

    public String getObjectResultsMaxRecordsToKeep() {
        return getMaxRecordsToKeepValueByContainerResourceKey("CleanupPoliciesType.objectResults");
    }

    private void setCleanupIntervalValueByContainerResourceKey(String containerResourceKey, String interval) {
        getContainerFormPanel(containerResourceKey)
                .showEmptyAttributes(containerResourceKey)
                .addAttributeValue("Cleanup interval", interval);
    }

    private String getCleanupIntervalValueByContainerResourceKey(String containerResourceKey) {
        return getContainerFormPanel(containerResourceKey)
                .findProperty("Cleanup interval")
                    .getText();
    }

    private String getMaxRecordsToKeepValueByContainerResourceKey(String containerResourceKey) {
        return getContainerFormPanel(containerResourceKey)
                .findProperty("Max records to keep")
                    .getText();
    }

    private void setMaxRecordsToKeepValueByContainerResourceKey(String containerResourceKey, String maxRecordsToKeep) {
        getContainerFormPanel(containerResourceKey)
                .showEmptyAttributes(containerResourceKey)
                .addAttributeValue("Max records to keep", maxRecordsToKeep);
    }

    private PrismForm<PrismContainerPanel<PrismForm<TabWithContainerWrapper<SystemPage>>>> getContainerFormPanel(String containerResourceKey) {
        return form()
                .expandContainerPropertiesPanel("Cleanup policy")
                .expandContainerPropertiesPanel(containerResourceKey)
                    .getPrismContainerPanelByResourceKey(containerResourceKey)
                        .getContainerFormFragment();
    }
}
