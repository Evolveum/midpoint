/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.deleg.RefinedAttributeDefinitionDelegator;
import com.evolveum.midpoint.prism.deleg.PropertyDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.MutableRawResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.deleg.AttributeDefinitionDelegator;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TransformablePropertyDefinition<T> extends TransformableItemDefinition<PrismProperty<T>, PrismPropertyDefinition<T>>
    implements PropertyDefinitionDelegator<T>, PartiallyMutableItemDefinition.Property<T> {


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
    protected TransformablePropertyDefinition<T> copy() {
        return new TransformablePropertyDefinition<>(this);
    }

    @Override
    public MutablePrismPropertyDefinition<T> toMutable() {
        return this;
    }

    @Override
    public @NotNull PrismProperty<T> instantiate() {
        return instantiate(getItemName());
    }

    @Override
    public @NotNull PrismProperty<T> instantiate(QName name) {
        try {
            return super.instantiate(name);
        } catch (SchemaException e) {
            throw new IllegalStateException("Should not happened");
        }
    }

    @Override
    public @NotNull PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return PropertyDefinitionDelegator.super.createEmptyDelta(path);
    }

    @Override
    public Class<T> getTypeClass() {
        return PropertyDefinitionDelegator.super.getTypeClass();
    }

    @Override
    protected PrismPropertyDefinition<T> publicView() {
        return this;
    }

    public static class ResourceAttribute<T>
            extends TransformablePropertyDefinition<T>
            implements AttributeDefinitionDelegator<T>, PartiallyMutableItemDefinition.Attribute<T> {
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
            return copy();
        }

        @Override
        public ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
            return copy(); // FIXME
        }

        @Override
        protected ResourceAttribute<T> copy() {
            return new ResourceAttribute<>(this);
        }

        @Override
        public @NotNull MutableRawResourceAttributeDefinition<T> toMutable() {
            return this;
        }

        @Override
        public boolean canAdd() {
            return delegate().canAdd();
        }

        @Override
        public boolean canModify() {
            return delegate().canModify();
        }

        @Override
        public boolean canRead() {
            return delegate().canRead();
        }

        @Override
        public @NotNull com.evolveum.midpoint.schema.processor.ResourceAttribute<T> instantiate() {
            return instantiate(getItemName());
        }

        @Override
        public @NotNull com.evolveum.midpoint.schema.processor.ResourceAttribute<T> instantiate(QName name) {
            var deleg = delegate().instantiate(name);
            deleg.setDefinition(this);
            return deleg;
        }
    }

    public static class RefinedAttribute<T> extends ResourceAttribute<T> implements RefinedAttributeDefinitionDelegator<T> {

        public RefinedAttribute(PrismPropertyDefinition<T> delegate) {
            super(delegate);
        }

        @Override
        public ResourceAttributeDefinition<T> delegate() {
            return (ResourceAttributeDefinition<T>) super.delegate();
        }

        @Override
        public @NotNull ResourceAttributeDefinition<T> clone() {
            return copy();
        }

        @Override
        protected RefinedAttribute<T> copy() {
            return new RefinedAttribute<>(this);
        }

        @Override
        public ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canAdd() {
            return delegate().canAdd();
        }

        @Override
        public boolean canModify() {
            return delegate().canModify();
        }

        @Override
        public boolean canRead() {
            return delegate().canRead();
        }

        @Override
        public @NotNull com.evolveum.midpoint.schema.processor.ResourceAttribute<T> instantiate() {
            return instantiate(getItemName());
        }

        @Override
        public @NotNull com.evolveum.midpoint.schema.processor.ResourceAttribute<T>  instantiate(QName name) {
            var deleg = delegate().instantiate(name);
            deleg.setDefinition(this);
            return deleg;
        }
    }


}
