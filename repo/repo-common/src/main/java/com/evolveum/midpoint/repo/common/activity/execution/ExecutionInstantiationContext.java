/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.task.GenericTaskExecution;

/**
 * Context for instantiating activity execution. It was originally provided as separate class because of the flexibility needed
 * (root vs non-root activities). This is no longer true, so we have to decide about the future of this class.
 *
 * @param <WD> The definition of the work.
 */
public class ExecutionInstantiationContext<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> {

    /** Definition of the activity. */
    @NotNull private final Activity<WD, AH> activity;

    @NotNull private final GenericTaskExecution taskExecution;

    public ExecutionInstantiationContext(@NotNull Activity<WD, AH> activity, @NotNull GenericTaskExecution taskExecution) {
        this.activity = activity;
        this.taskExecution = taskExecution;
    }

    @NotNull
    public Activity<WD, AH> getActivity() {
        return activity;
    }

    @NotNull
    public GenericTaskExecution getTaskExecution() {
        return taskExecution;
    }
}
