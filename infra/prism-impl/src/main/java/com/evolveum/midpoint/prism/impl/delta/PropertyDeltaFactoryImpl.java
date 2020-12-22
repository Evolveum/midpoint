/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public class PropertyDeltaFactoryImpl implements DeltaFactory.Property {

    @NotNull private final PrismContext prismContext;

    public PropertyDeltaFactoryImpl(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public <T> PropertyDelta<T> create(PrismPropertyDefinition<T> propertyDefinition) {
        return new PropertyDeltaImpl<>(propertyDefinition, prismContext);
    }

    @Override
    public <T> PropertyDelta<T> create(ItemPath path, PrismPropertyDefinition<T> definition) {
        return new PropertyDeltaImpl<>(path, definition, prismContext);
    }

    @Override
    public <T> PropertyDelta<T> create(ItemPath itemPath, QName name, PrismPropertyDefinition<T> propertyDefinition) {
        return new PropertyDeltaImpl<>(itemPath, name, propertyDefinition, prismContext);
    }

    @Override
    public <O extends Objectable, T> PropertyDelta<T> createReplaceDeltaOrEmptyDelta(PrismObjectDefinition<O> objectDefinition,
            QName propertyName, T realValue) {
        return PropertyDeltaImpl.createReplaceDeltaOrEmptyDelta(objectDefinition, propertyName, realValue);
    }


    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createAddDelta(PrismObjectDefinition<? extends Objectable> objectDefinition,
            ItemName propertyName, T... realValues) {
        return PropertyDeltaImpl.createAddDelta(objectDefinition, propertyName, realValues);
    }

    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createDeleteDelta(PrismObjectDefinition<? extends Objectable> objectDefinition,
            ItemName propertyName, T... realValues) {
        return PropertyDeltaImpl.createDeleteDelta(objectDefinition, propertyName, realValues);
    }

    @SafeVarargs
    @Override
    public final <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
            QName propertyName, T... realValues) {
        return PropertyDeltaImpl.createReplaceDelta(containerDefinition, propertyName, realValues);
    }

    @Override
    public final <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
            QName propertyName, PrismPropertyValue<T> pValue) {
        //noinspection unchecked
        return PropertyDeltaImpl.createReplaceDelta(containerDefinition, propertyName, pValue);
    }

    @SafeVarargs
    @Override
    public final <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
            QName propertyName, PrismPropertyValue<T>... pValues) {
        return PropertyDeltaImpl.createReplaceDelta(containerDefinition, propertyName, pValues);
    }

    @Override
    public <O extends Objectable> PropertyDelta createReplaceEmptyDelta(PrismObjectDefinition<O> objectDefinition,
            ItemPath propertyPath) {
        return PropertyDeltaImpl.createReplaceEmptyDelta(objectDefinition, propertyPath);
    }

    @Override
    public <O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, PrismObjectDefinition<O> objectDefinition) {
        return PropertyDeltaImpl.createDelta(propertyPath, objectDefinition);
    }

    @Override
    public <O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, Class<O> compileTimeClass) {
        return PropertyDeltaImpl.createDelta(propertyPath, compileTimeClass, prismContext);
    }

    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath,
            PrismObjectDefinition<?> objectDefinition,
            T... propertyValues) {
        return PropertyDeltaImpl.createModificationReplaceProperty(propertyPath, objectDefinition, propertyValues);
    }

    @Override
    public <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath,
            PrismObjectDefinition<?> objectDefinition,
            Collection<T> propertyValues) {
        return PropertyDeltaImpl.createModificationReplaceProperty(propertyPath, objectDefinition, propertyValues);
    }

    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath path, PrismPropertyDefinition propertyDefinition,
            T... propertyValues) {
        return PropertyDeltaImpl.createModificationReplaceProperty(path, propertyDefinition, propertyValues);
    }

    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath,
            PrismPropertyDefinition propertyDefinition,
            T... propertyValues) {
        return PropertyDeltaImpl.createModificationAddProperty(propertyPath, propertyDefinition, propertyValues);
    }

    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath,
            PrismObjectDefinition<?> objectDefinition,
            T... propertyValues) {
        return PropertyDeltaImpl.createModificationAddProperty(propertyPath, objectDefinition, propertyValues);
    }

    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath,
            PrismPropertyDefinition propertyDefinition,
            T... propertyValues) {
        return PropertyDeltaImpl.createModificationDeleteProperty(propertyPath, propertyDefinition, propertyValues);
    }

    @SafeVarargs
    @Override
    public final <T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath,
            PrismObjectDefinition<?> objectDefinition,
            T... propertyValues) {
        return PropertyDeltaImpl.createModificationDeleteProperty(propertyPath, objectDefinition, propertyValues);
    }

    @Override
    public Collection<? extends ItemDelta<?, ?>> createModificationReplacePropertyCollection(QName propertyName,
            PrismObjectDefinition<?> objectDefinition, Object... propertyValues) {
        return PropertyDeltaImpl.createModificationReplacePropertyCollection(propertyName, objectDefinition, propertyValues);
    }

}
