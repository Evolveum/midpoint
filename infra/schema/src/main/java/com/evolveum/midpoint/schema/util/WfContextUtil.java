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
		if (wfc == null) {
			return null;
		}
		Integer stageNumber = wfc.getStageNumber();
		String stageName = wfc.getStageDisplayName() != null ? wfc.getStageDisplayName() : wfc.getStageName();
		if (stageName == null && stageNumber == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		if (stageName != null) {
			sb.append(stageName);
		}
		appendNumber(wfc, stageNumber, sb);
		return sb.toString();
	}

	@Nullable
	public static String getCompleteStageInfo(WfContextType wfc) {
		if (wfc == null) {
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
		appendNumber(wfc, stageNumber, sb);
		return sb.toString();
	}

	private static void appendNumber(WfContextType wfc, Integer stageNumber, StringBuilder sb) {
		if (stageNumber != null) {
			boolean parentheses = sb.length() > 0;
			if (parentheses) {
				sb.append(" (");
			}
			sb.append(stageNumber);
			ItemApprovalProcessStateType processInfo = getItemApprovalProcessInfo(wfc);
			ApprovalSchemaType schema = processInfo != null ? processInfo.getApprovalSchema() : null;
			if (schema != null) {
				sb.append("/").append(schema.getLevel().size());
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
