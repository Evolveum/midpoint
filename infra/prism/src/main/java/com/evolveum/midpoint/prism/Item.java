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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public abstract class Item<V extends PrismValue, D extends ItemDefinition> implements Itemable, DebugDumpable, Visitable, PathVisitable, Serializable, Revivable {

    private static final long serialVersionUID = 510000191615288733L;

    // The object should basically work without definition and prismContext. This is the
	// usual case when it is constructed "out of the blue", e.g. as a new JAXB object
	// It may not work perfectly, but basic things should work
    protected QName elementName;
    protected PrismValue parent;
    protected D definition;
    @NotNull protected final List<V> values = new ArrayList<>();
    private transient Map<String,Object> userData = new HashMap<>();;

	protected boolean immutable;
	protected boolean incomplete;

    protected transient PrismContext prismContext;          // beware, this one can easily be null

    /**
     * This is used for definition-less construction, e.g. in JAXB beans.
     *
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    Item(QName elementName) {
        super();
        this.elementName = elementName;
    }

    Item(QName elementName, PrismContext prismContext) {
        super();
        this.elementName = elementName;
        this.prismContext = prismContext;
    }


    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    Item(QName elementName, D definition, PrismContext prismContext) {
        super();
        this.elementName = elementName;
        this.definition = definition;
        this.prismContext = prismContext;
    }

    /**
     * Returns applicable property definition.
     * <p>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
    public D getDefinition() {
        return definition;
    }

	public boolean hasCompleteDefinition() {
		return getDefinition() != null;
	}


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
    public QName getElementName() {
        return elementName;
    }

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
    public void setElementName(QName elementName) {
		checkMutability();
        this.elementName = elementName;
    }

    /**
     * Sets applicable property definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(D definition) {
		checkMutability();
    	checkDefinition(definition);
        this.definition = definition;
    }

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
    public String getDisplayName() {
        return getDefinition() == null ? null : getDefinition().getDisplayName();
    }

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
    public String getHelp() {
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
    public boolean isIncomplete() {
		return incomplete;
	}

	public void setIncomplete(boolean incomplete) {
		this.incomplete = incomplete;
	}

	@Override
    public PrismContext getPrismContext() {
		if (prismContext != null) {
			return prismContext;
		} else if (parent != null) {
			return parent.getPrismContext();
		} else {
			return null;
		}
    }

    // Primarily for testing
    public PrismContext getPrismContextLocal() {
		return prismContext;
	}

    public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public PrismValue getParent() {
    	return parent;
    }

    public void setParent(PrismValue parentValue) {
    	if (this.parent != null && parentValue != null && this.parent != parentValue) {
    		throw new IllegalStateException("Attempt to reset parent of item "+this+" from "+this.parent+" to "+parentValue);
    	}
    	// Immutability check can be skipped, as setting the parent doesn't alter this object.
		// However, if existing parent itself is immutable, adding/removing its child item will cause the exception.
    	this.parent = parentValue;
    }

    public ItemPath getPath() {
    	 if (parent == null) {
    		 return new ItemPath(getElementName());
    	 }
    	 return parent.getPath().subPath(getElementName());
    }

    public Map<String, Object> getUserData() {
		if (userData == null) {
			userData = new HashMap<>();
		}
		if (immutable) {
			return Collections.unmodifiableMap(userData);			// TODO beware, objects in userData themselves are mutable
		} else {
			return userData;
		}
	}

    public Object getUserData(String key) {
		// TODO make returned data immutable (?)
		return getUserData().get(key);
	}

    public void setUserData(String key, Object value) {
		checkMutability();
    	getUserData().put(key, value);
    }

    @NotNull
	public List<V> getValues() {
		return values;
	}

    public V getValue(int index) {
    	if (index < 0) {
    		index = values.size() + index;
    	}
		return values.get(index);
	}

    public abstract <X> X getRealValue();

    @NotNull
    public abstract <X> Collection<X> getRealValues();

    public boolean hasValue(PrismValue value, boolean ignoreMetadata) {
    	return (findValue(value, ignoreMetadata) != null);
    }

    public boolean hasValue(PrismValue value) {
        return hasValue(value, false);
    }

    public boolean hasRealValue(PrismValue value) {
    	return hasValue(value, true);
    }

    public boolean isSingleValue() {
    	// TODO what about dynamic definitions? See MID-3922
		if (getDefinition() != null) {
    		if (getDefinition().isMultiValue()) {
    			return false;
    		}
    	}
		return values.size() <= 1;
	}

    /**
     * Returns value that is equal or equivalent to the provided value.
     * The returned value is an instance stored in this item, while the
     * provided value argument may not be.
     */
    public PrismValue findValue(PrismValue value, boolean ignoreMetadata) {
        for (PrismValue myVal : getValues()) {
            if (myVal.equalsComplex(value, ignoreMetadata, false)) {
                return myVal;
            }
        }
        return null;
    }

    public List<? extends PrismValue> findValuesIgnoreMetadata(PrismValue value) {
    	return getValues().stream()
			    .filter(v -> v.equalsComplex(value, true, false))
			    .collect(Collectors.toList());
    }

    /**
     * Returns value that is previous to the specified value.
     * Note that the order is semantically insignificant and this is used only
     * for presentation consistency in order-sensitive formats such as XML or JSON.
     */
    public PrismValue getPreviousValue(PrismValue value) {
    	PrismValue previousValue = null;
    	for (PrismValue myVal : getValues()) {
    		if (myVal == value) {
    			return previousValue;
    		}
    		previousValue = myVal;
    	}
    	throw new IllegalStateException("The value "+value+" is not any of "+this+" values, therefore cannot determine previous value");
    }

    /**
     * Returns values that is following the specified value.
     * Note that the order is semantically insignificant and this is used only
     * for presentation consistency in order-sensitive formats such as XML or JSON.
     */
    public PrismValue getNextValue(PrismValue value) {
    	Iterator<V> iterator = getValues().iterator();
    	while (iterator.hasNext()) {
    		PrismValue myVal = iterator.next();
    		if (myVal == value) {
    			if (iterator.hasNext()) {
    				return iterator.next();
    			} else {
    				return null;
    			}
    		}
    	}
    	throw new IllegalStateException("The value "+value+" is not any of "+this+" values, therefore cannot determine next value");
    }

    public Collection<V> getClonedValues() {
    	Collection<V> clonedValues = new ArrayList<V>(getValues().size());
    	for (V val: getValues()) {
    		clonedValues.add((V)val.clone());
    	}
		return clonedValues;
	}

    public boolean contains(V value) {
    	return contains(value, false);
    }

    public boolean containsEquivalentValue(V value) {
    	return contains(value, true);
    }

    public boolean contains(V value, boolean ignoreMetadata, Comparator<V> comparator) {
    	if (comparator == null){
    		return contains(value, ignoreMetadata);
    	} else{
    		for (V myValue: getValues()) {
        		if (comparator.compare(myValue, value) == 0) {
        			return true;
        		}
        	}

    	}

    	return false;
    }

    public boolean contains(V value, boolean ignoreMetadata) {
    	for (V myValue: getValues()) {
    		if (myValue.equals(value, ignoreMetadata)) {
    			return true;
    		}
    	}
    	return false;
    }

    public boolean containsRealValue(V value) {
    	for (V myValue: getValues()) {
    		if (myValue.equalsRealValue(value)) {
    			return true;
    		}
    	}
    	return false;
    }

    public boolean valuesExactMatch(Collection<V> matchValues, Comparator<V> comparator) {
    	return MiscUtil.unorderedCollectionCompare(values, matchValues, comparator );
    }

    public int size() {
    	return values.size();
    }

    public boolean addAll(Collection<V> newValues) throws SchemaException {
		checkMutability();			// TODO consider weaker condition, like testing if there's a real change
    	boolean changed = false;
    	for (V val: newValues) {
    		if (add(val)) {
    			changed = true;
    		}
    	}
    	return changed;
    }

    public boolean add(@NotNull V newValue) throws SchemaException {
    	return add(newValue, true);
    }

    public boolean add(@NotNull V newValue, boolean checkUniqueness) throws SchemaException {
		checkMutability();
		if (newValue.getPrismContext() == null) {
			newValue.setPrismContext(prismContext);
		}
    	if (checkUniqueness && containsEquivalentValue(newValue)) {
    		return false;
    	}
	    newValue.setParent(this);
	    D definition = getDefinition();
	    if (definition != null) {
		    if (!values.isEmpty() && definition.isSingleValue()) {
			    throw new SchemaException("Attempt to put more than one value to single-valued item " + this + "; newly added value: " + newValue);
		    }
    		newValue.applyDefinition(definition, false);
    	}
    	return values.add(newValue);
    }

    public boolean removeAll(Collection<V> newValues) {
		checkMutability();					// TODO consider if there is real change
    	boolean changed = false;
    	for (V val: newValues) {
    		if (remove(val)) {
    			changed = true;
    		}
    	}
    	return changed;
    }

    public boolean remove(V newValue) {
		checkMutability();					// TODO consider if there is real change
    	boolean changed = false;
    	Iterator<V> iterator = values.iterator();
    	while (iterator.hasNext()) {
    		V val = iterator.next();
			// the same algorithm as when deleting the item value from delete delta
			// TODO either make equalsRealValue return false if both PCVs have IDs and these IDs are different
			// TODO or include a special test condition here; see MID-3828
			if (val.representsSameValue(newValue, false) || val.equalsRealValue(newValue)) {
    			iterator.remove();
    			val.setParent(null);
    			changed = true;
    		}
    	}
    	return changed;
    }

    public V remove(int index) {
		checkMutability();					// TODO consider if there is real change
    	return values.remove(index);
    }

    public void replaceAll(Collection<V> newValues) throws SchemaException {
		checkMutability();					// TODO consider if there is real change
    	values.clear();
    	addAll(newValues);
    }

    public void replace(V newValue) {
		checkMutability();					// TODO consider if there is real change
    	values.clear();
        newValue.setParent(this);
    	values.add(newValue);
    }

    public void clear() {
		checkMutability();					// TODO consider if there is real change
		values.clear();
    }

    public void normalize() {
		checkMutability();					// TODO consider if there is real change
		for (V value : values) {
			value.normalize();
		}
    }

    /**
     * Merge all the values of other item to this item.
     */
    public void merge(Item<V,D> otherItem) throws SchemaException {
    	for (V otherValue: otherItem.getValues()) {
    		if (!contains(otherValue)) {
    			add((V) otherValue.clone());
    		}
    	}
    }

    public abstract Object find(ItemPath path);

    public abstract <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    // We want this method to be consistent with property diff
    public ItemDelta<V,D> diff(Item<V,D> other) {
    	return diff(other, true, false);
    }

    // We want this method to be consistent with property diff
    public ItemDelta<V,D> diff(Item<V,D> other, boolean ignoreMetadata, boolean isLiteral) {
    	List<? extends ItemDelta> itemDeltas = new ArrayList<>();
		diffInternal(other, itemDeltas, ignoreMetadata, isLiteral);
		if (itemDeltas.isEmpty()) {
			return null;
		}
		if (itemDeltas.size() > 1) {
			throw new UnsupportedOperationException("Item multi-delta diff is not supported yet");
		}
		return itemDeltas.get(0);
    }

    protected void diffInternal(Item<V,D> other, Collection<? extends ItemDelta> deltas,
    		boolean ignoreMetadata, boolean isLiteral) {
    	ItemDelta delta = createDelta();
    	if (other == null) {
    		//other doesn't exist, so delta means delete all values
            for (PrismValue value : getValues()) {
            	PrismValue valueClone = value.clone();
                delta.addValueToDelete(valueClone);
            }
    	} else {
    		// the other exists, this means that we need to compare the values one by one
    		Collection<PrismValue> outstandingOtheValues = new ArrayList<PrismValue>(other.getValues().size());
    		outstandingOtheValues.addAll(other.getValues());
    		for (PrismValue thisValue : getValues()) {
    			Iterator<PrismValue> iterator = outstandingOtheValues.iterator();
    			boolean found = false;
    			while (iterator.hasNext()) {
    				PrismValue otherValue = iterator.next();
    				if (thisValue.representsSameValue(otherValue, true) || delta == null) {
    					found = true;
    					// Matching IDs, look inside to figure out internal deltas
    					thisValue.diffMatchingRepresentation(otherValue, deltas,
    							ignoreMetadata, isLiteral);
    					// No need to process this value again
    					iterator.remove();
    					break;
					// TODO either make equalsRealValue return false if both PCVs have IDs and these IDs are different
					// TODO or include a special test condition here; see MID-3828
					} else if (thisValue.equalsComplex(otherValue, ignoreMetadata, isLiteral)) {
    					found = true;
    					// same values. No delta
    					// No need to process this value again
    					iterator.remove();
    					break;
					}
    			}
				if (!found) {
					// We have the value and the other does not, this is delete of the entire value
					delta.addValueToDelete(thisValue.clone());
				}
            }
    		// outstandingOtheValues are those values that the other has and we could not
    		// match them to any of our values. These must be new values to add
    		for (PrismValue outstandingOtherValue : outstandingOtheValues) {
    			delta.addValueToAdd(outstandingOtherValue.clone());
            }
    		// Some deltas may need to be polished a bit. E.g. transforming
    		// add/delete delta to a replace delta.
    		delta = fixupDelta(delta, other, ignoreMetadata);
    	}
    	if (delta != null && !delta.isEmpty()) {
    		((Collection)deltas).add(delta);
    	}
    }

	protected ItemDelta<V,D> fixupDelta(ItemDelta<V,D> delta, Item<V,D> other,
			boolean ignoreMetadata) {
		return delta;
	}

	/**
     * Creates specific subclass of ItemDelta appropriate for type of item that this definition
     * represents (e.g. PropertyDelta, ContainerDelta, ...)
     */
	public abstract ItemDelta<V,D> createDelta();

	public abstract ItemDelta<V,D> createDelta(ItemPath path);

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
		for(PrismValue value: getValues()) {
			value.accept(visitor);
		}
	}

	@Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
		// This implementation is supposed to only work for non-hierarchical items, such as properties and references.
		// hierarchical items must override it.
		if (recursive) {
			accept(visitor);
		} else {
			visitor.visit(this);
		}
	}

	public void filterValues(Function<V, Boolean> function) {
		Iterator<V> iterator = values.iterator();
		while (iterator.hasNext()) {
			Boolean keep = function.apply(iterator.next());
			if (keep == null || !keep) {
				iterator.remove();
			}
		}
	}

	public void applyDefinition(D definition) throws SchemaException {
		applyDefinition(definition, true);
	}

	public void applyDefinition(D definition, boolean force) throws SchemaException {
		checkMutability();					// TODO consider if there is real change
		if (definition != null) {
			checkDefinition(definition);
		}
		if (this.prismContext == null && definition != null) {
			this.prismContext = definition.getPrismContext();
		}
		this.definition = definition;
		for (PrismValue pval: getValues()) {
			pval.applyDefinition(definition, force);
		}
	}

    public void revive(PrismContext prismContext) throws SchemaException {
        // it is necessary to do e.g. PolyString recomputation even if PrismContext is set
    	if (this.prismContext == null) {
            this.prismContext = prismContext;
            if (definition != null) {
                definition.revive(prismContext);
            }
        }
		for (V value: values) {
			value.revive(prismContext);
		}
	}

    public abstract Item clone();

    protected void copyValues(Item clone) {
        clone.elementName = this.elementName;
        clone.definition = this.definition;
        clone.prismContext = this.prismContext;
        // Do not clone parent so the cloned item can be safely placed to
        // another item
        clone.parent = null;
        clone.userData = MiscUtil.cloneMap(this.userData);
        clone.incomplete = this.incomplete;
		// Also do not copy 'immutable' flag so the clone is free to be modified
    }

    protected void propagateDeepCloneDefinition(boolean ultraDeep, D clonedDefinition) {
    	// nothing to do by default
    }

	public static <T extends Item> Collection<T> cloneCollection(Collection<T> items) {
    	Collection<T> clones = new ArrayList<T>(items.size());
    	for (T item: items) {
    		clones.add((T)item.clone());
    	}
    	return clones;
    }

    /**
     * Sets all parents to null. This is good if the items are to be "transplanted" into a
     * different Containerable.
     */
	public static <T extends Item> Collection<T> resetParentCollection(Collection<T> items) {
    	for (T item: items) {
    		item.setParent(null);
    	}
    	return items;
	}

    public static <T extends Item> T createNewDefinitionlessItem(QName name, Class<T> type, PrismContext prismContext) {
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

    public void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope) {
    	checkConsistenceInternal(this, requireDefinitions, false, scope);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw) {
        checkConsistenceInternal(this, requireDefinitions, prohibitRaw, ConsistencyCheckScope.THOROUGH);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
    	checkConsistenceInternal(this, requireDefinitions, prohibitRaw, scope);
    }

    public void checkConsistence() {
    	checkConsistenceInternal(this, false, false, ConsistencyCheckScope.THOROUGH);
    }

    public void checkConsistence(ConsistencyCheckScope scope) {
        checkConsistenceInternal(this, false, false, scope);
    }


    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
    	ItemPath path = getPath();
    	if (elementName == null) {
    		throw new IllegalStateException("Item "+this+" has no name ("+path+" in "+rootItem+")");
    	}

    	if (definition != null) {
    		checkDefinition(definition);
    	} else if (requireDefinitions && !isRaw()) {
    		throw new IllegalStateException("No definition in item "+this+" ("+path+" in "+rootItem+")");
    	}
		for (V val: values) {
			if (prohibitRaw && val.isRaw()) {
				throw new IllegalStateException("Raw value "+val+" in item "+this+" ("+path+" in "+rootItem+")");
			}
			if (val == null) {
				throw new IllegalStateException("Null value in item "+this+" ("+path+" in "+rootItem+")");
			}
			if (val.getParent() == null) {
				throw new IllegalStateException("Null parent for value "+val+" in item "+this+" ("+path+" in "+rootItem+")");
			}
			if (val.getParent() != this) {
				throw new IllegalStateException("Wrong parent for value "+val+" in item "+this+" ("+path+" in "+rootItem+"), "+
						"bad parent: " + val.getParent());
			}
			val.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
		}
	}

	protected abstract void checkDefinition(D def);

    public void assertDefinitions() throws SchemaException {
    	assertDefinitions("");
    }

	public void assertDefinitions(String sourceDescription) throws SchemaException {
		assertDefinitions(false, sourceDescription);
	}

	public void assertDefinitions(boolean tolarateRawValues, String sourceDescription) throws SchemaException {
		if (tolarateRawValues && isRaw()) {
			return;
		}
		if (definition == null) {
			throw new SchemaException("No definition in "+this+" in "+sourceDescription);
		}
	}

	/**
	 * Returns true is all the values are raw.
	 */
	public boolean isRaw() {
		for (V val: getValues()) {
			if (!val.isRaw()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true is at least one of the values is raw.
	 */
	public boolean hasRaw() {
		for (V val: getValues()) {
			if (val.isRaw()) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty() {
	    return hasNoValues();
    }

    public boolean hasNoValues() {
		return getValues().isEmpty();
    }

	public static boolean hasNoValues(Item<?, ?> item) {
		return item == null || item.getValues().isEmpty();
	}

	@Override
	public int hashCode() {
		int valuesHash = MiscUtil.unorderedCollectionHashcode(values, null);
		if (valuesHash == 0) {
			// empty or non-significant container. We do not want this to destroy hashcode of
			// parent item
			return 0;
		}
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elementName == null) ? 0 : elementName.hashCode());
		result = prime * result + valuesHash;
		return result;
	}

	public boolean equalsRealValue(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item<?,?> other = (Item<?,?>) obj;
		if (elementName == null) {
			if (other.elementName != null)
				return false;
		} else if (!elementName.equals(other.elementName))
			return false;
		// Do not compare parent at all. This is not relevant.
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!equalsRealValues(this.values, other.values))
			return false;
		return true;
	}

	private boolean equalsRealValues(List<V> thisValue, List<?> otherValues) {
		return MiscUtil.unorderedCollectionEquals(thisValue, otherValues,
				(o1, o2) -> {
					if (o1 instanceof PrismValue && o2 instanceof PrismValue) {
						PrismValue v1 = (PrismValue)o1;
						PrismValue v2 = (PrismValue)o2;
						return v1.equalsRealValue(v2);
					} else {
						return false;
					}
				});
	}

	private boolean match(List<V> thisValue, List<?> otherValues) {
		return MiscUtil.unorderedCollectionEquals(thisValue, otherValues,
				(o1, o2) -> {
					if (o1 instanceof PrismValue && o2 instanceof PrismValue) {
						PrismValue v1 = (PrismValue)o1;
						PrismValue v2 = (PrismValue)o2;
						return v1.match(v2);
					} else {
						return false;
					}
				});
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item<?,?> other = (Item<?,?>) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
			return false;
		if (elementName == null) {
			if (other.elementName != null)
				return false;
		} else if (!elementName.equals(other.elementName))
			return false;
		if (incomplete != other.incomplete) {
				return false;
		}
		// Do not compare parent at all. This is not relevant.
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!MiscUtil.unorderedCollectionEquals(this.values, other.values))
			return false;
		return true;
	}

	public boolean match(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item<?,?> other = (Item<?,?>) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
			return false;
		if (elementName == null) {
			if (other.elementName != null)
				return false;
		} else if (!elementName.equals(other.elementName))
			return false;
		if (incomplete != other.incomplete) {
			return false;
		}
		// Do not compare parent at all. This is not relevant.
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!match(this.values, other.values))
			return false;
		return true;
	}

	/**
	 * Returns true if this item is metadata item that should be ignored
	 * for metadata-insensitive comparisons and hashCode functions.
	 */
	public boolean isMetadata() {
		D def = getDefinition();
		if (def != null) {
			return def.isOperational();
		} else {
			return false;
		}
	}

	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + PrettyPrinter.prettyPrint(getElementName()) + ")";
    }

    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        if (DebugUtil.isDetailedDebugDump()) {
        	sb.append(getDebugDumpClassName()).append(": ");
        }
        sb.append(DebugUtil.formatElementName(getElementName()));
        return sb.toString();
    }

	/**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "Item";
    }

    protected void appendDebugDumpSuffix(StringBuilder sb) {
    	if (incomplete) {
    		sb.append(" (incomplete)");
    	}
    }

	public boolean isImmutable() {
		return immutable;
	}

	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
		for (V value : getValues()) {
			value.setImmutable(immutable);
		}
	}

	protected void checkMutability() {
		if (immutable) {
			throw new IllegalStateException("An attempt to modify an immutable item: " + toString());
		}
	}

	public void checkImmutability() {
    	synchronized (this) {		// because of modifyUnfrozen
			if (!immutable) {
				throw new IllegalStateException("Item is not immutable even if it should be: " + this);
			}
		}
	}

	// should be always called on non-overlapping objects! (for the synchronization to work correctly)
	public void modifyUnfrozen(Runnable mutator) {
		synchronized (this) {
			boolean wasImmutable = immutable;
			if (wasImmutable) {
				setImmutable(false);
			}
			try {
				mutator.run();
			} finally {
				if (wasImmutable) {
					setImmutable(true);
				}
			}
		}
	}

	@NotNull
	public static <V extends PrismValue> Collection<V> getValues(Item<V, ?> item) {
    	return item != null ? item.getValues() : Collections.emptySet();
	}

	// Path may contain ambiguous segments (e.g. assignment/targetRef when there are more assignments)
	// Note that the path can contain name segments only (at least for now)
	@NotNull
	public Collection<PrismValue> getAllValues(ItemPath path) {
    	return values.stream()
			    .flatMap(v -> v.getAllValues(path).stream())
			    .collect(Collectors.toList());
	}

	@NotNull
	public static Collection<PrismValue> getAllValues(Item<?, ?> item, ItemPath path) {
    	return item != null ? item.getAllValues(path) : Collections.emptySet();
	}
}
