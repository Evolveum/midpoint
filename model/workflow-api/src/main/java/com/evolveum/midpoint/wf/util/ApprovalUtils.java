/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOutcomeType;
import org.apache.commons.lang.BooleanUtils;

/**
 * @author mederly
 */
public class ApprovalUtils {
    private static final String DECISION_APPROVED_NICE = "Approved";
    private static final String DECISION_REJECTED_NICE = "Rejected";

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

    public static Boolean approvalBooleanValue(AbstractWorkItemOutputType result) {
        return result != null ? approvalBooleanValue(fromUri(result.getOutcome())) : null;
    }

    public static Boolean approvalBooleanValue(String uri) {
        return uri != null ? approvalBooleanValue(fromUri(uri)) : null;
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

    public static boolean isApproved(AbstractWorkItemOutputType result) {
        return BooleanUtils.isTrue(approvalBooleanValue(result));
    }

    private static boolean isApproved(WorkItemOutcomeType outcome) {
        return BooleanUtils.isTrue(approvalBooleanValue(outcome));
    }

    public static boolean isApproved(String result) {
        return BooleanUtils.isTrue(approvalBooleanValue(result));
    }

    public static String toUri(WorkItemOutcomeType workItemOutcomeType) {
        if (workItemOutcomeType == null) {
            return null;
        }
        switch (workItemOutcomeType) {
            case APPROVE: return SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE;
            case REJECT: return SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT;
            default: throw new AssertionError("Unexpected outcome: " + workItemOutcomeType);
        }
    }

    public static String toUri(ApprovalLevelOutcomeType outcome) {
        if (outcome == null) {
            return null;
        }
        switch (outcome) {
            case APPROVE: return SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE;
            case REJECT: return SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT;
            case SKIP: return SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP;
            default: throw new AssertionError("Unexpected outcome: " + outcome);
        }
    }

    public static String toUri(Boolean approved) {
        if (approved == null) {
            return null;
        } else {
            return approved ? SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE : SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT;
        }
    }

    public static WorkItemOutcomeType fromUri(String uri) {
        if (uri == null) {
            return null;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)) {
            return WorkItemOutcomeType.APPROVE;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
            return WorkItemOutcomeType.REJECT;
        } else {
            throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
    }

    public static ApprovalLevelOutcomeType approvalLevelOutcomeFromUri(String uri) {
        if (uri == null) {
            return null;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)) {
            return ApprovalLevelOutcomeType.APPROVE;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
            return ApprovalLevelOutcomeType.REJECT;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP)) {
            return ApprovalLevelOutcomeType.SKIP;
        } else {
            throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
    }

    public static String makeNiceFromUri(String outcome) {
        Boolean value = approvalBooleanValueFromUri(outcome);
        if (value != null) {
            return value ? DECISION_APPROVED_NICE : DECISION_REJECTED_NICE;
        } else {
            return outcome;
        }
    }

    public static Boolean approvalBooleanValueFromUri(String uri) {
        return approvalBooleanValue(fromUri(uri));
    }

    public static boolean isApprovedFromUri(String uri) {
        return isApproved(fromUri(uri));
    }

    public static AbstractWorkItemOutputType createApproveOutput(PrismContext prismContext) {
        return new AbstractWorkItemOutputType(prismContext)
                .outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
    }
}
