/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShoppingCartItem implements Serializable, Comparable<ShoppingCartItem> {

    private AssignmentType assignment;

    private int count;

    public ShoppingCartItem(AssignmentType assignment, int count) {
        this.assignment = assignment;
        this.count = count;
    }

    public AssignmentType getAssignment() {
        return assignment;
    }

    public int getCount() {
        return count;
    }

    public String getName() {
        if (assignment == null || assignment.getTargetRef() == null) {
            return null;
        }

        ObjectReferenceType ref = assignment.getTargetRef();
        PolyStringType targetName = ref.getTargetName();
        if (targetName != null) {
            return targetName.getOrig();
        }

        return ref.getOid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        ShoppingCartItem that = (ShoppingCartItem) o;

        if (count != that.count) {return false;}
        return assignment != null ? assignment.equals(that.assignment) : that.assignment == null;
    }

    @Override
    public int hashCode() {
        int result = assignment != null ? assignment.hashCode() : 0;
        result = 31 * result + count;
        return result;
    }

    @Override
    public int compareTo(@NotNull ShoppingCartItem o) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.getName(), o.getName());
    }
}
