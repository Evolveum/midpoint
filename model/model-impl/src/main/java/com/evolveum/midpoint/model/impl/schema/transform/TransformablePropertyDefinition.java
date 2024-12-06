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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.deleg.ResourceAttributeDefinitionDelegator;
import com.evolveum.midpoint.prism.deleg.PropertyDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.MutableRawResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serial;

public class TransformablePropertyDefinition<T> extends TransformableItemDefinition<PrismProperty<T>, PrismPropertyDefinition<T>>
    implements PropertyDefinitionDelegator<T>, PartiallyMutableItemDefinition.Property<T> {

    @Serial private static final long serialVersionUID = 1L;

    TransformablePropertyDefinition(PrismPropertyDefinition<T> delegate) {
        super(delegate);
    }

    public static <T> PrismPropertyDefinition<T> of(PrismPropertyDefinition<T> originalItem) {
        if (originalItem instanceof TransformablePropertyDefinition) {
            return originalItem;
        }
        if (originalItem instanceof ShadowSimpleAttributeDefinition) {
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
    public @NotNull TransformablePropertyDefinition<T> clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected TransformablePropertyDefinition<T> copy() {
        return new TransformablePropertyDefinition<>(this);
    }

    @Override
    public PrismPropertyDefinitionMutator<T> mutator() {
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

    @Override
    public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
    }

    public static class ResourceAttribute<T>
            extends TransformablePropertyDefinition<T>
            implements ResourceAttributeDefinitionDelegator<T>, PartiallyMutableItemDefinition.Attribute<T> {
        @Serial private static final long serialVersionUID = 1L;

        public ResourceAttribute(PrismPropertyDefinition<T> delegate) {
            super(delegate);
        }

        @Override
        public ShadowSimpleAttributeDefinition<T> delegate() {
            return (ShadowSimpleAttributeDefinition<T>) super.delegate();
        }

        @Override
        public @NotNull ResourceAttribute<T> clone() {
            return copy();
        }

        @Override
        public ShadowSimpleAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
            return copy(); // FIXME
        }

        @Override
        protected ResourceAttribute<T> copy() {
            return new ResourceAttribute<>(this);
        }

        @Override
        public @NotNull MutableRawResourceAttributeDefinition<T> mutator() {
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
        public @NotNull ShadowSimpleAttribute<T> instantiate() {
            return instantiate(getItemName());
        }

        @Override
        public @NotNull ShadowSimpleAttribute<T> instantiate(QName name) {
            var deleg = delegate().instantiate(name);
            deleg.setDefinition(this);
            return deleg;
        }

        @Override
        public PrismPropertyValue<T> createPrismValueFromRealValue(@NotNull Object realValue) throws SchemaException {
            return delegate().createPrismValueFromRealValue(realValue);
        }

        @Override
        public @NotNull Class<T> getTypeClass() {
            return super.getTypeClass();
        }

        @Override
        public void shortDump(StringBuilder sb) {
            delegate().shortDump(sb);
        }

        @Override
        public boolean isVolatileOnAddOperation() {
            return delegate().isVolatileOnAddOperation();
        }

        @Override
        public boolean isVolatileOnModifyOperation() {
            return delegate().isVolatileOnModifyOperation();
        }
    }

    /** TODO is this used? */
    public static class RefinedAttribute<T> extends ResourceAttribute<T> implements ResourceAttributeDefinitionDelegator<T> {

        public RefinedAttribute(PrismPropertyDefinition<T> delegate) {
            super(delegate);
        }

        @Override
        public ShadowSimpleAttributeDefinition<T> delegate() {
            return (ShadowSimpleAttributeDefinition<T>) super.delegate();
        }

        @Override
        public @NotNull RefinedAttribute<T> clone() {
            return copy();
        }

        @Override
        protected RefinedAttribute<T> copy() {
            return new RefinedAttribute<>(this);
        }

        @Override
        public ShadowSimpleAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
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
        public @NotNull ShadowSimpleAttribute<T> instantiate() {
            return instantiate(getItemName());
        }

        @Override
        public @NotNull ShadowSimpleAttribute<T> instantiate(QName name) {
            var deleg = delegate().instantiate(name);
            deleg.setDefinition(this);
            return deleg;
        }

        @Override
        public void shortDump(StringBuilder sb) {
            delegate().shortDump(sb);
        }

        @Override
        public @NotNull ItemDefinition<PrismProperty<T>> cloneWithNewName(@NotNull ItemName itemName) {
            throw new UnsupportedOperationException();
        }
    }
}
