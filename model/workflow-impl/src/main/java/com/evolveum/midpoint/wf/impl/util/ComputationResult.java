/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.wf.impl.processors.primary.cases.StageClosingResultImpl;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOutcomeType;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * TEMPORARY!!!
 *
 * TODO name
 * TODO visibiility
 */
public class ComputationResult {
    public ApprovalLevelOutcomeType predeterminedOutcome;
    public AutomatedCompletionReasonType automatedCompletionReason;
    public Set<ObjectReferenceType> approverRefs;
    public boolean noApproversFound;   // computed but not found (i.e. not set when outcome is given by an auto-outcome expression)

    public ApprovalLevelOutcomeType getPredeterminedOutcome() {
        return predeterminedOutcome;
    }

    public AutomatedCompletionReasonType getAutomatedCompletionReason() {
        return automatedCompletionReason;
    }

    public Set<ObjectReferenceType> getApproverRefs() {
        return approverRefs;
    }

    public boolean noApproversFound() {
        return noApproversFound;
    }

    public StageClosingResult createStageClosingInformation() {
        if (predeterminedOutcome != null) {
            WorkItemOutcomeType caseOutcome =
                    switch (predeterminedOutcome) {
                        case APPROVE, SKIP -> WorkItemOutcomeType.APPROVE;
                        case REJECT -> WorkItemOutcomeType.REJECT;
                    };
            return new StageClosingResultImpl(
                    shouldProcessingContinue(predeterminedOutcome),
                    ApprovalUtils.toUri(caseOutcome),
                    ApprovalUtils.toUri(predeterminedOutcome),
                    automatedCompletionReason);
        } else {
            return null;
        }
    }

    private boolean shouldProcessingContinue(@NotNull ApprovalLevelOutcomeType outcome) {
        switch (outcome) {
            case APPROVE:
            case SKIP:
                return true;
            case REJECT:
                return false;
            default:
                throw new AssertionError(outcome);
        }
    }
}
