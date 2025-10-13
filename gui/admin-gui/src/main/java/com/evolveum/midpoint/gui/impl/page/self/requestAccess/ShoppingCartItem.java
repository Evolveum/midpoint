/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

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

    private List<PolyStringType> poiNames;

    public ShoppingCartItem(AssignmentType assignment, int count, List<PolyStringType> poiNames) {
        this.assignment = assignment;
        this.count = count;
        this.poiNames = poiNames;
    }

    public List<PolyStringType> getPoiNames() {
        if (poiNames == null) {
            poiNames = new ArrayList<>();
        }
        return poiNames;
    }

    public AssignmentType getAssignment() {
        return assignment;
    }

    public int getCount() {
        return count;
    }

    public String getName() {
        ObjectReferenceType ref = getTargetRef();
        if (ref == null) {
            return null;
        }
        PolyStringType targetName = ref.getTargetName();
        if (targetName != null) {
            return targetName.getOrig();
        }

        return ref.getOid();
    }

    public QName getRelation() {
        ObjectReferenceType ref = getTargetRef();
        if (ref == null) {
            return null;
        }

        return ref.getRelation();
    }

    private ObjectReferenceType getTargetRef() {
        if (assignment == null || assignment.getTargetRef() == null) {
            return null;
        }

        return assignment.getTargetRef();
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

    @Override
    public String toString() {
        return "SCI{" + assignment + ", count=" + count + '}';
    }
}
