/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * State of activity not connected to the current activity run.
 * Does not maintain live progress/statistics and assumes that the work state was already created.
 * (Otherwise it cannot use it.)
 */
public class OtherActivityState extends ActivityState {

    /**
     * Task in which this activity state resides.
     */
    @NotNull private final Task task;

    @NotNull private final ActivityPath activityPath;

    @Nullable private final ComplexTypeDefinition workStateComplexTypeDefinition;

    OtherActivityState(
            @NotNull Task task,
            @NotNull TaskActivityStateType taskActivityState,
            @NotNull ActivityPath activityPath,
            @Nullable QName workStateTypeName) {
        this.task = task;
        this.activityPath = activityPath;
        this.stateItemPath = ActivityStateUtil.getStateItemPath(taskActivityState, activityPath);
        if (workStateTypeName != null) {
            workStateComplexTypeDefinition = determineWorkStateDefinition(workStateTypeName);
        } else {
            var workState =
                    taskActivityState.asPrismContainerValue().findItem(stateItemPath.append(ActivityStateType.F_WORK_STATE));
            ItemDefinition<?> workStateItemDef = workState != null ? workState.getDefinition() : null;
            workStateComplexTypeDefinition = workStateItemDef instanceof PrismContainerDefinition<?> prismContainerDefinition ?
                    prismContainerDefinition.getComplexTypeDefinition() : null;
        }
    }

    public @NotNull Task getTask() {
        return task;
    }

    @Override
    public @NotNull ActivityPath getActivityPath() {
        return activityPath;
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "Task", String.valueOf(task), indent + 1);
    }

    @Override
    public @Nullable ComplexTypeDefinition getWorkStateComplexTypeDefinition() {
        return workStateComplexTypeDefinition;
    }

    @Override
    public String toString() {
        return "OtherActivityState{" +
                "activityPath=" + activityPath +
                ", task=" + task +
                ", stateItemPath=" + stateItemPath +
                '}';
    }
}
