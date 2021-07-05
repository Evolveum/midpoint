/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractWorkDefinition implements WorkDefinition {

    private ActivityDefinition<?> owningActivityDefinition;

    @NotNull private ExecutionModeType executionMode = ExecutionModeType.EXECUTE;

    /**
     * TODO decide if the tailoring should be here or in {@link ActivityDefinition}.
     *  The argument for being here is that it can add new sub-activities. The argument
     *  for being there is that it modifies non-functional aspects of existing activities,
     *  just like distribution, flow control, etc does.
     */
    @NotNull private final ActivityTailoring activityTailoring = new ActivityTailoring();

    @Override
    public ActivityDefinition<?> getOwningActivityDefinition() {
        return owningActivityDefinition;
    }

    public void setOwningActivityDefinition(ActivityDefinition<?> owningActivity) {
        this.owningActivityDefinition = owningActivity;
    }

    @Override
    public @NotNull ExecutionModeType getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionModeType executionMode) {
        this.executionMode = MoreObjects.firstNonNull(executionMode, ExecutionModeType.EXECUTE);
    }

    @Override
    public @NotNull ActivityTailoring getActivityTailoring() {
        return activityTailoring;
    }

    void addTailoringFrom(ActivityDefinitionType activityDefinitionBean) {
        activityTailoring.addFrom(activityDefinitionBean);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        debugDumpContent(sb, indent);
        if (!activityTailoring.isEmpty()) {
            DebugUtil.debugDumpWithLabelLn(sb, "activity tailoring", String.valueOf(activityTailoring), indent + 1);
        }
        DebugUtil.debugDumpWithLabel(sb, "execution mode", String.valueOf(executionMode), indent+1);
        return sb.toString();
    }

    protected abstract void debugDumpContent(StringBuilder sb, int indent);
}
