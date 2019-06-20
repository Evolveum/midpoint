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

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.CompleteWorkItemsRequest;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 */
public class CompleteWorkItemsAction extends RequestedAction<CompleteWorkItemsRequest> {

	private static final Trace LOGGER = TraceManager.getTrace(CompleteWorkItemsAction.class);

	public CompleteWorkItemsAction(EngineInvocationContext ctx, @NotNull CompleteWorkItemsRequest request) {
		super(ctx, request);
	}

	@Override
	public Action execute(OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		traceEnter(LOGGER);
		LOGGER.trace("Completions: {}", request.getCompletions());

		ApprovalStageDefinitionType stageDef = ctx.getCurrentStageDefinition();
		LevelEvaluationStrategyType levelEvaluationStrategyType = stageDef.getEvaluationStrategy();

		boolean closeOtherWorkItems = false;

		XMLGregorianCalendar now = engine.clock.currentTimeXMLGregorianCalendar();
		for (CompleteWorkItemsRequest.SingleCompletion completion : request.getCompletions()) {
			CaseWorkItemType workItem = ctx.findWorkItemById(completion.getWorkItemId());

			if (!engine.authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.COMPLETE,
					ctx.getTask(), result)) {
				throw new SecurityViolationException("You are not authorized to complete the work item.");
			}

			if (workItem.getCloseTimestamp() != null) {
				LOGGER.trace("Work item {} was already completed on {}", workItem.getId(), workItem.getCloseTimestamp());
				result.recordWarning("Work item " + workItem.getId() + " was already completed on "
						+ workItem.getCloseTimestamp());
				continue;
			}

			String outcome = completion.getOutcome();

			LOGGER.trace("+++ recordCompletionOfWorkItem ENTER: workItem={}, outcome={}", workItem, outcome);
			LOGGER.trace("======================================== Recording individual decision of {}", ctx.getPrincipal());

			@NotNull WorkItemResultType itemResult = new WorkItemResultType(engine.prismContext);
			itemResult.setOutcome(outcome);
			itemResult.setComment(completion.getComment());
			boolean isApproved = ApprovalUtils.isApproved(itemResult);
			if (isApproved && completion.getAdditionalDelta() != null) {
				ObjectDeltaType additionalDeltaBean = DeltaConvertor.toObjectDeltaType(completion.getAdditionalDelta());
				ObjectTreeDeltasType treeDeltas = new ObjectTreeDeltasType();
				treeDeltas.setFocusPrimaryDelta(additionalDeltaBean);
				itemResult.setAdditionalDeltas(treeDeltas);
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Recording decision for approval process instance {} (case oid {}), stage {}: decision: {}",
						ctx.getProcessInstanceName(), ctx.getCaseOid(),
						ApprovalContextUtil.getStageDiagName(stageDef), itemResult.getOutcome());
			}

			ObjectReferenceType performerRef = ObjectTypeUtil.createObjectRef(ctx.getPrincipal().getUser(), engine.prismContext);
			workItem.setOutput(itemResult);
			workItem.setPerformerRef(performerRef);
			workItem.setCloseTimestamp(now);

			if (levelEvaluationStrategyType == LevelEvaluationStrategyType.FIRST_DECIDES) {
				LOGGER.trace("Finishing the stage, because the stage evaluation strategy is 'firstDecides'.");
				closeOtherWorkItems = true;
			} else if ((levelEvaluationStrategyType == null || levelEvaluationStrategyType == LevelEvaluationStrategyType.ALL_MUST_AGREE) && !isApproved) {
				LOGGER.trace("Finishing the stage, because the stage eval strategy is 'allMustApprove' and the decision was 'reject'.");
				closeOtherWorkItems = true;
			}

			engine.workItemHelper.recordWorkItemClosure(ctx, workItem, true, request.getCauseInformation(), result);
		}

		Action next;
		if (closeOtherWorkItems) {
			doCloseOtherWorkItems(ctx, request.getCauseInformation(), now, result);
			next = new CloseStageAction(ctx, null);
		} else if (!ctx.isAnyCurrentStageWorkItemOpen()) {
			next = new CloseStageAction(ctx, null);
		} else {
			next = null;
		}

		traceExit(LOGGER, next);
		return next;
	}

	private void doCloseOtherWorkItems(EngineInvocationContext ctx, WorkItemEventCauseInformationType causeInformation,
			XMLGregorianCalendar now, OperationResult result) throws SchemaException {
		WorkItemEventCauseTypeType causeType = causeInformation != null ? causeInformation.getType() : null;
		LOGGER.trace("+++ closeOtherWorkItems ENTER: ctx={}, cause type={}", ctx, causeType);
		for (CaseWorkItemType workItem : ctx.getCurrentCase().getWorkItem()) {
			if (workItem.getCloseTimestamp() == null) {
				workItem.setCloseTimestamp(now);
				engine.workItemHelper.recordWorkItemClosure(ctx, workItem, false, causeInformation, result);
			}
		}
		LOGGER.trace("--- closeOtherWorkItems EXIT: ctx={}", ctx);
	}
}
