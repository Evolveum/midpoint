/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.reiterateCampaign;

import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.certification.impl.task.AccessCertificationCampaignActivityHandler;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationOpenNextStageWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationReiterateCampaignWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AccessCertificationReiterateCampaignActivityHandler
        extends AccessCertificationCampaignActivityHandler<AccessCertificationReiterateCampaignWorkDefinition, AccessCertificationReiterateCampaignActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_REITERATE_CAMPAIGN_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CertificationReiterateCampaignWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_CERTIFICATION_REITERATE_CAMPAIGN,
                AccessCertificationReiterateCampaignWorkDefinition.class, AccessCertificationReiterateCampaignWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                CertificationReiterateCampaignWorkDefinitionType.COMPLEX_TYPE, AccessCertificationReiterateCampaignWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<AccessCertificationReiterateCampaignWorkDefinition, AccessCertificationReiterateCampaignActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationReiterateCampaignWorkDefinition, AccessCertificationReiterateCampaignActivityHandler> context,
            @NotNull OperationResult result) {
        return new AccessCertificationReiterateCampaignRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "certification-reiterate-campaign";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
