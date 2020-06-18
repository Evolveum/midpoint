/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * A key for assignment:mode => modifications map (for policy state).
 */
public class AssignmentSpec implements Serializable {

    @NotNull public final AssignmentType assignment;
    @NotNull public final PlusMinusZero mode; // regarding the current object state (not the old one)

    public AssignmentSpec(@NotNull AssignmentType assignment, @NotNull PlusMinusZero mode) {
        this.assignment = assignment;
        this.mode = mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AssignmentSpec))
            return false;
        AssignmentSpec that = (AssignmentSpec) o;
        return Objects.equals(assignment, that.assignment) && mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignment, mode);
    }

    @Override
    public String toString() {
        return mode + ":" + assignment;
    }
}
