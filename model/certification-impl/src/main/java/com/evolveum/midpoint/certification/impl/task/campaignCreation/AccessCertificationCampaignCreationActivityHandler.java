/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.campaignCreation;

import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.model.impl.lens.tasks.TaskOperationalDataManager;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AccessCertificationCampaignCreationActivityHandler
        extends ModelActivityHandler<AccessCertificationCampaignCreationWorkDefinition, AccessCertificationCampaignCreationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_CAMPAIGN_CREATION_TASK.value();

    @Autowired private TaskManager taskManager;
    @Autowired private CertificationManagerImpl certificationManager;
    @Autowired private AccCertGeneralHelper helper;
    @Autowired private AccCertCaseOperationsHelper caseHelper;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private AccCertOpenerHelper openerHelper;
    @Autowired private TaskOperationalDataManager taskOperationalDataManager;

    public AccCertOpenerHelper getOpenerHelper() {
        return openerHelper;
    }

    public TaskOperationalDataManager getTaskOperationalDataManager() {
        return taskOperationalDataManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public CertificationManagerImpl getCertificationManager() {
        return certificationManager;
    }

    public AccCertGeneralHelper getHelper() {
        return helper;
    }

    public AccCertCaseOperationsHelper getCaseHelper() {
        return caseHelper;
    }

    public AccCertQueryHelper getQueryHelper() {
        return queryHelper;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CertificationCampaignCreationWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_CERTIFICATION_CAMPAIGN_CREATION,
                AccessCertificationCampaignCreationWorkDefinition.class, AccessCertificationCampaignCreationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                CertificationCampaignCreationWorkDefinitionType.COMPLEX_TYPE, AccessCertificationCampaignCreationWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<AccessCertificationCampaignCreationWorkDefinition, AccessCertificationCampaignCreationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationCampaignCreationWorkDefinition, AccessCertificationCampaignCreationActivityHandler> context,
            @NotNull OperationResult result) {
        return new AccessCertificationCampaignCreationRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "certification-campaign-creation";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(CertificationCampaignCreationWorkStateType.COMPLEX_TYPE);
    }
}
