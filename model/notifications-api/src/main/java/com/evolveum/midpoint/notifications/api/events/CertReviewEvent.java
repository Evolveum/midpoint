/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Event related to given certification case.
 *  ADD = case created (first stage)
 *  MODIFY = stage deadline is approaching
 *  DELETE = stage closed
 *
 * @author mederly
 */
public class CertReviewEvent extends AccessCertificationEvent {

    private List<AccessCertificationCaseType> cases;
    private SimpleObjectRef actualReviewer;					// Actual reviewer - the person which the work item is assigned to. I.e. _not_ his deputy.
															// Must be set to non-null value just after instantiation (TODO do more cleanly)

    public CertReviewEvent(LightweightIdentifierGenerator idGenerator, List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, EventOperationType opType) {
        super(idGenerator, campaign, opType);
        this.cases = cases;
    }

	public SimpleObjectRef getActualReviewer() {
		return actualReviewer;
	}

	public void setActualReviewer(SimpleObjectRef actualReviewer) {
		this.actualReviewer = actualReviewer;
	}

	@Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return super.isCategoryType(eventCategoryType) ||
                EventCategoryType.CERT_CASE_EVENT.equals(eventCategoryType);
    }

    @Deprecated	// obsolete name
    public Collection<AccessCertificationCaseType> getCasesAwaitingResponseFromRequestee() {
    	return getCasesAwaitingResponseFromActualReviewer();
	}

    public Collection<AccessCertificationCaseType> getCasesAwaitingResponseFromActualReviewer() {
        List<AccessCertificationCaseType> rv = new ArrayList<>();
        for (AccessCertificationCaseType aCase : cases) {
            if (awaitsResponseFrom(aCase, actualReviewer.getOid(), campaign.getStageNumber())) {
                rv.add(aCase);
            }
        }
        return rv;
    }

    private boolean awaitsResponseFrom(AccessCertificationCaseType aCase, String reviewerOid, int currentStageNumber) {
        for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
            if (workItem.getStageNumber() == currentStageNumber
					&& WorkItemTypeUtil.getOutcome(workItem) == null
					&& workItem.getCloseTimestamp() == null
					&& ObjectTypeUtil.containsOid(workItem.getAssigneeRef(), reviewerOid)) {
                return true;
            }
        }
        return false;
    }

    public List<AccessCertificationCaseType> getCases() {
        return cases;
    }
    
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
		debugDumpCommon(sb, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "cases", cases, indent + 1);
		return sb.toString();
	}
}
