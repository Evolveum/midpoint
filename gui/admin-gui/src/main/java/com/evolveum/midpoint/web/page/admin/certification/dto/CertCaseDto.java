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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DTO for displaying cases as part of certification campaign information.
 * Aggregates more decisions.
 *
 * TODO cleanup a bit
 *
 * @author mederly
 */
public class CertCaseDto extends CertCaseOrDecisionDto {

    public static final String F_REMEDIED_AT = "remediedAt";
    public static final String F_REVIEWERS = "reviewers";
    public static final String F_REVIEWED_AT = "reviewedAt";
    public static final String F_REVIEWED_BY = "reviewedBy";
    public static final String F_COMMENTS = "comments";
    public static final String F_CURRENT_RESPONSE_STAGE_NUMBER = "currentResponseStageNumber";

    private String allReviewers;
    private List<String> reviewerNames = new ArrayList<>();
    private List<String> comments = new ArrayList<>();

    public CertCaseDto(AccessCertificationCaseType _case, PageBase page, Task task, OperationResult result) {
        super(_case, page);
        for (AccessCertificationDecisionType decision : _case.getDecision()) {
            if (decision.getResponse() == null && StringUtils.isEmpty(decision.getComment())) {
                continue;
            }
            if (StringUtils.isNotEmpty(decision.getComment())) {
                comments.add(decision.getComment());
            }
            PrismObject<UserType> reviewerObject = WebModelServiceUtils.resolveReferenceRaw(decision.getReviewerRef(), page, task, result);
            if (reviewerObject != null) {
                reviewerNames.add(WebComponentUtil.getName(reviewerObject));
            }
        }
        List<String> names = new ArrayList<>();
        // TODO show by work items
        for (ObjectReferenceType reviewerRef : CertCampaignTypeUtil.getReviewers(_case)) {
            // TODO optimize - don't resolve reviewers twice
            PrismObject<UserType> reviewerObject = WebModelServiceUtils.resolveReferenceRaw(reviewerRef, page, task, result);
            if (reviewerObject != null) {
                names.add(WebComponentUtil.getName(reviewerObject));
            }
        }
        if (names.isEmpty()) {
            allReviewers = page.getString("PageCertCampaign.noReviewers");
        } else {
            allReviewers = StringUtils.join(names, ", ");
        }
    }

    public String getReviewers() {
        return allReviewers;
    }

    public String getReviewedBy() {
        return StringUtils.join(reviewerNames, ", ");
    }

    public String getComments() {
        return StringUtils.join(comments, "; ");
    }

    public String getRemediedAt() {
        return WebComponentUtil.formatDate(getCertCase().getRemediedTimestamp());
    }

    public String getReviewedAt() {
        return WebComponentUtil.formatDate(getReviewedTimestamp(getCertCase()));
    }

    private Date getReviewedTimestamp(AccessCertificationCaseType certCase) {
        return CertCampaignTypeUtil.getReviewedTimestamp(certCase.getDecision());
    }

    public AccessCertificationResponseType getOverallOutcome() {
        return getCertCase().getOverallOutcome();
    }

    public Integer getCurrentResponseStageNumber() {
        return getCertCase().getCurrentStageNumber();
    }

}
