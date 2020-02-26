/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.annotations.VisibleForTesting;

/**
 * Item is a common abstraction of Property, Reference and Container.
 * <p>
 * This is supposed to be a superclass for all items. Items are things
 * that can appear in containers, which generally means only a property, reference
 * and container itself. Therefore this is in fact superclass for those
 * three definitions.
 *
 * @author Radovan Semancik
 */
public interface Item<V extends PrismValue, D extends ItemDefinition> extends Itemable, DebugDumpable, Visitable, PathVisitable,
        ParentVisitable, Serializable, Revivable, Freezable, PrismContextSensitive {

    /**
     * Returns applicable definition.
     * <p>
     * May return null if no definition is applicable or the definition is not known.
     *
     * @return applicable definition
     */
    D getDefinition();

    /**
     * Returns true if this item and all contained items have proper definition.
     */
    default boolean hasCompleteDefinition() {
        return getDefinition() != null;
    }

    /**
     * Returns the name of the item.
     * <p>
     * The name is a QName. It uniquely defines an item.
     * <p>
     * The name may be null, but such an item will not work.
     * <p>
     * The name is the QName of XML element in the XML representation.
     *
     * @return item name
     *
     * TODO consider making element name obligatory
     */
    @Override
    ItemName getElementName();

    /**
     * Sets the name of the item.
     * <p>
     * The name is a QName. It uniquely defines an item.
     * <p>
     * The name may be null, but such an item will not work.
     * <p>
     * The name is the QName of XML element in the XML representation.
     *
     * @param elementName the name to set
     *
     * TODO consider removing this method
     */
    @VisibleForTesting
    void setElementName(QName elementName);

    /**
     * Sets applicable item definition.
     *
     * @param definition the definition to set
     *
     * TODO consider removing this method
     */
    @VisibleForTesting
    void setDefinition(@Nullable D definition);

    /**
     * Returns a display name for the item.
     * <p>
     * Returns null if the display name cannot be determined.
     * <p>
     * The display name is fetched from the definition. If no definition
     * (schema) is available, the display name will not be returned.
     *
     * @return display name for the item
     */
    default String getDisplayName() {
        return getDefinition() == null ? null : getDefinition().getDisplayName();
    }

    /**
     * Returns help message defined for the item.
     * <p>
     * Returns null if the help message cannot be determined.
     * <p>
     * The help message is fetched from the definition. If no definition
     * (schema) is available, the help message will not be returned.
     *
     * @return help message for the item
     */
    default String getHelp() {
        return getDefinition() == null ? null : getDefinition().getHelp();
    }

    /**
     * Flag that indicates incomplete item. If set to true then the
     * values in this item are not complete. If this flag is true
     * then it can be assumed that the object that this item represents
     * has at least one value. This is a method how to indicate that
     * the item really has some values, but are not here. This may
     * be used for variety of purposes. It may indicate that the
     * account has a password, but the password value is not revealed.
     * This may indicate that a user has a photo, but the photo was not
     * requested and therefore is not returned. This may be used to indicate
     * that only part of the attribute values were returned from the search.
     * And so on.
     */
    boolean isIncomplete();

    /**
     * Flags the item as incomplete.
     * @see Item#isIncomplete()
     *
     * FIXME: Should be package-visible to implementation
     *
     * @param incomplete The new value
     */
    void setIncomplete(boolean incomplete);

    /**
     * Returns the parent of this item (if exists). Currently this has to be a PrismContainerValue.
     *
     * @return The parent if exists
     */
    @Nullable
    PrismContainerValue<?> getParent();

    /**
     * Sets the parent of this item.
     *
     * @param parentValue The new parent
     */
    void setParent(@Nullable PrismContainerValue<?> parentValue);

    /**
     * Returns the path of this item (sequence of names from the "root" container or similar object to this item).
     * Note that if the containing object is a delta (usually a container delta), then the path
     *
     * @return the path
     */
    @NotNull
    ItemPath getPath();

    /**
     * Returns the "user data", a map that allows attaching arbitrary named data to this item.
     * @return the user data map
     */
    @NotNull
    Map<String, Object> getUserData();

    /**
     * Returns the user data for the given key (name).
     */
    <T> T getUserData(String key);

    /**
     * Sets the user data for the given key (name).
     */
    void setUserData(String key, Object value);

    /**
     * Returns the values for this item. Although the ordering of this values is not important, and each value should
     * be present at most once, we currently return them as a list instead of a set. TODO reconsider this
     */
    @NotNull
    List<V> getValues();

    /**
     * Returns the number of values for this item.
     */
    int size();

    /**
     * Returns any of the values. Usually called when we are quite confident that there is only a single value;
     * or we don't care which of the values we get. Does not create values if there are none.
     */
    default V getAnyValue() {
        return !getValues().isEmpty() ? getValues().get(0) : null;
    }

    /**
     * Returns the value, if there is only one. Throws exception if there are more values.
     * If there is no value, this method either:
     * - returns null (for properties)
     * - throws an exception (for items that can hold multiple values)
     * - creates an empty value (for containers and references).
     *
     * TODO think again whether getOrCreateValue would not be better
     */
    V getValue();

    /**
     * Returns the "real value" (content) of this item:
     *  - value contained in PrismPropertyValue
     *  - Referencable in PrismReferenceValue
     *  - Containerable in PrismContainerValue
     *  - Objectable in PrismObjectValue
     *
     * Note that the real value can contain operational items.
     *
     * It can also contain container IDs (although they are not considered to be part of the real value).
     *
     * It does not contain information about item element name nor other metadata like origin, definition, etc.
     * (Although e.g. Containerable can be converted back into PrismContainerValue that can be used to retrieve this information.)
     */
    @Nullable
    Object getRealValue();

    /**
     * Type override, also for compatibility.
     */
    <X> X getRealValue(Class<X> type);

    /**
     * Type override, also for compatibility.
     */
    <X> X[] getRealValuesArray(Class<X> type);

    /**
     * Returns (potentially empty) collection of "real values".
     * @see Item#getRealValue().
     */
    @NotNull
    Collection<?> getRealValues();

    /**
     * Returns true if the item contains 0 or 1 values and (by definition) is not multivalued.
     */
    boolean isSingleValue();

    //region Add and remove

    /**
     * Adds a given value, unless an equivalent one is already there (if checkUniqueness is true).
     *
     * @return true if this item changed as a result of the call (i.e. if the value was really added)
     *
     * Note that even if checkUniqueness is false we check the cardinality of the item according to its definition,
     * i.e. we do not allow single-valued item to contain more than one value.
     */
    boolean add(@NotNull V newValue, boolean checkUniqueness) throws SchemaException;

    /**
     * Adds a given value, unless an equivalent one is already there. It is the same as calling add with checkUniqueness=true.
     *
     * @return true if this item changed as a result of the call (i.e. if the value was really added)
     */
    default boolean add(@NotNull V newValue) throws SchemaException {
        return add(newValue, true);
    }

    /**
     * Adds a given value, unless an equivalent one is already there. It is the same as calling add with checkUniqueness=true.
     * Uses given strategy for equivalence testing.
     *
     * @return true if this item changed as a result of the call (i.e. if the value was really added)
     */
    boolean add(@NotNull V newValue, @NotNull EquivalenceStrategy equivalenceStrategy) throws SchemaException;

    /**
     * Adds given values, with the same semantics as repeated add(..) calls.
     *
     * @return true if this item changed as a result of the call (i.e. if at least one value was really added)
     */
    boolean addAll(Collection<V> newValues) throws SchemaException;

    /**
     * Adds given values, with the same semantics as repeated add(..) calls.
     * For equality testing uses given strategy.
     *
     * @return true if this item changed as a result of the call (i.e. if at least one value was really added)
     */
    boolean addAll(Collection<V> newValues, EquivalenceStrategy strategy) throws SchemaException;

    /**
     * Adds given values, with the same semantics as repeated add(..) calls.
     * For equality testing uses given strategy.
     *
     * @return true if this item changed as a result of the call (i.e. if at least one value was really added)
     */
    boolean addAll(Collection<V> newValues, boolean checkUniqueness, EquivalenceStrategy strategy) throws SchemaException;

    /**
     * Removes given value from the item.
     *
     * "Given value" currently means any value that is considered equivalent via REAL_VALUE equivalence strategy
     * or a value that is considered "the same" via "representsSameValue(.., lax=false)" method.
     *
     * @return true if this item changed as a result of the call (i.e. if at least one value was really removed)
     *
     * Note that there can be more than one values removed.
     */
    boolean remove(V value);

    /**
     * Removes values equivalent to given value from the item; under specified equivalence strategy
     * OR when values represent the same value via "representsSameValue(.., lax=false)" method.
     *
     * @return true if this item changed as a result of the call (i.e. if at least one value was really removed)
     */
    boolean remove(V value, @NotNull EquivalenceStrategy strategy);

    /**
     * Removes all given values from the item. It is basically a shortcut for repeated remove(value) call.
     *
     * @return true if this item changed as a result of the call (i.e. if at least one value was really removed)
     */
    boolean removeAll(Collection<V> values);

    /**
     * Removes all values from the item.
     */
    void clear();

    /**
     * Replaces all values of the item by given values.
     */
    void replaceAll(Collection<V> newValues, EquivalenceStrategy strategy) throws SchemaException;

    /**
     * Replaces all values of the item by given value.
     */
    void replace(V newValue) throws SchemaException;

    //endregion

    //region Finding and comparing values

    /**
     * Compares this item to the specified object under NOT_LITERAL strategy (if no other is pre-set).
     */
    @Override
    boolean equals(Object obj);

    /**
     * Compares this item to the specified object under given strategy.
     */
    boolean equals(Object obj, @NotNull EquivalenceStrategy equivalenceStrategy);

    /**
     * Compares this item to the specified object under given strategy.
     */
    boolean equals(Object obj, @NotNull ParameterizedEquivalenceStrategy equivalenceStrategy);

    /**
     * Computes hash code to be used under NOT_LITERAL equivalence strategy.
     */
    @Override
    int hashCode();

    /**
     * Computes hash code to be used under given equivalence strategy.
     */
    int hashCode(@NotNull EquivalenceStrategy equivalenceStrategy);

    /**
     * Computes hash code to be used under given equivalence strategy.
     */
    int hashCode(@NotNull ParameterizedEquivalenceStrategy equivalenceStrategy);

    /**
     * @return true if the item contains a given value (by default using NOT_LITERAL strategy)
     *
     * Note that the "sameness" (ID-only value matching) is NOT considered here.
     */
    boolean contains(V value);

    /**
     * @return true if the item contains a given value under specified equivalence strategy
     *
     * Note that the "sameness" (ID-only value matching) is NOT considered here.
     */
    boolean contains(V value, @NotNull EquivalenceStrategy strategy);

    /**
     * @return true if the item contains a given value using comparator (if not null) or
     * under specified equivalence strategy (if comparator is null).
     *
     * Note that the "sameness" (ID-only value matching) is NOT considered here.
     */
    boolean contains(V value, @Nullable EquivalenceStrategy strategy, @Nullable Comparator<V> comparator);

    /**
     * @return true if the item contains an equivalent value (the same as {@link #containsEquivalentValue(PrismValue, Comparator)}
     * with comparator being null)
     */
    boolean containsEquivalentValue(V value);

    /**
     * @return true if the item contains an equivalent value
     *
     * Item value is considered to be equivalent to the given value if:
     * 1) given value is ID-only container value and item value has the same ID, or
     * 2) comparator is not null and it gives "equals" (0) result when comparing these values, or
     * 3) comparator is null and values match under IGNORE_METADATA_CONSIDER_DIFFERENT_IDS strategy
     */
    boolean containsEquivalentValue(V value, @Nullable Comparator<V> comparator);

    /**
     * @return a value of this item that is equivalent to the given one under given equivalence strategy
     * (or null if no such value exists)
     */
    V findValue(V value, @NotNull EquivalenceStrategy strategy);

    /**
     * @return true if the values of this item match the "matchValues" collection, under given comparator.
     *
     * If comparator is null the default equals(..) comparison is used.
     */
    boolean valuesEqual(Collection<V> matchValues, @Nullable Comparator<V> comparator);

    /**
     * Computes a difference (delta) with the specified item using IGNORE_METADATA_CONSIDER_DIFFERENT_IDS equivalence strategy.
     */
    ItemDelta<V,D> diff(Item<V,D> other);

    /**
     * Computes a difference (delta) with the specified item using given equivalence strategy.
     * Note this method cannot accept general EquivalenceStrategy here; it needs the parameterized strategy.
     */
    ItemDelta<V,D> diff(Item<V,D> other, @NotNull ParameterizedEquivalenceStrategy strategy);

    //endregion


    Collection<V> getClonedValues();

    void normalize();

    /**
     * Merge all the values of other item to this item.
     */
    void merge(Item<V,D> otherItem) throws SchemaException;

    /**
     * Returns object (Item or PrismValue) pointed to by the given path.
     */
    Object find(ItemPath path);

    <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    /**
     * Creates specific subclass of ItemDelta appropriate for type of item that this definition
     * represents (e.g. PropertyDelta, ContainerDelta, ...)
     */
    ItemDelta<V,D> createDelta();

    ItemDelta<V,D> createDelta(ItemPath path);

    /**
     * Accepts a visitor that visits each item/value on the way to the structure root.
     */
    void acceptParentVisitor(@NotNull Visitor visitor);

    /**
     * Re-apply PolyString (and possible other) normalizations to the object.
     */
    void recomputeAllValues();

    void filterValues(Function<V, Boolean> function);

    void applyDefinition(D definition) throws SchemaException;

    void applyDefinition(D definition, boolean force) throws SchemaException;

    void revive(PrismContext prismContext) throws SchemaException;

    /**
     * Literal clone.
     */
    Item clone();

    Item createImmutableClone();

    /**
     * Complex clone with different cloning strategies.
     * @see CloneStrategy
     */
    Item cloneComplex(CloneStrategy strategy);

    static <T extends Item<?,?>> Collection<T> cloneCollection(Collection<T> items) {
        Collection<T> clones = new ArrayList<>(items.size());
        for (T item: items) {
            //noinspection unchecked
            clones.add((T)((Item<?,?>)item).clone());
        }
        return clones;
    }

    /**
     * Sets all parents to null. This is good if the items are to be "transplanted" into a
     * different Containerable.
     */
    @SuppressWarnings("unused")
    static <T extends Item> Collection<T> resetParentCollection(Collection<T> items) {
        for (T item: items) {
            //noinspection unchecked
            item.setParent(null);
        }
        return items;
    }

    void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope);

    void checkConsistence(boolean requireDefinitions, boolean prohibitRaw);

    void checkConsistence(boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope);

    void checkConsistence();

    void checkConsistence(ConsistencyCheckScope scope);

    void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope);

    void assertDefinitions() throws SchemaException;

    void assertDefinitions(String sourceDescription) throws SchemaException;

    void assertDefinitions(boolean tolerateRawValues, String sourceDescription) throws SchemaException;

    /**
     * Returns true is all the values are raw.
     */
    boolean isRaw();

    /**
     * Returns true is at least one of the values is raw.
     */
    boolean hasRaw();

    default boolean isEmpty() {
        return hasNoValues();
    }

    default boolean hasNoValues() {
        return getValues().isEmpty();
    }


    @SuppressWarnings("unused")
    static boolean hasNoValues(Item<?, ?> item) {
        return item == null || item.getValues().isEmpty();
    }


    /**
     * Returns true if this item is metadata item that should be ignored
     * for metadata-insensitive comparisons and hashCode functions.
     */
    default boolean isOperational() {
        D def = getDefinition();
        if (def != null) {
            return def.isOperational();
        } else {
            return false;
        }
    }

    @NotNull
    static <V extends PrismValue> Collection<V> getValues(Item<V, ?> item) {
        return item != null ? item.getValues() : Collections.emptySet();
    }

    // Path may contain ambiguous segments (e.g. assignment/targetRef when there are more assignments)
    // Note that the path can contain name segments only (at least for now)
    @NotNull
    Collection<PrismValue> getAllValues(ItemPath path);

    @NotNull
    static Collection<PrismValue> getAllValues(Item<?, ?> item, ItemPath path) {
        return item != null ? item.getAllValues(path) : Collections.emptySet();
    }

    // Primarily for testing
    @VisibleForTesting
    PrismContext getPrismContextLocal();

    void setPrismContext(PrismContext prismContext);        // todo remove

    Long getHighestId();
}
