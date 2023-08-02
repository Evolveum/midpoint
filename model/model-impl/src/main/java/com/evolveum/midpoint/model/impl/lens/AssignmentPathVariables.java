/*
 * Copyright (c) 2014-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class AssignmentPathVariables implements Serializable {

    private AssignmentPathImpl assignmentPath;
    private ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> magicAssignment;
    private ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> immediateAssignment;
    private ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> thisAssignment;
    private ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> focusAssignment;
    private PrismObject<? extends AbstractRoleType> immediateRole;

    public AssignmentPathImpl getAssignmentPath() {
        return assignmentPath;
    }

    public void setAssignmentPath(AssignmentPathImpl assignmentPath) {
        this.assignmentPath = assignmentPath;
    }

    public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getMagicAssignment() {
        return magicAssignment;
    }

    public void setMagicAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> magicAssignment) {
        this.magicAssignment = magicAssignment;
    }

    public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getImmediateAssignment() {
        return immediateAssignment;
    }

    public void setImmediateAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> immediateAssignment) {
        this.immediateAssignment = immediateAssignment;
    }

    public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getThisAssignment() {
        return thisAssignment;
    }

    public void setThisAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> thisAssignment) {
        this.thisAssignment = thisAssignment;
    }

    public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getFocusAssignment() {
        return focusAssignment;
    }

    public void setFocusAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> focusAssignment) {
        this.focusAssignment = focusAssignment;
    }

    public PrismObject<? extends AbstractRoleType> getImmediateRole() {
        return immediateRole;
    }

    public void setImmediateRole(PrismObject<? extends AbstractRoleType> immediateRole) {
        this.immediateRole = immediateRole;
    }

    public PrismContainerDefinition<AssignmentType> getAssignmentDefinition() {
        if (magicAssignment != null) {
            return magicAssignment.getDefinition();
        }
        if (immediateAssignment != null) {
            return immediateAssignment.getDefinition();
        }
        if (thisAssignment != null) {
            return thisAssignment.getDefinition();
        }
        if (focusAssignment != null) {
            return focusAssignment.getDefinition();
        }
        return null;
    }

}
