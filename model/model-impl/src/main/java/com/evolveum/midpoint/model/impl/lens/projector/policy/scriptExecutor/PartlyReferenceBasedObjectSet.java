/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

/**
 * Object set partly represented as prism object references (for current object and link targets).
 * Link sources could be represented as a query or references.
 */
abstract class PartlyReferenceBasedObjectSet extends ObjectSet<PrismReferenceValue> {

    PartlyReferenceBasedObjectSet(ActionContext actx, OperationResult result) {
        super(actx, result);
    }

    @Override
    PrismReferenceValue toIndividualObject(PrismObject<?> object) {
        return ObjectTypeUtil.createObjectRef(object, beans.prismContext).asReferenceValue();
    }
}
