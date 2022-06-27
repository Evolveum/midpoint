/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictItem implements Serializable {

    private AssignmentType assignment;

    private String displayName;

    private boolean existing;

    public ConflictItem(AssignmentType assignment, String displayName, boolean existing) {
        this.assignment = assignment;
        this.displayName = displayName;
        this.existing = existing;
    }

    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }

        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef == null) {
            return null;
        }

        return WebComponentUtil.getName(targetRef);
    }

    public AssignmentType getAssignment() {
        return assignment;
    }

    public boolean isExisting() {
        return existing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        ConflictItem that = (ConflictItem) o;

        if (existing != that.existing) {return false;}
        return assignment != null ? assignment.equals(that.assignment) : that.assignment == null;
    }

    @Override
    public int hashCode() {
        int result = assignment != null ? assignment.hashCode() : 0;
        result = 31 * result + (existing ? 1 : 0);
        return result;
    }
}
