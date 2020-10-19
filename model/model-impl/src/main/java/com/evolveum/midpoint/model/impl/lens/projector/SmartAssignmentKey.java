/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public class SmartAssignmentKey {

    @NotNull private final PrismContainerValue<AssignmentType> assignmentCVal;

    SmartAssignmentKey(@NotNull PrismContainerValue<AssignmentType> assignmentCVal) {
        this.assignmentCVal = assignmentCVal;
    }

    // This is a key to an assignment map.
    // hashCode() and equals() are very important here.
    // Especially equals(). We want to make the comparison reliable, but reasonably quick.

    @Override
    public int hashCode() {
        return assignmentCVal.hashCode(EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return assignmentCVal.equals(((SmartAssignmentKey) obj).assignmentCVal, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
    }

    @Override
    public String toString() {
        return "SmartAssignmentKey(" + assignmentCVal + ")";
    }

}
