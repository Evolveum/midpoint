/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.certification.dto;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.util.SelectableRow;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.certification.CertDecisionHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A common superclass for CertCaseDto + CertWorkItemDto.
 * <p>
 * TODO cleanup a bit
 */
public class CertCaseOrWorkItemDto extends Selectable<CertCaseOrWorkItemDto> implements SelectableRow<CertCaseOrWorkItemDto> {

    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_TARGET_NAME = "targetName";
    @SuppressWarnings("unused")
    public static final String F_TARGET_TYPE = "targetType";
    public static final String F_CAMPAIGN_NAME = "campaignName";
    public static final String F_REVIEW_REQUESTED = "reviewRequested";
    public static final String F_DEADLINE_AS_STRING = "deadlineAsString";
    public static final String F_CONFLICTING_TARGETS = "conflictingTargets";
    public static final String F_ITERATION = "iteration";

    private final AccessCertificationCaseType certCase;
    private final String objectName;
    private final String targetName;
    private final String deadlineAsString;
    private final QName defaultRelation;

    CertCaseOrWorkItemDto(@NotNull AccessCertificationCaseType _case, PageBase page) {
        this.certCase = _case;
        this.objectName = getName(_case.getObjectRef());
        this.targetName = getName(_case.getTargetRef());
        this.deadlineAsString = computeDeadlineAsString(page);
        this.defaultRelation = page.getPrismContext().getDefaultRelation();
    }

    // ugly hack (for now) - we extract the name from serialization metadata
    private String getName(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        }
        String name = ref.getTargetName() != null ? ref.getTargetName().getOrig() : null;
        if (name == null) {
            return "(" + ref.getOid() + ")";
        } else {
            return name.trim();
        }
    }

    public String getObjectName() {
        return objectName;
    }

    public QName getObjectType() {
        return certCase.getObjectRef().getType();
    }

    public QName getObjectType(CertDecisionHelper.WhichObject which) {
        switch (which) {
            case OBJECT:
                return getObjectType();
            case TARGET:
                return getTargetType();
            default:
                return null;
        }
    }

    public Integer getIteration() {
        return norm(certCase.getIteration());
    }

    public String getTargetName() {
        return targetName;
    }

    public QName getTargetType() {
        return certCase.getTargetRef().getType();
    }

    public ObjectReferenceType getCampaignRef() {
        return ObjectTypeUtil.createObjectRef(getCampaign(), defaultRelation);
    }

    public Long getCaseId() {
        return certCase.asPrismContainerValue().getId();
    }

    public AccessCertificationCaseType getCertCase() {
        return certCase;
    }

    public AccessCertificationCampaignType getCampaign() {
        return CertCampaignTypeUtil.getCampaign(certCase);
    }

    public String getCampaignName() {
        AccessCertificationCampaignType campaign = getCampaign();
        return campaign != null ? campaign.getName().getOrig() : "";
    }

    public Integer getCampaignStageNumber() {
        AccessCertificationCampaignType campaign = getCampaign();
        return campaign != null ? campaign.getStageNumber() : null;      // numbers after # of stages should not occur, as there are no cases in these stages
    }

    public Integer getCampaignStageCount() {
        AccessCertificationCampaignType campaign = getCampaign();
        return CertCampaignTypeUtil.getNumberOfStages(campaign);
    }

    @SuppressWarnings("unused")
    public Date getReviewRequested() {
        XMLGregorianCalendar date = certCase.getCurrentStageCreateTimestamp();
        return XmlTypeConverter.toDate(date);
    }

    public Date getStageStarted() {
        AccessCertificationCampaignType campaign = getCampaign();
        if (campaign == null) {
            return null;
        }
        int stageNumber = campaign.getStageNumber();
        if (stageNumber <= 0 || stageNumber > CertCampaignTypeUtil.getNumberOfStages(campaign)) {
            return null;
        }
        AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, stageNumber);
        return XmlTypeConverter.toDate(stage.getStartTimestamp());
    }

    public String getCurrentStageName() {
        AccessCertificationCampaignType campaign = getCampaign();
        if (campaign == null) {
            return null;
        }
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        if (stage == null) {
            return null;
        }
        return stage.getName();
    }

    public String getHandlerUri() {
        AccessCertificationCampaignType campaign = getCampaign();
        return campaign != null ? campaign.getHandlerUri() : null;
    }

    private String computeDeadlineAsString(PageBase page) {
        XMLGregorianCalendar deadline = certCase.getCurrentStageDeadline();

        if (deadline == null) {
            return "";
        } else {
            long delta = XmlTypeConverter.toMillis(deadline) - System.currentTimeMillis();

            // round to hours; we always round down
            long precision = 3600000L;      // 1 hour
            if (Math.abs(delta) > precision) {
                delta = (delta / precision) * precision;
            }

            if (delta > 0) {
                return PageBase.createStringResourceStatic("PageCert.in", WebComponentUtil
                                .formatDurationWordsForLocal(delta, true, true, page))
                        .getString();
            } else if (delta < 0) {
                return PageBase.createStringResourceStatic("PageCert.ago", WebComponentUtil
                                .formatDurationWordsForLocal(-delta, true, true, page))
                        .getString();
            } else {
                return page.getString("PageCert.now");
            }
        }
    }

    @SuppressWarnings("unused")
    public String getDeadlineAsString() {
        return deadlineAsString;
    }

    /**
     * Preliminary implementation. Eventually we will create a list of hyperlinks pointing to the actual objects.
     */
    @SuppressWarnings("unused")
    public String getConflictingTargets() {
        if (!(certCase instanceof AccessCertificationAssignmentCaseType)) {
            return "";
        }
        AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) certCase;
        if (assignmentCase.getAssignment() == null) {
            return "";
        }
        Set<String> exclusions = new TreeSet<>();
        List<EvaluatedExclusionTriggerType> allExclusionTriggers = PolicyRuleTypeUtil
                .getAllExclusionTriggers(assignmentCase.getAssignment().getTriggeredPolicyRule());

        for (EvaluatedExclusionTriggerType trigger : allExclusionTriggers) {
            ObjectReferenceType conflicting = trigger.getConflictingObjectRef();
            if (conflicting == null) {
                continue;
            }
            if (conflicting.getTargetName() != null) {
                exclusions.add(conflicting.getTargetName().getOrig());
            } else {
                exclusions.add(conflicting.getOid()); // TODO try to resolve?
            }
        }
        return StringUtils.join(exclusions, ", ");
    }
}
