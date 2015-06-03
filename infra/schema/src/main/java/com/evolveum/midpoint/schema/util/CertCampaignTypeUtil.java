/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRemediationStyleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;

import java.util.List;

/**
 * @author mederly
 */
public class CertCampaignTypeUtil {

    public static AccessCertificationStageType getCurrentStage(AccessCertificationCampaignType campaign) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (stage.getNumber() == campaign.getCurrentStageNumber()) {
                return stage;
            }
        }
        return null;
    }

    public static AccessCertificationStageDefinitionType findStageDefinition(AccessCertificationCampaignType campaign, int stageNumber) {
        for (AccessCertificationStageDefinitionType stage : campaign.getStageDefinition()) {
            if (stage.getNumber() == stageNumber) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " in " + ObjectTypeUtil.toShortString(campaign));
    }

    public static AccessCertificationStageType findStage(AccessCertificationCampaignType campaign, int stageNumber) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (stage.getNumber() == stageNumber) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " in " + ObjectTypeUtil.toShortString(campaign));
    }

    public static AccessCertificationCaseType findCase(AccessCertificationCampaignType campaign, long caseId) {
        for (AccessCertificationCaseType _case : campaign.getCase()) {
            if (_case.asPrismContainerValue().getId() != null && _case.asPrismContainerValue().getId() == caseId) {
                return _case;
            }
        }
        return null;
    }

    public static AccessCertificationDecisionType findDecision(AccessCertificationCaseType _case, int stageNumber, String reviewerOid) {
        for (AccessCertificationDecisionType d : _case.getDecision()) {
            if (d.getStageNumber() == stageNumber && d.getReviewerRef().getOid().equals(reviewerOid)) {
                return d;
            }
        }
        return null;
    }

    public static int getNumberOfStages(AccessCertificationCampaignType campaign) {
        return campaign.getStageDefinition().size();
    }

    public static AccessCertificationDefinitionType getDefinition(AccessCertificationCampaignType campaign) {
        if (campaign.getDefinitionRef() == null) {
            throw new IllegalStateException("No definition reference in " + ObjectTypeUtil.toShortString(campaign));
        }
        PrismReferenceValue referenceValue = campaign.getDefinitionRef().asReferenceValue();
        if (referenceValue.getObject() == null) {
            throw new IllegalStateException("No definition object in " + ObjectTypeUtil.toShortString(campaign));
        }
        return (AccessCertificationDefinitionType) (referenceValue.getObject().asObjectable());
    }

    public static boolean isRemediationAutomatic(AccessCertificationCampaignType campaign) {
        return campaign.getRemediationDefinition() != null &&
                AccessCertificationRemediationStyleType.AUTOMATED.equals(campaign.getRemediationDefinition().getStyle());
    }

    public static boolean isCampaignClosed(AccessCertificationCampaignType campaign) {
        int currentStage = campaign.getCurrentStageNumber();
        int stages = getNumberOfStages(campaign);
        return AccessCertificationCampaignStateType.CLOSED.equals(campaign.getState()) || currentStage > stages;
    }

    // TODO rework signalling problems
    public static void checkStageDefinitionConsistency(List<AccessCertificationStageDefinitionType> stages) {
        int count = stages.size();
        boolean[] numberPresent = new boolean[count];
        for (AccessCertificationStageDefinitionType stage : stages) {
            int num = stage.getNumber();
            if (num < 1 || num > count) {
                throw new IllegalStateException("Invalid stage number: " + num + " (stage count: " + count +")");
            }
            if (numberPresent[num-1]) {
                throw new IllegalStateException("Stage with number " + num + " is defined multiple times");
            }
            numberPresent[num-1] = true;
        }
        // maybe redundant test
        for (int i = 0; i < numberPresent.length; i++) {
            if (!numberPresent[i]) {
                throw new IllegalStateException("Stage with number " + (i+1) + " was not defined");
            }
        }
    }
}
