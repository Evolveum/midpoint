/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.util.validation;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  This is a simple implementation of MidpointFormValidator interface
 *
 *  @author shood
 * */
public class MidpointFormValidatorImpl implements MidpointFormValidator {

    @Override
    public Collection<SimpleValidationError> validateObject(PrismObject<? extends ObjectType> object, Collection<ObjectDelta<? extends ObjectType>> deltas) {
        List<SimpleValidationError> errors = new ArrayList<>();

        if(object != null){
            UserType user = (UserType)object.asObjectable();

            if(user.getName() == null){
                errors.add(new SimpleValidationError("The name of the user can't be null", new ItemPathType(UserType.F_NAME.getLocalPart())));
            }

            if(user.getGivenName() == null){
                errors.add(new SimpleValidationError("The given name of the user can't be null", new ItemPathType(UserType.F_GIVEN_NAME.getLocalPart())));
            }
        }

        return errors;
    }

    @Override
    public Collection<SimpleValidationError> validateAssignment(AssignmentType assignment) {
        List<SimpleValidationError> errors = new ArrayList<>();

        if(assignment == null){
            return errors;
        }

        if(assignment.getTargetRef() == null){
            errors.add(new SimpleValidationError("The target ref of the assignment can't be null", new ItemPathType(AssignmentType.F_TARGET_REF.getLocalPart())));
        }

        if(assignment.getTenantRef() == null){
            errors.add(new SimpleValidationError("The tenant ref of the assignment can't be null", new ItemPathType(AssignmentType.F_TENANT_REF.getLocalPart())));
        }

        return errors;
    }
}
