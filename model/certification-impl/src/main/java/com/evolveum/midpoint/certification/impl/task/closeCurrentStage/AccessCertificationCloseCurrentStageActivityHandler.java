/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.closeCurrentStage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.certification.impl.task.AccessCertificationCampaignActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationCloseCurrentStageWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@Component
public class AccessCertificationCloseCurrentStageActivityHandler
        extends AccessCertificationCampaignActivityHandler<AccessCertificationCloseCurrentStageWorkDefinition, AccessCertificationCloseCurrentStageActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_CLOSE_CURRENT_STAGE_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CertificationCloseCurrentStageWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_CERTIFICATION_CLOSE_CURRENT_STAGE,
                AccessCertificationCloseCurrentStageWorkDefinition.class, AccessCertificationCloseCurrentStageWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                CertificationCloseCurrentStageWorkDefinitionType.COMPLEX_TYPE, AccessCertificationCloseCurrentStageWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<AccessCertificationCloseCurrentStageWorkDefinition, AccessCertificationCloseCurrentStageActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationCloseCurrentStageWorkDefinition, AccessCertificationCloseCurrentStageActivityHandler> context,
            @NotNull OperationResult result) {
        return new AccessCertificationCloseCurrentStageRun(context);
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
