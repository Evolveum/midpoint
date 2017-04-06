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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WfTimedActionTriggerHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.activiti.engine.delegate.DelegateTask;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getCacheRepositoryService;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_EVENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESSOR_SPECIFIC_STATE;

/**
 * @author mederly
 */
public class MidpointUtil {

	private static final Trace LOGGER = TraceManager.getTrace(MidpointUtil.class);

	public static ApprovalStageDefinitionType getApprovalStageDefinition(String taskOid) {
		RepositoryService cacheRepositoryService = getCacheRepositoryService();
		OperationResult result = new OperationResult(MidpointUtil.class.getName() + ".getApprovalStageDefinition");
		try {
			PrismObject<TaskType> task = cacheRepositoryService.getObject(TaskType.class, taskOid, null, result);
			return WfContextUtil.getCurrentStageDefinition(task.asObjectable().getWorkflowContext());
		} catch (Exception e) {
			throw new SystemException("Couldn't retrieve approval stage for task " + taskOid + ": " + e.getMessage(), e);
		}
	}

	// additional delta is a bit hack ... TODO refactor (but without splitting the modify operation!)
	public static void recordEventInTask(CaseEventType event, ObjectDeltaType additionalDelta, String taskOid, OperationResult result) {
		RepositoryService cacheRepositoryService = getCacheRepositoryService();
		PrismContext prismContext = getPrismContext();
		try {
			S_ItemEntry deltaBuilder = DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
					.item(F_WORKFLOW_CONTEXT, F_EVENT).add(event);

			if (additionalDelta != null) {
				PrismObject<TaskType> task = cacheRepositoryService.getObject(TaskType.class, taskOid, null, result);
				WfPrimaryChangeProcessorStateType state = WfContextUtil
						.getPrimaryChangeProcessorState(task.asObjectable().getWorkflowContext());
				ObjectTreeDeltasType updatedDelta = ObjectTreeDeltas.mergeDeltas(state.getDeltasToProcess(),
						additionalDelta, prismContext);
				ItemPath deltasToProcessPath = new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE, WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS);		// assuming it already exists!
				ItemDefinition<?> deltasToProcessDefinition = getPrismContext().getSchemaRegistry()
						.findContainerDefinitionByCompileTimeClass(WfPrimaryChangeProcessorStateType.class)
						.findItemDefinition(WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS);
				deltaBuilder = deltaBuilder.item(deltasToProcessPath, deltasToProcessDefinition)
						.replace(updatedDelta);
			}
			cacheRepositoryService.modifyObject(TaskType.class, taskOid, deltaBuilder.asItemDeltas(), result);
		} catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
			throw new SystemException("Couldn't record decision to the task " + taskOid + ": " + e.getMessage(), e);
		}
	}

	public static Set<ObjectReferenceType> expandGroups(Set<ObjectReferenceType> approverRefs) {
		PrismContext prismContext = getPrismContext();
		Set<ObjectReferenceType> rv = new HashSet<>();
		for (ObjectReferenceType approverRef : approverRefs) {
			@SuppressWarnings({ "unchecked", "raw" })
			Class<? extends Containerable> clazz = (Class<? extends Containerable>)
					prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(approverRef.getType());
			if (clazz == null) {
				throw new IllegalStateException("Unknown object type " + approverRef.getType());
			}
			if (UserType.class.isAssignableFrom(clazz)) {
				rv.add(approverRef.clone());
			} else if (AbstractRoleType.class.isAssignableFrom(clazz)) {
				rv.addAll(expandAbstractRole(approverRef, prismContext));
			} else {
				LOGGER.warn("Unexpected type {} for approver: {}", clazz, approverRef);
				rv.add(approverRef.clone());
			}
		}
		return rv;
	}

	private static Collection<ObjectReferenceType> expandAbstractRole(ObjectReferenceType approverRef, PrismContext prismContext) {
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(approverRef.asReferenceValue())
				.build();
		try {
			return getCacheRepositoryService()
					.searchObjects(UserType.class, query, null, new OperationResult("dummy"))
					.stream()
					.map(o -> ObjectTypeUtil.createObjectRef(o))
					.collect(Collectors.toList());
		} catch (SchemaException e) {
			throw new SystemException("Couldn't resolve " + approverRef + ": " + e.getMessage(), e);
		}
	}

	static void setTaskDeadline(DelegateTask delegateTask, Duration duration) {
		XMLGregorianCalendar deadline = XmlTypeConverter.createXMLGregorianCalendar(new Date());
		deadline.add(duration);
		delegateTask.setDueDate(XmlTypeConverter.toDate(deadline));
	}

	public static void createTriggersForTimedActions(String workItemId, int escalationLevel, Date workItemCreateTime,
			Date workItemDeadline, Task wfTask, List<WorkItemTimedActionsType> timedActionsList, OperationResult result) {
		LOGGER.trace("Creating triggers for timed actions for work item {}, escalation level {}, create time {}, deadline {}, {} timed action(s)",
				workItemId, escalationLevel, workItemCreateTime, workItemDeadline, timedActionsList.size());
		try {
			PrismContext prismContext = getPrismContext();
			List<TriggerType> triggers = new ArrayList<>();
			for (WorkItemTimedActionsType timedActionsEntry : timedActionsList) {
				Integer levelFrom;
				Integer levelTo;
				if (timedActionsEntry.getEscalationLevelFrom() == null && timedActionsEntry.getEscalationLevelTo() == null) {
					levelFrom = levelTo = 0;
				} else {
					levelFrom = timedActionsEntry.getEscalationLevelFrom();
					levelTo = timedActionsEntry.getEscalationLevelTo();
				}
				if (levelFrom != null && escalationLevel < levelFrom) {
					LOGGER.trace("Current escalation level is before 'escalationFrom', skipping timed actions {}", timedActionsEntry);
					continue;
				}
				if (levelTo != null && escalationLevel > levelTo) {
					LOGGER.trace("Current escalation level is after 'escalationTo', skipping timed actions {}", timedActionsEntry);
					continue;
				}
				// TODO evaluate the condition
				List<WfTimeSpecificationType> timeSpecifications = CloneUtil.cloneCollectionMembers(timedActionsEntry.getTime());
				if (timeSpecifications.isEmpty()) {
					timeSpecifications.add(new WfTimeSpecificationType());
				}
				for (WfTimeSpecificationType timeSpec : timeSpecifications) {
					if (timeSpec.getValue().isEmpty()) {
						timeSpec.getValue().add(XmlTypeConverter.createDuration(0));
					}
					for (Duration duration : timeSpec.getValue()) {
						XMLGregorianCalendar mainTriggerTime = computeTriggerTime(duration, timeSpec.getBase(),
								workItemCreateTime, workItemDeadline);
						TriggerType mainTrigger = createTrigger(mainTriggerTime, workItemId, timedActionsEntry.getActions(), null, prismContext);
						triggers.add(mainTrigger);
						List<Pair<Duration, AbstractWorkItemActionType>> notifyInfoList = getNotifyBefore(timedActionsEntry);
						for (Pair<Duration, AbstractWorkItemActionType> notifyInfo : notifyInfoList) {
							XMLGregorianCalendar notifyTime = (XMLGregorianCalendar) mainTriggerTime.clone();
							notifyTime.add(notifyInfo.getKey().negate());
							TriggerType notifyTrigger = createTrigger(notifyTime, workItemId, null, notifyInfo, prismContext);
							triggers.add(notifyTrigger);
						}
					}
				}
			}
			LOGGER.trace("Adding {} triggers to {}:\n{}", triggers.size(), wfTask,
					PrismUtil.serializeQuietlyLazily(prismContext, triggers));
			if (triggers.isEmpty()) {
				return;
			}
			List<PrismContainerValue<TriggerType>> pcvList = triggers.stream()
					.map(t -> (PrismContainerValue<TriggerType>) t.asPrismContainerValue())
					.collect(Collectors.toList());
			List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(TaskType.class, prismContext)
					.item(TaskType.F_TRIGGER).add(pcvList)
					.asItemDeltas();
			getCacheRepositoryService().modifyObject(TaskType.class, wfTask.getOid(), itemDeltas, result);
		} catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
			throw new SystemException("Couldn't add trigger(s) to " + wfTask + ": " + e.getMessage(), e);
		}
	}

	@NotNull
	private static TriggerType createTrigger(XMLGregorianCalendar triggerTime, String workItemId,
			WorkItemActionsType actions, Pair<Duration, AbstractWorkItemActionType> notifyInfo, PrismContext prismContext)
			throws SchemaException {
		TriggerType trigger = new TriggerType(prismContext);
		trigger.setTimestamp(triggerTime);
		trigger.setHandlerUri(WfTimedActionTriggerHandler.HANDLER_URI);
		ExtensionType extension = new ExtensionType(prismContext);
		trigger.setExtension(extension);

		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		// workItemId
		@SuppressWarnings("unchecked")
		@NotNull PrismPropertyDefinition<String> workItemIdDef =
				schemaRegistry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
		PrismProperty<String> workItemIdProp = workItemIdDef.instantiate();
		workItemIdProp.addRealValue(workItemId);
		extension.asPrismContainerValue().add(workItemIdProp);
		// actions
		if (actions != null) {
			@NotNull PrismContainerDefinition<WorkItemActionsType> workItemActionsDef =
					schemaRegistry.findContainerDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
			PrismContainer<WorkItemActionsType> workItemActionsCont = workItemActionsDef.instantiate();
			workItemActionsCont.add(actions.asPrismContainerValue().clone());
			extension.asPrismContainerValue().add(workItemActionsCont);
		}
		// time before + action
		if (notifyInfo != null) {
			@NotNull PrismContainerDefinition<AbstractWorkItemActionType> workItemActionDef =
					schemaRegistry.findContainerDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTION);
			PrismContainer<AbstractWorkItemActionType> workItemActionCont = workItemActionDef.instantiate();
			workItemActionCont.add(notifyInfo.getValue().asPrismContainerValue().clone());
			extension.asPrismContainerValue().add(workItemActionCont);
			@SuppressWarnings("unchecked")
			@NotNull PrismPropertyDefinition<Duration> timeBeforeActionDef =
					schemaRegistry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_TIME_BEFORE_ACTION);
			PrismProperty<Duration> timeBeforeActionProp = timeBeforeActionDef.instantiate();
			timeBeforeActionProp.addRealValue(notifyInfo.getKey());
			extension.asPrismContainerValue().add(timeBeforeActionProp);
		}
		return trigger;
	}

	private static List<Pair<Duration,AbstractWorkItemActionType>> getNotifyBefore(WorkItemTimedActionsType timedActions) {
		List<Pair<Duration,AbstractWorkItemActionType>> rv = new ArrayList<>();
		WorkItemActionsType actions = timedActions.getActions();
		if (actions.getComplete() != null) {
			collectNotifyBefore(rv, actions.getComplete());
		}
		if (actions.getDelegate() != null) {
			collectNotifyBefore(rv, actions.getDelegate());
		}
		if (actions.getEscalate() != null) {
			collectNotifyBefore(rv, actions.getEscalate());
		}
		return rv;
	}

	private static void collectNotifyBefore(List<Pair<Duration,AbstractWorkItemActionType>> rv, CompleteWorkItemActionType complete) {
		collectNotifyBefore(rv, complete.getNotifyBeforeAction(), complete);
	}

	private static void collectNotifyBefore(List<Pair<Duration,AbstractWorkItemActionType>> rv, DelegateWorkItemActionType delegate) {
		collectNotifyBefore(rv, delegate.getNotifyBeforeAction(), delegate);
	}

	private static void collectNotifyBefore(List<Pair<Duration, AbstractWorkItemActionType>> rv,
			List<Duration> beforeTimes, AbstractWorkItemActionType action) {
		beforeTimes.forEach(beforeTime -> rv.add(new ImmutablePair<>(beforeTime, action)));
	}

	@NotNull
	private static XMLGregorianCalendar computeTriggerTime(Duration duration, WfTimeBaseType base, Date start, Date deadline) {
		Date baseTime;
		if (base == null) {
			base = duration.getSign() <= 0 ? WfTimeBaseType.DEADLINE : WfTimeBaseType.WORK_ITEM_CREATION;
		}
		switch (base) {
			case DEADLINE:
				if (deadline == null) {
					throw new IllegalStateException("Couldn't set timed action relative to work item's deadline because"
							+ " the deadline is not set. Requested interval: " + duration);
				}
				baseTime = deadline;
				break;
			case WORK_ITEM_CREATION:
				if (start == null) {
					throw new IllegalStateException("Task's start time is null");
				}
				baseTime = start;
				break;
			default:
				throw new IllegalArgumentException("base: " + base);
		}
		XMLGregorianCalendar rv = XmlTypeConverter.createXMLGregorianCalendar(baseTime);
		rv.add(duration);
		return rv;
	}

	public static void removeTriggersForWorkItem(Task wfTask, String workItemId, OperationResult result) {
		List<PrismContainerValue<TriggerType>> toDelete = new ArrayList<>();
		for (TriggerType triggerType : wfTask.getTaskPrismObject().asObjectable().getTrigger()) {
			if (WfTimedActionTriggerHandler.HANDLER_URI.equals(triggerType.getHandlerUri())) {
				PrismProperty workItemIdProperty = triggerType.getExtension().asPrismContainerValue()
						.findProperty(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
				if (workItemIdProperty != null && workItemId.equals(workItemIdProperty.getRealValue())) {
					toDelete.add(triggerType.clone().asPrismContainerValue());
				}
			}
		}
		removeSelectedTriggers(wfTask, toDelete, result);
	}

	// not necessary any more, as work item triggers are deleted when the work item (task) is deleted
	// (and there are currently no triggers other than work-item-related)
	public static void removeAllStageTriggersForWorkItem(Task wfTask, OperationResult result) {
		List<PrismContainerValue<TriggerType>> toDelete = new ArrayList<>();
		for (TriggerType triggerType : wfTask.getTaskPrismObject().asObjectable().getTrigger()) {
			if (WfTimedActionTriggerHandler.HANDLER_URI.equals(triggerType.getHandlerUri())) {
				toDelete.add(triggerType.clone().asPrismContainerValue());
			}
		}
		removeSelectedTriggers(wfTask, toDelete, result);
	}

	private static void removeSelectedTriggers(Task wfTask, List<PrismContainerValue<TriggerType>> toDelete, OperationResult result) {
		try {
			LOGGER.trace("About to delete {} triggers from {}: {}", toDelete.size(), wfTask, toDelete);
			List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
					.item(TaskType.F_TRIGGER).delete(toDelete)
					.asItemDeltas();
			getCacheRepositoryService().modifyObject(TaskType.class, wfTask.getOid(), itemDeltas, result);
		} catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove triggers from task {}", e, wfTask);
		}
	}


}
