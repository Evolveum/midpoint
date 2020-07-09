/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;

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
public interface PropertyDelta<T> extends ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> {

    PrismPropertyDefinition<T> getPropertyDefinition();

    void setPropertyDefinition(PrismPropertyDefinition<T> propertyDefinition);

    @Override
    void setDefinition(PrismPropertyDefinition<T> definition);

    @Override
    void applyDefinition(PrismPropertyDefinition<T> definition) throws SchemaException;

    @Override
    Class<PrismProperty> getItemClass();

    /**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
    <T> Collection<PrismPropertyValue<T>> getValues(Class<T> type);

    T getAnyRealValue();

    <P extends PrismProperty> P instantiateEmptyProperty();

    boolean isApplicableToType(Item item);

    @Override
    PropertyDelta<T> clone();

    boolean isRealValueToAdd(PrismPropertyValue<?> value);

    boolean isRealValueToDelete(PrismPropertyValue<?> value);

    /**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     */
    PrismProperty<T> getPropertyNewMatchingPath() throws SchemaException;

    /**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     */
    PrismProperty<T> getPropertyNewMatchingPath(PrismProperty<T> propertyOld) throws SchemaException;

    /**
     * Returns the narrowed delta that will have the same effect on the object as the current one.
     * Expects that the delta executor (can be e.g. the connector!) considers values equivalent if
     * they are equal according to the specified comparator.
     */
    PropertyDelta<T> narrow(PrismObject<? extends Objectable> object,
            @NotNull Comparator<PrismPropertyValue<T>> plusComparator,
            @NotNull Comparator<PrismPropertyValue<T>> minusComparator,
            boolean assumeMissingItems);

//    boolean isRedundant(PrismObject<? extends Objectable> object, ParameterizedEquivalenceStrategy strategy, MatchingRule<T> matchingRule, boolean assumeMissingItems);

    // convenience method
    void setRealValuesToReplace(T... newValues);

    void addRealValuesToAdd(T... newValues);

    void addRealValuesToDelete(T... newValues);

    void addRealValuesToAdd(Collection<T> newValues);

    void addRealValuesToDelete(Collection<T> values);
}
