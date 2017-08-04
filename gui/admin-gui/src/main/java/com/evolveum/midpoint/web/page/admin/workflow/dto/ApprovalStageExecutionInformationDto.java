/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI-friendly information about historic, current or future execution of a given approval stage.
 *
 * @author mederly
 */

public class ApprovalStageExecutionInformationDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ApprovalStageExecutionInformationDto.class);

	private final int stageNumber;
	private final String stageName;
	private final String stageDisplayName;
	private final LevelEvaluationStrategyType evaluationStrategy;                           // ALL_MUST_AGREE or FIRST_DECIDES
	private ApprovalLevelOutcomeType automatedOutcome;                                      // if the stage was (is to be) automatically completed, here is the (expected) outcome
	private AutomatedCompletionReasonType automatedCompletionReason;                        // if the stage was (is to be) automatically completed, here is the reason
	private final List<ApproverEngagementDto> approverEngagements = new ArrayList<>();      // approvers (to be) engaged during this stage, potentially with their responses
	private String errorMessage;                                                            // error message indicating that the preview couldn't be computed

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
		if (stageNumber <= processInfo.getCurrentStageNumber()) {
			addInformationFromRecordedStage(rv, stageInfo.getExecutionRecord(), resolver, session, opTask, result);
		} else {
			addInformationFromPreviewedStage(rv, stageInfo.getExecutionPreview(), resolver, session, opTask, result);
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
			ApprovalStageExecutionRecordType executionRecord, ObjectResolver resolver,
			ObjectResolver.Session session, Task opTask, OperationResult result) {
		for (CaseEventType event : executionRecord.getEvent()) {
			if (event instanceof WorkItemEventType) {
				WorkItemEventType workItemEvent = (WorkItemEventType) event;
				ObjectReferenceType approver = workItemEvent.getOriginalAssigneeRef();
				if (approver == null) {
					LOGGER.warn("No original assignee in work item event {} -- ignoring it", workItemEvent);
					continue;
				}
				String externalWorkItemId = workItemEvent.getExternalWorkItemId();
				if (externalWorkItemId == null) {
					LOGGER.warn("No external work item ID in work item event {} -- ignoring it", workItemEvent);
					continue;
				}
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
		for (WorkItemType workItem : executionRecord.getWorkItem()) {
			ObjectReferenceType approver = workItem.getOriginalAssigneeRef();
			if (approver == null) {
				LOGGER.warn("No original assignee in work item {} -- ignoring it", workItem);
				continue;
			}
			String externalWorkItemId = workItem.getExternalId();
			if (externalWorkItemId == null) {
				LOGGER.warn("No external work item ID in work item {} -- ignoring it", workItem);
				continue;
			}
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

	private ApproverEngagementDto findApproverEngagement(ObjectReferenceType approver, String externalWorkItemId) {
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
		return WfContextUtil.getStageInfo(stageNumber, totalStageNumber, stageName, stageDisplayName);
	}
}
