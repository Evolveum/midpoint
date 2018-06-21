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

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
    public static final String F_CURRENT_REVIEWERS = "currentReviewers";
    public static final String F_REVIEWED_AT = "reviewedAt";
    public static final String F_REVIEWED_BY = "reviewedBy";
    public static final String F_COMMENTS = "comments";
    public static final String F_CURRENT_RESPONSE_STAGE_NUMBER = "currentResponseStageNumber";

    private final List<String> currentReviewers = new ArrayList<>();
    private final List<String> reviewedBy = new ArrayList<>();
    private final List<String> comments = new ArrayList<>();
    private final String noReviewersLabel;

    CertCaseDto(AccessCertificationCaseType _case, PageBase page, Task task, OperationResult result) {
        super(_case, page);
        Map<String, String> names = new HashMap<>();
        for (AccessCertificationWorkItemType workItem : _case.getWorkItem()) {
            if (StringUtils.isNotEmpty(WorkItemTypeUtil.getComment(workItem))) {
                comments.add(WorkItemTypeUtil.getComment(workItem));
            }
			if (hasResponse(workItem) && workItem.getPerformerRef() != null) {
				reviewedBy.add(getName(workItem.getPerformerRef(), page, names, task, result));
			}
			for (ObjectReferenceType assigneeRef : workItem.getAssigneeRef()) {
				if (workItem.getCloseTimestamp() == null
						&& java.util.Objects.equals(workItem.getStageNumber(), _case.getStageNumber())) {
					currentReviewers.add(getName(assigneeRef, page, names, task, result));
				}
			}
        }
        noReviewersLabel = page.getString("PageCertCampaign.noReviewers");
    }

	private String getName(ObjectReferenceType ref, PageBase page,
			Map<String, String> names, Task task, OperationResult result) {
    	if (ref == null || ref.getOid() == null) {
    		return null;		// shouldn't occur
		}
		return names.computeIfAbsent(ref.getOid(), oid -> {
			PrismObject<UserType> reviewerObject = WebModelServiceUtils.resolveReferenceNoFetch(ref, page, task, result);
			return reviewerObject != null ? WebComponentUtil.getName(reviewerObject) : ref.getOid();
		});
	}

	private boolean hasResponse(AccessCertificationWorkItemType workItem) {
        return workItem.getOutput() != null && (workItem.getOutput().getOutcome() != null || !StringUtils.isEmpty(workItem.getOutput().getComment()));
    }

    @SuppressWarnings("unused")
    public String getCurrentReviewers() {
		if (currentReviewers.isEmpty()) {
			return noReviewersLabel;
		} else {
			return StringUtils.join(currentReviewers, ", ");
		}
    }

    public String getReviewedBy() {
        return StringUtils.join(reviewedBy, ", ");
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
        return OutcomeUtils.fromUri(getCertCase().getOutcome());
    }

	@SuppressWarnings("unused")
    public Integer getCurrentResponseStageNumber() {
        return getCertCase().getStageNumber();
    }

}
