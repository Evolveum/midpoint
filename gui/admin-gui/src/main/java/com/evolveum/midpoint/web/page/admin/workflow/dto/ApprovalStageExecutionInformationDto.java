/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;

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

	private static final Trace LOGGER = TraceManager.getTrace(ApprovalStageExecutionInformationDto.class);
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

	static ApprovalStageExecutionInformationDto createFrom(ApprovalSchemaExecutionInformationType processInfo, int stageIndex,
			ObjectResolver resolver, ObjectResolver.Session session, Task opTask, OperationResult result) {
		ApprovalStageExecutionInformationType stageInfo = processInfo.getStage().get(stageIndex);
		ApprovalStageExecutionInformationDto rv = new ApprovalStageExecutionInformationDto(stageInfo.getDefinition());
		int stageNumber = stageIndex+1;
		int currentStageNumber = defaultIfNull(processInfo.getCurrentStageNumber(), 0);
		if (stageNumber <= currentStageNumber) {
			addInformationFromRecordedStage(rv, processInfo, stageInfo.getExecutionRecord(), currentStageNumber, resolver, session, opTask, result);
		} else {
			addInformationFromPreviewedStage(rv, stageInfo.getExecutionPreview(), resolver, session, opTask, result);
		}
		// computing stage outcome that is to be displayed
		if (rv.automatedOutcome != null) {
			rv.outcome = rv.automatedOutcome;
		} else {
			if (stageNumber < currentStageNumber) {
				rv.outcome = ApprovalLevelOutcomeType.APPROVE;      // no stage before current stage could be manually rejected
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

	private static void addInformationFromPreviewedStage(ApprovalStageExecutionInformationDto rv,
			ApprovalStageExecutionPreviewType executionPreview, ObjectResolver resolver,
			ObjectResolver.Session session, Task opTask, OperationResult result) {
		if (executionPreview.getExpectedAutomatedCompletionReason() != null) {
			rv.automatedCompletionReason = executionPreview.getExpectedAutomatedCompletionReason();
			rv.automatedOutcome = executionPreview.getExpectedAutomatedOutcome();
		} else {
			for (ObjectReferenceType approver : executionPreview.getExpectedApproverRef()) {
				resolve(approver, resolver, session, opTask, result);
				rv.addApproverEngagement(new ApproverEngagementDto(approver, null));
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

	private static void addInformationFromRecordedStage(ApprovalStageExecutionInformationDto rv,
			ApprovalSchemaExecutionInformationType processInfo, ApprovalStageExecutionRecordType executionRecord,
			int currentStageNumber, ObjectResolver resolver,
			ObjectResolver.Session session, Task opTask, OperationResult result) {
		for (CaseEventType event : executionRecord.getEvent()) {
			if (event instanceof WorkItemEventType) {
				WorkItemEventType workItemEvent = (WorkItemEventType) event;
				ObjectReferenceType approver;
				if (event instanceof WorkItemDelegationEventType){
					List<ObjectReferenceType> delegateToList = ((WorkItemDelegationEventType)event).getDelegatedTo();
					approver = CollectionUtils.isNotEmpty(delegateToList) ? delegateToList.get(0) : null;
				} else {
					approver = workItemEvent.getOriginalAssigneeRef();
				}
				if (approver == null) {
					LOGGER.warn("No original assignee in work item event {} -- ignoring it", workItemEvent);
					continue;
				}
				if (workItemEvent.getExternalWorkItemId() == null) {
					LOGGER.warn("No external work item ID in work item event {} -- ignoring it", workItemEvent);
					continue;
				}
				WorkItemId externalWorkItemId = WorkItemId.create(workItemEvent.getExternalWorkItemId());
				ApproverEngagementDto engagement = rv.findApproverEngagement(approver, externalWorkItemId);
				if (engagement == null) {
					resolve(approver, resolver, session, opTask, result);
					engagement = new ApproverEngagementDto(approver, externalWorkItemId);
					rv.addApproverEngagement(engagement);
				}
				if (event instanceof WorkItemCompletionEventType) {
					WorkItemCompletionEventType completionEvent = (WorkItemCompletionEventType) event;
					engagement.setCompletedAt(completionEvent.getTimestamp());
					resolve(completionEvent.getInitiatorRef(), resolver, session, opTask, result);
					engagement.setCompletedBy(completionEvent.getInitiatorRef());
					engagement.setAttorney(completionEvent.getAttorneyRef());
					engagement.setOutput(completionEvent.getOutput());
				}
			} else if (event instanceof StageCompletionEventType) {
				StageCompletionEventType completionEvent = (StageCompletionEventType) event;
				if (completionEvent.getAutomatedDecisionReason() != null) {
					rv.automatedOutcome = ApprovalUtils.approvalLevelOutcomeFromUri(completionEvent.getOutcome());
					rv.automatedCompletionReason = completionEvent.getAutomatedDecisionReason();
				}
			}
		}
		// not needed after "create work item" events will be implemented
		for (CaseWorkItemType workItem : executionRecord.getWorkItem()) {
			if (workItem.getStageNumber() == null || workItem.getStageNumber() != currentStageNumber){
				continue;
			}
			ObjectReferenceType approver = CollectionUtils.isNotEmpty(workItem.getAssigneeRef()) ?
					workItem.getAssigneeRef().get(0) : workItem.getOriginalAssigneeRef();
			if (approver == null) {
				LOGGER.warn("No original assignee in work item {} -- ignoring it", workItem);
				continue;
			}
			WorkItemId externalWorkItemId = WorkItemId.create(processInfo.getCaseRef().getOid(), workItem.getId());
			ApproverEngagementDto engagement = rv.findApproverEngagement(approver, externalWorkItemId);
			if (engagement == null) {
				resolve(approver, resolver, session, opTask, result);
				engagement = new ApproverEngagementDto(approver, externalWorkItemId);
				rv.addApproverEngagement(engagement);
			}
		}
	}

	private void addApproverEngagement(ApproverEngagementDto engagement) {
		approverEngagements.add(engagement);
	}

	private ApproverEngagementDto findApproverEngagement(ObjectReferenceType approver, WorkItemId externalWorkItemId) {
		for (ApproverEngagementDto engagement : approverEngagements) {
			if (ObjectTypeUtil.matchOnOid(engagement.getApproverRef(), approver)
					&& java.util.Objects.equals(engagement.getExternalWorkItemId(), externalWorkItemId)) {
				return engagement;
			}
		}
		return null;
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

	public LevelEvaluationStrategyType getEvaluationStrategy() {
		return evaluationStrategy;
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

	public void setReachable(boolean reachable) {
		this.reachable = reachable;
	}
}
