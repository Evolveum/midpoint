/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.config.AssignmentConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ActivityUtil {

    public record ActivityAttachedVirtualAssignment(AssignmentConfigItem assignmentConfigItem, ActivityPath activityPath) {
    }
    /**
     * Recursively collects all virtual assignment from activity policies.
     */
    public static Collection<ActivityAttachedVirtualAssignment> getAllVirtualAssignments(
            Activity<?, ?> activity, TaskType rootTask) {
        List<ActivityAttachedVirtualAssignment> result = new ArrayList<>();
        ConfigurationItemOrigin origin = ConfigurationItemOrigin.inObjectApproximate(rootTask, TaskType.F_ACTIVITY);
        collectAllVirtualAssignments(activity, origin, result);
        return result;
    }

    private static void collectAllVirtualAssignments(
            Activity<?, ?> activity, ConfigurationItemOrigin origin, Collection<ActivityAttachedVirtualAssignment> result) {

        Activity<?, ?> parent = activity.getParent();
        if (parent != null) {
            collectAllVirtualAssignments(parent, origin, result);
        }

        if (ActivityPolicyUtils.isVirtualAssignmentPolicyProcessingDisabled(activity)) {
            return;
        }

        activity.getDefinition().getVirtualAssignmentsDefinition().getVirtualAssignments().forEach(
                virtualAssignmentBean -> result.add(new ActivityAttachedVirtualAssignment(
                        AssignmentConfigItem.of(virtualAssignmentBean, origin),
                        activity.getPath())));
    }
}
