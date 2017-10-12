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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static java.util.Collections.emptyList;

/**
 * TODO clean up these formatting methods
 *
 * @author mederly
 */
public class WfContextUtil {

	private static final Trace LOGGER = TraceManager.getTrace(WfContextUtil.class);

	@Nullable
	public static String getStageInfo(WfContextType wfc) {
		if (wfc == null || hasFinished(wfc)) {
			return null;
		}
		return getStageInfo(wfc.getStageNumber(), getStageCount(wfc), getStageName(wfc), getStageDisplayName(wfc));
	}

	@Nullable
	public static String getStageInfo(WorkItemType workItem) {
		if (workItem == null) {
			return null;
		}
		return getStageInfo(getWorkflowContext(workItem));
	}

	public static Integer getStageCount(WorkItemType workItem) {
		return getStageCount(getWorkflowContext(workItem));
	}

	public static String getStageName(WorkItemType workItem) {
		return getStageName(getWorkflowContext(workItem));
	}

	public static String getStageName(WfContextType wfc) {
		ApprovalStageDefinitionType def = getCurrentStageDefinition(wfc);
		return def != null ? def.getName() : null;
	}

	public static String getStageDisplayName(WfContextType wfc) {
		ApprovalStageDefinitionType def = getCurrentStageDefinition(wfc);
		return def != null ? def.getDisplayName() : null;
	}

	public static ApprovalSchemaType getApprovalSchema(WfContextType wfc) {
		ItemApprovalProcessStateType info = getItemApprovalProcessInfo(wfc);
		return info != null ? info.getApprovalSchema() : null;
	}

	public static Integer getStageCount(WfContextType wfc) {
		ApprovalSchemaType schema = getApprovalSchema(wfc);
		return schema != null ? schema.getStage().size() : null;
	}

	public static String getStageDisplayName(WorkItemType workItem) {
		return getStageDisplayName(getWorkflowContext(workItem));
	}

	// wfc is used to retrieve approval schema (if needed)
	public static String getStageInfo(Integer stageNumber, Integer stageCount, String stageName, String stageDisplayName) {
		String name = stageDisplayName != null ? stageDisplayName : stageName;
		if (name == null && stageNumber == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		if (name != null) {
			sb.append(name);
		}
		appendNumber(stageNumber, stageCount, sb);
		return sb.toString();
	}

	@Nullable
	public static String getEscalationLevelInfo(AbstractWorkItemType workItem) {
		if (workItem == null) {
			return null;
		}
		return getEscalationLevelInfo(workItem.getEscalationLevel());
	}

	// TODO move to better place
	public static String getEscalationLevelInfo(WorkItemEscalationLevelType e) {
		if (e == null || e.getNumber() == null || e.getNumber()  == 0) {
			return null;
		}
		String name = e.getDisplayName() != null ? e.getDisplayName() : e.getName();
		if (name != null) {
			return name + " (" + e.getNumber() + ")";
		} else {
			return String.valueOf(e.getNumber());
		}
	}

	public static boolean hasFinished(WfContextType wfc) {
		return wfc.getEndTimestamp() != null;
	}

	@Nullable
	public static String getCompleteStageInfo(WfContextType wfc) {
		if (wfc == null || hasFinished(wfc)) {
			return null;
		}
		Integer stageNumber = wfc.getStageNumber();
		String stageName = getStageName(wfc);
		String stageDisplayName = getStageDisplayName(wfc);
		if (stageNumber == null && stageName == null && stageDisplayName == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		if (stageName != null && stageDisplayName != null) {
			sb.append(stageName).append(" (").append(stageDisplayName).append(")");
		} else if (stageName != null) {
			sb.append(stageName);
		} else if (stageDisplayName != null) {
			sb.append(stageDisplayName);
		}
		appendNumber(stageNumber, getStageCount(wfc), sb);
		return sb.toString();
	}

	private static void appendNumber(Integer stageNumber, Integer stageCount, StringBuilder sb) {
		if (stageNumber != null) {
			boolean parentheses = sb.length() > 0;
			if (parentheses) {
				sb.append(" (");
			}
			sb.append(stageNumber);
			if (stageCount != null) {
				sb.append("/").append(stageCount);
			}
			if (parentheses) {
				sb.append(")");
			}
		}
	}

	public static ItemApprovalProcessStateType getItemApprovalProcessInfo(WfContextType wfc) {
		if (wfc == null) {
			return null;
		}
		WfProcessSpecificStateType processSpecificState = wfc.getProcessSpecificState();
		return processSpecificState instanceof ItemApprovalProcessStateType ?
				(ItemApprovalProcessStateType) processSpecificState : null;
	}

	public static WfPrimaryChangeProcessorStateType getPrimaryChangeProcessorState(WfContextType wfc) {
		if (wfc == null) {
			return null;
		}
		WfProcessorSpecificStateType state = wfc.getProcessorSpecificState();
		return state instanceof WfPrimaryChangeProcessorStateType ?
				(WfPrimaryChangeProcessorStateType) state : null;
	}

	public static ItemApprovalWorkItemPartType getItemApprovalWorkItemInfo(WorkItemType workItem) {
		return workItem.getProcessSpecificPart() instanceof ItemApprovalWorkItemPartType ?
				(ItemApprovalWorkItemPartType) workItem.getProcessSpecificPart() : null;
	}

	@NotNull
	public static List<SchemaAttachedPolicyRuleType> getAttachedPolicyRules(WfContextType workflowContext, int order) {
		ItemApprovalProcessStateType info = getItemApprovalProcessInfo(workflowContext);
		if (info == null || info.getPolicyRules() == null) {
			return emptyList();
		}
		return info.getPolicyRules().getEntry().stream()
				.filter(e -> e.getStageMax() != null && e.getStageMax() != null
						&& order >= e.getStageMin() && order <= e.getStageMax())
				.collect(Collectors.toList());
	}

	public static ApprovalStageDefinitionType getCurrentStageDefinition(WfContextType wfc) {
		if (wfc == null || wfc.getStageNumber() == null) {
			return null;
		}
		return getStageDefinition(wfc, wfc.getStageNumber());
	}

	// expects already normalized definition (using non-deprecated items, numbering stages from 1 to N)
	public static ApprovalStageDefinitionType getStageDefinition(WfContextType wfc, int stageNumber) {
		ItemApprovalProcessStateType info = getItemApprovalProcessInfo(wfc);
		if (info == null || info.getApprovalSchema() == null) {
			return null;
		}
		ApprovalSchemaType approvalSchema = info.getApprovalSchema();
		List<ApprovalStageDefinitionType> stages = approvalSchema.getStage().stream()
				.filter(level -> level.getNumber() != null && level.getNumber() == stageNumber)
				.collect(Collectors.toList());
		if (stages.size() > 1) {
			throw new IllegalStateException("More than one level with order of " + stageNumber + ": " + stages);
		} else if (stages.isEmpty()) {
			return null;
		} else {
			return stages.get(0);
		}
	}

	public static List<ApprovalStageDefinitionType> getStages(ApprovalSchemaType approvalSchema) {
		return !approvalSchema.getStage().isEmpty() ? approvalSchema.getStage() : approvalSchema.getLevel();
	}

	// we must be strict here; in case of suspicion, throw an exception
	@SuppressWarnings("unchecked")
	public static <T extends CaseEventType> List<T> getEventsForCurrentStage(@NotNull WfContextType wfc, @NotNull Class<T> clazz) {
		if (wfc.getStageNumber() == null) {
			throw new IllegalArgumentException("No stage number in workflow context; pid = " + wfc.getProcessInstanceId());
		}
		int stageNumber = wfc.getStageNumber();
		return wfc.getEvent().stream()
				.filter(e -> clazz.isAssignableFrom(e.getClass()) && e.getStageNumber() != null && stageNumber == e.getStageNumber())
				.map(e -> (T) e)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public static <T extends CaseEventType> List<T> getEvents(@NotNull WfContextType wfc, @NotNull Class<T> clazz) {
		return wfc.getEvent().stream()
				.filter(e -> clazz.isAssignableFrom(e.getClass()))
				.map(e -> (T) e)
				.collect(Collectors.toList());
	}

	public static <T extends WorkItemEventType> List<T> getWorkItemEvents(@NotNull WfContextType wfc, @NotNull String workItemId, Class<T> clazz) {
		return wfc.getEvent().stream()
				.filter(e -> clazz.isAssignableFrom(e.getClass()) && workItemId.equals(((WorkItemEventType) e).getExternalWorkItemId()))
				.map(e -> (T) e)
				.collect(Collectors.toList());
	}

	public static String getBriefDiagInfo(WfContextType wfc) {
		if (wfc == null) {
			return "null";
		}
		return "pid: " + wfc.getProcessInstanceId() + ", name: " + wfc.getProcessInstanceName() + ", stage: " + wfc.getStageNumber();
	}

	@NotNull
	public static String getCurrentStageOutcome(WfContextType wfc, List<StageCompletionEventType> stageEvents) {
		if (stageEvents.size() > 1) {
			throw new IllegalStateException("More than one stage-level event in " + getBriefDiagInfo(wfc) + ": " + stageEvents);
		}
		StageCompletionEventType event = stageEvents.get(0);
		if (event.getOutcome() == null) {
			throw new IllegalStateException("No outcome for stage-level event in " + getBriefDiagInfo(wfc));
		}
		return event.getOutcome();
	}

	// expects normalized definition
	public static String getStageDiagName(ApprovalStageDefinitionType level) {
		return level.getNumber() + ":" + level.getName()
				+ (level.getDisplayName() != null ? " (" + level.getDisplayName() + ")" : "");
	}

	public static void normalizeStages(ApprovalSchemaType schema) {
		// Sorting uses set(..) method which is not available on prism structures. So we do sort on a copy (ArrayList).
		List<ApprovalStageDefinitionType> stages = getSortedStages(schema);
		for (int i = 0; i < stages.size(); i++) {
			stages.get(i).setOrder(null);
			stages.get(i).setNumber(i+1);
		}
		schema.getLevel().clear();
		schema.getStage().clear();
		schema.getStage().addAll(CloneUtil.cloneCollectionMembers(stages));
	}

	@NotNull
	private static List<ApprovalStageDefinitionType> getSortedStages(ApprovalSchemaType schema) {
		List<ApprovalStageDefinitionType> stages = new ArrayList<>(getStages(schema));
		stages.sort(Comparator.comparing(stage -> getNumber(stage), Comparator.nullsLast(Comparator.naturalOrder())));
		return stages;
	}

	public static List<ApprovalStageDefinitionType> sortAndCheckStages(ApprovalSchemaType schema) {
		List<ApprovalStageDefinitionType> stages = getSortedStages(schema);
		for (int i = 0; i < stages.size(); i++) {
			ApprovalStageDefinitionType stage = stages.get(i);
			Integer number = getNumber(stage);
			if (number == null || number != i+1) {
				throw new IllegalArgumentException("Missing or wrong number of stage #" + (i+1) + ": " + number);
			}
			stage.setOrder(null);
			stage.setNumber(number);
		}
		return stages;
	}

	private static Integer getNumber(ApprovalStageDefinitionType stage) {
		return stage.getNumber() != null ? stage.getNumber() : stage.getOrder();
	}

	//	public static void checkLevelsOrdering(ApprovalSchemaType schema) {
//		for (int i = 0; i < schema.getLevel().size(); i++) {
//			ApprovalStageDefinitionType level = schema.getLevel().get(i);
//			if (level.getOrder() == null) {
//				throw new IllegalStateException("Level without order: " + level);
//			}
//			if (i > 0 && schema.getLevel().get(i-1).getOrder() >= level.getOrder()) {
//				throw new IllegalStateException("Level #" + i + " is not before level #" + (i+1) + " in " + schema);
//			}
//		}
//	}
//
//	public static void checkLevelsOrderingStrict(ApprovalSchemaType schema) {
//		for (int i = 0; i < schema.getLevel().size(); i++) {
//			Integer order = schema.getLevel().get(i).getOrder();
//			if (order == null || order != i+1) {
//				throw new IllegalStateException("Level #" + (i+1) + " has an incorrect order: " + order + " in " + schema);
//			}
//		}
//	}

	public static OperationBusinessContextType getBusinessContext(WfContextType wfc) {
		if (wfc == null) {
			return null;
		}
		for (CaseEventType event : wfc.getEvent()) {
			if (event instanceof CaseCreationEventType) {
				return ((CaseCreationEventType) event).getBusinessContext();
			}
		}
		return null;
	}

	// TODO take from the workflow context!
	public static String getStageInfoTODO(Integer stageNumber) {
		return getStageInfo(stageNumber, null, null, null);
	}

	public static String getProcessInstanceId(WorkItemType workItem) {
		return getWorkflowContext(workItem).getProcessInstanceId();
	}

	public static WfContextType getWorkflowContext(WorkItemType workItem) {
		PrismContainerValue<?> parent = PrismContainerValue.getParentContainerValue(workItem.asPrismContainerValue());
		if (parent == null) {
			LOGGER.error("No workflow context for workItem {}", workItem);
			// this is only a workaround, FIXME MID-4030
			return new WfContextType(workItem.asPrismContainerValue().getPrismContext());
		}
		Containerable parentReal = parent.asContainerable();
		if (!(parentReal instanceof WfContextType)) {
			throw new IllegalStateException("WorkItem's parent is not a WfContextType; it is " + parentReal);
		}
		return (WfContextType) parentReal;
	}

	public static WfContextType getWorkflowContext(ApprovalSchemaExecutionInformationType info) {
		if (info == null || info.getTaskRef() == null || info.getTaskRef().asReferenceValue().getObject() == null) {
			return null;
		}
		@SuppressWarnings({ "unchecked", "raw" })
		PrismObject<TaskType> task = info.getTaskRef().asReferenceValue().getObject();
		return task.asObjectable().getWorkflowContext();
	}

	@Nullable
	public static String getTaskOid(WorkItemType workItem) {
		TaskType task = getTask(workItem);
		return task != null ? task.getOid() : null;
	}

	@Nullable
	public static TaskType getTask(WorkItemType workItem) {
		return getTask(getWorkflowContext(workItem));
	}

	@Nullable
	public static TaskType getTask(WfContextType wfc) {
		if (wfc == null) {
			return null;
		}
		PrismContainerValue<?> parent = PrismContainerValue.getParentContainerValue(wfc.asPrismContainerValue());
		if (parent == null) {
			LOGGER.error("No containing task for " + wfc);
			return null;
		}
		Containerable parentReal = parent.asContainerable();
		if (!(parentReal instanceof TaskType)) {
			throw new IllegalStateException("WfContextType's parent is not a TaskType; it is " + parentReal);
		}
		return (TaskType) parentReal;
	}

	public static ObjectReferenceType getObjectRef(WorkItemType workItem) {
		return getWorkflowContext(workItem).getObjectRef();
	}

	public static ObjectReferenceType getTargetRef(WorkItemType workItem) {
		return getWorkflowContext(workItem).getTargetRef();
	}

	public static int getEscalationLevelNumber(AbstractWorkItemType workItem) {
		return getEscalationLevelNumber(workItem.getEscalationLevel());
	}

	public static int getEscalationLevelNumber(WorkItemEscalationLevelType level) {
		return level != null && level.getNumber() != null ? level.getNumber() : 0;
	}

	public static String getEscalationLevelName(WorkItemEscalationLevelType level) {
		return level != null ? level.getName() : null;
	}

	public static String getEscalationLevelDisplayName(WorkItemEscalationLevelType level) {
		return level != null ? level.getDisplayName() : null;
	}

	public static String getEscalationLevelName(AbstractWorkItemType workItem) {
		return getEscalationLevelName(workItem.getEscalationLevel());
	}

	public static String getEscalationLevelDisplayName(AbstractWorkItemType workItem) {
		return getEscalationLevelDisplayName(workItem.getEscalationLevel());
	}

	public static WorkItemEscalationLevelType createEscalationLevel(Integer number, String name, String displayName) {
		if ((number != null && number != 0) || name != null || displayName != null) {
			return new WorkItemEscalationLevelType().number(number).name(name).displayName(displayName);
		} else {
			return null;
		}
	}

	public static Integer getEscalationLevelNumber(WorkItemEventType event) {
		return getEscalationLevelNumber(event.getEscalationLevel());
	}

	public static WfContextType getWorkflowContext(PrismObject<TaskType> task) {
		return task != null ? task.asObjectable().getWorkflowContext() : null;
	}

	// TODO better place
	@NotNull
	public static WorkItemEventCauseInformationType createCause(AbstractWorkItemActionType action) {
		WorkItemEventCauseInformationType cause = new WorkItemEventCauseInformationType();
		cause.setType(WorkItemEventCauseTypeType.TIMED_ACTION);
		if (action != null) {
			cause.setName(action.getName());
			cause.setDisplayName(action.getDisplayName());
		}
		return cause;
	}

	// TODO better place
	@Nullable
	public static WorkItemOperationKindType getOperationKind(AbstractWorkItemActionType action) {
		WorkItemOperationKindType operationKind;
		if (action instanceof EscalateWorkItemActionType) {
			operationKind = WorkItemOperationKindType.ESCALATE;
		} else if (action instanceof DelegateWorkItemActionType) {
			operationKind = WorkItemOperationKindType.DELEGATE;
		} else if (action instanceof CompleteWorkItemActionType) {
			operationKind = WorkItemOperationKindType.COMPLETE;
		} else {
			// shouldn't occur
			operationKind = null;
		}
		return operationKind;
	}

	@NotNull
	public static WorkItemEscalationLevelType createEscalationLevelInformation(DelegateWorkItemActionType delegateAction) {
		String escalationLevelName;
		String escalationLevelDisplayName;
		if (delegateAction instanceof EscalateWorkItemActionType) {
			escalationLevelName = ((EscalateWorkItemActionType) delegateAction).getEscalationLevelName();
			escalationLevelDisplayName = ((EscalateWorkItemActionType) delegateAction).getEscalationLevelDisplayName();
			if (escalationLevelName == null && escalationLevelDisplayName == null) {
				escalationLevelName = delegateAction.getName();
				escalationLevelDisplayName = delegateAction.getDisplayName();
			}
		} else {
			// TODO ... a warning here?
			escalationLevelName = escalationLevelDisplayName = null;
		}
		return new WorkItemEscalationLevelType().name(escalationLevelName).displayName(escalationLevelDisplayName);
	}

	// TODO rethink interface of this method
	// returns parent-less values
	public static void computeAssignees(List<ObjectReferenceType> newAssignees, List<ObjectReferenceType> delegatedTo,
			List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method, AbstractWorkItemType workItem) {
		newAssignees.clear();
		delegatedTo.clear();
		switch (method) {
			case ADD_ASSIGNEES: newAssignees.addAll(CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef())); break;
			case REPLACE_ASSIGNEES: break;
			default: throw new UnsupportedOperationException("Delegation method " + method + " is not supported yet.");
		}
		for (ObjectReferenceType delegate : delegates) {
			if (delegate.getType() != null && !QNameUtil.match(UserType.COMPLEX_TYPE, delegate.getType())) {
				throw new IllegalArgumentException("Couldn't use non-user object as a delegate: " + delegate);
			}
			if (delegate.getOid() == null) {
				throw new IllegalArgumentException("Couldn't use no-OID reference as a delegate: " + delegate);
			}
			if (!ObjectTypeUtil.containsOid(newAssignees, delegate.getOid())) {
				newAssignees.add(delegate.clone());
				delegatedTo.add(delegate.clone());
			}
		}
	}

	public static WorkItemDelegationEventType createDelegationEvent(WorkItemEscalationLevelType newEscalation,
			List<ObjectReferenceType> assigneesBefore, List<ObjectReferenceType> delegatedTo,
			@NotNull WorkItemDelegationMethodType method,
			WorkItemEventCauseInformationType causeInformation) {
		WorkItemDelegationEventType event;
		if (newEscalation != null) {
			WorkItemEscalationEventType escEvent = new WorkItemEscalationEventType();
			escEvent.setNewEscalationLevel(newEscalation);
			event = escEvent;
		} else {
			event = new WorkItemDelegationEventType();
		}
		event.getAssigneeBefore().addAll(assigneesBefore);
		event.getDelegatedTo().addAll(delegatedTo);
		event.setDelegationMethod(method);
		event.setCause(causeInformation);
		return event;
	}

	@Nullable
	public static WorkItemEscalationLevelType createNewEscalation(int escalationLevel, WorkItemEscalationLevelType escalation) {
		WorkItemEscalationLevelType newEscalation;
		if (escalation != null) {
			newEscalation = escalation.clone();
			newEscalation.setNumber(escalationLevel + 1);
		} else {
			newEscalation = null;
		}
		return newEscalation;
	}

	@NotNull
	public static List<TriggerType> createTriggers(int escalationLevel, Date workItemCreateTime,
			Date workItemDeadline, List<WorkItemTimedActionsType> timedActionsList,
			PrismContext prismContext, Trace logger, @Nullable String workItemId, @NotNull String handlerUri)
			throws SchemaException {
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
				logger.trace("Current escalation level is before 'escalationFrom', skipping timed actions {}", timedActionsEntry);
				continue;
			}
			if (levelTo != null && escalationLevel > levelTo) {
				logger.trace("Current escalation level is after 'escalationTo', skipping timed actions {}", timedActionsEntry);
				continue;
			}
			// TODO evaluate the condition
			List<TimedActionTimeSpecificationType> timeSpecifications = CloneUtil.cloneCollectionMembers(timedActionsEntry.getTime());
			if (timeSpecifications.isEmpty()) {
				timeSpecifications.add(new TimedActionTimeSpecificationType());
			}
			for (TimedActionTimeSpecificationType timeSpec : timeSpecifications) {
				if (timeSpec.getValue().isEmpty()) {
					timeSpec.getValue().add(XmlTypeConverter.createDuration(0));
				}
				for (Duration duration : timeSpec.getValue()) {
					XMLGregorianCalendar mainTriggerTime = computeTriggerTime(duration, timeSpec.getBase(),
							workItemCreateTime, workItemDeadline);
					TriggerType mainTrigger = createTrigger(mainTriggerTime, timedActionsEntry.getActions(), null, prismContext, workItemId, handlerUri);
					triggers.add(mainTrigger);
					List<Pair<Duration, AbstractWorkItemActionType>> notifyInfoList = getNotifyBefore(timedActionsEntry);
					for (Pair<Duration, AbstractWorkItemActionType> notifyInfo : notifyInfoList) {
						XMLGregorianCalendar notifyTime = (XMLGregorianCalendar) mainTriggerTime.clone();
						notifyTime.add(notifyInfo.getKey().negate());
						TriggerType notifyTrigger = createTrigger(notifyTime, null, notifyInfo, prismContext, workItemId, handlerUri);
						triggers.add(notifyTrigger);
					}
				}
			}
		}
		return triggers;
	}

	@NotNull
	private static TriggerType createTrigger(XMLGregorianCalendar triggerTime, WorkItemActionsType actions,
			Pair<Duration, AbstractWorkItemActionType> notifyInfo, PrismContext prismContext, @Nullable String workItemId, @NotNull String handlerUri)
			throws SchemaException {
		TriggerType trigger = new TriggerType(prismContext);
		trigger.setTimestamp(triggerTime);
		trigger.setHandlerUri(handlerUri);
		ExtensionType extension = new ExtensionType(prismContext);
		trigger.setExtension(extension);

		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		if (workItemId != null) {
			// work item id
			@SuppressWarnings("unchecked")
			@NotNull PrismPropertyDefinition<String> workItemIdDef =
					prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
			PrismProperty<String> workItemIdProp = workItemIdDef.instantiate();
			workItemIdProp.addRealValue(workItemId);
			trigger.getExtension().asPrismContainerValue().add(workItemIdProp);
		}
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

	public static boolean isInStageBeforeLastOne(WfContextType wfc) {
		if (wfc == null || wfc.getStageNumber() == null) {
			return false;
		}
		ItemApprovalProcessStateType info = WfContextUtil.getItemApprovalProcessInfo(wfc);
		if (info == null) {
			return false;
		}
		return wfc.getStageNumber() < info.getApprovalSchema().getStage().size();
	}

	public static String getProcessName(ApprovalSchemaExecutionInformationType info) {
		return info != null ? getOrig(ObjectTypeUtil.getName(info.getTaskRef())) : null;
	}

	public static String getTargetName(ApprovalSchemaExecutionInformationType info) {
		WfContextType wfc = getWorkflowContext(info);
		return wfc != null ? getOrig(ObjectTypeUtil.getName(wfc.getTargetRef())) : null;
	}

	public static String getOutcome(ApprovalSchemaExecutionInformationType info) {
		WfContextType wfc = getWorkflowContext(info);
		return wfc != null ? wfc.getOutcome() : null;
	}

	public static List<EvaluatedPolicyRuleType> getAllRules(SchemaAttachedPolicyRulesType policyRules) {
		List<EvaluatedPolicyRuleType> rv = new ArrayList<>();
		for (SchemaAttachedPolicyRuleType entry : policyRules.getEntry()) {
			if (!rv.contains(entry.getRule())) {
				rv.add(entry.getRule());
			}
		}
		return rv;
	}

	public static List<List<EvaluatedPolicyRuleType>> getRulesPerStage(WfContextType wfc) {
		List<List<EvaluatedPolicyRuleType>> rv = new ArrayList<>();
		ItemApprovalProcessStateType info = getItemApprovalProcessInfo(wfc);
		if (info == null || info.getPolicyRules() == null) {
			return rv;
		}
		List<SchemaAttachedPolicyRuleType> entries = info.getPolicyRules().getEntry();
		for (int i = 0; i < info.getApprovalSchema().getStage().size(); i++) {
			rv.add(getRulesForStage(entries, i+1));
		}
		return rv;
	}

	@NotNull
	private static List<EvaluatedPolicyRuleType> getRulesForStage(List<SchemaAttachedPolicyRuleType> entries, int stageNumber) {
		List<EvaluatedPolicyRuleType> rulesForStage = new ArrayList<>();
		for (SchemaAttachedPolicyRuleType entry : entries) {
			if (entry.getStageMin() != null && stageNumber >= entry.getStageMin()
					&& entry.getStageMax() != null && stageNumber <= entry.getStageMax()) {
				rulesForStage.add(entry.getRule());
			}
		}
		return rulesForStage;
	}

	// Do not use in approval expressions, because they are evaluated also on process start/preview.
	// Use explicit stage number instead.
	@NotNull
	public static List<EvaluatedPolicyRuleType> getRulesForCurrentStage(WfContextType wfc) {
		return getRulesForStage(wfc, wfc.getStageNumber());
	}

	@NotNull
	public static List<EvaluatedPolicyRuleType> getRulesForStage(WfContextType wfc, Integer stageNumber) {
		ItemApprovalProcessStateType info = getItemApprovalProcessInfo(wfc);
		if (info == null || info.getPolicyRules() == null || stageNumber == null) {
			return emptyList();
		}
		return getRulesForStage(info.getPolicyRules().getEntry(), stageNumber);
	}
}
