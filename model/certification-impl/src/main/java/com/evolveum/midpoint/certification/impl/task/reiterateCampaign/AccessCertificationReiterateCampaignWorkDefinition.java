/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl.task.reiterateCampaign;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.certification.impl.task.AccessCertificationCampaignWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Work definition for certification campaign remediation.
 */
public final class AccessCertificationReiterateCampaignWorkDefinition extends AccessCertificationCampaignWorkDefinition {

    public AccessCertificationReiterateCampaignWorkDefinition(@NotNull WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
    }
}
