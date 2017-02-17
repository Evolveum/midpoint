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
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.io.Serializable;

/**
 * Created by honchar.
 */
public class AssignmentConflictDto<F extends FocusType> implements Serializable {
    PrismObject<F>  existingAssignmentTargetObj;
    PrismObject<F>  addedAssignmentTargetObj;
    boolean isRemovedOld = false;
    boolean isUnassignedNew = false;

    public AssignmentConflictDto(){
    }

    public AssignmentConflictDto(PrismObject<F>  existingAssignmentTargetObj, PrismObject<F>  addedAssignmentTargetObj){
        this.existingAssignmentTargetObj = existingAssignmentTargetObj;
        this.addedAssignmentTargetObj = addedAssignmentTargetObj;
    }


    public boolean isSolved() {
        return isRemovedOld || isUnassignedNew;
    }

    public PrismObject<F> getExistingAssignmentTargetObj() {
        return existingAssignmentTargetObj;
    }

    public void setExistingAssignmentTargetObj(PrismObject<F> existingAssignmentTargetObj) {
        this.existingAssignmentTargetObj = existingAssignmentTargetObj;
    }

    public PrismObject<F> getAddedAssignmentTargetObj() {
        return addedAssignmentTargetObj;
    }

    public void setAddedAssignmentTargetObj(PrismObject<F> addedAssignmentTargetObj) {
        this.addedAssignmentTargetObj = addedAssignmentTargetObj;
    }

    public boolean isRemovedOld() {
        return isRemovedOld;
    }

    public void setRemovedOld(boolean removedOld) {
        isRemovedOld = removedOld;
    }

    public boolean isUnassignedNew() {
        return isUnassignedNew;
    }

    public void setUnassignedNew(boolean unassignedNew) {
        isUnassignedNew = unassignedNew;
    }
}
