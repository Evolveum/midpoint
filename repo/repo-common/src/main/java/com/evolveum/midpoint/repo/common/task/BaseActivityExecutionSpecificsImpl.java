/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.state.CurrentActivityState;
import com.evolveum.midpoint.task.api.RunningTask;

/**
 * Base class for typical "activity execution specifics" implementations.
 */
public abstract class BaseActivityExecutionSpecificsImpl<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        AE extends IterativeActivityExecution<?, WD, AH, ?, ?, ?>> {

    /**
     * The generic execution object. This reference is needed to give the this specifics object the whole context
     * (activity execution, activity, activity definition, and so on).
     */
    @NotNull protected final AE activityExecution;

    BaseActivityExecutionSpecificsImpl(@NotNull AE activityExecution) {
        this.activityExecution = activityExecution;
    }

    public @NotNull Activity<WD, AH> getActivity() {
        return activityExecution.getActivity();
    }

    public @NotNull WD getWorkDefinition() {
        return getActivity().getWorkDefinition();
    }

    public @NotNull ActivityDefinition<WD> getActivityDefinition() {
        return getActivity().getDefinition();
    }

    public @NotNull RunningTask getRunningTask() {
        return activityExecution.getRunningTask();
    }

    public @NotNull AH getActivityHandler() {
        return activityExecution.getActivityHandler();
    }

    public @NotNull CommonTaskBeans getBeans() {
        return activityExecution.beans;
    }

    public @NotNull CurrentActivityState<?> getActivityState() {
        return activityExecution.getActivityState();
    }

    public @NotNull AE getActivityExecution() {
        return activityExecution;
    }
}
