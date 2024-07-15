/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.nextStage;

import com.evolveum.midpoint.repo.common.activity.definition.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Work definition for certification campaign creation.
 */
public class AccessCertificationNextStageWorkDefinition extends AbstractWorkDefinition {

//    private final @NotNull ObjectReferenceType campaignDefRef;

    AccessCertificationNextStageWorkDefinition(@NotNull WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
//        CertificationCampaignCreationWorkDefinitionType campaignDef = (CertificationCampaignCreationWorkDefinitionType) info.getBean();
//        this.campaignDefRef = MiscUtil.configNonNull(campaignDef.getCertificationCampaignDefinitionRef(), () -> "No campaign definition");
    }

//    public @NotNull ObjectReferenceType getCertificationCampaignDefRef() {
//        return this.campaignDefRef;
//    }
//
    protected void debugDumpContent(StringBuilder sb, int indent) {
//        DebugUtil.debugDumpWithLabel(sb, "campaignDefRef", String.valueOf(this.campaignDefRef), indent + 1);
    }

//    @Override
//    public @NotNull AffectedObjectsInformation getAffectedObjectSetInformation() throws SchemaException, ConfigurationException {
////        return AffectedObjectsInformation.ObjectSet.repository(
////                new BasicObjectSetType()
////                        .type(AccessCertificationDefinitionType.COMPLEX_TYPE)
////                        .objectRef(campaignDefRef.getOid(), campaignDefRef.getType()));
//
//        List<AffectedObjectsInformation> informationForChildren = new ArrayList<>();
//            var childDefinition = AffectedObjectsInformation.ObjectSet.repository(
//                    new BasicObjectSetType()
//                            .type(AccessCertificationDefinitionType.COMPLEX_TYPE)
//                            .objectRef(campaignDefRef.getOid(), campaignDefRef.getType()));
//            informationForChildren.add(childDefinition);
//        return AffectedObjectsInformation.complex(informationForChildren);
//    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException {
        return AffectedObjectsInformation.ObjectSet.notSupported(); // not easily determinable nor describable;
    }

    @Override
    public @NotNull List<AffectedObjectsInformation.ObjectSet> getListOfAffectedObjectSetInformation(
            @Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException {
        List<AffectedObjectsInformation.ObjectSet> list = new ArrayList<>();
//        list.add( AffectedObjectsInformation.ObjectSet.repository(
//                new BasicObjectSetType()
//                        .type(AccessCertificationDefinitionType.COMPLEX_TYPE)
//                        .objectRef(campaignDefRef.getOid(), campaignDefRef.getType())));
//        if (state instanceof CertificationCampaignCreationWorkStateType certState) {
//            list.add( AffectedObjectsInformation.ObjectSet.repository(
//                    new BasicObjectSetType()
//                            .type(AccessCertificationCampaignType.COMPLEX_TYPE)
//                            .objectRef(certState.getCreatedCampaignRef().clone())));
//        }
        return list;
    }
}
