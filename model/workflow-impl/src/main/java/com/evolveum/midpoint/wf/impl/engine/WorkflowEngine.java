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

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.impl.engine.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.engine.processes.ItemApprovalProcessOrchestrator;
import com.evolveum.midpoint.wf.impl.engine.processes.ProcessOrchestrator;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.WfStageComputeHelper;
import com.evolveum.midpoint.wf.impl.processes.common.WfTimedActionTriggerHandler;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.MidpointUtil;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.task.api.TaskExecutionStatus.WAITING;
import static com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames.LOOP_APPROVERS_IN_STAGE_STOP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.ESCALATE;

/**
 * This is a replacement of Activiti.
 */
@Component
public class WorkflowEngine {

	private static final Trace LOGGER = TraceManager.getTrace(WorkflowEngine.class);

	@Autowired private Clock clock;
	@Autowired private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;
	@Autowired private WfTaskController wfTaskController;
	@Autowired private TaskManager taskManager;
	@Autowired private WorkItemProvider workItemProvider;
	@Autowired private AuditService auditService;
	@Autowired private ItemApprovalProcessOrchestrator itemApprovalProcessOrchestrator;

	public <CTX extends EngineInvocationContext> void startProcessInstance(CTX ctx, ProcessOrchestrator<CTX> orchestrator,
			OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {

		LOGGER.trace("+++ startProcessInstance ENTER: ctx={}, orchestrator={}", ctx, orchestrator);

		CaseType wfCase = new CaseType(prismContext);
		ctx.setWfCase(wfCase);
		wfCase.setName(PolyStringType.fromOrig(createCaseName(ctx.wfContext.getProcessInstanceName(), ctx.wfTask.getOid())));
		wfCase.setTaskRef(createObjectRef(ctx.wfTask.getTaskType(), prismContext));
		wfCase.setState(SchemaConstants.CASE_STATE_OPEN);
		String caseOid = repositoryService.addObject(wfCase.asPrismObject(), null, result);

		ctx.wfContext.setCaseOid(caseOid);
		List<ItemDelta<?, ?>> caseRefModifications = prismContext.deltaFor(TaskType.class)
				.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_CASE_OID).replace(caseOid)
				.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_CASE_REF)
				.replace(createObjectRef(wfCase, prismContext))
				.asItemDeltas();
		repositoryService.modifyObject(TaskType.class, ctx.wfTask.getOid(), caseRefModifications, result);

		// TODO clean this up (remove from WfTaskController, remove WfTask)
		WfTask wfTask = getWfTask(ctx, result);
		wfTaskController.auditProcessStart(wfTask, ctx.wfContext, result);
		wfTaskController.notifyProcessStart(wfTask.getTask(), result);
		orchestrator.startProcessInstance(ctx, result);

		runTheProcess(ctx, orchestrator, result);

		LOGGER.trace("--- startProcessInstance EXIT: ctx={}", ctx);
	}

	public <CTX extends EngineInvocationContext> void runTheProcess(CTX ctx, ProcessOrchestrator<CTX> orchestrator,
			OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

		LOGGER.trace("+++ runTheProcess ENTER: ctx={}", ctx);
		for (;;) {
			refreshContext(ctx, result);
			if (isWaiting(ctx) || isClosed(ctx)) {
				LOGGER.trace("--- runTheProcess EXIT (waiting or closed): ctx={}", ctx);
				return;
			}
			if (ctx.isDone()) {
				recordProcessOutcome(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE, result);
				stopProcessInstance(ctx, result);
				LOGGER.trace("--- runTheProcess EXIT (done): ctx={}", ctx);
				return;
			}
			orchestrator.advanceProcessInstance(ctx, result);
		}
	}

	private <CTX extends EngineInvocationContext> boolean isClosed(CTX ctx) {
		return SchemaConstants.CASE_STATE_CLOSED.equals(ctx.wfCase.getState());
	}

	private <CTX extends EngineInvocationContext> boolean isWaiting(CTX ctx) {
		return ctx.wfCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null);
	}

	private <CTX extends EngineInvocationContext> void refreshContext(CTX ctx, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		ctx.wfCase = repositoryService.getObject(CaseType.class, ctx.wfCase.getOid(), null, result).asObjectable();
		ctx.wfTask.refresh(result);
		ctx.wfContext = ctx.wfTask.getWorkflowContext();
	}

	public void stopProcessInstance(EngineInvocationContext ctx, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LOGGER.trace("+++ stopProcessInstance ENTER: ctx={}", ctx);

		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		List<ItemDelta<?, ?>> caseModifications = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_CLOSED)
				.asItemDeltas();    // we intentionally do not set state information on the case; it is in wf context
		List<CaseWorkItemType> openWorkItems = ctx.wfCase.getWorkItem().stream()
				.filter(wi -> wi.getCloseTimestamp() == null)
				.collect(Collectors.toList());
		for (CaseWorkItemType workItem : openWorkItems) {
			closeWorkItemBatched(ctx, workItem.getId(), now, caseModifications, result);
		}
		repositoryService.modifyObject(CaseType.class, ctx.wfCase.getOid(), caseModifications, result);

		List<ItemDelta<?, ?>> taskModifications = prismContext.deltaFor(TaskType.class)
				.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_END_TIMESTAMP).replace(now)
				.asItemDeltas();
		repositoryService.modifyObject(TaskType.class, ctx.wfTask.getOid(), taskModifications, result);

		// TODO what about outcome?

		WfTask wfTask = getWfTask(ctx, result);
		wfTask.getChangeProcessor().onProcessEnd(ctx, result);
		wfTask.commitChanges(result);

		wfTaskController.auditProcessEnd(wfTask, ctx.wfContext, result);
		wfTaskController.notifyProcessEnd(wfTask, result);

		// passive tasks can be 'let go' at this point
		// TODO clean this up!
		if (wfTask.getTaskExecutionStatus() == WAITING) {
			wfTask.computeTaskResultIfUnknown(result);
			wfTask.removeCurrentTaskHandlerAndUnpause(result);            // removes WfProcessInstanceShadowTaskHandler
		}
		LOGGER.trace("--- stopProcessInstance EXIT: ctx={}", ctx);
	}

	/**
	 * Closes work item in repository as well as in memory.
	 */
	private void closeWorkItem(EngineInvocationContext ctx, AbstractWorkItemType workItem,
			XMLGregorianCalendar now, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LOGGER.trace("+++ closeWorkItem ENTER: workItem={}, ctx={}", workItem, ctx);
		List<ItemDelta<?, ?>> modifications = new ArrayList<>();
		closeWorkItemBatched(ctx, workItem.getId(), now, modifications, result);
		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), modifications, result);
		// removes "workItem[n]" from the item path to be directly applicable to the specific work item
		for (ItemDelta<?, ?> itemDelta : modifications) {
			itemDelta.setParentPath(itemDelta.getParentPath().rest(2));
			itemDelta.applyTo(workItem.asPrismContainerValue());
		}
		LOGGER.trace("--- closeWorkItem EXIT: workItem={}, ctx={}", workItem, ctx);
	}

	private void closeWorkItemBatched(EngineInvocationContext ctx, Long workItemId,
			XMLGregorianCalendar now, List<ItemDelta<?, ?>> modifications,
			OperationResult result) throws SchemaException {
		modifications.addAll(prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_WORK_ITEM, workItemId, CaseWorkItemType.F_CLOSE_TIMESTAMP).replace(now)
				.asItemDeltas());
	}

	@NotNull
	public static String createCaseName(String processInstanceName, String taskOid) {
		return processInstanceName + " (" + taskOid + "=task)";
	}

	public static String getTaskOidFromCase(CaseType aCase) {
		if (aCase != null && aCase.getName() != null && aCase.getName().getOrig() != null) {
			return getTaskOidFromCaseName(aCase.getName().getOrig());
		} else {
			return null;
		}
	}

	public static String getTaskOidFromCaseName(@NotNull String caseName) {
		if (caseName.endsWith("=task)")) {
			int i = caseName.lastIndexOf('(');
			if (i >= 0) {
				return caseName.substring(i+1, caseName.length()-6);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public void assertNoOpenWorkItems(CaseType aCase) {
		if (aCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null)) {
			throw new IllegalStateException("Open work item in " + aCase);
		}
	}

	public WorkItemType getFullWorkItem(String workItemId, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		String caseOid = WorkflowInterface.getCaseOidFromWorkItemId(workItemId);
		long id = WorkflowInterface.getIdFromWorkItemId(workItemId);
		PrismObject<CaseType> caseObject = repositoryService.getObject(CaseType.class, caseOid, null, result);

		//noinspection unchecked
		PrismContainerValue<CaseWorkItemType> pcv = (PrismContainerValue<CaseWorkItemType>) caseObject.find(ItemPath.create(CaseType.F_WORK_ITEM, id));
		if (pcv == null) {
			throw new IllegalStateException("No work item " + id + " in " + caseObject);
		}
		CaseWorkItemType workItem = pcv.asContainerable();
		return workItemProvider.toFullWorkItem(workItem, result);
	}

	// inStageClosure = we are calling this method as part of stage closure (when we detect timed action of COMPLETE)
	// TODO orchestrator
	public void completeWorkItem(String workItemId, WorkItemType workItem, String outcome, String comment,
			ObjectDelta<? extends ObjectType> additionalDelta,
			WorkItemEventCauseInformationType causeInformation, boolean inStageClosure, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

		LOGGER.trace("+++ completeWorkItem ENTER: workItem={}, outcome={}, inStageClosure={}", workItem, outcome, inStageClosure);
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

		EngineInvocationContext ctx = createInvocationContext(workItemId, result);

		WfTask wfTask = getWfTask(ctx, result);
		ApprovalStageDefinitionType stageDef = WfContextUtil.getCurrentStageDefinition(ctx.wfContext);

		MidPointPrincipal user = getMidPointPrincipal();

		LOGGER.trace("======================================== Recording individual decision of {}", user);

		@NotNull WorkItemResultType itemResult = new WorkItemResultType(prismContext);
		itemResult.setOutcome(outcome);
		itemResult.setComment(comment);
		boolean isApproved = ApprovalUtils.isApproved(itemResult);
		if (isApproved && additionalDelta != null) {
			ObjectDeltaType additionalDeltaBean = DeltaConvertor.toObjectDeltaType(additionalDelta);
			ObjectTreeDeltasType treeDeltas = new ObjectTreeDeltasType();
			treeDeltas.setFocusPrimaryDelta(additionalDeltaBean);
			itemResult.setAdditionalDeltas(treeDeltas);
		}

		LevelEvaluationStrategyType levelEvaluationStrategyType = stageDef.getEvaluationStrategy();
		boolean stopTheStage;
		if (levelEvaluationStrategyType == LevelEvaluationStrategyType.FIRST_DECIDES) {
			LOGGER.trace("Setting " + LOOP_APPROVERS_IN_STAGE_STOP + " to true, because the stage evaluation strategy is 'firstDecides'.");
			stopTheStage = true;
		} else if ((levelEvaluationStrategyType == null || levelEvaluationStrategyType == LevelEvaluationStrategyType.ALL_MUST_AGREE) && !isApproved) {
			LOGGER.trace("Setting " + LOOP_APPROVERS_IN_STAGE_STOP + " to true, because the stage eval strategy is 'allMustApprove' and the decision was 'reject'.");
			stopTheStage = true;
		} else {
			stopTheStage = false;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Recording decision for approval process instance {} (case oid {}), stage {}: decision: {}; stage stops now: {}",
					ctx.wfContext.getProcessInstanceName(), ctx.getCaseOid(),
					WfContextUtil.getStageDiagName(stageDef), itemResult.getOutcome(), stopTheStage);
		}

		List<ItemDelta<?, ?>> resultModifications = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_WORK_ITEM, workItem.getId(), CaseWorkItemType.F_OUTPUT).replace(itemResult)
				.asItemDeltas();
		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), resultModifications, result);
		workItem.setOutput(itemResult);

		closeWorkItem(ctx, workItem, now, result);
		onWorkItemClosure(ctx, workItem, wfTask, true, causeInformation, result);
		refreshContext(ctx, result);

		if (!inStageClosure) {
			boolean keepStageOpen;
			if (stopTheStage) {
				closeOtherWorkItems(ctx, wfTask, causeInformation, now, result);
				refreshContext(ctx, result);
				keepStageOpen = false;
			} else {
				keepStageOpen = ctx.wfCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null);
			}
			LOGGER.trace("computed keepStageOpen = {}", keepStageOpen);
			if (!keepStageOpen) {
				onStageClose(ctx, result);
			}
		}
		LOGGER.trace("--- completeWorkItem EXIT: workItem={}, inStageClosure={}", workItem, inStageClosure);
	}

	public void onStageClose(EngineInvocationContext ctx, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		LOGGER.trace("+++ onStageClose ENTER: ctx={}", ctx);
		boolean stopTheProcess = closeTheStage(ctx, result);
		if (stopTheProcess) {
			recordProcessOutcome(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT, result);
			stopProcessInstance(ctx, result);
		} else {
			runTheProcess(((ItemApprovalEngineInvocationContext) ctx), itemApprovalProcessOrchestrator, result);        // temporary
		}
		LOGGER.trace("--- onStageClose EXIT: ctx={}", ctx);
	}

	private void recordProcessOutcome(EngineInvocationContext ctx, String processOutcome, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(TaskType.class)
				.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_OUTCOME)
				.replace(processOutcome)
				.asItemDeltas();
		repositoryService.modifyObject(TaskType.class, ctx.wfTask.getOid(), modifications, result);
		ctx.wfTask.getWorkflowContext().setOutcome(processOutcome);
	}

	private boolean closeTheStage(EngineInvocationContext ctx, OperationResult result) {
		LOGGER.trace("+++ closeTheStage ENTER: ctx={}", ctx);
		ApprovalStageDefinitionType stageDef = WfContextUtil.getCurrentStageDefinition(ctx.wfContext);
//		List<StageCompletionEventType> stageEvents = WfContextUtil.getEventsForCurrentStage(ctx.wfContext, StageCompletionEventType.class);
		boolean approved;
		WfStageComputeHelper.ComputationResult preStageComputationResult = ((ItemApprovalEngineInvocationContext) ctx)
				.getPreStageComputationResult();
		if (preStageComputationResult != null) {
			ApprovalLevelOutcomeType outcome = preStageComputationResult.getPredeterminedOutcome();
			switch (outcome) {
				case APPROVE:
				case SKIP:
					approved = true;
					break;
				case REJECT:
					approved = false;
					break;
				default:
					throw new IllegalStateException("Unknown outcome: " + outcome);		// TODO less draconian handling
			}
			// TODO stage closure event
		} else {
			LOGGER.trace("****************************************** Summarizing decisions in stage {} (stage evaluation strategy = {}): ",
					stageDef.getName(), stageDef.getEvaluationStrategy());

			// TODO let's take the work items themselves
			List<WorkItemCompletionEventType> itemEvents = WfContextUtil.getEventsForCurrentStage(ctx.wfContext, WorkItemCompletionEventType.class);

			boolean allApproved = true;
			for (WorkItemCompletionEventType event : itemEvents) {
				LOGGER.trace(" - {}", event);
				allApproved &= ApprovalUtils.isApproved(event.getOutput());
			}
			approved = allApproved;
			if (stageDef.getEvaluationStrategy() == LevelEvaluationStrategyType.FIRST_DECIDES) {
				Set<String> outcomes = itemEvents.stream()
						.map(e -> e.getOutput().getOutcome())
						.collect(Collectors.toSet());
				if (outcomes.size() > 1) {
					LOGGER.warn("Ambiguous outcome with firstDecides strategy in {}: {} response(s), providing outcomes of {}",
							WfContextUtil.getBriefDiagInfo(ctx.wfContext), itemEvents.size(), outcomes);
					itemEvents.sort(Comparator.nullsLast(Comparator.comparing(event -> XmlTypeConverter.toMillis(event.getTimestamp()))));
					WorkItemCompletionEventType first = itemEvents.get(0);
					approved = ApprovalUtils.isApproved(first.getOutput());
					LOGGER.warn("Possible race condition, so taking the first one: {} ({})", approved, first);
				}
			}
		}

		MidpointUtil.removeAllStageTriggersForWorkItem(ctx.wfTask, result);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Closing the stage for approval process instance {} (case oid {}), stage {}: result of this stage: {}",
					ctx.wfContext.getProcessInstanceName(),
					ctx.getCaseOid(), WfContextUtil.getStageDiagName(stageDef), approved);
		}

		LOGGER.trace("--- closeTheStage EXIT: ctx={}, approved={}", ctx, approved);
		return !approved;
	}

	public MidPointPrincipal getMidPointPrincipal() {
		MidPointPrincipal user;
		try {
			user = SecurityUtil.getPrincipal();
		} catch (SecurityViolationException e) {
			throw new SystemException("Couldn't get midPoint principal: " + e.getMessage(), e);
		}
		return user;
	}

	private EngineInvocationContext createInvocationContext(String workItemId, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		String caseOid = WorkflowInterface.getCaseOidFromWorkItemId(workItemId);
		PrismObject<CaseType> caseObject = repositoryService.getObject(CaseType.class, caseOid, null, result);
		ObjectReferenceType taskRef = caseObject.asObjectable().getTaskRef();
		if (taskRef == null) {
			throw new IllegalStateException("No wf task in case " + caseObject);
		}
		PrismObject<TaskType> taskObject = repositoryService.getObject(TaskType.class, taskRef.getOid(), null, result);
		Task task = taskManager.createTaskInstance(taskObject, result);
		// temporary (we should get the task from the upper layers)
		Task opTask = taskManager.createTaskInstance();
		MidPointPrincipal user = getMidPointPrincipal();
		if (user != null) {
			opTask.setOwner(user.getUser().asPrismObject());
		}
		EngineInvocationContext ctx = new ItemApprovalEngineInvocationContext(task.getWorkflowContext(), task, opTask);
		ctx.setWfCase(caseObject.asObjectable());
		return ctx;
	}

	public void closeOtherWorkItems(EngineInvocationContext ctx, WfTask wfTask,
			WorkItemEventCauseInformationType causeInformation, XMLGregorianCalendar now,
			OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		WorkItemEventCauseTypeType causeType = causeInformation != null ? causeInformation.getType() : null;
		LOGGER.trace("+++ closeOtherWorkItems ENTER: ctx={}, cause type={}", ctx, causeType);
		for (CaseWorkItemType workItem : ctx.wfCase.getWorkItem()) {
			if (workItem.getCloseTimestamp() == null) {
				if (causeType == WorkItemEventCauseTypeType.TIMED_ACTION) {
					String workItemId = WorkflowInterface.createWorkItemId(ctx.getCaseOid(), workItem.getId());
					CompleteWorkItemActionType completeAction = willBeCompletedByTrigger(ctx, workItem, workItemId);
					if (completeAction != null) {
						LOGGER.trace("Sibling work item is being closed by triggered complete action: {}", completeAction);
						WorkItemType fullWorkItem = workItemProvider.toFullWorkItem(workItem, ctx.wfTask.getTaskType(), result);
						completeWorkItem(workItemId, fullWorkItem, completeAction.getOutcome(), null, null,
								causeInformation, true, result);
						continue;
					}
				}
				closeWorkItem(ctx, workItem, now, result);
				WorkItemType fullWorkItem = workItemProvider.toFullWorkItem(workItem, wfTask.getTask().getTaskType(), result);
				onWorkItemClosure(ctx, fullWorkItem, wfTask, false, causeInformation, result);
			}
		}
		LOGGER.trace("--- closeOtherWorkItems EXIT: ctx={}", ctx);
	}

	private CompleteWorkItemActionType willBeCompletedByTrigger(EngineInvocationContext ctx, CaseWorkItemType workItem,
			String workItemId) {
		LOGGER.trace("willBeCompletedByTrigger for {}", workItem);
		for (TriggerType trigger : ctx.wfTask.getTaskType().getTrigger()) {
			LOGGER.trace("considering trigger {}", trigger);
			String triggerWorkItemId = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
			WorkItemActionsType triggerActions = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
			if (WfTimedActionTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri())
					&& workItemId.equals(triggerWorkItemId)
					&& triggerActions != null && triggerActions.getComplete() != null
					&& trigger.getTimestamp() != null && XmlTypeConverter.toMillis(trigger.getTimestamp()) <= clock.currentTimeMillis()) {
				return triggerActions.getComplete();
			}
		}
		return null;
	}

	private void onWorkItemClosure(EngineInvocationContext ctx,
			WorkItemType workItem, WfTask wfTask,
			boolean realClosure,
			WorkItemEventCauseInformationType causeInformation,
			OperationResult result) {
		// this might be cancellation because of:
		//  (1) user completion of this task
		//  (2) timed completion of this task
		//  (3) user completion of another task
		//  (4) timed completion of another task
		//  (5) process stop/deletion
		//
		// Actually, when the source is (4) timed completion of another task, it is quite probable that this task
		// would be closed for the same reason. For a user it would be misleading if we would simply view this task
		// as 'cancelled', while, in fact, it is e.g. approved/rejected because of a timed action.

		LOGGER.trace("+++ onWorkItemClosure ENTER: workItem={}, ctx={}, realClosure={}", workItem, ctx, realClosure);
		WorkItemOperationKindType operationKind = realClosure ? WorkItemOperationKindType.COMPLETE : WorkItemOperationKindType.CANCEL;

		MidPointPrincipal user;
		try {
			user = SecurityUtil.getPrincipal();
		} catch (SecurityViolationException e) {
			throw new SystemException("Couldn't determine current user: " + e.getMessage(), e);
		}

		ObjectReferenceType userRef = user != null ? user.toObjectReference() : workItem.getPerformerRef();	// partial fallback

		// TODO This was an attempt to estimate "induced" task closure and correlate it to the real cause; we can do better now
//		if (!realClosure) {
//			TaskType task = wfTask.getTask().getTaskPrismObject().asObjectable();
//			int foundTimedActions = 0;
//			for (TriggerType trigger : task.getTrigger()) {
//				if (!WfTimedActionTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri())) {
//					continue;
//				}
//				String workItemId = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
//				if (!workItemEvent.getTaskId().equals(workItemId)) {
//					continue;
//				}
//				Duration timeBeforeAction = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_TIME_BEFORE_ACTION);
//				if (timeBeforeAction != null) {
//					continue;
//				}
//				WorkItemActionsType actions = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
//				if (actions == null || actions.getComplete() == null) {
//					continue;
//				}
//				long diff = XmlTypeConverter.toMillis(trigger.getTimestamp()) - clock.currentTimeMillis();
//				if (diff >= COMPLETION_TRIGGER_EQUALITY_THRESHOLD) {
//					continue;
//				}
//				CompleteWorkItemActionType completeAction = actions.getComplete();
//				operationKind = WorkItemOperationKindType.COMPLETE;
//				cause = new WorkItemEventCauseInformationType();
//				cause.setType(WorkItemEventCauseTypeType.TIMED_ACTION);
//				cause.setName(completeAction.getName());
//				cause.setDisplayName(completeAction.getDisplayName());
//				foundTimedActions++;
//				WorkItemResultType workItemOutput = new WorkItemResultType();
//				workItemOutput.setOutcome(completeAction.getOutcome() != null ? completeAction.getOutcome() : SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
//				workItem.setOutput(workItemOutput);
//			}
//			if (foundTimedActions > 1) {
//				LOGGER.warn("Multiple 'work item complete' timed actions ({}) for {}: {}", foundTimedActions,
//						ObjectTypeUtil.toShortString(task), task.getTrigger());
//			}
//		}

		// We don't pass userRef (initiator) to the audit method. It does need the whole object (not only the reference),
		// so it fetches it directly from the security enforcer (logged-in user). This could change in the future.
		AuditEventRecord auditEventRecord = wfTask.getChangeProcessor().prepareWorkItemDeletedAuditRecord(workItem, causeInformation, wfTask, result);
		auditService.audit(auditEventRecord, wfTask.getTask());
		try {
			List<ObjectReferenceType> assigneesAndDeputies = wfTaskController.getAssigneesAndDeputies(workItem, wfTask, result);
			WorkItemAllocationChangeOperationInfo operationInfo =
					new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputies, null);
			WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(userRef, causeInformation, null);
			if (workItem.getAssigneeRef().isEmpty()) {
				wfTaskController.notifyWorkItemDeleted(null, workItem, operationInfo, sourceInfo, wfTask, result);
			} else {
				for (ObjectReferenceType assigneeOrDeputy : assigneesAndDeputies) {
					wfTaskController.notifyWorkItemDeleted(assigneeOrDeputy, workItem, operationInfo, sourceInfo, wfTask, result);
				}
			}
			wfTaskController.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, null, wfTask.getTask(), result);
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't audit work item complete event", e);
		}

		String workItemExternalId = WorkflowInterface.createWorkItemId(ctx.getCaseOid(), workItem.getId());

		AbstractWorkItemOutputType output = workItem.getOutput();
		if (realClosure || output != null) {
			WorkItemCompletionEventType event = new WorkItemCompletionEventType();
			ActivitiUtil.fillInWorkItemEvent(event, user, workItemExternalId, workItem, prismContext);
			event.setCause(causeInformation);
			event.setOutput(output);
			ObjectDeltaType additionalDelta = output instanceof WorkItemResultType && ((WorkItemResultType) output).getAdditionalDeltas() != null ?
					((WorkItemResultType) output).getAdditionalDeltas().getFocusPrimaryDelta() : null;
			MidpointUtil.recordEventInTask(event, additionalDelta, wfTask.getTask().getOid(), result);
		}

		MidpointUtil.removeTriggersForWorkItem(wfTask.getTask(), workItemExternalId, result);

		LOGGER.trace("--- onWorkItemClosure EXIT: workItem={}, ctx={}, realClosure={}", workItem, ctx, realClosure);
	}

	public void deleteCase(String caseOid, OperationResult parentResult) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public void closeCase(String caseOid, String username, OperationResult result) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public SearchResultList<CaseWorkItemType> getWorkItemsForCase(String caseOid,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
		return repositoryService.searchContainers(CaseWorkItemType.class,
				prismContext.queryFor(CaseWorkItemType.class).ownerId(caseOid).build(),
				options, result);

	}

	public WfContextType getWorkflowContext(CaseWorkItemType caseWorkItem, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		CaseType wfCase = CaseWorkItemUtil.getCaseRequired(caseWorkItem);
		if (wfCase.getTaskRef() == null) {
			throw new IllegalStateException("No taskRef for case " + wfCase);
		}
		PrismObject<TaskType> taskObject = repositoryService.getObject(TaskType.class, wfCase.getTaskRef().getOid(), null, result);
		return taskObject.asObjectable().getWorkflowContext();
	}

	public Integer countWorkItems(ObjectQuery query, OperationResult result) {
		return repositoryService.countContainers(CaseWorkItemType.class, query, null, result);
	}

	public List<CaseWorkItemType> searchWorkItems(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult result) throws SchemaException {
		return repositoryService.searchContainers(CaseWorkItemType.class, query, options, result);
	}

	public void claim(String workItemId, String userOid, OperationResult result) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public void unclaim(String workItemId, OperationResult result) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public void createWorkItems(EngineInvocationContext ctx, List<CaseWorkItemType> workItems, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		LOGGER.trace("+++ createWorkItems ENTER: ctx={}, workItems={}", ctx, workItems);
		List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_WORK_ITEM).addRealValues(workItems)
				.asItemDeltas();
		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), modifications, result);
		refreshContext(ctx, result);
		replaceWorkItemsWithRepoVersions(ctx.wfCase, workItems);
		onWorkItemsCreation(ctx, workItems, result);
		LOGGER.trace("--- createWorkItems EXIT: ctx={}", ctx, workItems);
	}

	private void replaceWorkItemsWithRepoVersions(CaseType wfCase, List<CaseWorkItemType> workItems) {
		List<CaseWorkItemType> newItems = new ArrayList<>();
		item: for (CaseWorkItemType workItem : workItems) {
			for (CaseWorkItemType i : wfCase.getWorkItem()) {
				if (i.equals(workItem)) {
					newItems.add(i);
					continue item;
				}
			}
			throw new IllegalStateException("Work item " + workItem + " was not found among case work items in " + wfCase);
		}
		workItems.clear();
		workItems.addAll(newItems);
	}

	private void onWorkItemsCreation(EngineInvocationContext ctx, List<CaseWorkItemType> workItems, OperationResult result)
			throws SchemaException {
		LOGGER.trace("+++ onWorkItemsCreation ENTER: ctx={}, workItems={}", ctx, workItems);
		WfTask wfTask = getWfTask(ctx, result);
		List<WorkItemType> fullWorkItems = new ArrayList<>();
		for (CaseWorkItemType workItem : workItems) {
			fullWorkItems.add(workItemProvider.toFullWorkItem(workItem, ctx.getWfTask().getTaskType(), result));
		}
		ChangeProcessor changeProcessor = wfTask.getChangeProcessor();
		for (WorkItemType fullWorkItem : fullWorkItems) {
			AuditEventRecord auditEventRecord = changeProcessor.prepareWorkItemCreatedAuditRecord(fullWorkItem, wfTask, result);
			auditService.audit(auditEventRecord, ctx.opTask);
		}
		for (WorkItemType fullWorkItem : fullWorkItems) {
			try {
				List<ObjectReferenceType> assigneesAndDeputies = wfTaskController.getAssigneesAndDeputies(fullWorkItem, wfTask, result);
				for (ObjectReferenceType assigneesOrDeputy : assigneesAndDeputies) {
					wfTaskController.notifyWorkItemCreated(assigneesOrDeputy, fullWorkItem, wfTask, result);		// we assume originalAssigneeRef == assigneeRef in this case
				}
				WorkItemAllocationChangeOperationInfo operationInfo =
						new WorkItemAllocationChangeOperationInfo(null, Collections.emptyList(), assigneesAndDeputies);
				wfTaskController.notifyWorkItemAllocationChangeNewActors(fullWorkItem, operationInfo, null, wfTask.getTask(), result);
			} catch (SchemaException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't send notification about work item create event", e);
			}
		}
		LOGGER.trace("--- onWorkItemsCreation EXIT: ctx={}, workItems={}", ctx, workItems);
	}

	public String createWorkItemId(EngineInvocationContext ctx, CaseWorkItemType workItem) {
		return WorkflowInterface.createWorkItemId(ctx.getCaseOid(), workItem.getId());
	}

	private WfTask getWfTask(EngineInvocationContext ctx, OperationResult result) throws SchemaException {
		return wfTaskController.recreateWfTask(ctx.wfTask);
	}

	public void executeDelegation(String workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			WorkItemEscalationLevelType targetEscalationInfo, Duration newDuration, WorkItemEventCauseInformationType causeInformation,
			OperationResult result, MidPointPrincipal principal, ObjectReferenceType initiator, Task opTask,
			WorkItemType workItem) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

		EngineInvocationContext ctx = createInvocationContext(workItemId, result);
		long itemId = WorkflowInterface.getIdFromWorkItemId(workItemId);

		List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
		List<ObjectReferenceType> assigneesAndDeputiesBefore = wfTaskController.getAssigneesAndDeputies(workItem, opTask, result);

		WorkItemOperationKindType operationKind = targetEscalationInfo != null ? ESCALATE : DELEGATE;

		WorkItemAllocationChangeOperationInfo operationInfoBefore =
				new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, null);
		WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(initiator, causeInformation, null);
		wfTaskController.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfoBefore, sourceInfo, null, ctx.wfTask, result);

		if (method == null) {
			method = WorkItemDelegationMethodType.REPLACE_ASSIGNEES;
		}

		List<ObjectReferenceType> newAssignees = new ArrayList<>();
		List<ObjectReferenceType> delegatedTo = new ArrayList<>();
		WfContextUtil.computeAssignees(newAssignees, delegatedTo, delegates, method, workItem.getAssigneeRef());

		List<ItemDelta<?, ?>> workItemDeltas = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_WORK_ITEM, itemId, CaseWorkItemType.F_ASSIGNEE_REF).replaceRealValues(newAssignees)
				.asItemDeltas();
		if (newDuration != null) {
			XMLGregorianCalendar newDeadline = XmlTypeConverter.createXMLGregorianCalendar(new Date());
			newDeadline.add(newDuration);
			workItemDeltas.add(
					prismContext.deltaFor(CaseType.class)
							.item(CaseType.F_WORK_ITEM, itemId, CaseWorkItemType.F_DEADLINE).replace(newDeadline)
							.asItemDelta());
			workItem.setDeadline(newDeadline);
		}

		int escalationLevel = WfContextUtil.getEscalationLevelNumber(workItem);
		WorkItemEscalationLevelType newEscalationInfo;
		if (targetEscalationInfo != null) {
			newEscalationInfo = targetEscalationInfo.clone();
			newEscalationInfo.setNumber(++escalationLevel);
		} else {
			newEscalationInfo = null;
		}

		WorkItemDelegationEventType event = WfContextUtil.createDelegationEvent(newEscalationInfo, assigneesBefore, delegatedTo, method, causeInformation);
		if (newEscalationInfo != null) {
			workItemDeltas.add(
					prismContext.deltaFor(CaseType.class)
							.item(CaseType.F_WORK_ITEM, itemId, CaseWorkItemType.F_ESCALATION_LEVEL).replace(newEscalationInfo)
							.asItemDelta());
			workItem.setEscalationLevel(newEscalationInfo);
		}

		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), workItemDeltas, result);

		ActivitiUtil.fillInWorkItemEvent(event, principal, workItemId, workItem, prismContext);
		MidpointUtil.recordEventInTask(event, null, ctx.wfTask.getOid(), result);

		ApprovalStageDefinitionType level = WfContextUtil.getCurrentStageDefinition(ctx.wfContext);
		MidpointUtil.createTriggersForTimedActions(workItemId, escalationLevel,
				XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
				XmlTypeConverter.toDate(workItem.getDeadline()), ctx.wfTask, level.getTimedActions(), result);

		WorkItemType workItemAfter = workItemProvider.getWorkItem(workItemId, result);
		Task wfTaskAfter = taskManager.getTask(ctx.wfTask.getOid(), result);
		List<ObjectReferenceType> assigneesAndDeputiesAfter = wfTaskController.getAssigneesAndDeputies(workItemAfter, opTask, result);
		WorkItemAllocationChangeOperationInfo operationInfoAfter =
				new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, assigneesAndDeputiesAfter);
		wfTaskController.notifyWorkItemAllocationChangeNewActors(workItemAfter, operationInfoAfter, sourceInfo, wfTaskAfter, result);
	}

}
