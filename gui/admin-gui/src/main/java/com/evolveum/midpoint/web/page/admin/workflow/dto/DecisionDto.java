/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * @author lazyman
 */
public class DecisionDto extends Selectable {

    public static final String F_USER = "user";
    public static final String F_ATTORNEY = "attorney";
    public static final String F_ORIGINAL_ACTOR = "originalActor";
    public static final String F_ORIGINAL_ASSIGNEE = "originalAssignee";
    public static final String F_STAGE = "stage";
    public static final String F_OUTCOME = "outcome";
    public static final String F_COMMENT = "comment";
    public static final String F_TIME = "time";
    public static final String F_ESCALATION_LEVEL_NUMBER = "escalationLevelNumber";

    private String user;
    private String attorney;
    private String originalActor;
    private String originalAssignee;
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

	public String getAttorney() {
		return attorney;
	}

	public String getOriginalActor() {
		return originalActor;
	}

	public String getOriginalAssignee() {
		return originalAssignee;
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

	// if pageBase is null, references will not be resolved
    @Nullable
	public static DecisionDto create(CaseEventType e, @Nullable PageBase pageBase) {

		// we want to show user decisions, automatic decisions and delegations
		DecisionDto rv = new DecisionDto();
		rv.user = WebComponentUtil.getName(e.getInitiatorRef());
		rv.attorney = WebComponentUtil.getName(e.getAttorneyRef());
		rv.stage = ApprovalContextUtil.getStageInfoTODO(e.getStageNumber());
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
			rv.escalationLevelNumber = ApprovalContextUtil.getEscalationLevelNumber(completionEvent);
			if (completionEvent.getOriginalAssigneeRef() != null && pageBase != null) {
				// TODO optimize repo access
				rv.originalActor = WebModelServiceUtils.resolveReferenceName(completionEvent.getOriginalAssigneeRef(), pageBase);
			}
			return rv;
		} else if (e instanceof WorkItemDelegationEventType){
			ObjectReferenceType origAssigneeRef = CollectionUtils.isNotEmpty(((WorkItemDelegationEventType) e).getAssigneeBefore()) ?
					((WorkItemDelegationEventType) e).getAssigneeBefore().get(0) : null;
			rv.originalAssignee =  origAssigneeRef != null ? WebModelServiceUtils.resolveReferenceName(origAssigneeRef, pageBase) : null;
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
	}
}
