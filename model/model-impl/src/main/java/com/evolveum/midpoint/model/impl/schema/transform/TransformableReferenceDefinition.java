/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.MutablePrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.deleg.ReferenceDefinitionDelegator;

public class TransformableReferenceDefinition extends TransformableItemDefinition<PrismReference, PrismReferenceDefinition>
        implements ReferenceDefinitionDelegator, PartiallyMutableItemDefinition.Reference {

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
    public MutablePrismReferenceDefinition toMutable() {
        return this;
    }

    @Override
    public @NotNull PrismReference instantiate() {
        return instantiate(getItemName());
    }

    @Override
    public @NotNull PrismReference instantiate(QName name) {
        return getPrismContext().itemFactory().createReference(name, this);
    }

    @Override
    public @NotNull PrismReferenceDefinition clone() {
        return copy();
    }

    @Override
    protected TransformableReferenceDefinition copy() {
        return new TransformableReferenceDefinition(this);
    }
}
