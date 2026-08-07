/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.handlers;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesType;

public class ActivityHandlerUtils {

    /**
     * Clones an activity definition for use as a parent definition for child activities,
     * especially embedded activities.
     */
    public static <WD extends WorkDefinition> ActivityDefinition<WD> cloneWithoutIdForChildActivity(
            @NotNull ActivityDefinition<WD> original) {
        ActivityDefinition<WD> clone = original.cloneWithoutId();
        // Policies and virtual assignments should not be inherited by child activities:
        // they are collected from the declaring activity and its ancestors at run time,
        // so an inherited copy would be picked up twice.
        ActivityPoliciesType policies = clone.getPoliciesDefinition().getPolicies();
        policies.getPolicy().clear();
        policies.getPolicyRef().clear();
        clone.getVirtualAssignmentsDefinition().getVirtualAssignments().clear();

        return clone;
    }
}
