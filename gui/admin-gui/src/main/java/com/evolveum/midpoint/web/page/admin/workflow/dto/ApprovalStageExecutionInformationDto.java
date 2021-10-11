/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * GUI-friendly information about historic, current or future execution of a given approval stage.
 *
 * @author mederly
 */

public class ApprovalStageExecutionInformationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String F_APPROVER_ENGAGEMENTS = "approverEngagements";

    private final int stageNumber;
    private final String stageName;
    private final String stageDisplayName;
    private final LevelEvaluationStrategyType evaluationStrategy;                           // ALL_MUST_AGREE or FIRST_DECIDES
    private ApprovalLevelOutcomeType automatedOutcome;                                      // if the stage was (is to be) automatically completed, here is the (expected) outcome
    private AutomatedCompletionReasonType automatedCompletionReason;                        // if the stage was (is to be) automatically completed, here is the reason
    private final List<ApproverEngagementDto> approverEngagements = new ArrayList<>();      // approvers (to be) engaged during this stage, potentially with their responses
    private String errorMessage;                                                            // error message indicating that the preview couldn't be computed
    private ApprovalLevelOutcomeType outcome;                                               // real outcome (automated or "normal")
    private boolean reachable;                                                              // is it possible that this stage would be reachable (if the process would be running)
                                                                                            // currently all stages after first rejected one are not reachable, as the process would stop there

    private ApprovalStageExecutionInformationDto(ApprovalStageDefinitionType definition) {
        stageNumber = definition.getNumber();
        stageName = definition.getName();
        stageDisplayName = definition.getDisplayName();
        evaluationStrategy = definition.getEvaluationStrategy();
    }

    static ApprovalStageExecutionInformationDto createFrom(ApprovalSchemaExecutionInformationType processInfo, int stageNumber,
            ObjectResolver resolver, ObjectResolver.Session session, Task opTask, OperationResult result) {
        ApprovalStageExecutionInformationType stageInfo = ApprovalSchemaExecutionInformationUtil.getStage(processInfo, stageNumber);
        if (stageInfo == null) {
            throw new IllegalStateException("No stage execution information in " + processInfo);
        }
        ApprovalStageExecutionInformationDto rv = new ApprovalStageExecutionInformationDto(stageInfo.getDefinition());
        int currentStageNumber = defaultIfNull(processInfo.getCurrentStageNumber(), 0);
        if (stageNumber <= currentStageNumber) {
            addInformationFromPastOrCurrentStage(rv, processInfo, stageNumber, currentStageNumber, resolver, session, opTask, result);
        } else {
            addInformationFromFutureStage(rv, stageInfo.getExecutionPreview(), resolver, session, opTask, result);
        }
        // computing stage outcome that is to be displayed
        if (rv.automatedOutcome != null) {
            rv.outcome = rv.automatedOutcome;
        } else {
            if (stageNumber < currentStageNumber) {
                rv.outcome = ApprovalLevelOutcomeType.APPROVE; // no stage before current stage could be manually rejected
            } else if (stageNumber == currentStageNumber) {
                rv.outcome = ApprovalUtils.approvalLevelOutcomeFromUri(ApprovalContextUtil.getOutcome(processInfo));
            } else {
                rv.outcome = null;
            }
        }
        // set 'last' flag for all approvers at this stage
        for (int i = 0; i < rv.getApproverEngagements().size(); i++) {
            rv.getApproverEngagements().get(i).setLast(i == rv.getApproverEngagements().size()-1);
        }
        return rv;
    }

    private static void addInformationFromFutureStage(ApprovalStageExecutionInformationDto rv,
            ApprovalStageExecutionPreviewType executionPreview, ObjectResolver resolver,
            ObjectResolver.Session session, Task opTask, OperationResult result) {
        if (executionPreview.getExpectedAutomatedCompletionReason() != null) {
            rv.automatedCompletionReason = executionPreview.getExpectedAutomatedCompletionReason();
            rv.automatedOutcome = executionPreview.getExpectedAutomatedOutcome();
        } else {
            for (ObjectReferenceType approver : executionPreview.getExpectedApproverRef()) {
                resolve(approver, resolver, session, opTask, result);
                rv.addApproverEngagement(new ApproverEngagementDto(approver));
            }
        }
        rv.errorMessage = executionPreview.getErrorMessage();
    }

    private static void resolve(ObjectReferenceType ref, ObjectResolver resolver, ObjectResolver.Session session,
            Task opTask, OperationResult result) {
        if (ref != null) {
            resolver.resolveReference(ref.asReferenceValue(), "resolving approver", session, opTask, result);
        }
    }

    private static void addInformationFromPastOrCurrentStage(ApprovalStageExecutionInformationDto rv,
            ApprovalSchemaExecutionInformationType processInfo, int stageNumber, int currentStageNumber,
            ObjectResolver resolver, ObjectResolver.Session session, Task opTask, OperationResult result) {
        assert stageNumber <= currentStageNumber;
        CaseType aCase = ApprovalSchemaExecutionInformationUtil.getEmbeddedCaseBean(processInfo);

        for (CaseEventType event : CaseEventUtil.getEventsForStage(aCase, stageNumber)) {
            if (event instanceof WorkItemCompletionEventType) {
                WorkItemCompletionEventType completionEvent = (WorkItemCompletionEventType) event;
                ObjectReferenceType initiatorRef = completionEvent.getInitiatorRef();
                ObjectReferenceType attorneyRef = completionEvent.getAttorneyRef();
                resolve(initiatorRef, resolver, session, opTask, result);
                resolve(attorneyRef, resolver, session, opTask, result);
                ApproverEngagementDto engagement = new ApproverEngagementDto(initiatorRef);
                engagement.setCompletedAt(completionEvent.getTimestamp());
                engagement.setCompletedBy(initiatorRef);
                engagement.setAttorney(attorneyRef);
                engagement.setOutput(completionEvent.getOutput());
                rv.addApproverEngagement(engagement);
            } else if (event instanceof StageCompletionEventType) {
                StageCompletionEventType completionEvent = (StageCompletionEventType) event;
                if (completionEvent.getAutomatedDecisionReason() != null) {
                    rv.automatedOutcome = ApprovalUtils.approvalLevelOutcomeFromUri(completionEvent.getOutcome());
                    rv.automatedCompletionReason = completionEvent.getAutomatedDecisionReason();
                }
            }
        }

        // Obtaining information about open work items
        if (stageNumber == currentStageNumber) {
            for (CaseWorkItemType workItem : CaseWorkItemUtil.getWorkItemsForStage(aCase, stageNumber)) {
                if (CaseWorkItemUtil.isCaseWorkItemNotClosed(workItem)) {
                    for (ObjectReferenceType assigneeRef : workItem.getAssigneeRef()) {
                        resolve(assigneeRef, resolver, session, opTask, result);
                        rv.addApproverEngagement(new ApproverEngagementDto(assigneeRef));
                    }
                }
            }
        }
    }

    private void addApproverEngagement(ApproverEngagementDto engagement) {
        approverEngagements.add(engagement);
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStageDisplayName() {
        return stageDisplayName;
    }

    public ApprovalLevelOutcomeType getAutomatedOutcome() {
        return automatedOutcome;
    }

    public AutomatedCompletionReasonType getAutomatedCompletionReason() {
        return automatedCompletionReason;
    }

    public List<ApproverEngagementDto> getApproverEngagements() {
        return approverEngagements;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // TODO tweak this as needed
    public String getNiceStageName(int totalStageNumber) {
        return ApprovalContextUtil.getStageInfo(stageNumber, totalStageNumber, stageName, stageDisplayName);
    }

    public boolean isFirstDecides() {
        return evaluationStrategy == LevelEvaluationStrategyType.FIRST_DECIDES;
    }

    public ApprovalLevelOutcomeType getOutcome() {
        return outcome;
    }

    public boolean isReachable() {
        return reachable;
    }

    void setReachable(boolean reachable) {
        this.reachable = reachable;
    }
}
