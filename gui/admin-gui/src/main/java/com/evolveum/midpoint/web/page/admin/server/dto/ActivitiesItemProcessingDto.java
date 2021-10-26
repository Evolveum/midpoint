/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil.ActivityStateInContext;
import com.evolveum.midpoint.util.TreeNode;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil.hasItemProcessingInformation;

/**
 * Item processing information for activity tree.
 *
 * Currently the tree is represented as a simple list of activities.
 * Item processing information from worker tasks is aggregated into their coordinator.
 */
public class ActivitiesItemProcessingDto implements Serializable {

    public static final String F_ACTIVITIES = "activities";

    @NotNull private final List<ActivityItemProcessingDto> activities = new ArrayList<>();

    ActivitiesItemProcessingDto(@NotNull TreeNode<ActivityStateInContext> tree) {
        List<ActivityStateInContext> activityStates = tree.getAllDataDepthFirst();
        for (ActivityStateInContext stateInContext : activityStates) {
            if (hasItemProcessingInformation(stateInContext) && (activityStates.size() <= 1 || !stateInContext.getActivityPath().isEmpty())) {
                activities.add(new ActivityItemProcessingDto(stateInContext));
            }
        }
    }

    @SuppressWarnings("unused") // accessed dynamically
    public @NotNull List<ActivityItemProcessingDto> getActivities() {
        return activities;
    }
}
