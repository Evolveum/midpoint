/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * @author lazyman
 */
public class DecisionDto extends Selectable {

    public static final String F_USER = "user";
    public static final String F_ORIGINAL_ACTOR = "originalActor";
    public static final String F_STAGE = "stage";
    public static final String F_OUTCOME = "outcome";
    public static final String F_COMMENT = "comment";
    public static final String F_TIME = "time";
    public static final String F_ESCALATION_LEVEL_NUMBER = "escalationLevelNumber";

    private String user;
    private String originalActor;
    private String stage;
    private Boolean outcome;
    private String comment;
    private Date time;
    private Integer escalationLevelNumber;

	public String getTime() {
        return time.toLocaleString();      // todo formatting
    }

    public String getUser() {
        return user;
    }

	public String getOriginalActor() {
		return originalActor;
	}

	public String getStage() {
    	return stage;
	}

    public Boolean getOutcome() {
        return outcome;
    }

    public String getComment() {
        return comment;
    }

	public Integer getEscalationLevelNumber() {
		return escalationLevelNumber == null || escalationLevelNumber == 0 ? null : escalationLevelNumber;
	}

	@Deprecated
	public static DecisionDto create(DecisionType d) {
		DecisionDto rv = new DecisionDto();
		rv.user = WebComponentUtil.getName(d.getApproverRef());
		rv.originalActor = null;
		rv.stage = null;
		rv.outcome = d.isApproved();
		rv.comment = d.getComment();
		rv.time = XmlTypeConverter.toDate(d.getDateTime());
		rv.escalationLevelNumber = null;
		return rv;
	}

	// if pageBase is null, references will not be resolved
    @Nullable
	public static DecisionDto create(CaseEventType e, @Nullable PageBase pageBase) {

		// we want to show user decisions, automatic decisions and delegations
		DecisionDto rv = new DecisionDto();
		rv.user = WebComponentUtil.getName(e.getInitiatorRef());
		rv.stage = WfContextUtil.getStageInfoTODO(e.getStageNumber());
		rv.time = XmlTypeConverter.toDate(e.getTimestamp());

		if (e instanceof WorkItemCompletionEventType) {
			WorkItemCompletionEventType completionEvent = (WorkItemCompletionEventType) e;
			AbstractWorkItemOutputType output = completionEvent.getOutput();
			if (output != null) {
				rv.outcome = ApprovalUtils.approvalBooleanValue(output);
				rv.comment = output.getComment();
				// TODO what about additional delta?
			}
			WorkItemEventCauseInformationType cause = completionEvent.getCause();
			if (cause != null && cause.getType() == WorkItemEventCauseTypeType.TIMED_ACTION) {
				rv.user = PageBase.createStringResourceStatic(null,
							"DecisionDto." + (rv.outcome ? "approvedDueToTimeout" : "rejectedDueToTimeout")).getString();
				if (rv.comment == null) {
					if (cause.getDisplayName() != null) {
						rv.comment = cause.getDisplayName();
					} else if (cause.getName() != null) {
						rv.comment = cause.getName();
					}
				}
			}
			rv.escalationLevelNumber = WfContextUtil.getEscalationLevelNumber(completionEvent);
			if (completionEvent.getOriginalAssigneeRef() != null && pageBase != null) {
				// TODO optimize repo access
				rv.originalActor = WebModelServiceUtils.resolveReferenceName(completionEvent.getOriginalAssigneeRef(), pageBase);
			}
			return rv;
		} else if (e instanceof StageCompletionEventType) {
			StageCompletionEventType completion = (StageCompletionEventType) e;
			AutomatedCompletionReasonType reason = completion.getAutomatedDecisionReason();
			if (reason == null) {
				return null;			// not an automatic stage completion
			}
			ApprovalLevelOutcomeType outcome = ApprovalUtils.approvalLevelOutcomeFromUri(completion.getOutcome());
			if (outcome == ApprovalLevelOutcomeType.APPROVE || outcome == ApprovalLevelOutcomeType.REJECT) {
				rv.outcome = outcome == ApprovalLevelOutcomeType.APPROVE;
				rv.user = PageBase.createStringResourceStatic(null,
						"DecisionDto." + (rv.outcome ? "automaticallyApproved" : "automaticallyRejected")).getString();
				rv.comment = PageBase.createStringResourceStatic(null, "DecisionDto." + reason.name()).getString();
				return rv;
			} else {
				return null;			// SKIP (legal = should hide) or null (illegal)
			}
		} else {
			return null;
		}
		// delegations are not shown here
	}
}
