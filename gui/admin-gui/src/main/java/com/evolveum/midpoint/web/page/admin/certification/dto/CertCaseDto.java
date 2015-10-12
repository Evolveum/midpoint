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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
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
    public static final String F_REVIEWED_AT = "reviewedAt";
    public static final String F_REVIEWED_BY = "reviewedBy";
    public static final String F_COMMENTS = "comments";
    public static final String F_CURRENT_RESPONSE_STAGE_NUMBER = "currentResponseStageNumber";

    private List<String> reviewerNames = new ArrayList<>();
    private List<String> comments = new ArrayList<>();

    public CertCaseDto(AccessCertificationCaseType _case, PageBase page, Task task, OperationResult result) {
        super(_case, page);
        for (AccessCertificationDecisionType decision : _case.getDecision()) {
            if (decision.getComment() != null) {
                comments.add(decision.getComment());
            }
            PrismObject<UserType> reviewerObject = WebModelUtils.resolveReference(decision.getReviewerRef(), page, task, result);
            if (reviewerObject != null) {
                reviewerNames.add(WebMiscUtil.getName(reviewerObject));
            }
        }
    }

    public String getReviewedBy() {
        return StringUtils.join(reviewerNames, ", ");
    }

    public String getComments() {
        return StringUtils.join(comments, "; ");
    }

    public String getRemediedAt() {
        return WebMiscUtil.formatDate(getCertCase().getRemediedTimestamp());
    }

    public String getReviewedAt() {
        return WebMiscUtil.formatDate(getReviewedTimestamp(getCertCase()));
    }

    private Date getReviewedTimestamp(AccessCertificationCaseType certCase) {
        Date lastDate = null;
        for (AccessCertificationDecisionType decision : certCase.getDecision()) {
            Date decisionDate = XmlTypeConverter.toDate(decision.getTimestamp());
            if (lastDate == null || decisionDate.after(lastDate)) {
                lastDate = decisionDate;
            }
        }
        return lastDate;
    }

    public AccessCertificationResponseType getCurrentResponse() {
        return getCertCase().getCurrentResponse();
    }

    public Integer getCurrentResponseStageNumber() {
        return getCertCase().getCurrentResponseStage();
    }

}
