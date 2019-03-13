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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.api.CompleteAction;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.ModelHelper;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.api.CompleteAction.getWorkItems;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.F_EVENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESSOR_SPECIFIC_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.ESCALATE;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * This is a replacement of Activiti.
 */
@Component
public class WorkflowEngine {

	private static final Trace LOGGER = TraceManager.getTrace(WorkflowEngine.class);

	@Autowired private Clock clock;
	@Autowired private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;
	@Autowired private TaskManager taskManager;
	@Autowired private AuditService auditService;
	@Autowired private AuditHelper auditHelper;
	@Autowired private NotificationHelper notificationHelper;
	@Autowired private StageComputeHelper stageComputeHelper;
	@Autowired private PrimaryChangeProcessor primaryChangeProcessor;   // todo
	@Autowired private MiscHelper miscHelper;
	@Autowired private ModelHelper modelHelper;
	@Autowired private TriggerHelper triggerHelper;
	@Autowired private ExpressionEvaluationHelper expressionEvaluationHelper;

	public <CTX extends EngineInvocationContext> void startProcessInstance(CTX ctx, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {

		LOGGER.trace("+++ startProcessInstance ENTER: ctx={}", ctx);

		auditHelper.auditProcessStart(ctx.aCase, ctx.getWfContext(), primaryChangeProcessor, ctx.opTask, result);
		notificationHelper.notifyProcessStart(ctx.aCase, ctx.opTask, result);
		advanceProcessInstance(ctx, result);

		runTheProcess(ctx, result);

		LOGGER.trace("--- startProcessInstance EXIT: ctx={}", ctx);
	}

	private <CTX extends EngineInvocationContext> void runTheProcess(CTX ctx, OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

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
			advanceProcessInstance(ctx, result);
		}
	}

	private <CTX extends EngineInvocationContext> boolean isClosed(CTX ctx) {
		return SchemaConstants.CASE_STATE_CLOSED.equals(ctx.aCase.getState());
	}

	private <CTX extends EngineInvocationContext> boolean isWaiting(CTX ctx) {
		return ctx.aCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null);
	}

	private <CTX extends EngineInvocationContext> void refreshContext(CTX ctx, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		ctx.aCase = repositoryService.getObject(CaseType.class, ctx.getCaseOid(), null, result).asObjectable();
	}

	public void stopProcessInstance(EngineInvocationContext ctx, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LOGGER.trace("+++ stopProcessInstance ENTER: ctx={}", ctx);

		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		List<ItemDelta<?, ?>> caseModifications = new ArrayList<>();
		List<CaseWorkItemType> openWorkItems = ctx.aCase.getWorkItem().stream()
				.filter(wi -> wi.getCloseTimestamp() == null)
				.collect(Collectors.toList());
		for (CaseWorkItemType workItem : openWorkItems) {
			addWorkItemClosureModifications(ctx, workItem.getId(), now, caseModifications);
		}
		modifyCase(ctx, caseModifications, result);

		// TODO what about outcome? (beware of race conditions!)

		try {
			primaryChangeProcessor.onProcessEnd(ctx, result);
		} catch (PreconditionViolationException e) {
			throw new SystemException(e);       // TODO
		}

		auditHelper.auditProcessEnd(ctx.getCase(), ctx.getWfContext(), primaryChangeProcessor, ctx.opTask, result);
		notificationHelper.notifyProcessEnd(ctx.getCase(), ctx.opTask, result);

		// TODO execute the result!!!
		LOGGER.trace("--- stopProcessInstance EXIT: ctx={}", ctx);
	}

	public List<ItemDelta<?, ?>> prepareCaseClosureModifications(XMLGregorianCalendar now)
			throws SchemaException {
		return prismContext.deltaFor(CaseType.class)
					.item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_CLOSED)
					.item(CaseType.F_CLOSE_TIMESTAMP).replace(now)
					.asItemDeltas();
	}

	/**
	 * Closes work item in repository as well as in memory.
	 */
	private void closeWorkItem(EngineInvocationContext ctx, CaseWorkItemType workItem,
			XMLGregorianCalendar now, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LOGGER.trace("+++ closeWorkItem ENTER: workItem={}, ctx={}", workItem, ctx);
		List<ItemDelta<?, ?>> modifications = new ArrayList<>();
		addWorkItemClosureModifications(ctx, workItem.getId(), now, modifications);
		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), modifications, result);
		// removes "workItem[n]" from the item path to be directly applicable to the specific work item
		for (ItemDelta<?, ?> itemDelta : modifications) {
			itemDelta.setParentPath(itemDelta.getParentPath().rest(2));
			itemDelta.applyTo(workItem.asPrismContainerValue());
		}
		LOGGER.trace("--- closeWorkItem EXIT: workItem={}, ctx={}", workItem, ctx);
	}

	private void addWorkItemClosureModifications(EngineInvocationContext ctx, Long workItemId,
			XMLGregorianCalendar now, List<ItemDelta<?, ?>> modifications) throws SchemaException {
		modifications.addAll(prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_WORK_ITEM, workItemId, CaseWorkItemType.F_CLOSE_TIMESTAMP).replace(now)
				.asItemDeltas());
	}

	public void assertNoOpenWorkItems(CaseType aCase) {
		if (aCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null)) {
			throw new IllegalStateException("Open work item in " + aCase);
		}
	}

	// actions should have compatible causeInformation
	// TODO orchestrator
	public void completeWorkItems(Collection<CompleteAction> actions, MidPointPrincipal principal, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

		LOGGER.trace("+++ completeWorkItems ENTER: actions: {}", actions.size());
		if (actions.isEmpty()) {
			throw new IllegalArgumentException("No complete actions");
		}

		List<CompleteAction> alreadyCompleted = new ArrayList<>();
		for (Iterator<CompleteAction> iterator = actions.iterator(); iterator.hasNext(); ) {
			CompleteAction action = iterator.next();
			if (action.getWorkItem().getCloseTimestamp() != null) {
				alreadyCompleted.add(action);
				iterator.remove();
			}
		}
		if (!alreadyCompleted.isEmpty()) {
			LOGGER.warn("The following work items were already completed: {}", getWorkItems(alreadyCompleted));
			if (alreadyCompleted.size() == 1) {
				result.recordWarning("Work item " + alreadyCompleted.get(0).getWorkItemId() + " was already completed on "
						+ alreadyCompleted.get(0).getWorkItem().getCloseTimestamp());
			} else {
				result.recordWarning(alreadyCompleted.size() + " work items were already completed");
			}
			if (actions.isEmpty()) {
				return;
			}
		}

		EngineInvocationContext ctx = createInvocationContext(actions.iterator().next().getWorkItemId(), result);
		ApprovalStageDefinitionType stageDef = WfContextUtil.getCurrentStageDefinition(ctx.aCase);
		LevelEvaluationStrategyType levelEvaluationStrategyType = stageDef.getEvaluationStrategy();

		boolean stopTheStage = false;

		List<ItemDelta<?, ?>> caseModifications = new ArrayList<>();

		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		for (CompleteAction action : actions) {
			CaseWorkItemType workItem = action.getWorkItem();
			String outcome = action.getOutcome();

			String currentCaseOid = action.getWorkItemId().getCaseOid();
			if (!ctx.getCaseOid().equals(currentCaseOid)) {
				throw new IllegalArgumentException("Trying to complete work items from unrelated cases: " + ctx.getCaseOid() + " vs " + currentCaseOid);
			}

			LOGGER.trace("+++ recordCompletionOfWorkItem ENTER: workItem={}, outcome={}", workItem, outcome);
			LOGGER.trace("======================================== Recording individual decision of {}", principal);

			@NotNull WorkItemResultType itemResult = new WorkItemResultType(prismContext);
			itemResult.setOutcome(outcome);
			itemResult.setComment(action.getComment());
			boolean isApproved = ApprovalUtils.isApproved(itemResult);
			if (isApproved && action.getAdditionalDelta() != null) {
				ObjectDeltaType additionalDeltaBean = DeltaConvertor.toObjectDeltaType(action.getAdditionalDelta());
				ObjectTreeDeltasType treeDeltas = new ObjectTreeDeltasType();
				treeDeltas.setFocusPrimaryDelta(additionalDeltaBean);
				itemResult.setAdditionalDeltas(treeDeltas);
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Recording decision for approval process instance {} (case oid {}), stage {}: decision: {}",
						ctx.getProcessInstanceName(), ctx.getCaseOid(),
						WfContextUtil.getStageDiagName(stageDef), itemResult.getOutcome());
			}

			ObjectReferenceType performerRef = ObjectTypeUtil.createObjectRef(principal.getUser(), prismContext);
			caseModifications.addAll(prismContext.deltaFor(CaseType.class)
					.item(CaseType.F_WORK_ITEM, workItem.getId(), CaseWorkItemType.F_OUTPUT).replace(itemResult)
					.item(CaseType.F_WORK_ITEM, workItem.getId(), CaseWorkItemType.F_PERFORMER_REF).replace(performerRef)
					.asItemDeltas());
			addWorkItemClosureModifications(ctx, workItem.getId(), now, caseModifications);

			if (levelEvaluationStrategyType == LevelEvaluationStrategyType.FIRST_DECIDES) {
				LOGGER.trace("Finishing the stage, because the stage evaluation strategy is 'firstDecides'.");
				stopTheStage = true;
			} else if ((levelEvaluationStrategyType == null || levelEvaluationStrategyType == LevelEvaluationStrategyType.ALL_MUST_AGREE) && !isApproved) {
				LOGGER.trace("Finishing the stage, because the stage eval strategy is 'allMustApprove' and the decision was 'reject'.");
				stopTheStage = true;
			}
		}

		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), caseModifications, result);
		refreshContext(ctx, result);

		for (CompleteAction action : actions) {
			CaseWorkItemType workItem = ctx.findWorkItemById(action.getWorkItem().getId());
			onWorkItemClosure(ctx, workItem, true, action.getCauseInformation(), result);
		}

		refreshContext(ctx, result);

		boolean keepStageOpen;
		if (stopTheStage) {
			closeOtherWorkItems(ctx, actions.iterator().next().getCauseInformation(), now, result);
			refreshContext(ctx, result);
			keepStageOpen = false;
		} else {
			keepStageOpen = ctx.aCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null);
		}
		LOGGER.trace("computed keepStageOpen = {}", keepStageOpen);
		if (!keepStageOpen) {
			onStageClose(ctx, result);
		}

		if (LOGGER.isTraceEnabled()) {
			logCtx(ctx, "After completing work items: " + actions, result);
		}
		LOGGER.trace("--- completeWorkItems EXIT");
	}

	private void logCtx(EngineInvocationContext ctx, String message, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		String rootOid = ctx.aCase.getParentRef() != null ? ctx.aCase.getParentRef().getOid() : ctx.aCase.getOid();
		CaseType rootCase = repositoryService.getObject(CaseType.class, rootOid, null, result).asObjectable();
		LOGGER.trace("###### [ {} ] ######", message);
		LOGGER.trace("Root case:\n{}", modelHelper.dumpCase(rootCase));
		for (CaseType subcase : miscHelper.getSubcases(rootCase, result)) {
			LOGGER.trace("Subcase:\n{}", modelHelper.dumpCase(subcase));
		}
		LOGGER.trace("###### [ END OF {} ] ######", message);
	}

	public void onStageClose(EngineInvocationContext ctx, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		LOGGER.trace("+++ onStageClose ENTER: ctx={}", ctx);
		boolean stopTheProcess = closeTheStage(ctx, result);
		if (stopTheProcess) {
			recordProcessOutcome(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT, result);
			stopProcessInstance(ctx, result);
		} else {
			runTheProcess(ctx, result);        // temporary
		}
		LOGGER.trace("--- onStageClose EXIT: ctx={}", ctx);
	}

	private void recordProcessOutcome(EngineInvocationContext ctx, String processOutcome, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_OUTCOME)
				.replace(processOutcome)
				.asItemDeltas();
		modifyCase(ctx, modifications, result);
	}

	private void modifyCase(EngineInvocationContext ctx, List<ItemDelta<?, ?>> modifications, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), modifications, result);
		ItemDeltaCollectionsUtil.applyTo(modifications, ctx.aCase.asPrismContainerValue());
	}


	private boolean closeTheStage(EngineInvocationContext ctx, OperationResult result) {
		LOGGER.trace("+++ closeTheStage ENTER: ctx={}", ctx);
		ApprovalStageDefinitionType stageDef = WfContextUtil.getCurrentStageDefinition(ctx.aCase);
//		List<StageCompletionEventType> stageEvents = WfContextUtil.getEventsForCurrentStage(ctx.wfContext, StageCompletionEventType.class);
		boolean approved;
		StageComputeHelper.ComputationResult preStageComputationResult = ctx.getPreStageComputationResult();
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
			List<WorkItemCompletionEventType> itemEvents = WfContextUtil.getEventsForCurrentStage(ctx.aCase, WorkItemCompletionEventType.class);

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
							WfContextUtil.getBriefDiagInfo(ctx.aCase), itemEvents.size(), outcomes);
					itemEvents.sort(Comparator.nullsLast(Comparator.comparing(event -> XmlTypeConverter.toMillis(event.getTimestamp()))));
					WorkItemCompletionEventType first = itemEvents.get(0);
					approved = ApprovalUtils.isApproved(first.getOutput());
					LOGGER.warn("Possible race condition, so taking the first one: {} ({})", approved, first);
				}
			}
		}

		triggerHelper.removeAllStageTriggersForWorkItem(ctx.aCase, result);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Closing the stage for approval process instance {} (case oid {}), stage {}: result of this stage: {}",
					ctx.getProcessInstanceName(),
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

	private EngineInvocationContext createInvocationContext(WorkItemId workItemId, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		PrismObject<CaseType> caseObject = repositoryService.getObject(CaseType.class, workItemId.caseOid, null, result);
		// temporary (we should get the task from the upper layers)
		Task opTask = taskManager.createTaskInstance();
		MidPointPrincipal user = getMidPointPrincipal();
		if (user != null) {
			opTask.setOwner(user.getUser().asPrismObject());
		}
		return new EngineInvocationContext(caseObject.asObjectable(), opTask);
	}

	public void closeOtherWorkItems(EngineInvocationContext ctx, WorkItemEventCauseInformationType causeInformation, XMLGregorianCalendar now,
			OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		WorkItemEventCauseTypeType causeType = causeInformation != null ? causeInformation.getType() : null;
		LOGGER.trace("+++ closeOtherWorkItems ENTER: ctx={}, cause type={}", ctx, causeType);
		for (CaseWorkItemType workItem : ctx.aCase.getWorkItem()) {
			if (workItem.getCloseTimestamp() == null) {
				closeWorkItem(ctx, workItem, now, result);
				onWorkItemClosure(ctx, workItem, false, causeInformation, result);
			}
		}
		LOGGER.trace("--- closeOtherWorkItems EXIT: ctx={}", ctx);
	}

	private void onWorkItemClosure(EngineInvocationContext ctx, CaseWorkItemType workItem, boolean realClosure,
			WorkItemEventCauseInformationType causeInformation, OperationResult result) {
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

		// We don't pass userRef (initiator) to the audit method. It does need the whole object (not only the reference),
		// so it fetches it directly from the security enforcer (logged-in user). This could change in the future.
		AuditEventRecord auditEventRecord = primaryChangeProcessor.prepareWorkItemDeletedAuditRecord(workItem, causeInformation, ctx.aCase, result);
		auditService.audit(auditEventRecord, ctx.opTask);
		try {
			List<ObjectReferenceType> assigneesAndDeputies = miscHelper.getAssigneesAndDeputies(workItem, ctx.opTask, result);
			WorkItemAllocationChangeOperationInfo operationInfo =
					new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputies, null);
			WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(userRef, causeInformation, null);
			if (workItem.getAssigneeRef().isEmpty()) {
				notificationHelper.notifyWorkItemDeleted(null, workItem, operationInfo, sourceInfo, ctx.aCase, ctx.opTask, result);
			} else {
				for (ObjectReferenceType assigneeOrDeputy : assigneesAndDeputies) {
					notificationHelper.notifyWorkItemDeleted(assigneeOrDeputy, workItem, operationInfo, sourceInfo, ctx.aCase, ctx.opTask, result);
				}
			}
			notificationHelper.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, null, ctx.aCase, ctx.opTask, result);
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't audit work item complete event", e);
		}

		WorkItemId workItemId = WorkItemId.create(ctx.getCaseOid(), workItem.getId());

		AbstractWorkItemOutputType output = workItem.getOutput();
		if (realClosure || output != null) {
			WorkItemCompletionEventType event = new WorkItemCompletionEventType(prismContext);
			fillInWorkItemEvent(event, user, workItemId, workItem, prismContext);
			event.setCause(causeInformation);
			event.setOutput(output);
			ObjectDeltaType additionalDelta = output instanceof WorkItemResultType && ((WorkItemResultType) output).getAdditionalDeltas() != null ?
					((WorkItemResultType) output).getAdditionalDeltas().getFocusPrimaryDelta() : null;
			recordEventInCase(event, additionalDelta, ctx.getCaseOid(), result);
		}

		triggerHelper.removeTriggersForWorkItem(ctx.aCase, workItemId, result);

		LOGGER.trace("--- onWorkItemClosure EXIT: workItem={}, ctx={}, realClosure={}", workItem, ctx, realClosure);
	}

	public void deleteCase(String caseOid, OperationResult parentResult) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public void closeCase(String caseOid, Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		// TODO close case + work items + ...
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public void closeCaseInternal(CaseType aCase, Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LOGGER.debug("Marking case {} as closed", aCase);
		repositoryService.modifyObject(CaseType.class, aCase.getOid(),
				prepareCaseClosureModifications(clock.currentTimeXMLGregorianCalendar()), result);
	}

	/**
	 * We need to check
	 * 1) if there are any executable cases that depend on this one
	 * 2) if we can close the parent (root)
	 */
	public void checkDependentCases(String rootOid, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, PreconditionViolationException {
		CaseType aCase = repositoryService.getObject(CaseType.class, rootOid, null, result).asObjectable();
		if (CaseTypeUtil.isClosed(aCase)) {
			return;
		}
		List<CaseType> subcases = miscHelper.getSubcases(rootOid, result);
		List<String> openOids = subcases.stream()
				.filter(c -> !CaseTypeUtil.isClosed(c))
				.map(ObjectType::getOid)
				.collect(Collectors.toList());
		LOGGER.debug("open cases OIDs: {}", openOids);
		if (openOids.isEmpty()) {
			closeCaseInternal(aCase, task, result);
		} else {
			ObjectQuery query = prismContext.queryFor(TaskType.class)
					.item(TaskType.F_OBJECT_REF).ref(openOids.toArray(new String[0]))
					.and().item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING)
					.build();
			SearchResultList<PrismObject<TaskType>> waitingTasks = repositoryService
					.searchObjects(TaskType.class, query, null, result);
			LOGGER.debug("Waiting tasks: {}", waitingTasks);
			for (PrismObject<TaskType> waitingTask : waitingTasks) {
				String waitingCaseOid = waitingTask.asObjectable().getObjectRef().getOid();
				assert waitingCaseOid != null;
				List<CaseType> waitingCaseList = subcases.stream().filter(c -> waitingCaseOid.equals(c.getOid()))
						.collect(Collectors.toList());
				assert waitingCaseList.size() == 1;
				Set<String> prerequisiteOids = waitingCaseList.get(0).getPrerequisiteRef().stream()
						.map(ObjectReferenceType::getOid)
						.collect(Collectors.toSet());
				Collection<String> openPrerequisites = CollectionUtils.intersection(prerequisiteOids, openOids);
				LOGGER.trace("prerequisite OIDs = {}; intersection with open OIDs = {}", prerequisiteOids, openPrerequisites);
				if (openPrerequisites.isEmpty()) {
					LOGGER.trace("All prerequisites are fulfilled, going to release the task {}", waitingTask);
					taskManager.unpauseTask(taskManager.createTaskInstance(waitingTask, result), result);
				} else {
					LOGGER.trace("...task is not released and continues waiting for those cases");
				}
			}
		}
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

	public SearchResultList<CaseWorkItemType> searchWorkItems(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult result) throws SchemaException {
		return repositoryService.searchContainers(CaseWorkItemType.class, query, options, result);
	}

	public void claim(WorkItemId workItemId, MidPointPrincipal principal, Task task, OperationResult result) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public void unclaim(WorkItemId workItemId, MidPointPrincipal principal,
			Task task, OperationResult result) {
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
		replaceWorkItemsWithRepoVersions(ctx.aCase, workItems);
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
		for (CaseWorkItemType workItem : workItems) {
			AuditEventRecord auditEventRecord = primaryChangeProcessor.prepareWorkItemCreatedAuditRecord(workItem, ctx.aCase, result);
			auditService.audit(auditEventRecord, ctx.opTask);
		}
		for (CaseWorkItemType workItem : workItems) {
			try {
				List<ObjectReferenceType> assigneesAndDeputies = miscHelper.getAssigneesAndDeputies(workItem, ctx.opTask, result);
				for (ObjectReferenceType assigneesOrDeputy : assigneesAndDeputies) {
					notificationHelper.notifyWorkItemCreated(assigneesOrDeputy, workItem, ctx.aCase, ctx.opTask, result);		// we assume originalAssigneeRef == assigneeRef in this case
				}
				WorkItemAllocationChangeOperationInfo operationInfo =
						new WorkItemAllocationChangeOperationInfo(null, Collections.emptyList(), assigneesAndDeputies);
				notificationHelper.notifyWorkItemAllocationChangeNewActors(workItem, operationInfo, null, ctx.aCase, ctx.opTask, result);
			} catch (SchemaException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't send notification about work item create event", e);
			}
		}
		LOGGER.trace("--- onWorkItemsCreation EXIT: ctx={}, workItems={}", ctx, workItems);
	}

	public WorkItemId createWorkItemId(EngineInvocationContext ctx, CaseWorkItemType workItem) {
		return WorkItemId.create(ctx.getCaseOid(), workItem.getId());
	}

	public void executeDelegation(WorkItemId workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			WorkItemEscalationLevelType targetEscalationInfo, Duration newDuration, WorkItemEventCauseInformationType causeInformation,
			OperationResult result, MidPointPrincipal principal, ObjectReferenceType initiator, Task opTask,
			CaseWorkItemType workItem) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

		EngineInvocationContext ctx = createInvocationContext(workItemId, result);

		List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
		List<ObjectReferenceType> assigneesAndDeputiesBefore = miscHelper.getAssigneesAndDeputies(workItem, opTask, result);

		WorkItemOperationKindType operationKind = targetEscalationInfo != null ? ESCALATE : DELEGATE;

		WorkItemAllocationChangeOperationInfo operationInfoBefore =
				new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, null);
		WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(initiator, causeInformation, null);
		notificationHelper.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfoBefore, sourceInfo, null, ctx.aCase,
				ctx.opTask, result);

		if (method == null) {
			method = WorkItemDelegationMethodType.REPLACE_ASSIGNEES;
		}

		List<ObjectReferenceType> newAssignees = new ArrayList<>();
		List<ObjectReferenceType> delegatedTo = new ArrayList<>();
		WfContextUtil.computeAssignees(newAssignees, delegatedTo, delegates, method, workItem.getAssigneeRef());

		List<ItemDelta<?, ?>> workItemDeltas = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_WORK_ITEM, workItemId.id, CaseWorkItemType.F_ASSIGNEE_REF).replaceRealValues(newAssignees)
				.asItemDeltas();
		if (newDuration != null) {
			XMLGregorianCalendar newDeadline;
			if (workItem.getDeadline() != null) {
				newDeadline = (XMLGregorianCalendar) workItem.getDeadline().clone();
			} else {
				newDeadline = XmlTypeConverter.createXMLGregorianCalendar(new Date());
			}
			newDeadline.add(newDuration);
			workItemDeltas.add(
					prismContext.deltaFor(CaseType.class)
							.item(CaseType.F_WORK_ITEM, workItemId.id, CaseWorkItemType.F_DEADLINE).replace(newDeadline)
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

		WorkItemDelegationEventType event = WfContextUtil.createDelegationEvent(newEscalationInfo, assigneesBefore, delegatedTo, method, causeInformation, prismContext);
		if (newEscalationInfo != null) {
			workItemDeltas.add(
					prismContext.deltaFor(CaseType.class)
							.item(CaseType.F_WORK_ITEM, workItemId.id, CaseWorkItemType.F_ESCALATION_LEVEL).replace(newEscalationInfo)
							.asItemDelta());
			workItem.setEscalationLevel(newEscalationInfo);
		}

		repositoryService.modifyObject(CaseType.class, ctx.getCaseOid(), workItemDeltas, result);

		fillInWorkItemEvent(event, principal, workItemId, workItem, prismContext);
		recordEventInCase(event, null, ctx.getCaseOid(), result);

		ApprovalStageDefinitionType level = WfContextUtil.getCurrentStageDefinition(ctx.aCase);
		triggerHelper.createTriggersForTimedActions(workItemId, escalationLevel,
				XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
				XmlTypeConverter.toDate(workItem.getDeadline()), ctx.aCase, level.getTimedActions(), result);

		CaseWorkItemType workItemAfter = getWorkItem(workItemId, result);
		CaseType aCaseAfter = repositoryService.getObject(CaseType.class, ctx.getCaseOid(), null, result).asObjectable();
		List<ObjectReferenceType> assigneesAndDeputiesAfter = miscHelper.getAssigneesAndDeputies(workItemAfter, opTask, result);
		WorkItemAllocationChangeOperationInfo operationInfoAfter =
				new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, assigneesAndDeputiesAfter);
		notificationHelper.notifyWorkItemAllocationChangeNewActors(workItemAfter, operationInfoAfter, sourceInfo, aCaseAfter, ctx.opTask, result);
	}

	private static void fillInWorkItemEvent(WorkItemEventType event, MidPointPrincipal currentUser, WorkItemId workItemId,
			CaseWorkItemType workItem, PrismContext prismContext) {
		if (currentUser != null) {
			event.setInitiatorRef(ObjectTypeUtil.createObjectRef(currentUser.getUser(), prismContext));
			event.setAttorneyRef(ObjectTypeUtil.createObjectRef(currentUser.getAttorney(), prismContext));
		}
		event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
		event.setExternalWorkItemId(workItemId.asString());
		event.setOriginalAssigneeRef(workItem.getOriginalAssigneeRef());
		event.setStageNumber(workItem.getStageNumber());
		event.setEscalationLevel(workItem.getEscalationLevel());
	}

	public void advanceProcessInstance(EngineInvocationContext ctx, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		int stageCount = getStageCount(ctx);
		int currentStage = getCurrentStage(ctx);
		if (currentStage == stageCount) {
			ctx.setDone(true);
			return;
		}

		int stageToBe = currentStage + 1;
		List<ItemDelta<?, ?>> stageModifications = prismContext.deltaFor(CaseType.class)
				.item(CaseType.F_STAGE_NUMBER).replace(stageToBe)
				.asItemDeltas();
		modifyCase(ctx, stageModifications, result);

		ApprovalStageDefinitionType stageDef = WfContextUtil.getCurrentStageDefinition(ctx.aCase);

		StageComputeHelper.ComputationResult computationResult =
				stageComputeHelper.computeStageApprovers(stageDef,
						() -> stageComputeHelper.getDefaultVariables(ctx.aCase, ctx.getWfContext(), ctx.opTask.getChannel(), result), ctx.opTask, result);

		ctx.setPreStageComputationResult(computationResult);
		ApprovalLevelOutcomeType predeterminedOutcome = computationResult.getPredeterminedOutcome();
		Set<ObjectReferenceType> approverRefs = computationResult.getApproverRefs();

		if (LOGGER.isDebugEnabled()) {
			if (computationResult.noApproversFound()) {
				LOGGER.debug("No approvers at the stage '{}' for process {} (case oid {}) - outcome-if-no-approvers is {}", stageDef.getName(),
						ctx.getProcessInstanceName(), ctx.aCase.getOid(), stageDef.getOutcomeIfNoApprovers());
			}
			LOGGER.debug("Approval process instance {} (case oid {}), stage {}: predetermined outcome: {}, approvers: {}",
					ctx.getProcessInstanceName(), ctx.aCase.getOid(),
					WfContextUtil.getStageDiagName(stageDef), predeterminedOutcome, approverRefs);
		}

		if (predeterminedOutcome != null) {
			onStageClose(ctx, result);
			return;
		}

		List<CaseWorkItemType> workItems = new ArrayList<>();
		XMLGregorianCalendar createTimestamp = clock.currentTimeXMLGregorianCalendar();
		XMLGregorianCalendar deadline;
		if (stageDef.getDuration() != null) {
			deadline = (XMLGregorianCalendar) createTimestamp.clone();
			deadline.add(stageDef.getDuration());
		} else {
			deadline = null;
		}
		for (ObjectReferenceType approverRef : approverRefs) {
			CaseWorkItemType workItem = new CaseWorkItemType(prismContext)
					.name(ctx.getProcessInstanceName())
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
					ExpressionVariables variables = stageComputeHelper.getDefaultVariables(ctx.aCase, ctx.getWfContext(), ctx.getChannel(), result);
					List<InformationType> additionalInformation = expressionEvaluationHelper.evaluateExpression(stageDef.getAdditionalInformation(), variables,
							"additional information expression", InformationType.class, InformationType.COMPLEX_TYPE,
							true, this::createInformationType, ctx.opTask, result);
					workItem.getAdditionalInformation().addAll(additionalInformation);
				} catch (Throwable t) {
					throw new SystemException("Couldn't evaluate additional information expression in " + ctx, t);
				}
			}
			workItems.add(workItem);
		}
		createWorkItems(ctx, workItems, result);
		for (CaseWorkItemType workItem : workItems) {
			triggerHelper.createTriggersForTimedActions(createWorkItemId(ctx, workItem), 0,
					XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
					XmlTypeConverter.toDate(workItem.getDeadline()), ctx.aCase, stageDef.getTimedActions(), result);
		}
	}

	private Integer getCurrentStage(EngineInvocationContext ctx) {
		int rv = defaultIfNull(ctx.aCase.getStageNumber(), 0);
		checkCurrentStage(ctx, rv);
		return rv;
	}

	private void checkCurrentStage(EngineInvocationContext ctx, int rv) {
		if (rv < 0 || rv > getStageCount(ctx)) {
			LOGGER.error("Current stage is below 0 or beyond the number of stages: {}\n{}", rv, ctx.debugDump());
			throw new IllegalStateException("Current stage is below 0 or beyond the number of stages: " + rv);
		}
	}

	private int getStageCount(EngineInvocationContext ctx) {
		Integer stageCount = WfContextUtil.getStageCount(ctx.getWfContext());
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
		StageCompletionEventType event = new StageCompletionEventType(prismContext);
		event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
		event.setStageNumber(stageNumber);
		event.setAutomatedDecisionReason(reason);
		event.setOutcome(ApprovalUtils.toUri(outcome));
		recordEventInCase(event, null, taskOid, opResult);
	}

	@NotNull
	public CaseWorkItemType getWorkItem(WorkItemId id, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		PrismObject<CaseType> caseObject = repositoryService.getObject(CaseType.class, id.caseOid, null, result);
		//noinspection unchecked
		PrismContainerValue<CaseWorkItemType> pcv = (PrismContainerValue<CaseWorkItemType>)
				caseObject.find(ItemPath.create(CaseType.F_WORK_ITEM, id.id));
		if (pcv == null) {
			throw new ObjectNotFoundException("No work item " + id.id + " in " + caseObject);
		}
		return pcv.asContainerable();
	}

	// additional delta is a bit hack ... TODO refactor (but without splitting the modify operation!)
	private void recordEventInCase(CaseEventType event, ObjectDeltaType additionalDelta, String caseOid, OperationResult result) {
		try {
			S_ItemEntry deltaBuilder = prismContext.deltaFor(CaseType.class)
					.item(F_EVENT).add(event);

			if (additionalDelta != null) {
				PrismObject<CaseType> aCase = repositoryService.getObject(CaseType.class, caseOid, null, result);
				WfPrimaryChangeProcessorStateType state = WfContextUtil
						.getPrimaryChangeProcessorState(aCase.asObjectable().getWorkflowContext());
				ObjectTreeDeltasType updatedDelta = ObjectTreeDeltas.mergeDeltas(state.getDeltasToProcess(),
						additionalDelta, prismContext);
				ItemPath deltasToProcessPath = ItemPath.create(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE,
						WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS);		// assuming it already exists!
				ItemDefinition<?> deltasToProcessDefinition = prismContext.getSchemaRegistry()
						.findContainerDefinitionByCompileTimeClass(WfPrimaryChangeProcessorStateType.class)
						.findItemDefinition(WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS);
				deltaBuilder = deltaBuilder.item(deltasToProcessPath, deltasToProcessDefinition)
						.replace(updatedDelta);
			}
			repositoryService.modifyObject(CaseType.class, caseOid, deltaBuilder.asItemDeltas(), result);
		} catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
			throw new SystemException("Couldn't record decision to the case " + caseOid + ": " + e.getMessage(), e);
		}
	}

}
