/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.ObjectDefinitionDelegator;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TransformableObjectDefinition<O extends Objectable> extends TransformableContainerDefinition<O>
    implements ObjectDefinitionDelegator<O>, PrismObjectDefinition.PrismObjectDefinitionMutator<O> {

    public TransformableObjectDefinition(PrismObjectDefinition<O> delegate) {
        this(delegate, delegate.getComplexTypeDefinition());
    }

    public TransformableObjectDefinition(PrismObjectDefinition<O> delegate, ComplexTypeDefinition typedef) {
        super(delegate, typedef);
        //noinspection unchecked
        delegatedItem(new DelegatedItem.ObjectDef(delegate()));
    }

    @NotNull
    public static <O extends Objectable> TransformableObjectDefinition<O> of(PrismObjectDefinition<O> originalItem) {
        return new TransformableObjectDefinition<>(originalItem);
    }

    @Override
    ItemDefinition<?> attachTo(TransformableComplexTypeDefinition complexType) {
        // NOOP
        return this;
    }

    @Override
    protected PrismObjectDefinition<O> publicView() {
        return this;
    }

    @Override
    public @NotNull PrismObjectDefinition<O> clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismObjectDefinitionMutator<O> mutator() {
        return this;
    }

    @Override
    public PrismObjectDefinition<O> delegate() {
        return (PrismObjectDefinition<O>) super.delegate();
    }

    @Override
    public @NotNull PrismObjectDefinition<O> cloneWithNewDefinition(QName newItemName, ItemDefinition<?> newDefinition) {
        TransformableComplexTypeDefinition typedef = complexTypeDefinition.copy();
        typedef.replaceDefinition(newItemName, newDefinition);
        return new TransformableObjectDefinition<>(this, typedef);
    }

    @Override
    public PrismObjectDefinition<O> deepClone(@NotNull DeepCloneOperation operation) {
        return (PrismObjectDefinition<O>) super.deepClone(operation);
    }

    @Override
    protected TransformableContainerDefinition<O> copy(ComplexTypeDefinition def) {
        return new TransformableObjectDefinition<>(this, def);
    }

    @Override
    public @NotNull PrismObject<O> instantiate() throws SchemaException {
        return instantiate(getItemName());
    }

    @Override
    public @NotNull PrismObject<O> instantiate(QName name) throws SchemaException {
        return (PrismObject<O>) super.instantiate(name);
    }

    /** For a strange reason, IntelliJ IDEA complains about missing {@link #createValue()} method. So adding it here. */
    @Override
    public PrismObjectValue<O> createValue() {
        return delegate().createValue();
    }

}
