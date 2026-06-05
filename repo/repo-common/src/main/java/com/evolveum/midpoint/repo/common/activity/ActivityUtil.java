/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

public class ActivityUtil {

    /**
     * Recursively collects all virtual assignment from activity policies.
     */
    public static @NotNull Collection<Pair<AssignmentType, ActivityPath>> getAllVirtualAssignments(Activity<?, ?> activity) {
        if (activity == null) {
            return List.of();
        }

        List<Pair<AssignmentType, ActivityPath>> result = new ArrayList<>();
        collectAllVirtualAssignments(activity, result);

        return result;
    }

    private static void collectAllVirtualAssignments(
            Activity<?, ?> activity, Collection<Pair<AssignmentType, ActivityPath>> result) {

        if (activity == null) {
            return;
        }

        collectAllVirtualAssignments(activity.getParent(), result);

        ActivityDefinition<?> def = activity.getDefinition();
        Collection<AssignmentType> virtualAssignments = def.getVirtualAssignmentsDefinition().getVirtualAssignments();
        ActivityPath path = activity.getPath();

        virtualAssignments.forEach(virtualAssignment -> result.add(Pair.of(virtualAssignment, path)));
    }
}
