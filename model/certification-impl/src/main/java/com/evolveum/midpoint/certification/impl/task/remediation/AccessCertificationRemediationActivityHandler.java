/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.remediation;


import com.evolveum.midpoint.certification.impl.AccCertCaseOperationsHelper;
import com.evolveum.midpoint.certification.impl.AccCertGeneralHelper;
import com.evolveum.midpoint.certification.impl.AccCertQueryHelper;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class AccessCertificationRemediationActivityHandler
        extends ModelActivityHandler<AccessCertificationRemediationWorkDefinition, AccessCertificationRemediationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_REMEDIATION_TASK.value();

    @Autowired private TaskManager taskManager;
    @Autowired private CertificationManagerImpl certificationManager;
    @Autowired private AccCertGeneralHelper helper;
    @Autowired private AccCertCaseOperationsHelper caseHelper;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired public LocalizationService localizationService;

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

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CertificationRemediationWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_CERTIFICATION_REMEDIATION,
                AccessCertificationRemediationWorkDefinition.class, AccessCertificationRemediationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                CertificationRemediationWorkDefinitionType.COMPLEX_TYPE, AccessCertificationRemediationWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<AccessCertificationRemediationWorkDefinition, AccessCertificationRemediationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationRemediationWorkDefinition, AccessCertificationRemediationActivityHandler> context,
            @NotNull OperationResult result) {
        return new AccessCertificationRemediationRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "certification-remediation";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
