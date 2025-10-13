/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl.task.startCampaign;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.certification.impl.task.AccessCertificationCampaignActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationStartCampaignWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@Component
public class AccessCertificationStartCampaignActivityHandler
        extends AccessCertificationCampaignActivityHandler<AccessCertificationStartCampaignWorkDefinition, AccessCertificationStartCampaignActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_START_CAMPAIGN_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CertificationStartCampaignWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_CERTIFICATION_START_CAMPAIGN,
                AccessCertificationStartCampaignWorkDefinition.class, AccessCertificationStartCampaignWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                CertificationStartCampaignWorkDefinitionType.COMPLEX_TYPE, AccessCertificationStartCampaignWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<AccessCertificationStartCampaignWorkDefinition, AccessCertificationStartCampaignActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationStartCampaignWorkDefinition, AccessCertificationStartCampaignActivityHandler> context,
            @NotNull OperationResult result) {
        return new AccessCertificationStartCampaignRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "certification-first-stage";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

}
