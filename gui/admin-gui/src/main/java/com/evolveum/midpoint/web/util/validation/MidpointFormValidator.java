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

import java.util.Collection;

/**
 *  <p>
 *      A simple interface that aims to work as a custom validation plugin used in GUI.
 *      This plugin should be used BEFORE the changes made by user are sent for processing
 *      to model component.
 *  </p>
 *
 *  <p>
 *      This plugin serves as another form of validation process and can be used, when
 *      standard validation mechanism of GUI forms (usually aimed to validate one field
 *      at a time) is not enough. A classic use case may be a situation, when we need to
 *      examine the relationship between attributes edited via GUI before sending them
 *      for processing to model component.
 *  </p>
 *
 *  @author shood
 * */
public interface MidpointFormValidator {

    /**
     *  Performs a validation on an instance of object. Entire data of the object
     *  are accessible for validation purposes as well as a collection of ObjectDelta
     *  instances - the collection of current changes made by user prior to
     *  validation.
     *
     *  @param object
     *      An object to validate
     *
     *  @param deltas
     *      A collection of ObjectDelta instances - a representation of changes made by user
     *
     *  @return A collection of SimpleValidationError instances
     *
     * */
    Collection<SimpleValidationError> validateObject(PrismObject<? extends ObjectType> object, Collection<ObjectDelta<? extends ObjectType>> deltas);

    /**
     *  Performs a validation on an instance of AssignmentType that represents
     *  an assignment in midPoint.
     *
     *  @param assignment
     *      An object to validate
     *
     *  @return A collection of SimpleValidationError instances
     * */
    Collection<SimpleValidationError> validateAssignment(AssignmentType assignment);
}
