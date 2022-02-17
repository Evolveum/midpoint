/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskGroupExecutionLimitationType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
