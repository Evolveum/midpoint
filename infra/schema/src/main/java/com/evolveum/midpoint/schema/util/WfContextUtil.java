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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO clean up these formatting methods
 *
 * @author mederly
 */
public class WfContextUtil {

	@Nullable
	public static String getStageInfo(WfContextType wfc) {
		if (wfc == null || hasFinished(wfc)) {
			return null;
		}
		return getStageInfo(wfc.getStageNumber(), wfc.getStageCount(), wfc.getStageName(), wfc.getStageDisplayName());
	}

	@Nullable
	public static String getStageInfo(WorkItemType workItem) {
		if (workItem == null) {
			return null;
		}
		WfContextType wfc = getWorkflowContext(workItem);
		return getStageInfo(workItem.getStageNumber(), wfc.getStageCount(), wfc.getStageName(), wfc.getStageDisplayName());
	}

	public static Object getStageCount(WorkItemType workItem) {
		return getWorkflowContext(workItem).getStageCount();
	}

	public static Object getStageName(WorkItemType workItem) {
		return getWorkflowContext(workItem).getStageName();
	}

	public static Object getStageDisplayName(WorkItemType workItem) {
		return getWorkflowContext(workItem).getStageDisplayName();
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
	public static String getEscalationLevelInfo(WorkItemType workItem) {
		if (workItem == null) {
			return null;
		}
		return getEscalationLevelInfo(workItem.getEscalationLevel());
	}

	private static String getEscalationLevelInfo(WorkItemEscalationLevelType e) {
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
		String stageName = wfc.getStageName();
		String stageDisplayName = wfc.getStageDisplayName();
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
		appendNumber(stageNumber, wfc.getStageCount(), sb);
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
			return Collections.emptyList();
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

	private static List<ApprovalStageDefinitionType> getStages(ApprovalSchemaType approvalSchema) {
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
		List<ApprovalStageDefinitionType> stages = new ArrayList<>(getStages(schema));
		stages.sort(Comparator.comparing(stage -> getNumber(stage), Comparator.nullsLast(Comparator.naturalOrder())));
		for (int i = 0; i < stages.size(); i++) {
			stages.get(i).setOrder(null);
			stages.get(i).setNumber(i+1);
		}
		schema.getLevel().clear();
		schema.getStage().clear();
		schema.getStage().addAll(CloneUtil.cloneCollectionMembers(stages));
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
			throw new IllegalStateException("No containing workflow context for " + workItem);
		}
		Containerable parentReal = parent.asContainerable();
		if (!(parentReal instanceof WfContextType)) {
			throw new IllegalStateException("WorkItem's parent is not a WfContextType; it is " + parentReal);
		}
		return (WfContextType) parentReal;
	}

	public static TaskType getTask(WorkItemType workItem) {
		return getTask(getWorkflowContext(workItem));
	}

	public static TaskType getTask(WfContextType wfc) {
		PrismContainerValue<?> parent = PrismContainerValue.getParentContainerValue(wfc.asPrismContainerValue());
		if (parent == null) {
			throw new IllegalStateException("No containing task for " + wfc);
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
		return workItem.getEscalationLevel() != null ? workItem.getEscalationLevel().getNumber() : 0;
	}

	public static String getEscalationLevelName(AbstractWorkItemType workItem) {
		return workItem.getEscalationLevel() != null ? workItem.getEscalationLevel().getName() : null;
	}

	public static String getEscalationLevelDisplayName(AbstractWorkItemType workItem) {
		return workItem.getEscalationLevel() != null ? workItem.getEscalationLevel().getDisplayName() : null;
	}

	public static WorkItemEscalationLevelType createEscalationLevel(Integer number, String name, String displayName) {
		if ((number != null && number != 0) || name != null || displayName != null) {
			return new WorkItemEscalationLevelType().number(number).name(name).displayName(displayName);
		} else {
			return null;
		}
	}
}
