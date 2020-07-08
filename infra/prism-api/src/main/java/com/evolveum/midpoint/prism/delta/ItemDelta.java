/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Foreachable;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Radovan Semancik
 *
 */
public interface ItemDelta<V extends PrismValue,D extends ItemDefinition> extends Itemable, DebugDumpable, Visitable, PathVisitable, Foreachable<V>, Serializable, Freezable, PrismContextSensitive {

    ItemName getElementName();

    void setElementName(QName elementName);

    ItemPath getParentPath();

    void setParentPath(ItemPath parentPath);

    @NotNull
    @Override
    ItemPath getPath();

    D getDefinition();

    void setDefinition(D definition);

    @Override
    void accept(Visitor visitor);

    void accept(Visitor visitor, boolean includeOldValues);

    int size();

    // TODO think if estimated old values have to be visited as well
    @Override
    void accept(Visitor visitor, ItemPath path, boolean recursive);

    void applyDefinition(D definition) throws SchemaException;

    boolean hasCompleteDefinition();

    Class<? extends Item> getItemClass();

    Collection<V> getValuesToAdd();

    void clearValuesToAdd();

    Collection<V> getValuesToDelete();

    void clearValuesToDelete();

    Collection<V> getValuesToReplace();

    void clearValuesToReplace();

    void addValuesToAdd(Collection<V> newValues);

    void addValuesToAdd(V... newValues);

    void addValueToAdd(V newValue);

    boolean removeValueToAdd(PrismValue valueToRemove);

    boolean removeValueToDelete(PrismValue valueToRemove);

    boolean removeValueToReplace(PrismValue valueToRemove);

    void mergeValuesToAdd(Collection<V> newValues);

    void mergeValuesToAdd(V[] newValues);

    void mergeValueToAdd(V newValue);

    void addValuesToDelete(Collection<V> newValues);

    void addValuesToDelete(V... newValues);

    void addValueToDelete(V newValue);

    void mergeValuesToDelete(Collection<V> newValues);

    void mergeValuesToDelete(V[] newValues);

    void mergeValueToDelete(V newValue);

    void resetValuesToAdd();

    void resetValuesToDelete();

    void resetValuesToReplace();

    void setValuesToReplace(Collection<V> newValues);

    void setValuesToReplace(V... newValues);

    /**
     * Sets empty value to replace. This efficiently means removing all values.
     */
    void setValueToReplace();

    void setValueToReplace(V newValue);

    void addValueToReplace(V newValue);

    void mergeValuesToReplace(Collection<V> newValues);

    void mergeValuesToReplace(V[] newValues);

    void mergeValueToReplace(V newValue);

    boolean isValueToAdd(V value);

    boolean isValueToAdd(V value, boolean ignoreMetadata);

    boolean isValueToDelete(V value);

    boolean isValueToDelete(V value, boolean ignoreMetadata);

    boolean isValueToReplace(V value);

    boolean isValueToReplace(V value, boolean ignoreMetadata);

    V getAnyValue();

    boolean isEmpty();

    // TODO merge with isEmpty
    boolean isInFactEmpty();

    boolean addsAnyValue();

    void foreach(Processor<V> processor);

    /**
     * Returns estimated state of the old value before the delta is applied.
     * This information is not entirely reliable. The state might change
     * between the value is read and the delta is applied. This is property
     * is optional and even if provided it is only for for informational
     * purposes.
     *
     * If this method returns null then it should be interpreted as "I do not know".
     * In that case the delta has no information about the old values.
     * If this method returns empty collection then it should be interpreted that
     * we know that there were no values in this item before the delta was applied.
     *
     * @return estimated state of the old value before the delta is applied (may be null).
     */
    Collection<V> getEstimatedOldValues();

    void setEstimatedOldValues(Collection<V> estimatedOldValues);

    void addEstimatedOldValues(Collection<V> newValues);

    void addEstimatedOldValues(V... newValues);

    void addEstimatedOldValue(V newValue);

    void normalize();

    boolean isReplace();

    boolean isAdd();

    boolean isDelete();

    void clear();

    /**
     * Filters out all delta values that are meaningless to apply. E.g. removes all values to add that the property already has,
     * removes all values to delete that the property does not have, etc.
     * Returns null if the delta is not needed at all.
     *
     * @param assumeMissingItems Assumes that some items in the object may be missing. So replacing them by null or deleting some
     */
    default ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object, @NotNull ParameterizedEquivalenceStrategy strategy, boolean assumeMissingItems) {
        return narrow(object, strategy.prismValueComparator(), assumeMissingItems);
    }

    ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object, @NotNull Comparator<V> comparator, boolean assumeMissingItems);

    /**
     * Checks if the delta is redundant w.r.t. current state of the object.
     * I.e. if it changes the current object state.
     *
     * @param assumeMissingItems Assumes that some items in the object may be missing. So delta that replaces them by null
     */
    boolean isRedundant(PrismObject<? extends Objectable> object, ParameterizedEquivalenceStrategy strategy, boolean assumeMissingItems);

    void validate() throws SchemaException;

    void validate(String contextDescription) throws SchemaException;

    void validateValues(ItemDeltaValidator<V> validator) throws SchemaException;

    void validateValues(ItemDeltaValidator<V> validator, Collection<V> oldValues) throws SchemaException;

    void checkConsistence();

    void checkConsistence(ConsistencyCheckScope scope);

    void checkConsistence(boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope);

    /**
     * Distributes the replace values of this delta to add and delete with
     * respect to provided existing values.
     */
    void distributeReplace(Collection<V> existingValues);

    /**
     * Merge specified delta to this delta. This delta is assumed to be
     * chronologically earlier, delta provided in the parameter is chronologically later.
     *
     * TODO do we expect that the paths of "this" delta and deltaToMerge are the same?
     * From the code it seems so.
     */
    void merge(ItemDelta<V, D> deltaToMerge);

    Collection<V> getValueChanges(PlusMinusZero mode);

    /**
     * Transforms the delta to the simplest (and safest) form. E.g. it will transform add delta for
     * single-value properties to replace delta.
     */
    void simplify();

    void applyTo(PrismContainerValue containerValue) throws SchemaException;
    void applyTo(PrismContainerValue containerValue, ParameterizedEquivalenceStrategy strategy) throws SchemaException;

    void applyTo(Item item) throws SchemaException;
    void applyTo(Item item, ParameterizedEquivalenceStrategy strategy) throws SchemaException;

    /**
     * Applies delta to item were path of the delta and path of the item matches (skips path checks).
     */
    void applyToMatchingPath(Item item, ParameterizedEquivalenceStrategy strategy) throws SchemaException;

    ItemDelta<?,?> getSubDelta(ItemPath path);

    boolean isApplicableTo(Item item);

    /**
     * Returns the "new" state of the property - the state that would be after
     * the delta is applied.
     */
    Item<V,D> getItemNew() throws SchemaException;

    /**
     * Returns the "new" state of the property - the state that would be after
     * the delta is applied.
     */
    Item<V,D> getItemNew(Item<V, D> itemOld) throws SchemaException;

    Item<V,D> getItemNewMatchingPath(Item<V, D> itemOld) throws SchemaException;

    /**
     * Returns true if the other delta is a complete subset of this delta.
     * I.e. if all the statements of the other delta are already contained
     * in this delta. As a consequence it also returns true if the two
     * deltas are equal.
     */
    boolean contains(ItemDelta<V, D> other);

    /**
     * Returns true if the other delta is a complete subset of this delta.
     * I.e. if all the statements of the other delta are already contained
     * in this delta. As a consequence it also returns true if the two
     * deltas are equal.
     */
    boolean contains(ItemDelta<V, D> other, EquivalenceStrategy strategy);

    void filterValues(Function<V, Boolean> function);

    ItemDelta<V,D> clone();

    ItemDelta<V,D> cloneWithChangedParentPath(ItemPath newParentPath);

    PrismValueDeltaSetTriple<V> toDeltaSetTriple();

    PrismValueDeltaSetTriple<V> toDeltaSetTriple(Item<V, D> itemOld);

    void assertDefinitions(String sourceDescription) throws SchemaException;

    void assertDefinitions(boolean tolarateRawValues, String sourceDescription) throws SchemaException;

    boolean isRaw();

    void revive(PrismContext prismContext) throws SchemaException;

    void applyDefinition(D itemDefinition, boolean force) throws SchemaException;

    /**
     * Deltas are equivalent if they have the same result when
     * applied to an object. I.e. meta-data and other "decorations"
     * such as old values are not considered in this comparison.
     */
    boolean equivalent(ItemDelta other);

    @Override
    boolean equals(Object obj);

    @Override
    String toString();

    @Override
    String debugDump(int indent);

    void addToReplaceDelta();

    ItemDelta<V,D> createReverseDelta();

    V findValueToAddOrReplace(V value);

    /**
     * Set origin type to all values and subvalues
     */
    void setOriginTypeRecursive(OriginType originType);

    boolean isImmutable();
}
