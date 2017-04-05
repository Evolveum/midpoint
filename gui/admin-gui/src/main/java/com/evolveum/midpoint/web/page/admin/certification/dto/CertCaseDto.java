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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
public class CertCaseDto extends CertCaseOrWorkItemDto {

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
		List<String> allReviewersNames = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : _case.getWorkItem()) {
            if (StringUtils.isNotEmpty(workItem.getComment())) {
                comments.add(workItem.getComment());
            }
			boolean hasResponse = workItem.getOutcome() != null || !StringUtils.isEmpty(workItem.getComment());
			for (ObjectReferenceType reviewerRef : workItem.getAssigneeRef()) {
				PrismObject<UserType> reviewerObject = WebModelServiceUtils.resolveReferenceRaw(reviewerRef, page, task, result);
				String reviewerName = reviewerObject != null ? WebComponentUtil.getName(reviewerObject) : reviewerRef.getOid();
				allReviewersNames.add(reviewerName);
				if (hasResponse) {
					reviewerNames.add(reviewerName);
				}
			}
        }
        if (allReviewersNames.isEmpty()) {
            allReviewers = page.getString("PageCertCampaign.noReviewers");
        } else {
            allReviewers = StringUtils.join(allReviewersNames, ", ");
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
        return CertCampaignTypeUtil.getReviewedTimestamp(certCase.getWorkItem());
    }

    public AccessCertificationResponseType getOverallOutcome() {
        return getCertCase().getOverallOutcome();
    }

    public Integer getCurrentResponseStageNumber() {
        return getCertCase().getCurrentStageNumber();
    }

}
