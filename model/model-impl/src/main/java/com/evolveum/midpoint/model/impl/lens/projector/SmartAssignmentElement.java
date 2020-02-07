/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author semancik
 */
public class SmartAssignmentElement implements DebugDumpable {

    private PrismContainerValue<AssignmentType> assignmentCVal;
    private boolean isCurrent = false;
    private boolean isOld = false;
    private boolean isChanged = false;

    private boolean virtual;

    SmartAssignmentElement(PrismContainerValue<AssignmentType> assignmentCVal, boolean virtual) {
        this.assignmentCVal = assignmentCVal;
        this.virtual = virtual;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public boolean isOld() {
        return isOld;
    }

    public void setOld(boolean isOld) {
        this.isOld = isOld;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public void setChanged(boolean isChanged) {
        this.isChanged = isChanged;
    }

    public PrismContainerValue<AssignmentType> getAssignmentCVal() {
        return assignmentCVal;
    }

    public SmartAssignmentKey getKey() {
        return new SmartAssignmentKey(assignmentCVal);
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    @Override
    public String toString() {
        return "SAE(" + flag(isCurrent,"current") + flag(isOld,"old") + flag(isChanged,"changed") + ": " + assignmentCVal + ")";
    }

    private String flag(boolean b, String label) {
        if (b) {
            return label + ",";
        }
        return "";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("SmartAssignmentElement: ");
        flag(sb, isCurrent, "current");
        flag(sb, isOld, "old");
        flag(sb, isChanged, "changed");
        sb.append("\n");
        sb.append(assignmentCVal.debugDump(indent + 1));
        return sb.toString();
    }

    private void flag(StringBuilder sb, boolean b, String label) {
        if (b) {
            sb.append(label).append(",");
        }
    }

    // TODO: equals, hashCode


}
