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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRemediationStyleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;

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

    public static AccessCertificationStageDefinitionType findStageDefinition(AccessCertificationDefinitionType definition, int stageNumber) {
        for (AccessCertificationStageDefinitionType stage : definition.getStage()) {
            if (stage.getNumber() == stageNumber) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " in " + ObjectTypeUtil.toShortString(definition));
    }

    // campaign should have a definition included
    public static int getNumberOfStages(AccessCertificationCampaignType campaign) {
        return getDefinition(campaign).getStage().size();
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

    public static int getCurrentStageNumber(AccessCertificationCampaignType campaign) {
        if (campaign.getCurrentStageNumber() == null) {
            return 0;
        } else {
            return campaign.getCurrentStageNumber();
        }
    }

    public static boolean isRemediationAutomatic(AccessCertificationDefinitionType definition) {
        return definition.getRemediation() != null &&
                AccessCertificationRemediationStyleType.AUTOMATED.equals(definition.getRemediation().getStyle());
    }

    public static boolean isCampaignClosed(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition) {
        int currentStage = campaign.getCurrentStageNumber();
        int stages = definition.getStage().size();
        return currentStage > stages;
    }
}
