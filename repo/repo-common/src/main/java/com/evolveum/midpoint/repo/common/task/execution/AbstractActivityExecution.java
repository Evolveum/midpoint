/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.execution;

import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.repo.common.task.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.task.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Base class for activity executions.
 *
 * @param <WD> Definition of the work that this activity has to do.
 */
public abstract class AbstractActivityExecution<WD extends WorkDefinition,
        AH extends ActivityHandler<WD>> implements ActivityExecution {

    /**
     * The task execution in context of which this activity execution takes place.
     */
    @NotNull protected final TaskExecution taskExecution;

    /**
     * Parent activity execution, or null if this is the root one.
     */
    protected final CompositeActivityExecution parent;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final ActivityDefinition<WD> activityDefinition;

    /**
     * TODO
     */
    @NotNull protected final AH activityHandler;

    /**
     * Activity identifier, either provided by user or automatically generated. It is unique among siblings.
     */
    private String identifier;

    protected AbstractActivityExecution(@NotNull ActivityInstantiationContext<WD> context,
            @NotNull AH activityHandler) {
        this.taskExecution = context.getTaskExecution();
        this.parent = context.getParentActivityExecution();
        this.activityDefinition = context.getActivityDefinition();
        this.activityHandler = activityHandler;
    }

    @NotNull
    @Override
    public TaskExecution getTaskExecution() {
        return taskExecution;
    }

    public CompositeActivityExecution getParent() {
        return parent;
    }

    public @NotNull ActivityDefinition<WD> getActivityDefinition() {
        return activityDefinition;
    }

    @NotNull public AH getActivityHandler() {
        return activityHandler;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setupIdentifier(Supplier<String> defaultIdentifierSupplier) {
        String defined = activityDefinition.getIdentifier();
        if (defined != null) {
            identifier = defined;
            return;
        }

        String generated = defaultIdentifierSupplier.get();
        if (generated != null) {
            identifier = generated;
            return;
        }

        throw new IllegalStateException("Activity identifier was not defined nor generated");
    }

    public CommonTaskBeans getBeans() {
        return taskExecution.getBeans();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + identifier +
                ", def=" + activityDefinition +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        if (parent == null) {
            DebugUtil.debugDumpWithLabelLn(sb, "task execution", taskExecution.shortDump(), indent + 1);
        }
        DebugUtil.debugDumpWithLabel(sb, "definition", activityDefinition, indent + 1);
        return sb.toString();
    }
}
