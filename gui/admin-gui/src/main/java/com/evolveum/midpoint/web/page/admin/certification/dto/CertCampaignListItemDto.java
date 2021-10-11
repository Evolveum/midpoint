/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

/**
 * @author mederly
 */
public class CertCampaignListItemDto extends Selectable implements InlineMenuable {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_ITERATION = "iteration";
    public static final String F_STATE = "state";
    public static final String F_CURRENT_STAGE_NUMBER = "currentStageNumber";
    public static final String F_NUMBER_OF_STAGES = "numberOfStages";
    public static final String F_DEADLINE_AS_STRING = "deadlineAsString";
    public static final String F_ESCALATION_LEVEL_NUMBER = "escalationLevelNumber";

    @NotNull private final AccessCertificationCampaignType campaign;           // TODO replace by elementary items
    private List<InlineMenuItem> menuItems;
    private String deadlineAsString;

    CertCampaignListItemDto(@NotNull AccessCertificationCampaignType campaign, PageBase page) {
        this.campaign = campaign;
        deadlineAsString = computeDeadlineAsString(page);
    }

    @Override
    public List<InlineMenuItem> getMenuItems() {
        if (menuItems == null) {
            menuItems = new ArrayList<>();
        }
        return menuItems;
    }

    public String getName() {
        return WebComponentUtil.getName(campaign);
    }

    public String getOid() {
        return campaign.getOid();
    }

    public AccessCertificationCampaignStateType getState() {
        return campaign.getState();
    }

    public Integer getIteration() {
        return norm(campaign.getIteration());
    }

    public String getDescription() {
        return campaign.getDescription();
    }

    public Integer getCurrentStageNumber() {
        int currentStage = campaign.getStageNumber();
        if (AccessCertificationCampaignStateType.IN_REVIEW_STAGE.equals(campaign.getState()) ||
                AccessCertificationCampaignStateType.REVIEW_STAGE_DONE.equals(campaign.getState())) {
            return currentStage;
        } else {
            return null;
        }
    }

    public Integer getEscalationLevelNumber() {
        int n = CertCampaignTypeUtil.getCurrentStageEscalationLevelNumberSafe(campaign);
        return n != 0 ? n : null;
    }

    public Integer getNumberOfStages() {
        return CertCampaignTypeUtil.getNumberOfStages(campaign);
    }

    private String computeDeadlineAsString(PageBase page) {
        AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
        XMLGregorianCalendar end;
        Boolean stageLevelInfo;
        if (campaign.getStageNumber() == 0) {
            end = campaign.getEndTimestamp();            // quite useless, as "end" denotes real campaign end
            stageLevelInfo = false;
        } else if (currentStage != null) {
            end = currentStage.getDeadline();
            stageLevelInfo = true;
        } else {
            end = null;
            stageLevelInfo = null;
        }

        if (end == null) {
            return "";
        } else {
            long delta = XmlTypeConverter.toMillis(end) - System.currentTimeMillis();

            // round to hours; we always round down
            long precision = 3600000L;      // 1 hour
            if (Math.abs(delta) > precision) {
                delta = (delta / precision) * precision;
            }

            if (delta > 0) {
                String key = stageLevelInfo ? "PageCertCampaigns.inForStage" : "PageCertCampaigns.inForCampaign";
                return PageBase.createStringResourceStatic(page, key, WebComponentUtil.formatDurationWordsForLocal(delta, true, true, page)).getString();
            } else if (delta < 0) {
                String key = stageLevelInfo ? "PageCertCampaigns.agoForStage" : "PageCertCampaigns.agoForCampaign";
                return PageBase.createStringResourceStatic(page, key, WebComponentUtil.formatDurationWordsForLocal(-delta, true, true, page)).getString();
            } else {
                String key = stageLevelInfo ? "PageCertCampaigns.nowForStage" : "PageCertCampaigns.nowForCampaign";
                return page.getString(key);
            }
        }
    }

    // TODO remove this
    public AccessCertificationCampaignType getCampaign() {
        return campaign;
    }

    public boolean isReiterable() {
        return CertCampaignTypeUtil.isReiterable(campaign);
    }
}
