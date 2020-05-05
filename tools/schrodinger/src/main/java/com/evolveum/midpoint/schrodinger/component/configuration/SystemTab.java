/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SystemTab extends TabWithContainerWrapper<SystemTab, SystemPage> {

    public SystemTab(SystemPage parent, SelenideElement parentElement) {
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

