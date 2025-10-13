/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class AssignmentTableDto<T extends ObjectType> implements Serializable {

    private PrismObject<T> assignmentParent;

    public AssignmentTableDto(){}

    public AssignmentTableDto(PrismObject<T> assignmentParent){
        this.assignmentParent = assignmentParent;
    }

    public PrismObject<T> getAssignmentParent() {
        return assignmentParent;
    }

    public void setAssignmentParent(PrismObject<T> assignmentParent) {
        this.assignmentParent = assignmentParent;
    }
}
