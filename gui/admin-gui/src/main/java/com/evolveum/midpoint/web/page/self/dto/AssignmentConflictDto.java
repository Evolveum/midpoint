/*
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
