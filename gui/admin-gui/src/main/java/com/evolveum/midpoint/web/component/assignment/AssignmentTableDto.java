/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
