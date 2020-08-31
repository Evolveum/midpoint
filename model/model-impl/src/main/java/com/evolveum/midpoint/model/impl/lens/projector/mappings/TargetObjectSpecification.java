/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 *  Tells MappingSetEvaluator how to find target object.
 *
 *  It can be either the source object itself (standard template mappings, assigned focus mappings)
 *  or another object (persona mappings). It can be fixed or derived from current ODO that is being updated
 *  as mappings are evaluated.
 *
 *  (Currently we use only fixed target specification.)
 */

public abstract class TargetObjectSpecification<T extends AssignmentHolderType> {

    /**
     * @return The target object that is to be provided to the mapping evaluator. It is needed e.g. to find current values
     *         of mapping target item.
     */
    public abstract PrismObject<T> getTargetObject();

    /**
     * @return Is the target the same object as source, i.e. should mappings be chained?
     */
    public abstract boolean isSameAsSource();
}
