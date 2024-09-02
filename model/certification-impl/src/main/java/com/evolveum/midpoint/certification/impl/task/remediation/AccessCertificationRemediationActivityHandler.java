/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.remediation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.certification.impl.task.AccessCertificationCampaignActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationRemediationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@Component
public class AccessCertificationRemediationActivityHandler
        extends AccessCertificationCampaignActivityHandler<AccessCertificationRemediationWorkDefinition, AccessCertificationRemediationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CERTIFICATION_REMEDIATION_TASK.value();

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
