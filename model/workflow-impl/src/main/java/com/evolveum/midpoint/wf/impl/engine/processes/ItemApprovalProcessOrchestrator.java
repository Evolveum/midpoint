/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.wf.impl.engine.processes;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.MidpointParsingMigrator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.ItemApprovalEngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.impl.processes.common.WfExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.WfStageComputeHelper;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.MidpointUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
@Component
@SuppressWarnings("Duplicates")
public class ItemApprovalProcessOrchestrator implements ProcessOrchestrator<ItemApprovalEngineInvocationContext> {

	private static final Trace LOGGER = TraceManager.getTrace(ItemApprovalProcessOrchestrator.class);

	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private PrismContext prismContext;
	@Autowired private WfStageComputeHelper stageComputeHelper;
	@Autowired private RepositoryService repositoryService;

	@Override
	public void startProcessInstance(ItemApprovalEngineInvocationContext ctx, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
		advanceProcessInstance(ctx, result);
	}

	public void advanceProcessInstance(ItemApprovalEngineInvocationContext ctx, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		int stageCount = getStageCount(ctx);
		int currentStage = getCurrentStage(ctx);
		if (currentStage == stageCount) {
			workflowEngine.stopProcessInstance(ctx, result);
			return;
		}

		int stageToBe = currentStage + 1;
		List<ItemDelta<?, ?>> stageModifications = prismContext.deltaFor(TaskType.class)
				.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_STAGE_NUMBER).replace(stageToBe)
				.asItemDeltas();
		repositoryService.modifyObject(TaskType.class, ctx.wfTask.getOid(), stageModifications, result);
		ctx.wfContext.setStageNumber(stageToBe);

		ApprovalStageDefinitionType stageDef = WfContextUtil.getCurrentStageDefinition(ctx.wfContext);

		WfStageComputeHelper.ComputationResult computationResult =
				stageComputeHelper.computeStageApprovers(stageDef,
						() -> stageComputeHelper.getDefaultVariables(ctx.wfContext, ctx.opTask.getChannel(), result), ctx.opTask, result);

		ctx.setPreStageComputationResult(computationResult);
		ApprovalLevelOutcomeType predeterminedOutcome = computationResult.getPredeterminedOutcome();
		Set<ObjectReferenceType> approverRefs = computationResult.getApproverRefs();

		if (LOGGER.isDebugEnabled()) {
			if (computationResult.noApproversFound()) {
				LOGGER.debug("No approvers at the stage '{}' for process {} (case oid {}) - outcome-if-no-approvers is {}", stageDef.getName(),
						ctx.wfContext.getProcessInstanceName(), ctx.wfCase.getOid(), stageDef.getOutcomeIfNoApprovers());
			}
			LOGGER.debug("Approval process instance {} (case oid {}), stage {}: predetermined outcome: {}, approvers: {}",
					ctx.wfContext.getProcessInstanceName(), ctx.wfCase.getOid(),
					WfContextUtil.getStageDiagName(stageDef), predeterminedOutcome, approverRefs);
		}

		if (predeterminedOutcome == null) {
			List<CaseWorkItemType> workItems = new ArrayList<>();
			XMLGregorianCalendar createTimestamp = XmlTypeConverter.createXMLGregorianCalendar(new Date());
			XMLGregorianCalendar deadline;
			if (stageDef.getDuration() != null) {
				deadline = (XMLGregorianCalendar) createTimestamp.clone();
				deadline.add(stageDef.getDuration());
			} else {
				deadline = null;
			}
			for (ObjectReferenceType approverRef : approverRefs) {
				CaseWorkItemType workItem = new CaseWorkItemType(prismContext)
						.name(ctx.wfContext.getProcessInstanceName())
						.stageNumber(stageToBe)
						.createTimestamp(createTimestamp)
						.deadline(deadline);
				if (approverRef.getType() == null) {
					approverRef.setType(UserType.COMPLEX_TYPE);
				}
				if (QNameUtil.match(UserType.COMPLEX_TYPE, approverRef.getType())) {
					workItem.setOriginalAssigneeRef(approverRef.clone());
					workItem.getAssigneeRef().add(approverRef.clone());
				} else if (QNameUtil.match(RoleType.COMPLEX_TYPE, approverRef.getType()) ||
						QNameUtil.match(OrgType.COMPLEX_TYPE, approverRef.getType()) ||
						QNameUtil.match(ServiceType.COMPLEX_TYPE, approverRef.getType())) {
					workItem.getCandidateRef().add(approverRef.clone());
					// todo what about originalAssigneeRef?
				} else {
					throw new IllegalStateException("Unsupported type of the approver: " + approverRef.getType() + " in " + approverRef);
				}
				if (stageDef.getAdditionalInformation() != null) {
					try {
						WfExpressionEvaluationHelper evaluator = SpringApplicationContextHolder.getExpressionEvaluationHelper();
						WfStageComputeHelper stageComputer = SpringApplicationContextHolder.getStageComputeHelper();
						ExpressionVariables variables = stageComputer.getDefaultVariables(ctx.wfContext, ctx.getChannel(), result);
						List<InformationType> additionalInformation = evaluator.evaluateExpression(stageDef.getAdditionalInformation(), variables,
								"additional information expression", InformationType.class, InformationType.COMPLEX_TYPE,
								true, this::createInformationType, ctx.opTask, result);
						workItem.getAdditionalInformation().addAll(additionalInformation);
					} catch (Throwable t) {
						throw new SystemException("Couldn't evaluate additional information expression in " + ctx, t);
					}
				}
				workItems.add(workItem);
			}
			workflowEngine.createWorkItems(ctx, workItems, result);
			for (CaseWorkItemType workItem : ctx.wfCase.getWorkItem()) {
				if (workItem.getCloseTimestamp() == null) {     // presumably opened just now
					MidpointUtil.createTriggersForTimedActions(workflowEngine.createWorkItemId(ctx, workItem), 0,
							XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
							XmlTypeConverter.toDate(workItem.getDeadline()), ctx.wfTask, stageDef.getTimedActions(), result);
				}
			}
		}
	}

	private Integer getCurrentStage(ItemApprovalEngineInvocationContext ctx) {
		int rv = defaultIfNull(ctx.wfContext.getStageNumber(), 0);
		checkCurrentStage(ctx, rv);
		return rv;
	}

	private void checkCurrentStage(ItemApprovalEngineInvocationContext ctx, int rv) {
		if (rv < 0 || rv > getStageCount(ctx)) {
			LOGGER.error("Current stage is below 0 or beyond the number of stages: {}\n{}", rv, ctx.debugDump());
			throw new IllegalStateException("Current stage is below 0 or beyond the number of stages: " + rv);
		}
	}

	private int getStageCount(ItemApprovalEngineInvocationContext ctx) {
		Integer stageCount = WfContextUtil.getStageCount(ctx.wfContext);
		if (stageCount == null) {
			LOGGER.error("Couldn't determine stage count from the workflow context\n{}", ctx.debugDump());
			throw new IllegalStateException("Couldn't determine stage count from the workflow context");
		}
		return stageCount;
	}

	private InformationType createInformationType(Object o) {
		if (o == null || o instanceof InformationType) {
			return (InformationType) o;
		} else if (o instanceof String) {
			return MidpointParsingMigrator.stringToInformationType((String) o);
		} else {
			throw new IllegalArgumentException("Object cannot be converted into InformationType: " + o);
		}
	}

//	private void completeStage(EngineInvocationContext ctx, ApprovalLevelOutcomeType outcome,
//			AutomatedCompletionReasonType automatedCompletionReason, OperationResult result) {
//		recordAutoCompletionDecision(ctx.wfTask.getOid(), predeterminedOutcome, automatedCompletionReason, stageToBe, result);
//	}

	private void recordAutoCompletionDecision(String taskOid, ApprovalLevelOutcomeType outcome,
			AutomatedCompletionReasonType reason, int stageNumber, OperationResult opResult) {
		StageCompletionEventType event = new StageCompletionEventType();
		event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
		event.setStageNumber(stageNumber);
		event.setAutomatedDecisionReason(reason);
		event.setOutcome(ApprovalUtils.toUri(outcome));
		MidpointUtil.recordEventInTask(event, null, taskOid, opResult);
	}

}
