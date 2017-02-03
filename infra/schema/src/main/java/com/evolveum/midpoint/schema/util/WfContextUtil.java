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

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
		return getStageInfo(workItem.getStageNumber(), workItem.getStageCount(), workItem.getStageName(), workItem.getStageDisplayName());
	}

	// wfc is used to retrieve approval schema (if needed)
	private static String getStageInfo(Integer stageNumber, Integer stageCount, String stageName, String stageDisplayName) {
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

	public static SchemaAttachedPolicyRuleType getAttachedPolicyRule(WfContextType workflowContext, int order) {
		ItemApprovalProcessStateType info = getItemApprovalProcessInfo(workflowContext);
		if (info == null || info.getPolicyRules() == null) {
			return null;
		}
		return info.getPolicyRules().getEntry().stream()
				.filter(e -> e.getLevelMax() != null && e.getLevelMax() != null
						&& order >= e.getLevelMin() && order <= e.getLevelMax())
				.findFirst().orElse(null);
	}

	public static ApprovalLevelType getCurrentApprovalLevel(WfContextType wfc) {
		if (wfc == null || wfc.getStageNumber() == null) {
			return null;
		}
		ItemApprovalProcessStateType info = getItemApprovalProcessInfo(wfc);
		if (info == null || info.getApprovalSchema() == null) {
			return null;
		}
		int level = wfc.getStageNumber()-1;
		if (level < 0 || level >= info.getApprovalSchema().getLevel().size()) {
			return null;		// TODO log something here? or leave it to the caller?
		}
		return info.getApprovalSchema().getLevel().get(level);
	}

	// we must be strict here; in case of suspicion, throw an exception
	public static <T extends WfProcessEventType> List<T> getEventsForCurrentStage(@NotNull WfContextType wfc, @NotNull Class<T> clazz) {
		if (wfc.getStageNumber() == null) {
			throw new IllegalArgumentException("No stage number in workflow context; pid = " + wfc.getProcessInstanceId());
		}
		int stageNumber = wfc.getStageNumber();
		return wfc.getEvent().stream()
				.filter(e -> clazz.isAssignableFrom(e.getClass()) && e.getStageNumber() != null && stageNumber == e.getStageNumber())
				.map(e -> (T) e)
				.collect(Collectors.toList());
	}

	public static <T extends WfProcessEventType> List<T> getEvents(@NotNull WfContextType wfc, @NotNull Class<T> clazz) {
		return wfc.getEvent().stream()
				.filter(e -> clazz.isAssignableFrom(e.getClass()))
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
	public static ApprovalLevelOutcomeType getCurrentStageOutcome(WfContextType wfc, List<WfStageCompletionEventType> stageEvents) {
		if (stageEvents.size() > 1) {
			throw new IllegalStateException("More than one stage-level event in " + getBriefDiagInfo(wfc) + ": " + stageEvents);
		}
		WfStageCompletionEventType event = stageEvents.get(0);
		if (event.getOutcome() == null) {
			throw new IllegalStateException("No outcome for stage-level event in " + getBriefDiagInfo(wfc));
		}
		return event.getOutcome();
	}
}
