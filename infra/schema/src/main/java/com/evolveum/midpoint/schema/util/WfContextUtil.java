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
import org.jetbrains.annotations.Nullable;

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

	public static ItemApprovalWorkItemPartType getItemApprovalWorkItemInfo(WorkItemType workItem) {
		return workItem.getProcessSpecificPart() instanceof ItemApprovalWorkItemPartType ?
				(ItemApprovalWorkItemPartType) workItem.getProcessSpecificPart() : null;
	}

}
