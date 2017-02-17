/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemResultType;
import org.apache.commons.lang.BooleanUtils;

/**
 * @author mederly
 */
public class ApprovalUtils {
    public static final String DECISION_APPROVED = "__APPROVED__";
    public static final String DECISION_REJECTED = "__REJECTED__";
    public static final String DECISION_APPROVED_NICE = "Approved";
    public static final String DECISION_REJECTED_NICE = "Rejected";

    public static String approvalStringValue(Boolean approved) {
        if (approved == null) {
            return null;
        } else {
            return approved ? DECISION_APPROVED : DECISION_REJECTED;
        }
    }

    public static Boolean approvalBooleanValue(String decision) {
		return parse(decision, DECISION_APPROVED, DECISION_REJECTED);
	}

	public static Boolean approvalBooleanValueNice(String decision) {
		return parse(decision, DECISION_APPROVED_NICE, DECISION_REJECTED_NICE);
	}

	private static Boolean parse(String decision, String approved, String rejected) {
		if (approved.equals(decision)) {
			return true;
		} else if (rejected.equals(decision)) {
			return false;
		} else {
			return null;
		}
	}

	public static boolean isApproved(String decision) {
        return DECISION_APPROVED.equals(decision);
    }

    public static String makeNice(String decision) {
    	Boolean value = approvalBooleanValue(decision);
    	if (value != null) {
    		return value ? DECISION_APPROVED_NICE : DECISION_REJECTED_NICE;
		} else {
    		return decision;
		}
	}

	public static String approvalStringValue(WorkItemOutcomeType outcome) {
		return approvalStringValue(approvalBooleanValue(outcome));
	}

	public static Boolean approvalBooleanValue(WorkItemResultType result) {
		return result != null ? approvalBooleanValue(result.getOutcome()) : null;
	}

	private static Boolean approvalBooleanValue(WorkItemOutcomeType outcome) {
		if (outcome == null) {
			return null;
		}
		switch (outcome) {
			case APPROVE: return true;
			case REJECT: return false;
			default: throw new IllegalArgumentException("outcome: " + outcome);
		}
	}

	public static WorkItemOutcomeType approvalOutcomeValue(String decision) {
		Boolean b = approvalBooleanValue(decision);
		if (b == null) {
			return null;
		} else {
			return b ? WorkItemOutcomeType.APPROVE : WorkItemOutcomeType.REJECT;
		}
	}

	public static boolean isApproved(WorkItemResultType result) {
		return BooleanUtils.isTrue(approvalBooleanValue(result));
	}
}
