/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 *  Target object is a given (fixed) prism object: either the same as focus ODO (but with no updates!),
 *  or a different one.
 */
public class FixedTargetSpecification<T extends AssignmentHolderType> extends TargetObjectSpecification<T> {

    private final PrismObject<T> targetObject;
    private final boolean sameAsSource;

    public FixedTargetSpecification(PrismObject<T> targetObject, boolean sameAsSource) {
        this.targetObject = targetObject;
        this.sameAsSource = sameAsSource;
    }

    @Override
    public PrismObject<T> getTargetObject() {
        return targetObject;
    }

    @Override
    public boolean isSameAsSource() {
        return sameAsSource;
    }
}
