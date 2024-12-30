/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.helpers;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

public class CampaignStateHelper implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private static Map<AccessCertificationCampaignStateType, String> campaignStateClassMap = new HashMap<>();

    static {
        Map<AccessCertificationCampaignStateType, String> map = new HashMap<>();
        map.put(AccessCertificationCampaignStateType.CREATED, "colored-form-primary rounded");
        map.put(AccessCertificationCampaignStateType.IN_REVIEW_STAGE, "colored-form-info rounded");
        map.put(AccessCertificationCampaignStateType.IN_REMEDIATION, "colored-form-warning rounded");
        map.put(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE, "colored-form-success rounded");
        map.put(AccessCertificationCampaignStateType.CLOSED, "colored-form-secondary rounded");

        campaignStateClassMap = Collections.unmodifiableMap(map);
    }

    public enum CampaignActionConfirmation {
        START_CAMPAIGN_CONFIRMATION(new DisplayType()
                .singularLabel("PageCertCampaigns.message.startCampaignConfirmSingle")
                .pluralLabel("PageCertCampaigns.message.startCampaignConfirmMultiple")),
        OPEN_NEXT_STAGE_CONFIRMATION(new DisplayType()
                .singularLabel("PageCertCampaigns.message.startCampaignConfirmSingle")
                .pluralLabel("PageCertCampaigns.message.openNextStageConfirmMultiple")),
        CLOSE_STAGE_CONFIRMATION(new DisplayType()
                .singularLabel("PageCertCampaigns.message.closeStageConfirmSingle")
                .pluralLabel("PageCertCampaigns.message.closeStageConfirmMultiple")),
        START_REMEDIATION_CONFIRMATION(new DisplayType()
                .singularLabel("PageCertCampaigns.message.remediationConfirmSingle")
                .pluralLabel("PageCertCampaigns.message.remediationConfirmMultiple")),
        REITERATE_CAMPAIGN_CONFIRMATION(new DisplayType()
                .singularLabel("PageCertCampaigns.message.reiterateCampaignConfirmSingle")
                .pluralLabel("PageCertCampaigns.message.reiterateCampaignConfirmMultiple")),
        CLOSE_CAMPAIGN_CONFIRMATION(new DisplayType()
                .singularLabel("PageCertCampaigns.message.closeCampaignConfirmSingle")
                .pluralLabel("PageCertCampaigns.message.closeCampaignConfirmMultiple")),
        REMOVE_CAMPAIGN_CONFIRMATION(new DisplayType()
                .singularLabel("PageCertCampaigns.message.deleteCampaignConfirmSingle")
                .pluralLabel("PageCertCampaigns.message.deleteCampaignConfirmMultiple"));

        private final DisplayType confirmationDisplayType;

        CampaignActionConfirmation(DisplayType confirmationDisplayType) {
            this.confirmationDisplayType = confirmationDisplayType;
        }

        public String getSingleConfirmationMessageKey() {
            return confirmationDisplayType.getSingularLabel().getOrig();
        }

        public String getMultipleConfirmationMessageKey() {
            return confirmationDisplayType.getPluralLabel().getOrig();
        }
    }

    public enum CampaignAction {
        START_CAMPAIGN(new DisplayType()
                .label("CampaignAction.startCampaign")
                .cssClass("btn-primary")
                .icon(new IconType().cssClass("fa fa-play")),
                true,
                CampaignActionConfirmation.START_CAMPAIGN_CONFIRMATION),
        OPEN_NEXT_STAGE(new DisplayType()
                .label("CampaignAction.openNextStage")
                .cssClass("btn-primary")
                .icon(new IconType().cssClass("fa fa-play")),
                false,
                CampaignActionConfirmation.OPEN_NEXT_STAGE_CONFIRMATION),
        CLOSE_STAGE(new DisplayType()
                .label("CampaignAction.closeStage")
                .cssClass("btn-default")
                .icon(new IconType().cssClass("fa fa-regular fa-circle-xmark")),
                false,
                CampaignActionConfirmation.CLOSE_STAGE_CONFIRMATION),
        START_REMEDIATION(new DisplayType()
                .label("CampaignAction.startRemediation")
                .cssClass("btn-primary")
                .icon(new IconType().cssClass("fa fa-solid fa-badge-check")),
                false,
                CampaignActionConfirmation.START_REMEDIATION_CONFIRMATION),
        REITERATE_CAMPAIGN(new DisplayType()
                .label("CampaignAction.reiterateCampaign")
                .cssClass("btn-primary")
                .icon(new IconType().cssClass("fa fa-rotate-right")),
                true,
                CampaignActionConfirmation.REITERATE_CAMPAIGN_CONFIRMATION),
        CLOSE_CAMPAIGN(new DisplayType()
                .label("CampaignAction.closeCampaign")
                .cssClass("btn-default")
                .icon(new IconType().cssClass("fa fa-solid fa-circle-xmark")),
                true,
                CampaignActionConfirmation.CLOSE_CAMPAIGN_CONFIRMATION),
        REMOVE_CAMPAIGN(new DisplayType()
                .label("CampaignAction.removeCampaign")
                .cssClass("btn-danger")
                .icon(new IconType().cssClass("fa fa-minus-circle")),
                true,
                CampaignActionConfirmation.REMOVE_CAMPAIGN_CONFIRMATION);

        private final DisplayType displayType;
        private final boolean isBulkAction;
        private final CampaignActionConfirmation confirmation;

        CampaignAction(DisplayType displayType, boolean isBulkAction, CampaignActionConfirmation confirmation) {
            this.displayType = displayType;
            this.isBulkAction = isBulkAction;
            this.confirmation = confirmation;
        }

        public String getActionLabelKey() {
            return displayType.getLabel().getOrig();
        }

        public String getActionCssClass() {
            return displayType.getCssClass();
        }

        public IconType getActionIcon() {
            return displayType.getIcon();
        }

        public boolean isBulkAction() {
            return isBulkAction;
        }

        public CampaignActionConfirmation getConfirmation() {
            return confirmation;
        }
    }

    private static Map<AccessCertificationCampaignStateType, CampaignAction> campaignStateNextActionMap = new HashMap<>();

    static {
        Map<AccessCertificationCampaignStateType, CampaignAction> map = new HashMap<>();
        map.put(AccessCertificationCampaignStateType.CREATED, CampaignAction.START_CAMPAIGN);
        map.put(AccessCertificationCampaignStateType.IN_REVIEW_STAGE, CampaignAction.CLOSE_STAGE);
        map.put(AccessCertificationCampaignStateType.IN_REMEDIATION, CampaignAction.CLOSE_CAMPAIGN);    //todo is this correct?
        map.put(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE, CampaignAction.OPEN_NEXT_STAGE);
        map.put(AccessCertificationCampaignStateType.CLOSED, CampaignAction.REITERATE_CAMPAIGN);

        campaignStateNextActionMap = Collections.unmodifiableMap(map);
    }

    private static Map<AccessCertificationCampaignStateType, List<CampaignAction>> campaignStateAvailableActionsMap;

    static {
        Map<AccessCertificationCampaignStateType, List<CampaignAction>> map = new HashMap<>();
        map.put(AccessCertificationCampaignStateType.CREATED,
                Arrays.asList(CampaignAction.START_CAMPAIGN, CampaignAction.REMOVE_CAMPAIGN));
        map.put(AccessCertificationCampaignStateType.IN_REVIEW_STAGE,
                Arrays.asList(CampaignAction.CLOSE_STAGE, CampaignAction.REMOVE_CAMPAIGN));
        map.put(AccessCertificationCampaignStateType.IN_REMEDIATION,
                Arrays.asList(CampaignAction.CLOSE_STAGE, CampaignAction.REMOVE_CAMPAIGN));
        map.put(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE,
                Arrays.asList(CampaignAction.OPEN_NEXT_STAGE, CampaignAction.START_REMEDIATION,
                        CampaignAction.CLOSE_CAMPAIGN, CampaignAction.REMOVE_CAMPAIGN));
        map.put(AccessCertificationCampaignStateType.CLOSED,
                Arrays.asList(CampaignAction.REITERATE_CAMPAIGN, CampaignAction.REMOVE_CAMPAIGN));

        campaignStateAvailableActionsMap = Collections.unmodifiableMap(map);
    }

    private final AccessCertificationCampaignStateType campaignState;
//    private final AccessCertificationCampaignType campaign;
    private final int stageNumber;
    private final int stageDefinitionSize;

    public CampaignStateHelper(AccessCertificationCampaignType campaign) {
        this.campaignState = campaign.getState();
        this.stageNumber = or0(campaign.getStageNumber());
        this.stageDefinitionSize = campaign.getStageDefinition().size();
//        this.campaign = campaign;
    }

    public Badge createBadge() {
        Badge badge = new Badge(campaignStateClassMap.get(campaignState), LocalizationUtil.translateEnum(campaignState));
        badge.setTextCssClass("text-truncate text-sm");
        return badge;
    }

    public CampaignAction getNextAction() {
        if (AccessCertificationCampaignStateType.REVIEW_STAGE_DONE.equals(campaignState)) {
//            if (campaign.getStageNumber() == campaign.getStageDefinition().size()) {
//                return CampaignAction.CLOSE_CAMPAIGN;
//            }
            if (stageNumber == stageDefinitionSize) {
                return CampaignAction.START_REMEDIATION;
            }
        }
        return campaignStateNextActionMap.get(campaignState);
    }

    public String getNextActionKey() {
        return getNextAction().getActionLabelKey();
    }

    public List<CampaignAction> getAvailableActions() {
        List<CampaignAction> availableActions = campaignStateAvailableActionsMap.get(campaignState);
        if (availableActions == null) {
            return Collections.emptyList();
        }
        if (stageNumber == stageDefinitionSize) {
            return availableActions
                    .stream()
                    .filter(action -> action != CampaignAction.OPEN_NEXT_STAGE)
                    .toList();
        } else if (stageNumber < stageDefinitionSize) {
            return availableActions
                    .stream()
                    .filter(action -> action != CampaignAction.CLOSE_CAMPAIGN)
                    .toList();
        }
        return availableActions;
    }

    public static List<CampaignStateHelper.CampaignAction> getAllCampaignActions() {
        return Arrays.stream(CampaignAction.values()).sorted().toList();
    }
}
