/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.definition;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitiesTailoringType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractWorkDefinition implements WorkDefinition {

    private ActivityDefinition<?> owningActivity;

    @NotNull private ExecutionModeType executionMode = ExecutionModeType.EXECUTE;

    @NotNull private final ActivityTailoring activityTailoring = new ActivityTailoring();

    @Override
    public ActivityDefinition<?> getOwningActivity() {
        return owningActivity;
    }

    public void setOwningActivity(ActivityDefinition<?> owningActivity) {
        this.owningActivity = owningActivity;
    }

    @NotNull
    @Override
    public ExecutionModeType getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionModeType executionMode) {
        this.executionMode = MoreObjects.firstNonNull(executionMode, ExecutionModeType.EXECUTE);
    }

    public @NotNull ActivityTailoring getActivityTailoring() {
        return activityTailoring;
    }

    public void addTailoring(ActivitiesTailoringType tailoring) {
        activityTailoring.add(tailoring);
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
