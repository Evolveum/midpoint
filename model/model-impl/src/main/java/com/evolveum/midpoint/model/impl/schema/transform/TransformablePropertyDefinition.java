/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.deleg.PropertyDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.MutableResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.deleg.AttributeDefinitionDelegator;

public class TransformablePropertyDefinition<T> extends TransformableItemDefinition<PrismProperty<T>, PrismPropertyDefinition<T>> implements PropertyDefinitionDelegator<T> {


    private static final long serialVersionUID = 1L;

    public TransformablePropertyDefinition(PrismPropertyDefinition<T> delegate) {
        super(delegate);
    }

    public static <T> PrismPropertyDefinition<T> of(PrismPropertyDefinition<T> originalItem) {
        if (originalItem instanceof TransformablePropertyDefinition) {
            return originalItem;
        }
        if (originalItem instanceof ResourceAttributeDefinition) {
            return new ResourceAttribute<>(originalItem);
        }


        return new TransformablePropertyDefinition<>(originalItem);
    }

    @Override
    public void revive(PrismContext prismContext) {

    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public void freeze() {
        // NOOP
    }

    @Override
    public @NotNull PrismPropertyDefinition<T> clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MutablePrismPropertyDefinition<T> toMutable() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <ID extends ItemDefinition<?>> ID apply(ID originalItem) {
        return (ID) publicView();
    }

    @Override
    protected PrismPropertyDefinition<T> publicView() {
        return this;
    }

    public static class ResourceAttribute<T> extends TransformablePropertyDefinition<T> implements AttributeDefinitionDelegator<T> {
        private static final long serialVersionUID = 1L;

        public ResourceAttribute(PrismPropertyDefinition<T> delegate) {
            super(delegate);
        }

        @Override
        public ResourceAttributeDefinition<T> delegate() {
            return (ResourceAttributeDefinition<T>) super.delegate();
        }

        @Override
        public @NotNull ResourceAttributeDefinition<T> clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MutableResourceAttributeDefinition<T> toMutable() {
            throw new UnsupportedOperationException();
        }
    }


}
