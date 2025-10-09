/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityProgressUtil;
import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

/** TODO better name */
public class ActivityProgressAndStatisticsPrinter {

    /**
     * Prints all relevant statistics (including progress) for all activities in the given task.
     */
    public static String print(@NotNull TaskType task) {
        StringBuilder sb = new StringBuilder();
        ActivityTreeUtil.processLocalStates(task, (path, state) -> print(sb, path, state));
        return sb.toString();
    }

    private static void print(StringBuilder sb, @NotNull ActivityPath path, @NotNull ActivityStateType activityState) {
        sb.append("Activity: ")
                .append(path.toDebugName())
                .append("\n\n");

        if (activityState.getProgress() != null) {
            sb.append("Progress:\n\n");
            sb.append(ActivityProgressUtil.format(activityState.getProgress()));
            sb.append("\n");
        }

        sb.append(ActivityStatisticsUtil.format(activityState.getStatistics()));
    }
}
