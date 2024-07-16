/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Work definition for certification campaign remediation.
 */
public class AccessCertificationCampaignWorkDefinition extends AbstractWorkDefinition {

    private final @NotNull ObjectReferenceType campaignRef;

    public AccessCertificationCampaignWorkDefinition(@NotNull WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        AbstractCertificationWorkDefinitionType campaign = (AbstractCertificationWorkDefinitionType) info.getBean();
        this.campaignRef = MiscUtil.configNonNull(campaign.getCertificationCampaignRef(), () -> "No campaign");
    }

    public @NotNull ObjectReferenceType getCertificationCampaignRef() {
        return this.campaignRef;
    }

    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "campaignRef", String.valueOf(this.campaignRef), indent + 1);
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException {
        return AffectedObjectsInformation.ObjectSet.repository(
                new BasicObjectSetType()
                        .type(AccessCertificationCampaignType.COMPLEX_TYPE)
                        .objectRef(campaignRef.getOid(), campaignRef.getType()));
    }
}
