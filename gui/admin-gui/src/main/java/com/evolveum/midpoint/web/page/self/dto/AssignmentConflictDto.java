/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.io.Serializable;

/**
 * Created by honchar.
 */
public class AssignmentConflictDto<F extends FocusType> implements Serializable {
    PrismObject<F>  assignmentTargetObject;
    boolean resolved = false;
    boolean oldAssignment = false;

    public AssignmentConflictDto(){
    }

    public AssignmentConflictDto(PrismObject<F>  assignmentTargetObject, boolean oldAssignment){
        this.assignmentTargetObject = assignmentTargetObject;
        this.oldAssignment = oldAssignment;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public boolean isResolved() {
        return resolved;
    }

    public boolean isOldAssignment() {
        return oldAssignment;
    }

    public PrismObject<F> getAssignmentTargetObject() {
        return assignmentTargetObject;
    }

  }
