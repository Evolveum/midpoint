/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

/**
 * @author lazyman
 */
public enum RItemKind {

    PROPERTY(PrismProperty.class, PrismPropertyValue.class, PropertyDelta.class, PrismPropertyDefinition.class),
    CONTAINER(PrismContainer.class, PrismContainerValue.class, ContainerDelta.class, PrismContainerDefinition.class),
    OBJECT(PrismObject.class, PrismContainerValue.class, null, PrismObjectDefinition.class),
    REFERENCE(PrismReference.class, PrismReferenceValue.class, ReferenceDelta.class, PrismReferenceDefinition.class);

    private Class<? extends Item> itemClass;
    private Class<? extends PrismValue> valueClass;
    private Class<? extends ItemDelta> deltaClass;
    private Class<? extends ItemDefinition> definitionClass;

    RItemKind(Class<? extends Item> itemClass, Class<? extends PrismValue> valueClass, Class<? extends ItemDelta> deltaClass,
            Class<? extends ItemDefinition> definitionClass) {
        this.itemClass = itemClass;
        this.valueClass = valueClass;
        this.deltaClass = deltaClass;
        this.definitionClass = definitionClass;
    }

    public Class<? extends PrismValue> getValueClass() {
        return valueClass;
    }

    public Class<? extends Item> getItemClass() {
        return itemClass;
    }

    public Class<? extends ItemDelta> getDeltaClass() {
        return deltaClass;
    }

    public Class<? extends ItemDefinition> getDefinitionClass() {
        return definitionClass;
    }

    public static RItemKind getTypeFromItemClass(Class<? extends Item> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");
        for (RItemKind value : RItemKind.values()) {
            if (value.getItemClass().isAssignableFrom(clazz)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown enum value type for '" + clazz.getName() + "'.");
    }

    public static RItemKind getTypeFromItemDefinitionClass(@NotNull Class<? extends ItemDefinition> clazz) {
        for (RItemKind value : RItemKind.values()) {
            if (value.getDefinitionClass().isAssignableFrom(clazz)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown enum value for definition class '" + clazz.getName() + "'.");
    }

    public static RItemKind getTypeFromValueClass(Class<? extends PrismValue> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");
        for (RItemKind value : RItemKind.values()) {
            if (value.getValueClass().isAssignableFrom(clazz)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown enum value type for '" + clazz.getName() + "'.");
    }

    public static RItemKind getTypeFromDeltaClass(Class<? extends ItemDelta> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");
        for (RItemKind value : RItemKind.values()) {
            if (value.getDeltaClass() != null && value.getDeltaClass().isAssignableFrom(clazz)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown enum value type for '" + clazz.getName() + "'.");
    }
}
