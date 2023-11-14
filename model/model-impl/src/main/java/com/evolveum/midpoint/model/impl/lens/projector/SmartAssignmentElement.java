/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.AddDeleteReplace;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public class SmartAssignmentElement implements DebugDumpable {

    @NotNull private final PrismContainerValue<AssignmentType> assignmentCVal;
    @NotNull private final AssignmentOrigin origin; // [EP:APSO] DONE

    /** External ID can be obtained from repository as pre-allocated assignment PCV ID (for new assignments). */
    private Long externalId;

    SmartAssignmentElement(
            @NotNull PrismContainerValue<AssignmentType> assignmentCVal,
            boolean virtual,
            @NotNull ConfigurationItemOrigin origin) { // [EP:APSO] DONE 1/1
        this.assignmentCVal = assignmentCVal;
        this.origin = new AssignmentOrigin(virtual, origin); // [EP:APSO] DONE
    }

    public @NotNull AssignmentOrigin getOrigin() {
        return origin;
    }

    public boolean isNew() {
        return origin.isNew();
    }

    public boolean isCurrent() {
        return origin.isCurrent();
    }

    public boolean isOld() {
        return origin.isOld();
    }

    public @NotNull PrismContainerValue<AssignmentType> getAssignmentCVal() {
        return assignmentCVal;
    }

    public @NotNull AssignmentType getAssignment() {
        return assignmentCVal.asContainerable();
    }

    /** ID as was really specified in the object or delta. */
    public Long getBuiltInAssignmentId() {
        return assignmentCVal.getId();
    }

    /** Artificial ID that is used for new assignments. */
    public Long getExternalAssignmentId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
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
        sb.append("SmartAssignmentElement: ").append(origin);
        if (externalId != null) {
            sb.append(" (externalId=").append(externalId).append(")");
        }
        sb.append("\n");
        sb.append(assignmentCVal.debugDump(indent + 1));
        return sb.toString();
    }

    void updateOrigin(SmartAssignmentCollection.Mode mode, AddDeleteReplace deltaSet) {
        origin.update(mode, deltaSet);
    }

    // TODO: equals, hashCode
}
