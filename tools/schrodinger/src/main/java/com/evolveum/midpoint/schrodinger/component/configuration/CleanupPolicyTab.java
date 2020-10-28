package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

public class CleanupPolicyTab extends TabWithContainerWrapper<SystemPage> {

    public CleanupPolicyTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public void auditRecordsCleanupInterval(String interval) {
        // todo implement
    }

    public String auditRecordsCleanupInterval() {
        // todo implement
        return null;
    }

    public void closedCertificationCampaignsCleanupInterval(String interval) {
        // todo implement
    }

    public String closedCertificationCampaignsCleanupInterval() {
        // todo implement
        return null;
    }

    public void operationExecutionResultsCleanupInterval(String interval) {
        // todo implement
    }

    public String operationExecutionResultsCleanupInterval() {
        // todo implement
        return null;
    }
}
