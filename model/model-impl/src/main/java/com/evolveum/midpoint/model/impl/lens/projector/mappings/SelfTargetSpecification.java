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
 *
 */
public class SelfTargetSpecification<T extends AssignmentHolderType> extends TargetObjectSpecification<T> {

    @Override
    public <AH extends AssignmentHolderType> PrismObject<T> getTargetObject(ObjectDeltaObject<AH> updatedFocusOdo) {
        //noinspection unchecked
        return (PrismObject<T>) updatedFocusOdo.getNewObject();
    }

    @Override
    public boolean isUpdatedWithMappingResults() {
        return true;
    }
}
