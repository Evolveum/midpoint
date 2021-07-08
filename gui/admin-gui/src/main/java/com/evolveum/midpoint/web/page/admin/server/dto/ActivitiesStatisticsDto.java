/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.statistics.ActivityStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil.ActivityStateInContext;
import com.evolveum.midpoint.schema.util.task.TaskResolver;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationTransitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides statistics (item processing, synchronization, actions executed) for an activity with sub-activities.
 *
 * Currently some of the statistics are aggregated across all activity tree. (All except item processing.)
 */
public class ActivitiesStatisticsDto implements Serializable {

    public static final String F_ITEM_PROCESSING = "itemProcessing";
    public static final String F_SYNCHRONIZATION_TRANSITIONS = "synchronizationTransitions";
    public static final String F_RESULTING_ACTIONS_EXECUTED = "resultingActionsExecuted";
    public static final String F_ALL_ACTIONS_EXECUTED = "allActionsExecuted";

    private final ActivitiesItemProcessingDto itemProcessing;
    @NotNull private final List<SynchronizationSituationTransitionType> synchronizationTransitions = new ArrayList<>();
    @NotNull private final List<ObjectActionsExecutedEntryType> resultingActionsExecuted = new ArrayList<>();
    @NotNull private final List<ObjectActionsExecutedEntryType> allActionsExecuted = new ArrayList<>();

    private ActivitiesStatisticsDto() {
        itemProcessing = null;
    }

    private ActivitiesStatisticsDto(@NotNull TreeNode<ActivityStateInContext> tree) {
        itemProcessing = new ActivitiesItemProcessingDto(tree);
        synchronizationTransitions.addAll(ActivityStatisticsUtil.getSynchronizationTransitions(tree));
        resultingActionsExecuted.addAll(ActivityStatisticsUtil.getResultingActionsExecuted(tree));
        allActionsExecuted.addAll(ActivityStatisticsUtil.getAllActionsExecuted(tree));
    }

    public static ActivitiesStatisticsDto fromTaskTree(TaskType rootTask) {
        if (rootTask == null) {
            return new ActivitiesStatisticsDto();
        } else {
            return new ActivitiesStatisticsDto(
                    ActivityTreeUtil.toStateTree(rootTask, TaskResolver.empty()));
        }
    }

    @SuppressWarnings("unused") // accessed dynamically
    public ActivitiesItemProcessingDto getItemProcessing() {
        return itemProcessing;
    }

    @SuppressWarnings("unused") // accessed dynamically
    public @NotNull List<SynchronizationSituationTransitionType> getSynchronizationTransitions() {
        return synchronizationTransitions;
    }

    @SuppressWarnings("unused") // accessed dynamically
    public @NotNull List<ObjectActionsExecutedEntryType> getResultingActionsExecuted() {
        return resultingActionsExecuted;
    }

    @SuppressWarnings("unused") // accessed dynamically
    public @NotNull List<ObjectActionsExecutedEntryType> getAllActionsExecuted() {
        return allActionsExecuted;
    }
}
