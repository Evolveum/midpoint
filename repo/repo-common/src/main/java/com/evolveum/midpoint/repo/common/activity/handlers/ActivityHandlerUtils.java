/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.handlers;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;

public class ActivityHandlerUtils {

    /**
     * Clones an activity definition for use as a parent definition for child activities,
     * especially embedded activities.
     */
    public static <WD extends WorkDefinition> ActivityDefinition<WD> cloneWithoutIdForChildActivity(
            @NotNull ActivityDefinition<WD> original) {
        ActivityDefinition<WD> clone = original.cloneWithoutId();
        // policies should not be inherited by child activities
        clone.getPoliciesDefinition().getPolicies().getPolicy().clear();

        return clone;
    }
}
