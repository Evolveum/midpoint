/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismObjectDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.deleg.ObjectDefinitionDelegator;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TransformableObjectDefinition<O extends Objectable> extends TransformableContainerDefinition<O> implements ObjectDefinitionDelegator<O> {

    public TransformableObjectDefinition(PrismObjectDefinition<O> delegate) {
        super(delegate);
    }

    public TransformableObjectDefinition(PrismObjectDefinition<O> delegate, ComplexTypeDefinition typedef) {
        super(delegate, typedef);
    }

    public static <O extends Objectable> TransformableObjectDefinition<O> of(PrismObjectDefinition<O> originalItem) {
        return new TransformableObjectDefinition<>(originalItem);
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
    public MutablePrismObjectDefinition<O> toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismObjectDefinition<O> delegate() {
        return (PrismObjectDefinition<O>) super.delegate();
    }

    @Override
    public PrismObjectDefinition<O> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
        TransformableComplexTypeDefinition typedef = complexTypeDefinition.copy();
        typedef.replaceDefinition(itemName, newDefinition);
        return new TransformableObjectDefinition<>(this, typedef);
    }

    @Override
    public PrismObjectDefinition<O> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        throw new UnsupportedOperationException();
    }

    public static <O extends Objectable> PrismObjectDefinition<O> of(PrismObject<O> object) {
        PrismObjectDefinition<O> origDef = object.getDefinition();
        if (origDef instanceof TransformableObjectDefinition) {
            return origDef;
        }
        TransformableObjectDefinition<O> newDef = TransformableObjectDefinition.of(origDef);
        try {
            object.applyDefinition(newDef, true);
        } catch (SchemaException e) {
            throw new IllegalStateException("Can not replace definition for transformable one", e);
        }
        return newDef;
    }

}
