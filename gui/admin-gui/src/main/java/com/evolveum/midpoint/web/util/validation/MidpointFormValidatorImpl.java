/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util.validation;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  This is a simple implementation of MidpointFormValidator interface
 *
 *  @author shood
 * */
public class MidpointFormValidatorImpl implements MidpointFormValidator, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public Collection<SimpleValidationError> validateObject(PrismObject<? extends ObjectType> object, Collection<ObjectDelta<? extends ObjectType>> deltas) {
        List<SimpleValidationError> errors = new ArrayList<>();

        if(object != null){
            UserType user = (UserType)object.asObjectable();

            if(user.getName() == null){
                errors.add(new SimpleValidationError("The name of the user can't be null", new ItemPathType(UserType.F_NAME)));
            }

            if(user.getGivenName() == null){
                errors.add(new SimpleValidationError("The given name of the user can't be null", new ItemPathType(UserType.F_GIVEN_NAME)));
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
            errors.add(new SimpleValidationError("The target ref of the assignment can't be null", new ItemPathType(AssignmentType.F_TARGET_REF)));
        }

        if(assignment.getTenantRef() == null){
            errors.add(new SimpleValidationError("The tenant ref of the assignment can't be null", new ItemPathType(AssignmentType.F_TENANT_REF)));
        }

        return errors;
    }
}
