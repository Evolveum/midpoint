/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism.delta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PathVisitable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.Validate;

/**
 * @author Radovan Semancik
 * 
 */
public abstract class ItemDelta<V extends PrismValue> implements Itemable, DebugDumpable, Visitable, PathVisitable, Serializable {

	/**
	 * Name of the property
	 */
	protected QName elementName;
	/**
	 * Parent path of the property (path to the property container)
	 */
	protected ItemPath parentPath;
	protected ItemDefinition definition;

	protected Collection<V> valuesToReplace = null;
	protected Collection<V> valuesToAdd = null;
	protected Collection<V> valuesToDelete = null;

    transient private PrismContext prismContext;

	protected ItemDelta(ItemDefinition itemDefinition, PrismContext prismContext) {
		if (itemDefinition == null) {
			throw new IllegalArgumentException("Attempt to create item delta without a definition");
		}
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;
		this.elementName = itemDefinition.getName();
		this.parentPath = new ItemPath();
		this.definition = itemDefinition;
	}

	protected ItemDelta(QName elementName, ItemDefinition itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;
		this.elementName = elementName;
		this.parentPath = new ItemPath();
		this.definition = itemDefinition;
    }

	protected ItemDelta(ItemPath parentPath, QName elementName, ItemDefinition itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;
		this.elementName = elementName;
		this.parentPath = parentPath;
		this.definition = itemDefinition;
    }

	protected ItemDelta(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;

		if (path == null) {
			throw new IllegalArgumentException("Null path specified while creating item delta");
		}
		if (path.isEmpty()) {
			this.elementName = null;
		} else {
			ItemPathSegment last = path.last();
			if (!(last instanceof NameItemPathSegment)) {
				throw new IllegalArgumentException("Invalid delta path "+path+". Delta path must always point to item, not to value");
			}
			this.elementName = ((NameItemPathSegment)last).getName();
			this.parentPath = path.allExceptLast();
		}
		this.definition = itemDefinition;
	}

    // currently unused; we allow deltas without prismContext, except for some operations (e.g. serialization to ItemDeltaType)
//    private void checkPrismContext(PrismContext prismContext, ItemDefinition itemDefinition) {
//        if (prismContext == null) {
//            throw new IllegalStateException("No prismContext in delta for " + itemDefinition);
//        }
//    }

	public QName getElementName() {
		return elementName;
	}

	public void setElementName(QName elementName) {
		this.elementName = elementName;
	}

	public ItemPath getParentPath() {
		return parentPath;
	}

	public void setParentPath(ItemPath parentPath) {
		this.parentPath = parentPath;
	}

	@Override
	public ItemPath getPath() {
		return getParentPath().subPath(elementName);
	}

	public ItemDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(ItemDefinition definition) {
		this.definition = definition;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
		if (getValuesToAdd() != null) {
			for (V pval : getValuesToAdd()) {
				pval.accept(visitor);
			}
		}
		if (getValuesToDelete() != null) {
			for (V pval : getValuesToDelete()) {
				pval.accept(visitor);
			}
		}
		if (getValuesToReplace() != null) {
			for (V pval : getValuesToReplace()) {
				pval.accept(visitor);
			}
		}
	}
	
	@Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
		if (path == null || path.isEmpty()) {
			if (recursive) {
				accept(visitor);
			} else {
				visitor.visit(this);
			}
		} else {
			IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(path);
			ItemPath rest = ItemPath.pathRestStartingWithName(path);
			if (idSegment == null || idSegment.isWildcard()) {
				// visit all values
				if (getValuesToAdd() != null) {
					for (V pval : getValuesToAdd()) {
						pval.accept(visitor, rest, recursive);
					}
				}
				if (getValuesToDelete() != null) {
					for (V pval : getValuesToDelete()) {
						pval.accept(visitor, rest, recursive);
					}
				}
				if (getValuesToReplace() != null) {
					for (V pval : getValuesToReplace()) {
						pval.accept(visitor, rest, recursive);
					}
				}
			} else {
				Long id = idSegment.getId();
				acceptSet(getValuesToAdd(), id, visitor, rest, recursive);
				acceptSet(getValuesToDelete(), id, visitor, rest, recursive);
				acceptSet(getValuesToReplace(), id, visitor, rest, recursive);
			}
		}
	}

	private void acceptSet(Collection<V> set, Long id, Visitor visitor, ItemPath rest, boolean recursive) {
		if (set == null) {
			return;
		}
		for (V pval : set) {
			if (pval instanceof PrismContainerValue<?>) {
				PrismContainerValue<?> cval = (PrismContainerValue<?>)pval;
				if (id == null || id.equals(cval.getId())) {
					pval.accept(visitor, rest, recursive);
				}
			} else {
				throw new IllegalArgumentException("Attempt to fit container id to "+pval.getClass());
			}
		}		
	}

	public void applyDefinition(ItemDefinition definition) throws SchemaException {
		this.definition = definition;
		if (getValuesToAdd() != null) {
			for (V pval : getValuesToAdd()) {
				pval.applyDefinition(definition);
			}
		}
		if (getValuesToDelete() != null) {
			for (V pval : getValuesToDelete()) {
				pval.applyDefinition(definition);
			}
		}
		if (getValuesToReplace() != null) {
			for (V pval : getValuesToReplace()) {
				pval.applyDefinition(definition);
			}
		}
	}

	public static void applyDefinition(Collection<? extends ItemDelta> deltas,
			PrismObjectDefinition definition) throws SchemaException {
		for (ItemDelta<?> itemDelta : deltas) {
			ItemPath path = itemDelta.getPath();
			ItemDefinition itemDefinition = definition.findItemDefinition(path, ItemDefinition.class);
            if (itemDefinition == null) {
                throw new SchemaException("Object type " + definition.getTypeName() + " doesn't contain definition for path " + new XPathHolder(path).getXPathWithDeclarations(false));
            }
			itemDelta.applyDefinition(itemDefinition);
		}
	}
	
	public boolean hasCompleteDefinition() {
		return getDefinition() != null;
	}


	public PrismContext getPrismContext() {
		return prismContext;
	}

	public abstract Class<? extends Item> getItemClass();

	public Collection<V> getValuesToAdd() {
		return valuesToAdd;
	}
	
	public void clearValuesToAdd() {
		valuesToAdd = null;
	}

	public Collection<V> getValuesToDelete() {
		return valuesToDelete;
	}
	
	public void clearValuesToDelete() {
		valuesToDelete = null;
	}

	public Collection<V> getValuesToReplace() {
		return valuesToReplace;
	}
	
	public void clearValuesToReplace() {
		valuesToReplace = null;
	}

	public void addValuesToAdd(Collection<V> newValues) {
		for (V val : newValues) {
			addValueToAdd(val);
		}
	}
	
	public void addValuesToAdd(V... newValues) {
		for (V val : newValues) {
			addValueToAdd(val);
		}
	}

	public void addValueToAdd(V newValue) {
		if (valuesToReplace != null) {
			throw new IllegalStateException("Delta " + this
				+ " already has values to replace ("+valuesToReplace+"), attempt to add value ("+newValue+") is an error");
		}
		if (valuesToAdd == null) {
			valuesToAdd = newValueCollection();
		}
		if (PrismValue.containsRealValue(valuesToAdd,newValue)) {
			return;
		}
		valuesToAdd.add(newValue);
		newValue.setParent(this);
		newValue.recompute();
	}
	
	public boolean removeValueToAdd(V valueToRemove) {
		return removeValue(valueToRemove, valuesToAdd);
	}
	
	public boolean removeValueToDelete(V valueToRemove) {
		return removeValue(valueToRemove, valuesToDelete);
	}
	
	public boolean removeValueToReplace(V valueToRemove) {
		return removeValue(valueToRemove, valuesToReplace);
	}
	
	private boolean removeValue(V valueToRemove, Collection<V> set) {
		boolean removed = false;
		if (set == null) {
			return removed;
		}
		Iterator<V> valuesToReplaceIterator = set.iterator();
		while (valuesToReplaceIterator.hasNext()) {
			V valueToReplace = valuesToReplaceIterator.next();
			if (valueToReplace.equalsRealValue(valueToRemove)) {
				valuesToReplaceIterator.remove();
				removed = true;
			}
		}
		return removed;
	}

	public void mergeValuesToAdd(Collection<V> newValues) {
		for (V val : newValues) {
			mergeValueToAdd(val);
		}
	}
	
	public void mergeValuesToAdd(V[] newValues) {
		for (V val : newValues) {
			mergeValueToAdd(val);
		}
	}
	
	public void mergeValueToAdd(V newValue) {
		if (valuesToReplace != null) {
			if (!PrismValue.containsRealValue(valuesToReplace, newValue)) {
				valuesToReplace.add(newValue);
				newValue.setParent(this);
			}
		} else {
			if (!removeValueToDelete(newValue)) {
				addValueToAdd(newValue);
			}
		}
	}

	public void addValuesToDelete(Collection<V> newValues) {
		for (V val : newValues) {
			addValueToDelete(val);
		}
	}

	public void addValuesToDelete(V... newValues) {
		for (V val : newValues) {
			addValueToDelete(val);
		}
	}
	
	public void addValueToDelete(V newValue) {
		if (valuesToReplace != null) {
			throw new IllegalStateException("Delta " + this
					+ " already has values to replace ("+valuesToReplace+"), attempt to set value to delete ("+newValue+")");
		}
		if (valuesToDelete == null) {
			valuesToDelete = newValueCollection();
		}
		if (PrismValue.containsRealValue(valuesToDelete,newValue)) {
			return;
		}
		valuesToDelete.add(newValue);
		newValue.setParent(this);
		newValue.recompute();
	}
	
	public void mergeValuesToDelete(Collection<V> newValues) {
		for (V val : newValues) {
			mergeValueToDelete(val);
		}
	}

	public void mergeValuesToDelete(V[] newValues) {
		for (V val : newValues) {
			mergeValueToDelete(val);
		}
	}
	
	public void mergeValueToDelete(V newValue) {
		if (valuesToReplace != null) {
			removeValueToReplace(newValue);
		} else {
			if (!removeValueToAdd(newValue)) {
				addValueToDelete(newValue);
			}
		}
	}

    public void resetValuesToAdd() {
        valuesToAdd = null;
    }

	public void setValuesToReplace(Collection<V> newValues) {
		if (valuesToAdd != null) {
			throw new IllegalStateException("Delta " + this
					+ " already has values to add ("+valuesToAdd+"), attempt to set value to replace ("+newValues+")");
		}
		if (valuesToDelete != null) {
			throw new IllegalStateException("Delta " + this
					+ " already has values to delete, attempt to set value to replace");
		}
		if (valuesToReplace == null) {
			valuesToReplace = newValueCollection();
		} else {
			valuesToReplace.clear();
		}
		for (V val : newValues) {
			valuesToReplace.add(val);
			val.setParent(this);
			val.recompute();
		}
	}

	public void setValuesToReplace(V... newValues) {
		if (valuesToAdd != null) {
			throw new IllegalStateException("Delta " + this
					+ " already has values to add, attempt to set value to replace");
		}
		if (valuesToDelete != null) {
			throw new IllegalStateException("Delta " + this
					+ " already has values to delete, attempt to set value to replace");
		}
		if (valuesToReplace == null) {
			valuesToReplace = newValueCollection();
		} else {
			valuesToReplace.clear();
		}
		for (V val : newValues) {
			valuesToReplace.add(val);
			val.setParent(this);
			val.recompute();
		}
	}

	/**
	 * Sets empty value to replace. This efficiently means removing all values.
	 */
	public void setValueToReplace() {
		if (valuesToReplace == null) {
			valuesToReplace = newValueCollection();
		} else {
			valuesToReplace.clear();
		}
	}
	
	public void setValueToReplace(V newValue) {
		if (valuesToAdd != null) {
			throw new IllegalStateException("Delta " + this
					+ " already has values to add, attempt to set value to replace");
		}
		if (valuesToDelete != null) {
			throw new IllegalStateException("Delta " + this
					+ " already has values to delete, attempt to set value to replace");
		}
		if (valuesToReplace == null) {
			valuesToReplace = newValueCollection();
		} else {
			valuesToReplace.clear();
		}
		valuesToReplace.add(newValue);
		newValue.setParent(this);
		newValue.recompute();
	}
	
	public void mergeValuesToReplace(Collection<V> newValues) {
		// No matter what type the delta was before. We are just discarding all the previous
		// state as the replace that we are applying will overwrite that anyway.
		valuesToAdd = null;
		valuesToDelete = null;
		setValuesToReplace(newValues);
	}

	public void mergeValuesToReplace(V[] newValues) {
		// No matter what type the delta was before. We are just discarding all the previous
		// state as the replace that we are applying will overwrite that anyway.
		valuesToAdd = null;
		valuesToDelete = null;
		setValuesToReplace(newValues);
	}
	
	public void mergeValueToReplace(V newValue) {
		// No matter what type the delta was before. We are just discarding all the previous
		// state as the replace that we are applying will overwrite that anyway.
		valuesToAdd = null;
		valuesToDelete = null;
		setValueToReplace(newValue);
	}

	private Collection<V> newValueCollection() {
		return new ArrayList<V>();
	}

	public boolean isValueToAdd(V value) {
		return isValueSet(value, false, valuesToAdd);
	}

	public boolean isValueToAdd(V value, boolean ignoreMetadata) {
		return isValueSet(value, ignoreMetadata, valuesToAdd);
	}

	public boolean isValueToDelete(V value) {
		return isValueSet(value, false, valuesToDelete);
	}

	public boolean isValueToDelete(V value, boolean ignoreMetadata) {
		return isValueSet(value, ignoreMetadata, valuesToDelete);
	}

	public boolean isValueToReplace(V value) {
		return isValueSet(value, false, valuesToReplace);
	}

	public boolean isValueToReplace(V value, boolean ignoreMetadata) {
		return isValueSet(value, ignoreMetadata, valuesToReplace);
	}

	private boolean isValueSet(V value, boolean ignoreMetadata, Collection<V> set) {
		if (set == null) {
			return false;
		}
		for (V myVal: set) {
			if (myVal.equals(value, ignoreMetadata)) {
				return true;
			}
		}
		return false;
	}
	
	public V getAnyValue() {
		V anyValue = getAnyValue(valuesToAdd);
		if (anyValue != null) {
			return anyValue;
		}
		anyValue = getAnyValue(valuesToDelete);
		if (anyValue != null) {
			return anyValue;
		}
		anyValue = getAnyValue(valuesToReplace);
		if (anyValue != null) {
			return anyValue;
		}
		return null;
	}

	private V getAnyValue(Collection<V> set) {
		if (set == null || set.isEmpty()) {
			return null;
		}
		return set.iterator().next();
	}

	public boolean isEmpty() {
		if (valuesToAdd == null && valuesToDelete == null && valuesToReplace == null) {
			return true;
		}
		return false;
	}
	
	public boolean addsAnyValue() {
		return hasAnyValue(valuesToAdd) || hasAnyValue(valuesToReplace); 
	}
	
	private boolean hasAnyValue(Collection<V> set) {
		return (set != null && !set.isEmpty());
	}

	public void normalize() {
		normalize(valuesToAdd);
		normalize(valuesToDelete);
		normalize(valuesToReplace);
	}

	private void normalize(Collection<V> set) {
		if (set == null) {
			return;
		}
		Iterator<V> iterator = set.iterator();
		while (iterator.hasNext()) {
			V value = iterator.next();
			value.normalize();
			if (value.isEmpty()) {
				iterator.remove();
			}
		}
	}

	public boolean isReplace() {
		return (valuesToReplace != null);
	}

	public boolean isAdd() {
		return (valuesToAdd != null && !valuesToAdd.isEmpty());
	}

	public boolean isDelete() {
		return (valuesToDelete != null && !valuesToDelete.isEmpty());
	}

	public void clear() {
		valuesToReplace = null;
		valuesToAdd = null;
		valuesToDelete = null;
	}
	
	public static PropertyDelta findPropertyDelta(Collection<? extends ItemDelta> deltas, QName propertyName) {
        return findPropertyDelta(deltas, new ItemPath(propertyName));
    }

    public static PropertyDelta findPropertyDelta(Collection<? extends ItemDelta> deltas, ItemPath parentPath, QName propertyName) {
        return findPropertyDelta(deltas, new ItemPath(parentPath, propertyName));
    }
    
    public static PropertyDelta findPropertyDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath) {
    	return findItemDelta(deltas, propertyPath, PropertyDelta.class);
    }
    
    public static <X extends Containerable> ContainerDelta<X> findContainerDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath) {
    	return findItemDelta(deltas, propertyPath, ContainerDelta.class);
    }

    public static <X extends Containerable> ContainerDelta<X> findContainerDelta(Collection<? extends ItemDelta> deltas, QName name) {
    	return findContainerDelta(deltas, new ItemPath(name));
    }

    public static <D extends ItemDelta> D findItemDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath, Class<D> deltaType) {
        if (deltas == null) {
            return null;
        }
        for (ItemDelta<?> delta : deltas) {
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equivalent(propertyPath)) {
                return (D) delta;
            }
        }
        return null;
    }
    
    public static Collection<? extends ItemDelta<?>> findItemDeltasSubPath(Collection<? extends ItemDelta<?>> deltas, ItemPath itemPath) {
    	Collection<ItemDelta<?>> foundDeltas = new ArrayList<ItemDelta<?>>();
        if (deltas == null) {
            return foundDeltas;
        }
        for (ItemDelta<?> delta : deltas) {
            if (itemPath.isSubPath(delta.getPath())) {
                foundDeltas.add(delta);
            }
        }
        return foundDeltas;
    }
    
    public static <D extends ItemDelta> D findItemDelta(Collection<? extends ItemDelta> deltas, QName itemName, Class<D> deltaType) {
    	return findItemDelta(deltas, new ItemPath(itemName), deltaType);
    }
    
    public static ReferenceDelta findReferenceModification(Collection<? extends ItemDelta> deltas, QName itemName) {
    	return findItemDelta(deltas, itemName, ReferenceDelta.class);
    }
    
    public static <D extends ItemDelta> void removeItemDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath, Class<D> deltaType) {
        if (deltas == null) {
            return;
        }
        Iterator<? extends ItemDelta> deltasIterator = deltas.iterator();
        while (deltasIterator.hasNext()) {
        	ItemDelta<?> delta = deltasIterator.next();
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equivalent(propertyPath)) {
                deltasIterator.remove();
            }
        }
    }
    
    /**
     * Filters out all delta values that are meaningless to apply. E.g. removes all values to add that the property already has,
     * removes all values to delete that the property does not have, etc. 
     */
    public ItemDelta<V> narrow(PrismObject<? extends Objectable> object) {
    	Item<V> currentItem = (Item<V>) object.findItem(getPath());
    	if (currentItem == null) {
    		if (valuesToDelete != null) {
    			ItemDelta<V> clone = clone();
    			clone.valuesToDelete = null;
    			return clone;
    		} else {
    			// Nothing to narrow
    			return this;
    		}
    	} else {
    		ItemDelta<V> clone = clone();
    		if (clone.valuesToDelete != null) {
    			Iterator<V> iterator = clone.valuesToDelete.iterator();
    			while (iterator.hasNext()) {
    				V valueToDelete = iterator.next();
    				if (!currentItem.contains(valueToDelete, true)) {
    					iterator.remove();
    				}
    			}
    		}
    		if (clone.valuesToAdd != null) {
    			Iterator<V> iterator = clone.valuesToAdd.iterator();
    			while (iterator.hasNext()) {
    				V valueToDelete = iterator.next();
    				if (currentItem.contains(valueToDelete, true)) {
    					iterator.remove();
    				}
    			}
    		}
    		return clone;
    	}
    }
    
    public void validate() throws SchemaException {
    	validate(null);
    }
    
    public void validate(String contextDescription) throws SchemaException {
    	if (definition == null) {
    		throw new IllegalStateException("Attempt to validate delta without a definition: "+this);
    	}
    	if (definition.isSingleValue()) {
    		if (valuesToAdd != null && valuesToAdd.size() > 1) {
    			throw new SchemaException("Attempt to add "+valuesToAdd.size()+" values to a single-valued item "+getPath() +
    					(contextDescription == null ? "" : " in "+contextDescription));
    		}
    		if (valuesToReplace != null && valuesToReplace.size() > 1) {
    			throw new SchemaException("Attempt to replace "+valuesToReplace.size()+" values to a single-valued item "+getPath() +
    					(contextDescription == null ? "" : " in "+contextDescription));
    		}
    	}
    	if (definition.isMandatory()) {
    		if (valuesToReplace != null && valuesToReplace.isEmpty()) {
    			throw new SchemaException("Attempt to clear all values of a mandatory item "+getPath() +
    					(contextDescription == null ? "" : " in "+contextDescription));
    		}
    	}
    }

	public static void checkConsistence(Collection<? extends ItemDelta> deltas) {
		checkConsistence(deltas, false, false);
	}
	
	public static void checkConsistence(Collection<? extends ItemDelta> deltas, boolean requireDefinition, boolean prohibitRaw) {
		for (ItemDelta<?> delta : deltas) {
			delta.checkConsistence(requireDefinition, prohibitRaw);
		}
	}
	
	public void checkConsistence() {
		checkConsistence(false, false);
	}

	public void checkConsistence(boolean requireDefinition, boolean prohibitRaw) {
		if (parentPath == null) {
			throw new IllegalStateException("Null parent path in " + this);
		}
		if (requireDefinition && definition == null) {
			throw new IllegalStateException("Null definition in "+this);
		}
		if (valuesToReplace != null && (valuesToAdd != null || valuesToDelete != null)) {
			throw new IllegalStateException(
					"The delta cannot be both 'replace' and 'add/delete' at the same time");
		}
		assertSetConsistence(valuesToReplace, "replace", requireDefinition, prohibitRaw);
		assertSetConsistence(valuesToAdd, "add", requireDefinition, prohibitRaw);
		assertSetConsistence(valuesToDelete, "delete", requireDefinition, prohibitRaw);
	}

	private void assertSetConsistence(Collection<V> values, String type, boolean requireDefinitions, boolean prohibitRaw) {
		if (values == null) {
			return;
		}
		// This may be not be 100% correct but we can tolerate it now
		// if (values.isEmpty()) {
		// throw new
		// IllegalStateException("The "+type+" values set in "+this+" is not-null but it is empty");
		// }
		for (V val : values) {
			if (val == null) {
				throw new IllegalStateException("Null value in the " + type + " values set in " + this);
			}
			if (val.getParent() != this) {
				throw new IllegalStateException("Wrong parent for " + val + " in " + type + " values set in " + this + ": " + val.getParent());
			}
			val.checkConsistenceInternal(this, requireDefinitions, prohibitRaw);
		}
	}

	/**
	 * Distributes the replace values of this delta to add and delete with
	 * respect to provided existing values.
	 */
	public void distributeReplace(Collection<V> existingValues) {
		Collection<V> origValuesToReplace = getValuesToReplace();
		// We have to clear before we distribute, otherwise there will be replace/add or replace/delete conflict
		clearValuesToReplace();
		if (existingValues != null) {
			for (V existingVal : existingValues) {
				if (!isIn(origValuesToReplace, existingVal)) {
					addValueToDelete((V) existingVal.clone());
				}
			}
		}
		for (V replaceVal : origValuesToReplace) {
			if (!isIn(existingValues, replaceVal) && !isIn(getValuesToAdd(), replaceVal)) {
				addValueToAdd((V) replaceVal.clone());
			}
		}
	}

	private boolean isIn(Collection<V> values, V val) {
		if (values == null) {
			return false;
		}
		for (V v : values) {
			if (v.equalsRealValue(val)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Merge specified delta to this delta. This delta is assumed to be
	 * chronologically earlier, delta provided in the parameter is chronilogically later.
	 */
	public void merge(ItemDelta<V> deltaToMerge) {
		if (deltaToMerge.isEmpty()) {
			return;
		}
		if (deltaToMerge.valuesToReplace != null) {
			mergeValuesToReplace(PrismValue.cloneValues(deltaToMerge.valuesToReplace));
		} else {
			if (deltaToMerge.valuesToAdd != null) {
				mergeValuesToAdd(PrismValue.cloneValues(deltaToMerge.valuesToAdd));
			}
			if (deltaToMerge.valuesToDelete != null) {
				mergeValuesToDelete(PrismValue.cloneValues(deltaToMerge.valuesToDelete));
			} 
		}
		// We do not want to clean up the sets during merging (e.g. in removeValue methods) because the set
		// may become empty and the a values may be added later. So just clean it up when all is done.
		removeEmptySets();
	}
	
	private void removeEmptySets() {
		if (valuesToReplace != null && valuesToReplace.isEmpty()) {
			valuesToReplace = null;
		}
		if (valuesToAdd != null && valuesToAdd.isEmpty()) {
			valuesToAdd = null;
		}
		if (valuesToDelete != null && valuesToDelete.isEmpty()) {
			valuesToDelete = null;
		}
	}

	/**
	 * Transforms the delta to the simplest (and safest) form. E.g. it will transform add delta for
	 * single-value properties to replace delta. 
	 */
	public void simplify() {
		ItemDefinition itemDefinition = getDefinition();
		if (itemDefinition == null) {
			throw new IllegalStateException("Attempt to simplify delta without a definition");
		}
		if (itemDefinition.isSingleValue() && isAdd()) {
			valuesToReplace = valuesToAdd;
			valuesToAdd = null;
			valuesToDelete = null;
		}
	}

	/**
	 * Apply this delta (path) to a property container.
	 */
	public void applyTo(PrismContainer<?> propertyContainer) throws SchemaException {
		ItemPath itemPath = getPath();
		Item<?> item = propertyContainer.findOrCreateItem(itemPath, getItemClass(), getDefinition());
		if (item == null) {
			throw new IllegalStateException("Cannot apply delta because cannot find item "+itemPath+" in "+propertyContainer);
		}
		applyTo(item);
		cleanupAllTheWayUp(item);
	}
	
	private void cleanupAllTheWayUp(Item<?> item) {
		if (item.isEmpty()) {
			PrismValue itemParent = item.getParent();
			if (itemParent != null) {
				if (itemParent instanceof PrismContainerValue<?>) {
					((PrismContainerValue<?>)itemParent).remove(item);
					if (itemParent.isEmpty()) {
						Itemable itemGrandparent = itemParent.getParent();
						if (itemGrandparent != null) {
							if (itemGrandparent instanceof Item<?>) {
								cleanupAllTheWayUp((Item<?>)itemGrandparent);
							}
						}
					}
				}
			}
		}
	}

	public static void applyTo(Collection<? extends ItemDelta> deltas, PrismContainer propertyContainer)
			throws SchemaException {
		for (ItemDelta delta : deltas) {
			delta.applyTo(propertyContainer);
		}
	}
	
	/**
	 * Apply this delta (path) to a property container.
	 */
	public void applyTo(PrismContainerValue<?> propertyContainerVal) throws SchemaException {
		ItemPath itemPath = getPath();
		Item<?> item = propertyContainerVal.findOrCreateItem(itemPath, getItemClass(), getDefinition());
		if (item == null) {
			throw new IllegalStateException("Cannot apply delta because cannot find item "+itemPath+" in "+propertyContainerVal);
		}
		applyTo(item);
		if (item.isEmpty()) {
			propertyContainerVal.remove(item);
		}
	}

	/**
	 * Apply this delta (path) to a property.
	 */
	public void applyTo(Item item) throws SchemaException {
		if (item.getDefinition() == null && getDefinition() != null){
			item.applyDefinition(getDefinition());
		}
		if (valuesToReplace != null) {
			item.replaceAll(PrismValue.cloneCollection(valuesToReplace));
			return;
		}
		if (valuesToAdd != null) {
			if (item.getDefinition() != null && item.getDefinition().isSingleValue()) {
				item.replaceAll(PrismValue.cloneCollection(valuesToAdd));
			} else {
                for (V valueToAdd : valuesToAdd) {
                    if (!item.containsEquivalentValue(valueToAdd)) {
                        item.add(valueToAdd.clone());
                    }
                }
			}
		}
		if (valuesToDelete != null) {
			item.removeAll(valuesToDelete);
		}
		
	}
	
	public static void accept(Collection<? extends ItemDelta> modifications, Visitor visitor, ItemPath path,
			boolean recursive) {
		for (ItemDelta modification: modifications) {
			ItemPath modPath = modification.getPath();
			CompareResult rel = modPath.compareComplex(path);
			if (rel == CompareResult.EQUIVALENT) {
				modification.accept(visitor, null, recursive);
			} else if (rel == CompareResult.SUBPATH) {
				modification.accept(visitor, path.substract(modPath), recursive);
			}
		}
	}

	
	public <I extends Item> I computeChangedItem(I oldItem) throws SchemaException {
		if (isEmpty()) {
			return oldItem;
		}
		if (oldItem == null) {
			// Instantiate empty item
			oldItem = (I) getDefinition().instantiate();
		}
		applyTo(oldItem);
		return oldItem;
	}

	/**
	 * Returns the "new" state of the property - the state that would be after
	 * the delta is applied.
	 */
	public Item<V> getItemNew() throws SchemaException {
		return getItemNew(null);
	}
	
	/**
	 * Returns the "new" state of the property - the state that would be after
	 * the delta is applied.
	 */
	public Item<V> getItemNew(Item<V> itemOld) throws SchemaException {
		if (definition == null) {
			throw new IllegalStateException("No definition in "+this);
		}
		if (itemOld == null) {
			if (isEmpty()) {
				return null;
			}
			itemOld = definition.instantiate(getElementName());
		}
		Item<V> itemNew = itemOld.clone();
		applyTo(itemNew);
		return itemNew;
	}
	
	/**
	 * Returns true if the other delta is a complete subset of this delta.
	 * I.e. if all the statements of the other delta are already contained
	 * in this delta. As a consequence it also returns true if the two
	 * deltas are equal. 
	 */
	public boolean contains(ItemDelta<V> other) {
		if (!this.getPath().equivalent(other.getPath())) {
			return false;
		}
		if (!containsSet(this.valuesToAdd, other.valuesToAdd)) {
			return false;
		}
		if (!containsSet(this.valuesToDelete, other.valuesToDelete)) {
			return false;
		}
		if (!containsSet(this.valuesToReplace, other.valuesToReplace)) {
			return false;
		}
		return true;
	}

	private boolean containsSet(Collection<V> thisSet, Collection<V> otherSet) {
		if (thisSet == null && otherSet == null) {
			return true;
		}
		if (otherSet == null) {
			return true;
		}
		if (thisSet == null) {
			return false;
		}
		return thisSet.containsAll(otherSet);
	}

	public abstract ItemDelta<V> clone();

	protected void copyValues(ItemDelta<V> clone) {
		clone.definition = this.definition;
		clone.elementName = this.elementName;
		clone.parentPath = this.parentPath;
		clone.valuesToAdd = cloneSet(clone, this.valuesToAdd);
		clone.valuesToDelete = cloneSet(clone, this.valuesToDelete);
		clone.valuesToReplace = cloneSet(clone, this.valuesToReplace);
	}

	private Collection<V> cloneSet(ItemDelta clone, Collection<V> thisSet) {
		if (thisSet == null) {
			return null;
		}
		Collection<V> clonedSet = newValueCollection();
		for (V thisVal : thisSet) {
			V clonedVal = (V) thisVal.clone();
			clonedVal.setParent(clone);
			clonedSet.add(clonedVal);
		}
		return clonedSet;
	}

	@Deprecated
	public static <T extends PrismValue> PrismValueDeltaSetTriple<T> toDeltaSetTriple(Item<T> item, ItemDelta<T> delta, 
			boolean oldValuesValid, boolean newValuesValid) {
		if (item == null && delta == null) {
			return null;
		}
		if (!oldValuesValid && !newValuesValid) {
			return null;
		}
		if (oldValuesValid && !newValuesValid) {
			// There were values but they no longer are -> everything to minus set
			PrismValueDeltaSetTriple<T> triple = new PrismValueDeltaSetTriple<T>();
			if (item != null) {
				triple.addAllToMinusSet(item.getValues());
			}
			return triple;
		}
		if (item == null && delta != null) {
			return delta.toDeltaSetTriple(item);
		}
		if (delta == null || (!oldValuesValid && newValuesValid)) {
			PrismValueDeltaSetTriple<T> triple = new PrismValueDeltaSetTriple<T>();
			if (item != null) {
				triple.addAllToZeroSet(item.getValues());
			}
			return triple;
		}
		return delta.toDeltaSetTriple(item);
	}
	
	public static <T extends PrismValue> PrismValueDeltaSetTriple<T> toDeltaSetTriple(Item<T> item, ItemDelta<T> delta) {
		if (item == null && delta == null) {
			return null;
		}
		if (delta == null) {
			PrismValueDeltaSetTriple<T> triple = new PrismValueDeltaSetTriple<T>();
			triple.addAllToZeroSet(PrismValue.cloneCollection(item.getValues()));
			return triple;
		}
		return delta.toDeltaSetTriple(item);
	}
	
	public PrismValueDeltaSetTriple<V> toDeltaSetTriple() {
		return toDeltaSetTriple(null);
	}
	
	public PrismValueDeltaSetTriple<V> toDeltaSetTriple(Item<V> itemOld) {
		PrismValueDeltaSetTriple<V> triple = new PrismValueDeltaSetTriple<V>();
		if (isReplace()) {
			triple.getPlusSet().addAll(PrismValue.cloneCollection(getValuesToReplace()));
			if (itemOld != null) {
				triple.getMinusSet().addAll(PrismValue.cloneCollection(itemOld.getValues()));
			}
			return triple;
		}
		if (isAdd()) {
			triple.getPlusSet().addAll(PrismValue.cloneCollection(getValuesToAdd()));
		}
		if (isDelete()) {
			triple.getMinusSet().addAll(PrismValue.cloneCollection(getValuesToDelete()));
		}
		if (itemOld != null && itemOld.getValues() != null) {
			for (V itemVal: itemOld.getValues()) {
				if (!PrismValue.containsRealValue(valuesToDelete, itemVal) && !PrismValue.containsRealValue(valuesToAdd, itemVal)) {
					triple.getZeroSet().add((V) itemVal.clone());
				}
			}
		}
		return triple;
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
		assertDefinitions(tolarateRawValues, valuesToAdd, "values to add in "+sourceDescription);
		assertDefinitions(tolarateRawValues, valuesToReplace, "values to replace in "+sourceDescription);
		assertDefinitions(tolarateRawValues, valuesToDelete, "values to delete in "+sourceDescription);
	}

	private void assertDefinitions(boolean tolarateRawValues, Collection<V> values, String sourceDescription) throws SchemaException {
		if (values == null) {
			return;
		}
		for(V val: values) {
			if (val instanceof PrismContainerValue<?>) {
				PrismContainerValue<?> cval = (PrismContainerValue<?>)val;
				if (cval.getItems() == null){
					continue;
				}
				for (Item<?> item: cval.getItems()) {
					item.assertDefinitions(tolarateRawValues, cval.toString()+" in "+sourceDescription);
				}
			}
		}
	}
	
	public boolean isRaw() {
		Boolean isRaw = MiscUtil.and(isRawSet(valuesToAdd), isRawSet(valuesToReplace), isRawSet(valuesToDelete));
		if (isRaw == null) {
			return false;
		}
		return isRaw;
	}

	private Boolean isRawSet(Collection<V> set) {
		if (set == null) {
			return null;
		}
		for (V val: set) {
			if (!val.isRaw()) {
				return false;
			}
		}
		return true;
	}
	
	public void revive(PrismContext prismContext) throws SchemaException {
        this.prismContext = prismContext;
		reviveSet(valuesToAdd, prismContext);
        reviveSet(valuesToDelete, prismContext);
        reviveSet(valuesToReplace, prismContext);
	}

	private void reviveSet(Collection<V> set, PrismContext prismContext) throws SchemaException {
		if (set == null) {
			return;
		}
		for (V val: set) {
            val.revive(prismContext);
		}
	}
	
	public void applyDefinition(ItemDefinition itemDefinition, boolean force) throws SchemaException {
		if (this.definition != null && !force) {
			return;
		}
		this.definition = itemDefinition;
		applyDefinitionSet(valuesToAdd, itemDefinition, force);
        applyDefinitionSet(valuesToReplace, itemDefinition, force);
        applyDefinitionSet(valuesToDelete, itemDefinition, force);
	}

	private void applyDefinitionSet(Collection<V> set, ItemDefinition itemDefinition, boolean force) throws SchemaException {
		if (set == null) {
			return;
		}
		for (V val: set) {
			val.applyDefinition(itemDefinition, force);
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((elementName == null) ? 0 : elementName.hashCode());
        // if equals uses parentPath.equivalent call, we should not use default implementation of parentPath.hashCode
		//result = prime * result + ((parentPath == null) ? 0 : parentPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemDelta other = (ItemDelta) obj;
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
		if (parentPath == null) {
			if (other.parentPath != null)
				return false;
		} else if (!parentPath.equivalent(other.parentPath))                    // or "equals" ?
			return false;
		if (!equalsSetRealValue(this.valuesToAdd, other.valuesToAdd))
			return false;
		if (!equalsSetRealValue(this.valuesToDelete, other.valuesToDelete))
			return false;
		if (!equalsSetRealValue(this.valuesToReplace, other.valuesToReplace))
			return false;
		return true;
	}

	private boolean equalsSetRealValue(Collection<V> thisValue, Collection<V> otherValues) {
		Comparator<?> comparator = new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1 instanceof PrismValue && o2 instanceof PrismValue) {
					PrismValue v1 = (PrismValue)o1;
					PrismValue v2 = (PrismValue)o2;
					return v1.equalsRealValue(v2) ? 0 : 1;
				} else {
					return -1;
				}
			}
		};
		return MiscUtil.unorderedCollectionEquals(thisValue, otherValues, comparator);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append("(");
		sb.append(parentPath).append(" / ").append(PrettyPrinter.prettyPrint(elementName));
		if (valuesToReplace != null) {
			sb.append(", REPLACE");
		}

		if (valuesToAdd != null) {
			sb.append(", ADD");
		}

		if (valuesToDelete != null) {
			sb.append(", DELETE");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		if (DebugUtil.isDetailedDebugDump()) {
			sb.append(getClass().getSimpleName()).append(":");
		}
		ItemPath path = getPath();
		sb.append(path);
		
		if (definition != null && DebugUtil.isDetailedDebugDump()) {
			sb.append(" def");
		}

		if (valuesToReplace != null) {
			sb.append("\n");
			dumpValues(sb, "REPLACE", valuesToReplace, indent + 1);
		}

		if (valuesToAdd != null) {
			sb.append("\n");
			dumpValues(sb, "ADD", valuesToAdd, indent + 1);
		}

		if (valuesToDelete != null) {
			sb.append("\n");
			dumpValues(sb, "DELETE", valuesToDelete, indent + 1);
		}

		return sb.toString();

	}

	protected void dumpValues(StringBuilder sb, String label, Collection<V> values, int indent) {
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(label).append(": ");
		if (values == null) {
			sb.append("(null)");
		} else {
			if (DebugUtil.isDetailedDebugDump()) {
				for (V value: values) {
					sb.append("\n");
					sb.append(value.debugDump(indent + 1));
				}
			} else {
				Iterator<V> i = values.iterator();
				while (i.hasNext()) {
					V value = i.next();
					sb.append(value.toHumanReadableString());
					if (i.hasNext()) {
						sb.append(", ");
					}
				}
			}
		}
	}

}
