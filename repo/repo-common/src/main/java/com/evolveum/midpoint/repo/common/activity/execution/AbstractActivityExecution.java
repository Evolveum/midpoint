/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Base class for activity executions.
 *
 * @param <WD> Definition of the work that this activity has to do.
 */
public abstract class AbstractActivityExecution<WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>> implements ActivityExecution {

    /**
     * The task execution in context of which this activity execution takes place.
     */
    @NotNull protected final TaskExecution taskExecution;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final Activity<WD, AH> activity;

    protected AbstractActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        this.taskExecution = context.getTaskExecution();
        this.activity = context.getActivity();
    }

    @NotNull
    @Override
    public TaskExecution getTaskExecution() {
        return taskExecution;
    }

    public @NotNull Activity<WD, AH> getActivity() {
        return activity;
    }

    public CommonTaskBeans getBeans() {
        return taskExecution.getBeans();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "act=" + activity +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        if (activity.isRoot()) {
            DebugUtil.debugDumpWithLabelLn(sb, "task execution", taskExecution.shortDump(), indent + 1);
        }
        return sb.toString();
    }
}
