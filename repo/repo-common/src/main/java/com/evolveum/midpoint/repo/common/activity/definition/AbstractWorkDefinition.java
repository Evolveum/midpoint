/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * IMPLEMENTATION NOTE: The fields in sub-classes should be immutable! (TODO: why?)
 */
public abstract class AbstractWorkDefinition implements WorkDefinition {

    /** Type of the activity identified by the work definition item name e.g. `c:reconciliation` or `c:composite`. */
    @NotNull private final QName activityTypeName;

    /**
     * *TODO* decide if the tailoring should be here or in {@link ActivityDefinition}.
     *   The argument for being here is that it can add new sub-activities. The argument
     *   for being there is that it modifies non-functional aspects of existing activities,
     *   just like distribution, flow control, etc does.
     */
    @NotNull private ActivityTailoring activityTailoring = new ActivityTailoring();

    protected AbstractWorkDefinition(@NotNull QName activityTypeName) {
        this.activityTypeName = activityTypeName;
    }

    @Override
    public @NotNull QName getActivityTypeName() {
        return activityTypeName;
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
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "activity tailoring", String.valueOf(activityTailoring), indent + 1);
        }
        return sb.toString();
    }

    /** Provides specific debug dump. Should not append last newline. */
    protected abstract void debugDumpContent(StringBuilder sb, int indent);

    @Override
    public WorkDefinition clone() {
        try {
            AbstractWorkDefinition clone = (AbstractWorkDefinition) super.clone();
            clone.activityTailoring = activityTailoring.clone(); // Reconsider if this is really needed.
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new SystemException(e);
        }
    }
}
