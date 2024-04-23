/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CampaignStateHelper implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private static Map<AccessCertificationCampaignStateType, String> campaignStateClassMap = new HashMap<>();

    static {
        Map<AccessCertificationCampaignStateType, String> map = new HashMap<>();
        map.put(AccessCertificationCampaignStateType.CREATED, Badge.State.PRIMARY.getCss());
        map.put(AccessCertificationCampaignStateType.IN_REVIEW_STAGE, Badge.State.INFO.getCss());
        map.put(AccessCertificationCampaignStateType.IN_REMEDIATION, Badge.State.WARNING.getCss());
        map.put(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE, Badge.State.SUCCESS.getCss());
        map.put(AccessCertificationCampaignStateType.CLOSED, Badge.State.SECONDARY.getCss());

        campaignStateClassMap = Collections.unmodifiableMap(map);
    }

    public static enum CampaignAction {
        START_CAMPAIGN(new DisplayType()
                .label("CampaignAction.startCampaign")
                .cssClass("btn-primary")
                .icon(new IconType().cssClass("fa fa-play"))),
        CLOSE_STAGE(new DisplayType()
                .label("CampaignAction.closeStage")
                .cssClass("btn-secondary")
                .icon(new IconType().cssClass("fa fa-regular fa-circle-xmark"))),
        START_REMEDIATION(new DisplayType()
                .label("CampaignAction.startRemediation")
                .cssClass("btn-warning")
                .icon(new IconType().cssClass("fa fa-solid fa-badge-check"))),
        CLOSE_CAMPAIGN(new DisplayType()
                .label("CampaignAction.closeCampaign")
                .cssClass("btn-secondary")
                .icon(new IconType().cssClass("fa fa-solid fa-circle-xmark")));

        private DisplayType displayType;

        CampaignAction(DisplayType displayType) {
            this.displayType = displayType;
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
    }

    private static Map<AccessCertificationCampaignStateType, CampaignAction> campaignStateNextActionMap = new HashMap<>();

    static {
        Map<AccessCertificationCampaignStateType, CampaignAction> map = new HashMap<>();
        map.put(AccessCertificationCampaignStateType.CREATED, CampaignAction.START_CAMPAIGN);
        map.put(AccessCertificationCampaignStateType.IN_REVIEW_STAGE, CampaignAction.CLOSE_STAGE);
        map.put(AccessCertificationCampaignStateType.IN_REMEDIATION, CampaignAction.CLOSE_CAMPAIGN);
        map.put(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE, CampaignAction.CLOSE_STAGE);
        map.put(AccessCertificationCampaignStateType.CLOSED, CampaignAction.CLOSE_CAMPAIGN);

        campaignStateNextActionMap = Collections.unmodifiableMap(map);
    }

    private final AccessCertificationCampaignStateType campaignState;

    public CampaignStateHelper(AccessCertificationCampaignStateType campaignState) {
        this.campaignState = campaignState;
    }

    public Badge createBadge() {
        return new Badge(campaignStateClassMap.get(campaignState), LocalizationUtil.translateEnum(campaignState));
    }

    public CampaignAction getNextAction() {
        return campaignStateNextActionMap.get(campaignState);
    }

    public String getNextActionKey() {
        return getNextAction().getActionLabelKey();
    }
}
