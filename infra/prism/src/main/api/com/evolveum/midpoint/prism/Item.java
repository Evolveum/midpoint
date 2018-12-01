/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;

/**
 * Item is a common abstraction of Property and PropertyContainer.
 * <p>
 * This is supposed to be a superclass for all items. Items are things
 * that can appear in property containers, which generally means only a property
 * and property container itself. Therefore this is in fact superclass for those
 * two definitions.
 *
 * @author Radovan Semancik
 */
public interface Item<V extends PrismValue, D extends ItemDefinition> extends Itemable, DebugDumpable, Visitable, PathVisitable, Serializable, Revivable {

//    Item(QName elementName) {
//        super();
//        this.elementName = elementName;
//    }
//
//    Item(QName elementName, PrismContext prismContext) {
//        super();
//        this.elementName = elementName;
//        this.prismContext = prismContext;
//    }
//
//
//    /**
//     * The constructors should be used only occasionally (if used at all).
//     * Use the factory methods in the ResourceObjectDefintion instead.
//     */
//    Item(QName elementName, D definition, PrismContext prismContext) {
//        super();
//        this.elementName = elementName;
//        this.definition = definition;
//        this.prismContext = prismContext;
//    }

    /**
     * Returns applicable property definition.
     * <p>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
    D getDefinition();

	boolean hasCompleteDefinition();


    /**
     * Returns the name of the property.
     * <p>
     * The name is a QName. It uniquely defines a property.
     * <p>
     * The name may be null, but such a property will not work.
     * <p>
     * The name is the QName of XML element in the XML representation.
     *
     * @return property name
     */
    @Override
    ItemName getElementName();

    /**
     * Sets the name of the property.
     * <p>
     * The name is a QName. It uniquely defines a property.
     * <p>
     * The name may be null, but such a property will not work.
     * <p>
     * The name is the QName of XML element in the XML representation.
     *
     * @param elementName the name to set
     */
    void setElementName(QName elementName);     // todo remove

    /**
     * Sets applicable property definition.
     *
     * @param definition the definition to set
     */
    void setDefinition(D definition);           // todo remove

	/**
     * Returns a display name for the property type.
     * <p>
     * Returns null if the display name cannot be determined.
     * <p>
     * The display name is fetched from the definition. If no definition
     * (schema) is available, the display name will not be returned.
     *
     * @return display name for the property type
     */
    String getDisplayName();

    /**
     * Returns help message defined for the property type.
     * <p>
     * Returns null if the help message cannot be determined.
     * <p>
     * The help message is fetched from the definition. If no definition
     * (schema) is available, the help message will not be returned.
     *
     * @return help message for the property type
     */
    String getHelp();

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

	void setIncomplete(boolean incomplete);     // todo reconsider

	@Override
    PrismContext getPrismContext();

    // Primarily for testing
    PrismContext getPrismContextLocal();

    void setPrismContext(PrismContext prismContext);        // todo remove

	PrismValue getParent();

    void setParent(PrismValue parentValue);                 // todo remove

    UniformItemPath getPath();

    Map<String, Object> getUserData();

    <T> T getUserData(String key);

    void setUserData(String key, Object value);

    @NotNull
	List<V> getValues();

    V getValue(int index);

    Object getRealValue();

    @NotNull
    Collection<?> getRealValues();

    boolean hasValue(PrismValue value, boolean ignoreMetadata);

    boolean hasValue(PrismValue value);

    boolean hasRealValue(PrismValue value);

	// TODO what about dynamic definitions? See MID-3922
    boolean isSingleValue();

    /**
     * Returns value that is equal or equivalent to the provided value.
     * The returned value is an instance stored in this item, while the
     * provided value argument may not be.
     */
    PrismValue findValue(PrismValue value, boolean ignoreMetadata);

    List<? extends PrismValue> findValuesIgnoreMetadata(PrismValue value);

    /**
     * Returns value that is previous to the specified value.
     * Note that the order is semantically insignificant and this is used only
     * for presentation consistency in order-sensitive formats such as XML or JSON.
     */
    PrismValue getPreviousValue(PrismValue value);

    /**
     * Returns values that is following the specified value.
     * Note that the order is semantically insignificant and this is used only
     * for presentation consistency in order-sensitive formats such as XML or JSON.
     */
    PrismValue getNextValue(PrismValue value);

    Collection<V> getClonedValues();

    boolean contains(V value);

    boolean containsEquivalentValue(V value);
    
    boolean containsEquivalentValue(V value, Comparator<V> comparator);

    boolean contains(V value, boolean ignoreMetadata, Comparator<V> comparator);

    boolean contains(V value, boolean ignoreMetadata);

    boolean containsRealValue(V value);

    boolean valuesExactMatch(Collection<V> matchValues, Comparator<V> comparator);

    int size();

    boolean addAll(Collection<V> newValues) throws SchemaException;

    boolean add(@NotNull V newValue) throws SchemaException;

    boolean add(@NotNull V newValue, boolean checkUniqueness) throws SchemaException;

    boolean removeAll(Collection<V> newValues);

    boolean remove(V newValue);

    V remove(int index);

    void replaceAll(Collection<V> newValues) throws SchemaException;

    void replace(V newValue);

    void clear();

    void normalize();

    /**
     * Merge all the values of other item to this item.
     */
    void merge(Item<V,D> otherItem) throws SchemaException;

    Object find(ItemPath path);

    <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    // We want this method to be consistent with property diff
    ItemDelta<V,D> diff(Item<V,D> other);

    // We want this method to be consistent with property diff
    ItemDelta<V,D> diff(Item<V,D> other, boolean ignoreMetadata, boolean isLiteral);

	/**
     * Creates specific subclass of ItemDelta appropriate for type of item that this definition
     * represents (e.g. PropertyDelta, ContainerDelta, ...)
     */
	ItemDelta<V,D> createDelta();

	ItemDelta<V,D> createDelta(ItemPath path);

	@Override
	void accept(Visitor visitor);

	@Override
	void accept(Visitor visitor, ItemPath path, boolean recursive);

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

    /**
     * Complex clone with different cloning strategies.
     * @see CloneStrategy
     */
    Item cloneComplex(CloneStrategy strategy);
    
	static <T extends Item> Collection<T> cloneCollection(Collection<T> items) {
    	Collection<T> clones = new ArrayList<>(items.size());
    	for (T item: items) {
    		clones.add((T)item.clone());
    	}
    	return clones;
    }

    /**
     * Sets all parents to null. This is good if the items are to be "transplanted" into a
     * different Containerable.
     */
	static <T extends Item> Collection<T> resetParentCollection(Collection<T> items) {
    	for (T item: items) {
    		item.setParent(null);
    	}
    	return items;
	}

    static <T extends Item> T createNewDefinitionlessItem(QName name, Class<T> type, PrismContext prismContext) {
    	T item = null;
		try {
			Constructor<T> constructor = type.getConstructor(QName.class);
			item = constructor.newInstance(name);
            if (prismContext != null) {
                item.revive(prismContext);
            }
		} catch (Exception e) {
			throw new SystemException("Error creating new definitionless "+type.getSimpleName()+": "+e.getClass().getName()+" "+e.getMessage(),e);
		}
    	return item;
    }

    void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope);

	void checkConsistence(boolean requireDefinitions, boolean prohibitRaw);

	void checkConsistence(boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope);

	void checkConsistence();

	void checkConsistence(ConsistencyCheckScope scope);

	void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope);

    void assertDefinitions() throws SchemaException;

	void assertDefinitions(String sourceDescription) throws SchemaException;

	void assertDefinitions(boolean tolarateRawValues, String sourceDescription) throws SchemaException;

	/**
	 * Returns true is all the values are raw.
	 */
	boolean isRaw();

	/**
	 * Returns true is at least one of the values is raw.
	 */
	boolean hasRaw();

	boolean isEmpty();

	boolean hasNoValues();

	static boolean hasNoValues(Item<?, ?> item) {
		return item == null || item.getValues().isEmpty();
	}

	boolean equalsRealValue(Object obj);

	boolean match(Object obj);

	/**
	 * Returns true if this item is metadata item that should be ignored
	 * for metadata-insensitive comparisons and hashCode functions.
	 */
	boolean isMetadata();

	boolean isImmutable();

	void setImmutable(boolean immutable);

	void checkImmutability();

	// should be always called on non-overlapping objects! (for the synchronization to work correctly)
	void modifyUnfrozen(Runnable mutator);

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
}
