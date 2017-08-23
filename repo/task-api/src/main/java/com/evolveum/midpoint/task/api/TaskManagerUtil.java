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

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskGroupExecutionLimitationType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author mederly
 */
public class TaskManagerUtil {

	/**
	 * Returns how many threads are available for execution of a given group on a list of nodes.
	 * If returned key is null, this means "unlimited" (i.e. limited only by the node thread pool size).
	 */
	@NotNull
	public static Map<String, Integer> getNodeRestrictions(String group, @NotNull List<NodeType> nodes) {
		Map<String, Integer> rv = new HashMap<>();
		for (NodeType node : nodes) {
			rv.put(node.getNodeIdentifier(), getNodeLimitation(node, group));
		}
		return rv;
	}

	private static Integer getNodeLimitation(NodeType node, String group) {
		if (node.getTaskExecutionLimitations() == null) {
			return null;
		}
		group = MiscUtil.nullIfEmpty(group);
		for (TaskGroupExecutionLimitationType limit : node.getTaskExecutionLimitations().getGroupLimitation()) {
			if (Objects.equals(group, MiscUtil.nullIfEmpty(limit.getGroupName()))) {
				return limit.getLimit();
			}
		}
		for (TaskGroupExecutionLimitationType limit : node.getTaskExecutionLimitations().getGroupLimitation()) {
			if (TaskConstants.LIMIT_FOR_OTHER_GROUPS.equals(limit.getGroupName())) {
				return limit.getLimit();
			}
		}
		return null;
	}

}
