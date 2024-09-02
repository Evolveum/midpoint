/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.closeCurrentStage;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.certification.impl.task.AccessCertificationCampaignWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Work definition for certification campaign remediation.
 */
public final class AccessCertificationCloseCurrentStageWorkDefinition extends AccessCertificationCampaignWorkDefinition {

    public AccessCertificationCloseCurrentStageWorkDefinition(@NotNull WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
    }
}
