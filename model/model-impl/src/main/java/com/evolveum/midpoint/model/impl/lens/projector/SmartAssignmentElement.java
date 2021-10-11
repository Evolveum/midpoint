/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.AddDeleteReplace;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public class SmartAssignmentElement implements DebugDumpable {

    @NotNull private final PrismContainerValue<AssignmentType> assignmentCVal;
    @NotNull private final AssignmentOrigin origin;

    SmartAssignmentElement(@NotNull PrismContainerValue<AssignmentType> assignmentCVal, boolean virtual) {
        this.assignmentCVal = assignmentCVal;
        this.origin = new AssignmentOrigin(virtual);
    }

    @NotNull
    public AssignmentOrigin getOrigin() {
        return origin;
    }

    public boolean isCurrent() {
        return origin.isCurrent();
    }

    public boolean isOld() {
        return origin.isOld();
    }

    public boolean isChanged() {
        return origin.isChanged();
    }

    @NotNull
    public PrismContainerValue<AssignmentType> getAssignmentCVal() {
        return assignmentCVal;
    }

    public SmartAssignmentKey getKey() {
        return new SmartAssignmentKey(assignmentCVal);
    }

    public boolean isVirtual() {
        return origin.isVirtual();
    }

    @Override
    public String toString() {
        return "SAE(" + origin + ": " + assignmentCVal + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("SmartAssignmentElement: ").append(origin).append("\n");
        sb.append(assignmentCVal.debugDump(indent + 1));
        return sb.toString();
    }

    void updateFlags(SmartAssignmentCollection.Mode mode, AddDeleteReplace deltaSet) {
        origin.updateFlags(mode, deltaSet);
    }

    // TODO: equals, hashCode
}
