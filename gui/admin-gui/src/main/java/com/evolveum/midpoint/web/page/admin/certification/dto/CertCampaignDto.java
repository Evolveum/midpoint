/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.gui.api.page.PageBase.createEnumResourceKey;
import static com.evolveum.midpoint.gui.api.page.PageBase.createStringResourceStatic;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.util.MiscUtil.or0;

public class CertCampaignDto extends Selectable {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OWNER_NAME = "ownerName";
    public static final String F_CURRENT_STATE = "currentState";
    public static final String F_ITERATION = "iteration";
    public static final String F_NUMBER_OF_STAGES = "numberOfStages";
    public static final String F_CAMPAIGN_START = "campaignStart";
    public static final String F_CAMPAIGN_END = "campaignEnd";
    public static final String F_STAGE_START = "stageStart";
    public static final String F_STAGE_DEADLINE = "stageDeadline";
    public static final String F_ESCALATION_LEVEL_INFO = "escalationLevelInfo";

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
        PrismObject<? extends ObjectType> ownerObject = WebModelServiceUtils.resolveReferenceNoFetch(ownerRef, page, task, result);
        if (ownerObject == null) {
            return null;
        }
        ObjectType owner = ownerObject.asObjectable();
        if (owner instanceof UserType) {
            UserType user = (UserType) owner;
            return WebComponentUtil.getName(user) + " (" + WebComponentUtil.getOrigStringFromPoly(user.getFullName()) + ")";
        } else {
            return WebComponentUtil.getName(owner);
        }
    }

    public String getName() {
        return WebComponentUtil.getName(campaign);
    }

    public String getDescription() {
        return campaign.getDescription();
    }

    public String getCurrentState() {
        return currentStateName;
    }

    public Integer getIteration() {
        return norm(campaign.getIteration());
    }

    public int getNumberOfStages() {
        return CertCampaignTypeUtil.getNumberOfStages(campaign);
    }

    private String resolveCurrentStateName(PageBase page) {
        int stageNumber = or0(campaign.getStageNumber());
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
                if (stageName != null) {
                    String key = createEnumResourceKey(state) + "_FULL";
                    return createStringResourceStatic(key, stageNumber, stageName).getString();
                } else {
                    String key = createEnumResourceKey(state);
                    return createStringResourceStatic(key).getString() + " " + stageNumber;
                }
            default:
                return null;        // todo warning/error?
        }
    }

    public String getCampaignStart() {
        return WebComponentUtil.formatDate(campaign.getStartTimestamp());
    }

    public String getCampaignEnd() {
        return WebComponentUtil.formatDate(campaign.getEndTimestamp());
    }

    public String getStageStart() {
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        return stage != null ? WebComponentUtil.formatDate(stage.getStartTimestamp()) : null;
    }

    public String getStageDeadline() {
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        return stage != null ? WebComponentUtil.formatDate(stage.getDeadline()) : null;
    }

    public String getStageEnd() {
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        return stage != null ? WebComponentUtil.formatDate(stage.getEndTimestamp()) : null;
    }

    public AccessCertificationCampaignStateType getState() {
        return campaign.getState();
    }

    public int getCurrentStageNumber() {
        return or0(campaign.getStageNumber());
    }

    public String getHandlerUri() {
        return campaign.getHandlerUri();
    }

    public String getEscalationLevelInfo() {
        return CertCampaignTypeUtil.getEscalationLevelInfo(campaign);
    }
}
