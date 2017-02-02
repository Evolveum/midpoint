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
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRefImpl;
import com.evolveum.midpoint.wf.impl.processes.common.WfTimedActionTriggerHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateTask;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getCacheRepositoryService;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESSOR_SPECIFIC_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESS_SPECIFIC_STATE;

/**
 * @author mederly
 */
public class MidpointUtil {

	private static final Trace LOGGER = TraceManager.getTrace(MidpointUtil.class);

	public static ApprovalLevelType getApprovalLevelType(String taskOid) {
		RepositoryService cacheRepositoryService = getCacheRepositoryService();
		OperationResult result = new OperationResult(MidpointUtil.class.getName() + ".getApprovalLevelType");
		try {
			PrismObject<TaskType> task = cacheRepositoryService.getObject(TaskType.class, taskOid, null, result);
			return WfContextUtil.getCurrentApprovalLevel(task.asObjectable().getWorkflowContext());
		} catch (Exception e) {
			throw new SystemException("Couldn't retrieve approval level for task " + taskOid + ": " + e.getMessage(), e);
		}
	}

	public static void recordDecisionInTask(Decision decision, String taskOid) {
		RepositoryService cacheRepositoryService = getCacheRepositoryService();
		PrismContext prismContext = getPrismContext();
		OperationResult result = new OperationResult(MidpointUtil.class.getName() + ".recordDecisionInTask");
		try {
			DecisionType decisionType = decision.toDecisionType(prismContext);
			ItemPath decisionsPath = new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESS_SPECIFIC_STATE, ItemApprovalProcessStateType.F_DECISIONS);		// assuming it already exists!
			ItemDefinition<?> decisionsDefinition = getPrismContext().getSchemaRegistry()
					.findContainerDefinitionByCompileTimeClass(ItemApprovalProcessStateType.class)
					.findItemDefinition(ItemApprovalProcessStateType.F_DECISIONS);
			S_ItemEntry deltaBuilder = DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
					.item(decisionsPath, decisionsDefinition).add(decisionType);

			if (decisionType.getAdditionalDelta() != null) {
				PrismObject<TaskType> task = cacheRepositoryService.getObject(TaskType.class, taskOid, null, result);
				WfPrimaryChangeProcessorStateType state = WfContextUtil
						.getPrimaryChangeProcessorState(task.asObjectable().getWorkflowContext());
				ObjectTreeDeltasType updatedDelta = ObjectTreeDeltas.mergeDeltas(state.getDeltasToProcess(),
						decisionType.getAdditionalDelta(), prismContext);
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

	public static Set<LightweightObjectRef> expandGroups(Set<LightweightObjectRef> approverRefs) {
		PrismContext prismContext = getPrismContext();
		Set<LightweightObjectRef> rv = new HashSet<>();
		for (LightweightObjectRef approverRef : approverRefs) {
			Class<? extends Containerable> clazz = (Class<? extends Containerable>)
					prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(approverRef.getType());
			if (clazz == null) {
				throw new IllegalStateException("Unknown object type " + approverRef.getType());
			}
			if (UserType.class.isAssignableFrom(clazz)) {
				rv.add(approverRef);
			} else if (AbstractRoleType.class.isAssignableFrom(clazz)) {
				rv.addAll(expandAbstractRole(approverRef, prismContext));
			} else {
				LOGGER.warn("Unexpected type {} for approver: {}", clazz, approverRef);
				rv.add(approverRef);
			}
		}
		return rv;
	}

	private static Collection<? extends LightweightObjectRef> expandAbstractRole(LightweightObjectRef approverRef, PrismContext prismContext) {
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(approverRef.toObjectReferenceType().asReferenceValue())
				.build();
		List<PrismObject<UserType>> objects;
		try {
			objects = getCacheRepositoryService()
					.searchObjects(UserType.class, query, null, new OperationResult("dummy"));
		} catch (SchemaException e) {
			throw new SystemException("Couldn't resolve " + approverRef + ": " + e.getMessage(), e);
		}
		return objects.stream()
				.map(o -> new LightweightObjectRefImpl(o.getOid(), UserType.COMPLEX_TYPE, null))
				.collect(Collectors.toList());
	}

	public static void setTaskDeadline(DelegateTask delegateTask, Duration duration, OperationResult result) {
		XMLGregorianCalendar deadline = XmlTypeConverter.createXMLGregorianCalendar(new Date());
		deadline.add(duration);
		delegateTask.setDueDate(XmlTypeConverter.toDate(deadline));
	}

	public static void createTriggersForTimedActions(DelegateTask delegateTask, Task wfTask,
			List<WorkItemTimedActionsType> timedActions,
			OperationResult result) {
		try {
			PrismContext prismContext = getPrismContext();
			SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
			@SuppressWarnings("unchecked")
			@NotNull PrismPropertyDefinition<String> workItemIdDef =
					schemaRegistry.findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_WORK_ITEM_ID);
			@NotNull PrismContainerDefinition<WorkItemActionsType> workItemActionsDef =
					schemaRegistry.findContainerDefinitionByElementName(SchemaConstantsGenerated.C_WORK_ITEM_ACTIONS);
			List<TriggerType> triggers = new ArrayList<>();
			for (WorkItemTimedActionsType timedAction : timedActions) {
				List<Duration> durations = new ArrayList<>(timedAction.getTime());
				if (durations.isEmpty()) {
					durations.add(XmlTypeConverter.createDuration(0));
				}
				for (Duration duration : durations) {
					TriggerType trigger = new TriggerType(prismContext);
					trigger.setTimestamp(computeTriggerTime(duration, delegateTask.getCreateTime(), delegateTask.getDueDate()));
					trigger.setHandlerUri(WfTimedActionTriggerHandler.HANDLER_URI);
					ExtensionType extension = new ExtensionType(prismContext);
					trigger.setExtension(extension);
					PrismProperty<String> workItemIdProp = workItemIdDef.instantiate();
					workItemIdProp.addRealValue(delegateTask.getId());
					extension.asPrismContainerValue().add(workItemIdProp);
					PrismContainer<WorkItemActionsType> workItemActionsCont = workItemActionsDef.instantiate();
					workItemActionsCont.add(timedAction.getActions().asPrismContainerValue().clone());
					extension.asPrismContainerValue().add(workItemActionsCont);
					triggers.add(trigger);
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
	private static XMLGregorianCalendar computeTriggerTime(Duration duration, Date start, Date deadline) {
		Date base;
		if (duration.getSign() <= 0) {
			if (deadline == null) {
				throw new IllegalStateException("Couldn't set timed action relative to work item's deadline because"
						+ " the deadline is not set. Requested interval: " + duration);
			}
			base = deadline;
		} else {
			if (start == null) {
				throw new IllegalStateException("Task's start time is null");
			}
			base = start;
		}
		XMLGregorianCalendar rv = XmlTypeConverter.createXMLGregorianCalendar(base);
		rv.add(duration);
		return rv;
	}
}
