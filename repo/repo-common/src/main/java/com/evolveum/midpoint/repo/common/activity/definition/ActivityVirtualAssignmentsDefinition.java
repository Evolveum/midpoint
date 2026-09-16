/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualAssignmentsType;

/**
 * Definition of virtual assignments for an activity.
 * Virtual assignments are a special kind of assignments that are being assigned to processed
 * object only during activity processing.
 *
 */
public class ActivityVirtualAssignmentsDefinition implements Cloneable, DebugDumpable {

    @NotNull private final VirtualAssignmentsType bean;

    public ActivityVirtualAssignmentsDefinition(@NotNull VirtualAssignmentsType bean) {
        this.bean = bean;
    }

    public static @NotNull ActivityVirtualAssignmentsDefinition create(ActivityDefinitionType activityDefinitionBean) {
        if (activityDefinitionBean == null) {
            return new ActivityVirtualAssignmentsDefinition(new VirtualAssignmentsType());
        }

        VirtualAssignmentsType virtualAssignments = activityDefinitionBean.getVirtualAssignments();
        if (virtualAssignments == null) {
            virtualAssignments = new VirtualAssignmentsType();
        } else {
            virtualAssignments = virtualAssignments.clone();
        }

        return new ActivityVirtualAssignmentsDefinition(virtualAssignments);
    }

    @Override
    public @NotNull ActivityVirtualAssignmentsDefinition clone() {
        return new ActivityVirtualAssignmentsDefinition(bean.clone());
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent);
    }

    @NotNull
    public Collection<AssignmentType> getVirtualAssignments() {
        return bean.getAssignment();
    }
}
