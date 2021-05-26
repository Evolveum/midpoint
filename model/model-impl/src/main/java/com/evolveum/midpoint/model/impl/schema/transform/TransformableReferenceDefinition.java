/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.deleg.ReferenceDefinitionDelegator;

public class TransformableReferenceDefinition extends TransformableItemDefinition<PrismReference, PrismReferenceDefinition> implements ReferenceDefinitionDelegator {

    private static final long serialVersionUID = 1L;

    protected TransformableReferenceDefinition(PrismReferenceDefinition delegate) {
        super(delegate);
    }

    public static TransformableReferenceDefinition of(PrismReferenceDefinition original) {
        return new TransformableReferenceDefinition(original);
    }

    @Override
    protected PrismReferenceDefinition publicView() {
        return this;
    }

    @Override
    public void revive(PrismContext prismContext) {

    }

    @Override
    public MutableItemDefinition<PrismReference> toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PrismReferenceDefinition clone() {
        throw new UnsupportedOperationException();
    }
}
