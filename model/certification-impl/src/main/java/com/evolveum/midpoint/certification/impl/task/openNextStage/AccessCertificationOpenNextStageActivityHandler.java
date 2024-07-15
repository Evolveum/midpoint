/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.openNextStage;

import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
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
public class AccessCertificationOpenNextStageActivityHandler
        extends ModelActivityHandler<AccessCertificationOpenNextStageWorkDefinition, AccessCertificationOpenNextStageActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_OPEN_NEXT_STAGE_TASK.value();

    @Autowired private TaskManager taskManager;
    @Autowired private CertificationManagerImpl certificationManager;
    @Autowired private AccCertGeneralHelper helper;
    @Autowired private AccCertQueryHelper queryHelper;
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

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CertificationOpenNextStageWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_CERTIFICATION_OPEN_NEXT_STAGE,
                AccessCertificationOpenNextStageWorkDefinition.class, AccessCertificationOpenNextStageWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                CertificationOpenNextStageWorkDefinitionType.COMPLEX_TYPE, AccessCertificationOpenNextStageWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<AccessCertificationOpenNextStageWorkDefinition, AccessCertificationOpenNextStageActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationOpenNextStageWorkDefinition, AccessCertificationOpenNextStageActivityHandler> context,
            @NotNull OperationResult result) {
        return new AccessCertificationOpenNextStageRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "certification-next-stage";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
