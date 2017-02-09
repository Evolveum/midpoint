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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * @author lazyman
 */
public class DecisionDto extends Selectable {

    public static final String F_USER = "user";
    public static final String F_STAGE = "stage";
    public static final String F_OUTCOME = "outcome";
    public static final String F_COMMENT = "comment";
    public static final String F_TIME = "time";
    public static final String F_ESCALATION_LEVEL_NUMBER = "escalationLevelNumber";

    private String user;
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
		rv.stage = null;
		rv.outcome = d.isApproved();
		rv.comment = d.getComment();
		rv.time = XmlTypeConverter.toDate(d.getDateTime());
		rv.escalationLevelNumber = null;
		return rv;
	}

    @Nullable
	public static DecisionDto create(WfProcessEventType e) {

		// we want to show user decisions, automatic decisions and delegations
		DecisionDto rv = new DecisionDto();
		rv.user = WebComponentUtil.getName(e.getInitiatorRef());
		rv.stage = WfContextUtil.getStageInfo(e.getStageNumber(), null, e.getStageName(), e.getStageDisplayName());
		//rv.stage = MiscUtil.getFirstNonNullString(e.getStageDisplayName(), e.getStageName(), e.getStageNumber());
		rv.time = XmlTypeConverter.toDate(e.getTimestamp());

		if (e instanceof WorkItemCompletionEventType) {
			WorkItemResultType result = ((WorkItemCompletionEventType) e).getResult();
			if (result != null) {
				rv.outcome = ApprovalUtils.approvalBooleanValue(result);
				rv.comment = result.getComment();
				// TODO what about additional delta?
			}
			rv.escalationLevelNumber = ((WorkItemCompletionEventType) e).getEscalationLevelNumber();
			return rv;
		} else if (e instanceof WfStageCompletionEventType) {
			WfStageCompletionEventType completion = (WfStageCompletionEventType) e;
			AutomatedDecisionReasonType reason = completion.getAutomatedDecisionReason();
			if (reason == null) {
				return null;			// not an automatic stage completion
			}
			ApprovalLevelOutcomeType outcome = completion.getOutcome();
			if (outcome == ApprovalLevelOutcomeType.APPROVE || outcome == ApprovalLevelOutcomeType.REJECT) {
				rv.outcome = outcome == ApprovalLevelOutcomeType.APPROVE;
				rv.comment = String.valueOf(reason);		// TODO
				return rv;
			} else {
				return null;			// SKIP (legal = should hide) or null (illegal)
			}
		} else if (e instanceof WorkItemDelegationEventType) {
			WorkItemDelegationEventType delegation = (WorkItemDelegationEventType) e;
			rv.comment = String.valueOf(((WorkItemDelegationEventType) e).getDelegatedTo());		// TODO
//			delegation.getDelegatedTo()
			return rv;
		} else {
			return null;
		}
		// delegations are not shown here
	}
}
