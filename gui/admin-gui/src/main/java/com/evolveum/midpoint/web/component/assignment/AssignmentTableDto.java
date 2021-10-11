/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
