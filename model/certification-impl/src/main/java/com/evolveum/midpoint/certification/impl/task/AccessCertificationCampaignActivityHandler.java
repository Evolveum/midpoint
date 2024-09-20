/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;

public abstract class AccessCertificationCampaignActivityHandler<WD extends AccessCertificationCampaignWorkDefinition, AH extends AccessCertificationCampaignActivityHandler<WD, AH>> extends ModelActivityHandler<WD, AH> {

    @Autowired private TaskManager taskManager;
    @Autowired private CertificationManagerImpl certificationManager;
    @Autowired private AccCertGeneralHelper helper;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired private AccCertReviewersHelper reviewersHelper;
    @Autowired private AccCertResponseComputationHelper computationHelper;
    @Autowired private AccCertUpdateHelper updateHelper;
    @Autowired private AccCertEventHelper eventHelper;
    @Autowired private AccCertCaseOperationsHelper caseHelper;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public CertificationManagerImpl getCertificationManager() {
        return certificationManager;
    }

    public AccCertGeneralHelper getHelper() {
        return helper;
    }

    public AccCertQueryHelper getQueryHelper() {
        return queryHelper;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public AccCertReviewersHelper getReviewersHelper() {
        return reviewersHelper;
    }

    public AccCertResponseComputationHelper getComputationHelper() {
        return computationHelper;
    }

    public AccCertUpdateHelper getUpdateHelper() {
        return updateHelper;
    }

    public AccCertEventHelper getEventHelper() {
        return eventHelper;
    }

    public AccCertCaseOperationsHelper getCaseHelper() {
        return caseHelper;
    }
}
