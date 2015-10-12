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

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

import static com.evolveum.midpoint.web.page.PageBase.createEnumResourceKey;
import static com.evolveum.midpoint.web.page.PageBase.createStringResourceStatic;

/**
 * @author mederly
 */
public class CertCampaignDto extends Selectable {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OWNER_NAME = "ownerName";
    public static final String F_CURRENT_STATE = "currentState";
    public static final String F_NUMBER_OF_STAGES = "numberOfStages";
    public static final String F_CAMPAIGN_START = "campaignStart";
    public static final String F_CAMPAIGN_END = "campaignEnd";
    public static final String F_STAGE_START = "stageStart";
    public static final String F_STAGE_END = "stageEnd";

    private AccessCertificationCampaignType campaign;           // TODO consider replacing this by constituent primitive data items
    private String ownerName;
    private String currentStateName;

    public CertCampaignDto(AccessCertificationCampaignType campaign, PageBase page, Task task, OperationResult result) {
        this.campaign = campaign;
        ownerName = resolveOwnerName(campaign.getOwnerRef(), page, task, result);
        currentStateName = resolveCurrentStateName(page);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public static String resolveOwnerName(ObjectReferenceType ownerRef, PageBase page, Task task, OperationResult result) {
        if (ownerRef == null) {
            return null;
        }
        PrismObject<? extends ObjectType> ownerObject = WebModelUtils.resolveReference(ownerRef, page, task, result);
        if (ownerObject == null) {
            return null;
        }
        ObjectType owner = ownerObject.asObjectable();
        if (owner instanceof UserType) {
            UserType user = (UserType) owner;
            return WebMiscUtil.getName(user) + " (" + WebMiscUtil.getOrigStringFromPoly(user.getFullName()) + ")";
        } else {
            return WebMiscUtil.getName(owner);
        }
    }

    public String getName() {
        return WebMiscUtil.getName(campaign);
    }

    public String getDescription() {
        return campaign.getDescription();
    }

    public String getCurrentState() {
        return currentStateName;
    }

    public int getNumberOfStages() {
        return CertCampaignTypeUtil.getNumberOfStages(campaign);
    }

    private String resolveCurrentStateName(PageBase page) {
        int stageNumber = campaign.getCurrentStageNumber();
        AccessCertificationCampaignStateType state = campaign.getState();
        switch (state) {
            case CREATED:
            case IN_REMEDIATION:
            case CLOSED:
                return createStringResourceStatic(page, state).getString();
            case IN_REVIEW_STAGE:
            case REVIEW_STAGE_DONE:
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
                String stageName = stage != null ? stage.getName() : null;
                String key = createEnumResourceKey(state) + "_FULL";
                return createStringResourceStatic(page, key, stageNumber, stageName).getString();
            default:
                return null;        // todo warning/error?
        }
    }

    public String getCampaignStart() {
        return WebMiscUtil.formatDate(campaign.getStart());
    }

    public String getCampaignEnd() {
        return WebMiscUtil.formatDate(campaign.getEnd());
    }

    public String getStageStart() {
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        return stage != null ? WebMiscUtil.formatDate(stage.getStart()) : null;
    }

    public String getStageEnd() {
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        return stage != null ? WebMiscUtil.formatDate(stage.getEnd()) : null;
    }

    public AccessCertificationCampaignStateType getState() {
        return campaign.getState();
    }

    public int getCurrentStageNumber() {
        return campaign.getCurrentStageNumber();
    }

    public String getHandlerUri() {
        return campaign.getHandlerUri();
    }
}
