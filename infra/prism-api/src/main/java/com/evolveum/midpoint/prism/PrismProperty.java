/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Property is a specific characteristic of an object. It may be considered
 * object "attribute" or "field". For example User has fullName property that
 * contains string value of user's full name.
 * <p>
 * Properties may be single-valued or multi-valued
 * <p>
 * Properties may contain primitive types or complex types (defined by XSD
 * schema)
 * <p>
 * Property values are unordered, implementation may change the order of values
 * <p>
 * Duplicate values of properties should be silently removed by implementations,
 * but clients must be able tolerate presence of duplicate values.
 * <p>
 * Operations that modify the objects work with the granularity of properties.
 * They add/remove/replace the values of properties, but do not "see" inside the
 * property.
 * <p>
 * Property is mutable.
 *
 * @author Radovan Semancik
 */
public interface PrismProperty<T> extends Item<PrismPropertyValue<T>,PrismPropertyDefinition<T>> {

    /**
     * Type override, also for compatibility.
     */
    <X> List<PrismPropertyValue<X>> getValues(Class<X> type);

    @NotNull
    @Override
    Collection<T> getRealValues();

    /**
     * Type override, also for compatibility.
     */

    <X> Collection<X> getRealValues(Class<X> type);

    T getAnyRealValue();

    @Override
    T getRealValue();

    /**
     * Type override, also for compatibility.
     */
    <X> X getRealValue(Class<X> type);

    /**
     * Type override, also for compatibility.
     */
    <X> X[] getRealValuesArray(Class<X> type);

    /**
     * Type override, also for compatibility.
     */
    <X> PrismPropertyValue<X> getValue(Class<X> type);

    /**
     * Means as a short-hand for setting just a value for single-valued attributes.
     * Will remove all existing values.
     */
    void setValue(PrismPropertyValue<T> value);

    void setRealValue(T realValue);

    void setRealValues(T... realValues);

    void addValues(Collection<PrismPropertyValue<T>> pValuesToAdd);

    void addValue(PrismPropertyValue<T> pValueToAdd);

    void addRealValue(T valueToAdd);

    // TODO Or should we add boolean parameter here?
    void addRealValueSkipUniquenessCheck(T valueToAdd);

    default void addRealValues(T... valuesToAdd) {
        for (T valueToAdd : valuesToAdd) {
            addRealValue(valueToAdd);
        }
    }

    boolean deleteValues(Collection<PrismPropertyValue<T>> pValuesToDelete);

    boolean deleteValue(PrismPropertyValue<T> pValueToDelete);

    void replaceValues(Collection<PrismPropertyValue<T>> valuesToReplace);

    boolean hasRealValue(PrismPropertyValue<T> value);

    Class<T> getValueClass();

    @Override
    PropertyDelta<T> createDelta();

    @Override
    PropertyDelta<T> createDelta(ItemPath path);

    @Override
    <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    PropertyDelta<T> diff(PrismProperty<T> other);

    PropertyDelta<T> diff(PrismProperty<T> other, ParameterizedEquivalenceStrategy strategy);

    @Override
    PrismProperty<T> clone();

    @Override
    PrismProperty<T> createImmutableClone();

    @Override
    PrismProperty<T> cloneComplex(CloneStrategy strategy);

    String toHumanReadableString();

    static <T> T getRealValue(PrismProperty<T> property) {
        return property != null ? property.getRealValue() : null;
    }
}
