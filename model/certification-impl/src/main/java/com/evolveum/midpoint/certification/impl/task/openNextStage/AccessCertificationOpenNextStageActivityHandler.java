/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.openNextStage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.certification.impl.task.AccessCertificationCampaignActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationOpenNextStageWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@Component
public class AccessCertificationOpenNextStageActivityHandler
        extends AccessCertificationCampaignActivityHandler<AccessCertificationOpenNextStageWorkDefinition, AccessCertificationOpenNextStageActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_OPEN_NEXT_STAGE_TASK.value();

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
