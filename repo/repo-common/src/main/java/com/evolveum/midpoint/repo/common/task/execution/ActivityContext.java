/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.execution;

import com.evolveum.midpoint.repo.common.task.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;

import org.jetbrains.annotations.NotNull;

/**
 * Context for instantiating activity execution. It is provided as separate class because of the flexibility needed:
 * root activities require the task execution, whereas non-root ones require the parent execution.
 *
 * @param <WD> The definition of the work.
 */
public abstract class ActivityContext<WD extends WorkDefinition> {

    /** Definition of the activity. */
    @NotNull
    private final ActivityDefinition<WD> activityDefinition;

    ActivityContext(@NotNull ActivityDefinition<WD> activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    public @NotNull ActivityDefinition<WD> getActivityDefinition() {
        return activityDefinition;
    }

    public abstract @NotNull TaskExecution getTaskExecution();

    public abstract CompositeActivityExecution getParentActivityExecution();

    public static class RootActivityContext<WD extends WorkDefinition> extends ActivityContext<WD> {

        @NotNull private final TaskExecution taskExecution;

        public RootActivityContext(@NotNull ActivityDefinition<WD> activityDefinition, @NotNull TaskExecution taskExecution) {
            super(activityDefinition);
            this.taskExecution = taskExecution;
        }

        @NotNull public TaskExecution getTaskExecution() {
            return taskExecution;
        }

        @Override
        public CompositeActivityExecution getParentActivityExecution() {
            return null;
        }
    }

    public static class SubActivityContext<WD extends WorkDefinition> extends ActivityContext<WD> {

        @NotNull private final CompositeActivityExecution parentActivityExecution;

        public SubActivityContext(@NotNull ActivityDefinition<WD> activityDefinition,
                @NotNull CompositeActivityExecution parentActivityExecution) {
            super(activityDefinition);
            this.parentActivityExecution = parentActivityExecution;
        }

        @Override
        public @NotNull TaskExecution getTaskExecution() {
            return parentActivityExecution.getTaskExecution();
        }

        @Override
        public @NotNull CompositeActivityExecution getParentActivityExecution() {
            return parentActivityExecution;
        }
    }
}
