/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author semancik
 */
public class SmartAssignmentKey {

    private static final Trace LOGGER = TraceManager.getTrace(SmartAssignmentKey.class);

    private PrismContainerValue<AssignmentType> assignmentCVal;

    public SmartAssignmentKey(PrismContainerValue<AssignmentType> assignmentCVal) {
        super();
        this.assignmentCVal = assignmentCVal;
    }

    // This is a key to an assignment hashmap.
    // hashCode() and equals() are very important here.
    // Especially equals(). We want to make the comparison reliable, but reasonably quick.

    @Override
    public int hashCode() {
        return assignmentCVal.hashCode();
        //return 1;
    }

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
        SmartAssignmentKey other = (SmartAssignmentKey) obj;
        if (assignmentCVal == null) {
            if (other.assignmentCVal != null) {
                return false;
            }
        } else if (!equalsAssignment(other.assignmentCVal)) {
            return false;
        }
        return true;
    }

    private boolean equalsAssignment(PrismContainerValue<AssignmentType> other) {
        return assignmentCVal.equals(other, EquivalenceStrategy.IGNORE_METADATA);
    }

    @Override
    public String toString() {
        return "SmartAssignmentKey(" + assignmentCVal + ")";
    }

}
