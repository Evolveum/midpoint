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

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang3.Validate;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.Date;

/**
 * @author mederly
 */
public class CertDecisionDto extends Selectable implements Serializable{

    public static final String F_SUBJECT_NAME = "subjectName";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_TARGET_TYPE = "targetType";
    public static final String F_COMMENT = "comment";
    public static final String F_RESPONSE = "response";
    public static final String F_CAMPAIGN_NAME = "campaignName";
    public static final String F_CAMPAIGN_STAGE = "campaignStage";
    public static final String F_REVIEW_REQUESTED = "reviewRequested";

    private AccessCertificationCaseType certCase;
    private String subjectName;
    private String targetName;
    private AccessCertificationDecisionType decision;

    public CertDecisionDto() {
        // TODO Auto-generated constructor stub
    }

    public CertDecisionDto(AccessCertificationCaseType _case) {
        Validate.notNull(_case);

        this.certCase = _case;
        this.subjectName = getName(_case.getSubjectRef());
        this.targetName = getName(_case.getTargetRef());
        if (_case.getDecision().isEmpty()) {
            decision = new AccessCertificationDecisionType();
        } else if (_case.getDecision().size() == 1) {
            decision = _case.getDecision().get(0);
        } else {
            throw new IllegalStateException("More than one relevant decision entry in a certification case: " + _case);
        }
    }

    // ugly hack (for now) - we extract the name from serialization metadata
    private String getName(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        }
        String name = (String) ref.asReferenceValue().getUserData(XNodeSerializer.USER_DATA_KEY_COMMENT);
        if (name == null) {
            return "(" + ref.getOid() + ")";
        } else {
            return name.trim();
        }
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetType() {
        // TODO improve
        return certCase.getTargetRef().getType().getLocalPart();
    }

    public String getComment() {
        return decision.getComment();
    }

    public void setComment(String value) {
        decision.setComment(value);
    }

    public AccessCertificationResponseType getResponse() {
        return decision.getResponse();
    }

    public ObjectReferenceType getCampaignRef() {
        return certCase.getCampaignRef();
    }

    public Long getCaseId() {
        return certCase.asPrismContainerValue().getId();
    }

    public AccessCertificationCaseType getCertCase() {
        return certCase;
    }

    public AccessCertificationCampaignType getCampaign() {
        try {
            return (AccessCertificationCampaignType) certCase.getCampaignRef().asReferenceValue().getObject().asObjectable();
        } catch (NullPointerException e) {
            return null;      // TODO fix this really crude hack
        }
    }

    public String getCampaignName() {
        AccessCertificationCampaignType campaign = getCampaign();
        return campaign != null ? campaign.getName().getOrig() : "";
    }

    public Integer getCampaignStageNumber() {
        AccessCertificationCampaignType campaign = getCampaign();
        return campaign != null ? campaign.getCurrentStageNumber() : null;      // numbers after # of stages should not occur, as there are no cases in these stages
    }

    public Integer getCampaignStageCount() {
        AccessCertificationCampaignType campaign = getCampaign();
        return CertCampaignTypeUtil.getNumberOfStages(campaign);
    }

    public Date getReviewRequested() {
        XMLGregorianCalendar date = certCase.getReviewRequestedTimestamp();
        return XmlTypeConverter.toDate(date);
    }

    public Date getStageStarted() {
        AccessCertificationCampaignType campaign = getCampaign();
        if (campaign == null) {
            return null;
        }
        int stageNumber = campaign.getCurrentStageNumber();
        if (stageNumber <= 0 || stageNumber > CertCampaignTypeUtil.getNumberOfStages(campaign)) {
            return null;
        }
        AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, stageNumber);
        return XmlTypeConverter.toDate(stage.getStart());
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
}
