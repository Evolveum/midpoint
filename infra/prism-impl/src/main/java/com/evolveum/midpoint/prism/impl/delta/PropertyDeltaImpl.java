/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.delta;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Relative difference (delta) of a property values.
 * <p>
 * This class describes what values are to be added, removed or replaced in the property.
 * The delta can be either add+delete or replace, but not both. It either describes what
 * values to add and delete from the property (add+delete) or what is the new set of values
 * (replace). Add+delete deltas can be merged without a loss. There are ideal for multi-value
 * properties. If replace deltas are merged, only the last value will be present. These are
 * better suited for single-value properties.
 *
 * @author Radovan Semancik
 * @see ObjectDelta
 */
public class PropertyDeltaImpl<T> extends ItemDeltaImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> implements
        PropertyDelta<T> {

    public PropertyDeltaImpl(PrismPropertyDefinition<T> propertyDefinition, PrismContext prismContext) {
        super(propertyDefinition, prismContext);
    }

    public PropertyDeltaImpl(ItemPath itemPath, QName name, PrismPropertyDefinition<T> propertyDefinition, PrismContext prismContext) {
        super(itemPath, name, propertyDefinition, prismContext);
    }

    public PropertyDeltaImpl(ItemPath propertyPath, PrismPropertyDefinition<T> propertyDefinition, PrismContext prismContext) {
        super(propertyPath, propertyDefinition, prismContext);
    }

    public PrismPropertyDefinition<T> getPropertyDefinition() {
        return super.getDefinition();
    }

    public void setPropertyDefinition(PrismPropertyDefinition<T> propertyDefinition) {
        super.setDefinition(propertyDefinition);
    }

    @Override
    public Class<PrismProperty> getItemClass() {
        return PrismProperty.class;
    }

    /**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
    public <V> Collection<PrismPropertyValue<V>> getValues(Class<V> type) {
        if (valuesToReplace != null) {
            return (Collection) valuesToReplace;
        }
        return (Collection) MiscUtil.union(valuesToAdd, valuesToDelete);
    }

    public T getAnyRealValue() {
        PrismPropertyValue<T> anyValue = getAnyValue();
        return anyValue != null ? anyValue.getValue() : null;
    }

    public <P extends PrismProperty> P instantiateEmptyProperty() {
        PrismPropertyDefinition propertyDefinition = getPropertyDefinition();
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("Cannot instantiate property " + getElementName() + " from delta " + this + ": no definition");
        }
        return (P) propertyDefinition.instantiate(getElementName());
    }

    @Override
    public boolean isApplicableToType(Item item) {
        return item instanceof PrismProperty;
    }

    @Override
    public PropertyDeltaImpl<T> clone() {
        PropertyDeltaImpl<T> clone = new PropertyDeltaImpl<>(getElementName(), getPropertyDefinition(), getPrismContext());
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PropertyDeltaImpl<T> clone) {
        super.copyValues(clone);
    }

    // TODO: the same as createModificationReplaceProperty?
    // btw, why was here 'PrismObjectDefinition'?
    public static <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
            QName propertyName, T... realValues) {
        PrismPropertyDefinition<T> propertyDefinition = containerDefinition.findPropertyDefinition(ItemName.fromQName(propertyName));
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No definition for " + propertyName + " in " + containerDefinition);
        }
        PropertyDelta<T> delta = new PropertyDeltaImpl<>(ItemName.fromQName(propertyName), propertyDefinition, containerDefinition.getPrismContext());            // hoping the prismContext is there
        Collection<PrismPropertyValue<T>> valuesToReplace = delta.getValuesToReplace();
        if (valuesToReplace == null) {
            valuesToReplace = new ArrayList<>(realValues.length);
        }
        for (T realVal : realValues) {
            valuesToReplace.add(new PrismPropertyValueImpl<>(realVal));
        }
        delta.setValuesToReplace(valuesToReplace);
        return delta;
    }

    public static <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
            QName propertyName, PrismPropertyValue<T>... pValues) {
        PrismPropertyDefinition<T> propertyDefinition = containerDefinition.findPropertyDefinition(ItemName.fromQName(propertyName));
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No definition for " + propertyName + " in " + containerDefinition);
        }
        PropertyDelta<T> delta = new PropertyDeltaImpl<>(ItemName.fromQName(propertyName), propertyDefinition, containerDefinition.getPrismContext());       // hoping the prismContext is there
        Collection<PrismPropertyValue<T>> valuesToReplace = new ArrayList<>(pValues.length);
        Collections.addAll(valuesToReplace, pValues);
        delta.setValuesToReplace(valuesToReplace);
        return delta;
    }

    public static <O extends Objectable> PropertyDelta createAddDelta(PrismContainerDefinition<O> containerDefinition,
            QName propertyName, Object... realValues) {
        PrismPropertyDefinition propertyDefinition = containerDefinition.findPropertyDefinition(ItemName.fromQName(propertyName));
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No definition for " + propertyName + " in " + containerDefinition);
        }
        PropertyDelta delta = new PropertyDeltaImpl(ItemName.fromQName(propertyName), propertyDefinition, containerDefinition.getPrismContext());       // hoping the prismContext is there
        for (Object realVal : realValues) {
            delta.addValueToAdd(new PrismPropertyValueImpl<>(realVal));
        }
        return delta;
    }

    public static <O extends Objectable> PropertyDelta createDeleteDelta(PrismContainerDefinition<O> containerDefinition,
            QName propertyName, Object... realValues) {
        PrismPropertyDefinition propertyDefinition = containerDefinition.findPropertyDefinition(ItemName.fromQName(propertyName));
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No definition for " + propertyName + " in " + containerDefinition);
        }
        PropertyDelta delta = new PropertyDeltaImpl(ItemName.fromQName(propertyName), propertyDefinition, containerDefinition.getPrismContext());       // hoping the prismContext is there
        for (Object realVal : realValues) {
            delta.addValueToDelete(new PrismPropertyValueImpl<>(realVal));
        }
        return delta;
    }

    /**
     * Create delta that deletes all values of the specified property.
     */
    public static <O extends Objectable> PropertyDelta createReplaceEmptyDelta(PrismObjectDefinition<O> objectDefinition,
            ItemPath propertyPath) {
        PrismPropertyDefinition propertyDefinition = objectDefinition.findPropertyDefinition(propertyPath);
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No definition for " + propertyPath + " in " + objectDefinition);
        }
        PropertyDelta delta = new PropertyDeltaImpl(propertyPath, propertyDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        delta.setValuesToReplace(new ArrayList<PrismPropertyValue>());
        return delta;
    }

    public static <O extends Objectable, T> PropertyDelta<T> createReplaceDeltaOrEmptyDelta(PrismObjectDefinition<O> objectDefinition,
            QName propertyName, T realValue) {

        if (realValue != null) {
            return createReplaceDelta(objectDefinition, propertyName, realValue);
        } else {
            return createReplaceEmptyDelta(objectDefinition, ItemName.fromQName(propertyName));
        }
    }

    public boolean isRealValueToAdd(PrismPropertyValue<?> value) {
        if (valuesToAdd == null) {
            return false;
        }

        for (PrismPropertyValue valueToAdd : valuesToAdd) {
            if (valueToAdd.equals(value, EquivalenceStrategy.REAL_VALUE)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRealValueToDelete(PrismPropertyValue<?> value) {
        if (valuesToDelete == null) {
            return false;
        }

        for (PrismPropertyValue valueToAdd : valuesToDelete) {
            if (valueToAdd.equals(value, EquivalenceStrategy.REAL_VALUE)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     */
    public PrismProperty<T> getPropertyNewMatchingPath() throws SchemaException {
        return (PrismProperty<T>) super.getItemNewMatchingPath(null);
    }

    /**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     */
    public PrismProperty<T> getPropertyNewMatchingPath(PrismProperty<T> propertyOld) throws SchemaException {
        return (PrismProperty<T>) super.getItemNewMatchingPath(propertyOld);
    }

    public static <O extends Objectable, T> PropertyDelta<T> createDelta(ItemPath propertyPath, PrismObjectDefinition<O> objectDefinition) {
        PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
        return new PropertyDeltaImpl<T>(propertyPath, propDef, objectDefinition.getPrismContext());        // hoping the prismContext is there
    }

    public static <O extends Objectable, T> PropertyDelta<T> createDelta(ItemPath propertyPath, Class<O> compileTimeClass, PrismContext prismContext) {
        PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(compileTimeClass);
        PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
        return new PropertyDeltaImpl<T>(propertyPath, propDef, prismContext);
    }

    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method.
     */
    public static <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
            T... propertyValues) {
        PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
        PropertyDelta<T> propertyDelta = new PropertyDeltaImpl<T>(propertyPath, propDef, objectDefinition.getPrismContext());              // hoping the prismContext is there
        Collection<PrismPropertyValue<T>> pValues = new ArrayList<>(propertyValues.length);
        for (T val : propertyValues) {
            pValues.add(new PrismPropertyValueImpl<>(val));
        }
        propertyDelta.setValuesToReplace(pValues);
        return propertyDelta;
    }

    public static <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
            Collection<T> propertyValues) {
        PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
        PropertyDelta<T> propertyDelta = new PropertyDeltaImpl<T>(propertyPath, propDef, objectDefinition.getPrismContext());              // hoping the prismContext is there
        Collection<PrismPropertyValue<T>> pValues = new ArrayList<>(propertyValues.size());
        for (T val : propertyValues) {
            pValues.add(new PrismPropertyValueImpl<>(val));
        }
        propertyDelta.setValuesToReplace(pValues);
        return propertyDelta;
    }

    public static <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath path, PrismPropertyDefinition propertyDefinition,
            T... propertyValues) {
        PropertyDelta<T> propertyDelta = new PropertyDeltaImpl<T>(path, propertyDefinition, propertyDefinition.getPrismContext());             // hoping the prismContext is there
        Collection<PrismPropertyValue<T>> pValues = new ArrayList<>(propertyValues.length);
        for (T val : propertyValues) {
            pValues.add(new PrismPropertyValueImpl<>(val));
        }
        propertyDelta.setValuesToReplace(pValues);
        return propertyDelta;
    }

    public static <T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition,
            T... propertyValues) {
        PropertyDelta<T> propertyDelta = new PropertyDeltaImpl<T>(propertyPath, propertyDefinition, propertyDefinition.getPrismContext());         // hoping the prismContext is there
        Collection<PrismPropertyValue<T>> pValues = new ArrayList<>(propertyValues.length);
        for (T val : propertyValues) {
            pValues.add(new PrismPropertyValueImpl<>(val));
        }
        propertyDelta.addValuesToAdd(pValues);
        return propertyDelta;
    }

    public static <T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
            T... propertyValues) {
        PrismPropertyDefinition<T> propertyDefinition = objectDefinition.findPropertyDefinition(propertyPath);
        return createModificationAddProperty(propertyPath, propertyDefinition, propertyValues);
    }

    public static <T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition,
            T... propertyValues) {
        PrismContext prismContext = propertyDefinition.getPrismContext();
        PropertyDelta<T> propertyDelta = new PropertyDeltaImpl<T>(prismContext.toUniformPath(propertyPath), propertyDefinition, prismContext);             // hoping the prismContext is there
        Collection<PrismPropertyValue<T>> pValues = new ArrayList<>(propertyValues.length);
        for (T val : propertyValues) {
            pValues.add(new PrismPropertyValueImpl<>(val));
        }
        propertyDelta.addValuesToDelete(pValues);
        return propertyDelta;
    }

    public static <T> PropertyDelta<T> createModificationDeleteProperty(
            ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition, T... propertyValues) {
        PrismPropertyDefinition<T> propertyDefinition = objectDefinition.findPropertyDefinition(propertyPath);
        return createModificationDeleteProperty(propertyPath, propertyDefinition, propertyValues);
    }

    /**
     * Convenience method for quick creation of object deltas that replace a single object property.
     * This is used quite often to justify a separate method.
     */
    public static Collection<? extends ItemDelta<?, ?>> createModificationReplacePropertyCollection(
            QName propertyName, PrismObjectDefinition<?> objectDefinition, Object... propertyValues) {
        return List.of(
                createModificationReplaceProperty(
                        ItemName.fromQName(propertyName), objectDefinition, propertyValues));
    }

    // convenience method
    @SafeVarargs
    public final void setRealValuesToReplace(T... newValues) {
        super.setValuesToReplace(PrismValueCollectionsUtil.wrap(getPrismContext(), newValues));
    }

    @SafeVarargs
    public final void addRealValuesToAdd(T... newValues) {
        super.addValuesToAdd(PrismValueCollectionsUtil.wrap(getPrismContext(), newValues));
    }

    @SafeVarargs
    public final void addRealValuesToDelete(T... newValues) {
        super.addValuesToDelete(PrismValueCollectionsUtil.wrap(getPrismContext(), newValues));
    }

    public final void addRealValuesToAdd(Collection<T> newValues) {
        super.addValuesToAdd(PrismValueCollectionsUtil.wrap(getPrismContext(), newValues));
    }

    public final void addRealValuesToDelete(Collection<T> values) {
        super.addValuesToDelete(PrismValueCollectionsUtil.wrap(getPrismContext(), values));
    }

    @Override
    public PropertyDelta<T> narrow(@NotNull PrismObject<? extends Objectable> object,
            @NotNull Comparator<PrismPropertyValue<T>> plusComparator,
            @NotNull Comparator<PrismPropertyValue<T>> minusComparator,
            boolean assumeMissingItems) {
        return (PropertyDelta<T>) super.narrow(object, plusComparator, minusComparator, assumeMissingItems);
    }
}
