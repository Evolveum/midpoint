/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

public class AssignmentConfigItem extends AbstractAssignmentConfigItem {

    /**
     * Activity path for this configuration item.
     * Necessary for correct evaluation of virtual assignments that are coming from activity policies
     * (mainly to correctly handle activity tree hierarchy and updating state and policy counters).
     */
    private ActivityPath activityPath;

    @SuppressWarnings("unused") // invoked dynamically
    public AssignmentConfigItem(@NotNull ConfigurationItem<AssignmentType> original) {
        super(original);
    }

    private AssignmentConfigItem(@NotNull AssignmentType value, @NotNull ConfigurationItemOrigin origin, ActivityPath activityPath) {
        super(value, origin, null);

        this.activityPath = activityPath;
    }

    public static AssignmentConfigItem of(
            @NotNull AssignmentType bean,
            @NotNull OriginProvider<? super AssignmentType> originProvider) {
        return of(bean, originProvider, null);
    }

    public static AssignmentConfigItem of(
            @NotNull AssignmentType bean,
            @NotNull OriginProvider<? super AssignmentType> originProvider,
            ActivityPath activityPath) {
        return new AssignmentConfigItem(bean, originProvider.origin(bean), activityPath);
    }

    @Override
    public @NotNull String localDescription() {
        return "assignment";
    }

    @Override
    public ActivityPath getActivityPath() {
        return activityPath;
    }
}
