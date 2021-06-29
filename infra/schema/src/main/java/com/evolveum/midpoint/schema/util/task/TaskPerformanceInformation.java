/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Quickly hacked "API" presenting task performance information.
 *
 * Use with care. Will change in next midPoint release.
 *
 * TODO deduplicate with {@link TaskProgressInformation}.
 */
@Experimental
@Deprecated
public class TaskPerformanceInformation implements DebugDumpable, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(TaskPerformanceInformation.class);

    /**
     * Information on the progress in individual parts. Indexed by part URI.
     */
    private final LinkedHashMap<ActivityPath, ActivityPerformanceInformation> activities = new LinkedHashMap<>();

    private TaskPerformanceInformation() {
    }

    /**
     * Precondition: the task contains fully retrieved and resolved subtasks.
     */
    public static TaskPerformanceInformation fromTaskTree(TaskType task) {
        throw new UnsupportedOperationException();
    }

    public Map<ActivityPath, ActivityPerformanceInformation> getActivities() {
        return activities;
    }

    @Override
    public String toString() {
        return "TaskPerformanceInformation{" +
                "activities=" + activities +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabel(sb, "Parts", activities, indent + 1);
        return sb.toString();
    }
}
