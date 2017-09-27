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
package com.evolveum.midpoint.prism.delta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Foreachable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author Radovan Semancik
 *
 */
public abstract class ItemDelta<V extends PrismValue,D extends ItemDefinition> implements Itemable, DebugDumpable, Visitable, PathVisitable, Foreachable<V>, Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(ItemDelta.class);

	/**
	 * Name of the property
	 */
	protected QName elementName;
	/**
	 * Parent path of the property (path to the property container)
	 */
	protected ItemPath parentPath;
	protected D definition;

	protected Collection<V> valuesToReplace = null;
	protected Collection<V> valuesToAdd = null;
	protected Collection<V> valuesToDelete = null;
	protected Collection<V> estimatedOldValues = null;

    transient private PrismContext prismContext;

	protected ItemDelta(D itemDefinition, PrismContext prismContext) {
		if (itemDefinition == null) {
			throw new IllegalArgumentException("Attempt to create item delta without a definition");
		}
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;
		this.elementName = itemDefinition.getName();
		this.parentPath = ItemPath.EMPTY_PATH;
		this.definition = itemDefinition;
	}

	protected ItemDelta(QName elementName, D itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;
		this.elementName = elementName;
		this.parentPath = ItemPath.EMPTY_PATH;
		this.definition = itemDefinition;
    }

	protected ItemDelta(ItemPath parentPath, QName elementName, D itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
		ItemPath.checkNoSpecialSymbols(parentPath);
        this.prismContext = prismContext;
		this.elementName = elementName;
		this.parentPath = parentPath;
		this.definition = itemDefinition;
    }

	protected ItemDelta(ItemPath path, D itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
		ItemPath.checkNoSpecialSymbols(path);
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
		if (getParentPath() == null) {
			throw new IllegalStateException("No parent path in "+this);
		}
		return getParentPath().subPath(elementName);
	}

	public D getDefinition() {
		return definition;
	}

	public void setDefinition(D definition) {
		this.definition = definition;
	}

	@Override
	public void accept(Visitor visitor) {
		accept(visitor, true);
	}

	public void accept(Visitor visitor, boolean includeOldValues) {
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
		if (includeOldValues && getEstimatedOldValues() != null) {
			for (V pval : getEstimatedOldValues()) {
				pval.accept(visitor);
			}
		}
	}

	public int size() {
		return sizeSet(valuesToReplace) + sizeSet(valuesToAdd) + sizeSet(valuesToDelete);
	}

	private int sizeSet(Collection<V> set) {
		if (set == null) {
			return 0;
		} else {
			return set.size();
		}
	}

	// TODO think if estimated old values have to be visited as well
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

	public void applyDefinition(D definition) throws SchemaException {
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

	public static void applyDefinitionIfPresent(Collection<? extends ItemDelta> deltas,
			PrismObjectDefinition definition, boolean tolerateNoDefinition) throws SchemaException {
		for (ItemDelta itemDelta : deltas) {
			ItemPath path = itemDelta.getPath();
			ItemDefinition itemDefinition = definition.findItemDefinition(path, ItemDefinition.class);
            if (itemDefinition != null) {
				itemDelta.applyDefinition(itemDefinition);
			} else if (!tolerateNoDefinition) {
				throw new SchemaException("Object type " + definition.getTypeName() + " doesn't contain definition for path " + new ItemPathHolder(path).getXPathWithDeclarations());
			}
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
		if (newValues == null) {
			return;
		}
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

	public boolean removeValueToAdd(PrismValue valueToRemove) {
		return removeValue(valueToRemove, valuesToAdd, false);
	}

	public boolean removeValueToDelete(PrismValue valueToRemove) {
		return removeValue(valueToRemove, valuesToDelete, true);
	}

	public boolean removeValueToReplace(PrismValue valueToRemove) {
		return removeValue(valueToRemove, valuesToReplace, false);
	}

	private boolean removeValue(PrismValue valueToRemove, Collection<V> set, boolean toDelete) {
		boolean removed = false;
		if (set == null) {
			return false;
		}
		Iterator<V> valuesIterator = set.iterator();
		while (valuesIterator.hasNext()) {
			V existingValue = valuesIterator.next();
			// TODO either make equalsRealValue return false if both PCVs have IDs and these IDs are different
			// TODO or include a special test condition here; see MID-3828
			if (existingValue.equalsRealValue(valueToRemove)
					|| toDelete && existingValue.representsSameValue(valueToRemove, false)) {		// the same algorithm as when deleting the item value
				valuesIterator.remove();
				removed = true;
			}
		}
		return removed;
	}

	public void mergeValuesToAdd(Collection<V> newValues) {
		if (newValues == null) {
			return;
		}
		for (V val : newValues) {
			mergeValueToAdd(val);
		}
	}

	public void mergeValuesToAdd(V[] newValues) {
		if (newValues == null) {
			return;
		}
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
		if (newValues == null) {
			return;
		}
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
		if (containsEquivalentValue(valuesToDelete, newValue)) {
			return;
		}
		valuesToDelete.add(newValue);
		newValue.setParent(this);
		newValue.recompute();
	}

	protected boolean containsEquivalentValue(Collection<V> collection, V value) {
		if (collection == null) {
			return false;
		}
		for (V colVal: collection) {
			if (isValueEquivalent(colVal, value)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isValueEquivalent(V a, V b) {
		return a.equalsRealValue(b);
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

	public void resetValuesToDelete() {
		valuesToDelete = null;
	}

	public void resetValuesToReplace() {
		valuesToReplace = null;
	}

	public void setValuesToReplace(Collection<V> newValues) {
		if (newValues == null) {
			return;
		}
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
		if (newValue != null) {
			valuesToReplace.add(newValue);
			newValue.setParent(this);
			newValue.recompute();
		}
	}

	public void addValueToReplace(V newValue) {
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
		}
		if (newValue != null) {
			valuesToReplace.add(newValue);
			newValue.setParent(this);
			newValue.recompute();
		}
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
		return valuesToAdd == null && valuesToDelete == null && valuesToReplace == null;
	}

	// TODO merge with isEmpty
	public boolean isInFactEmpty() {
		return CollectionUtils.isEmpty(valuesToAdd) && CollectionUtils.isEmpty(valuesToDelete) && valuesToReplace == null;
	}

	public static boolean isEmpty(ItemDeltaType itemDeltaType) {
		if (itemDeltaType == null) {
			return true;
		}
		if (itemDeltaType.getModificationType() == ModificationTypeType.REPLACE) {
			return false;
		}
		return !itemDeltaType.getValue().isEmpty();
	}

	public boolean addsAnyValue() {
		return hasAnyValue(valuesToAdd) || hasAnyValue(valuesToReplace);
	}

	private boolean hasAnyValue(Collection<V> set) {
		return (set != null && !set.isEmpty());
	}

	public void foreach(Processor<V> processor) {
		foreachSet(processor, valuesToAdd);
		foreachSet(processor, valuesToDelete);
		foreachSet(processor, valuesToReplace);
	}

	private void foreachSet(Processor<V> processor, Collection<V> set) {
		if (set == null) {
			return;
		}
		for (V val: set) {
			processor.process(val);
		}
	}

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
	public Collection<V> getEstimatedOldValues() {
		return estimatedOldValues;
	}

	public void setEstimatedOldValues(Collection<V> estimatedOldValues) {
		this.estimatedOldValues = estimatedOldValues;
	}

	public void addEstimatedOldValues(Collection<V> newValues) {
		for (V val : newValues) {
			addEstimatedOldValue(val);
		}
	}

	public void addEstimatedOldValues(V... newValues) {
		for (V val : newValues) {
			addEstimatedOldValue(val);
		}
	}

	public void addEstimatedOldValue(V newValue) {
		if (estimatedOldValues == null) {
			estimatedOldValues = newValueCollection();
		}
		if (PrismValue.containsRealValue(estimatedOldValues,newValue)) {
			return;
		}
		estimatedOldValues.add(newValue);
		newValue.setParent(this);
		newValue.recompute();
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
		for (V value : set) {
			value.normalize();
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

	public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> deltas, QName propertyName) {
        return findPropertyDelta(deltas, new ItemPath(propertyName));
    }

    public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> deltas, ItemPath parentPath, QName propertyName) {
        return findPropertyDelta(deltas, new ItemPath(parentPath, propertyName));
    }

    public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath) {
    	return findItemDelta(deltas, propertyPath, PropertyDelta.class);
    }

    public static <X extends Containerable> ContainerDelta<X> findContainerDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath) {
    	return findItemDelta(deltas, propertyPath, ContainerDelta.class);
    }

    public static <X extends Containerable> ContainerDelta<X> findContainerDelta(Collection<? extends ItemDelta> deltas, QName name) {
    	return findContainerDelta(deltas, new ItemPath(name));
    }

    public static <DD extends ItemDelta> DD findItemDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath, Class<DD> deltaType) {
        if (deltas == null) {
            return null;
        }
        for (ItemDelta<?,?> delta : deltas) {
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equivalent(propertyPath)) {
                return (DD) delta;
            }
            if ((delta instanceof ContainerDelta<?>) && delta.getPath().isSubPath(propertyPath)) {
            	return (DD) ((ContainerDelta)delta).getSubDelta(propertyPath.substract(delta.getPath()));
            }
        }
        return null;
    }

    public static Collection<? extends ItemDelta<?,?>> findItemDeltasSubPath(Collection<? extends ItemDelta<?,?>> deltas, ItemPath itemPath) {
    	Collection<ItemDelta<?,?>> foundDeltas = new ArrayList<ItemDelta<?,?>>();
        if (deltas == null) {
            return foundDeltas;
        }
        for (ItemDelta<?,?> delta : deltas) {
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
        	ItemDelta<?,?> delta = deltasIterator.next();
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equivalent(propertyPath)) {
                deltasIterator.remove();
            }
        }
    }

    public static <D extends ItemDelta> void removeItemDelta(Collection<? extends ItemDelta> deltas, ItemDelta deltaToRemove) {
        if (deltas == null) {
            return;
        }
        Iterator<? extends ItemDelta> deltasIterator = deltas.iterator();
        while (deltasIterator.hasNext()) {
        	ItemDelta<?,?> delta = deltasIterator.next();
            if (delta.equals(deltaToRemove)) {
                deltasIterator.remove();
            }
        }
    }

    /**
     * Filters out all delta values that are meaningless to apply. E.g. removes all values to add that the property already has,
     * removes all values to delete that the property does not have, etc.
     * Returns null if the delta is not needed at all.
     */
    public ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object) {
    	return narrow(object, null);
    }
	/**
     * Filters out all delta values that are meaningless to apply. E.g. removes all values to add that the property already has,
     * removes all values to delete that the property does not have, etc.
     * Returns null if the delta is not needed at all.
     */
    public ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object, Comparator<V> comparator) {
    	Item<V,D> currentItem = (Item<V,D>) object.findItem(getPath());
    	if (currentItem == null) {
    		if (valuesToDelete != null) {
    			ItemDelta<V,D> clone = clone();
    			clone.valuesToDelete = null;
    			return clone;
    		} else {
    			// Nothing to narrow
    			return this;
    		}
    	} else {
    		if (isReplace()) {
    			// We can narrow replace deltas only if the replace set matches
    			// current item exactly. Otherwise we may lose some values.
    			if (currentItem.valuesExactMatch(valuesToReplace, comparator)) {
    				return null;
    			} else {
    				return this;
    			}
    		} else {
	    		ItemDelta<V,D> clone = clone();
	    		if (clone.valuesToDelete != null) {
	    			Iterator<V> iterator = clone.valuesToDelete.iterator();
	    			while (iterator.hasNext()) {
	    				V valueToDelete = iterator.next();
	    				if (!currentItem.contains(valueToDelete, true, comparator)) {
	    					iterator.remove();
	    				}
	    			}
	    			if (clone.valuesToDelete.isEmpty()) {
	    				clone.valuesToDelete = null;
	    			}
	    		}
	    		if (clone.valuesToAdd != null) {
	    			Iterator<V> iterator = clone.valuesToAdd.iterator();
	    			while (iterator.hasNext()) {
	    				V valueToDelete = iterator.next();
	    				if (currentItem.contains(valueToDelete, true, comparator)) {
	    					iterator.remove();
	    				}
	    			}
	    			if (clone.valuesToAdd.isEmpty()) {
	    				clone.valuesToAdd = null;
	    			}
	    		}
	    		return clone;
    		}
    	}
    }

	/**
	 * Checks if the delta is redundant w.r.t. current state of the object.
	 * I.e. if it changes the current object state.
	 */
	public boolean isRedundant(PrismObject<? extends Objectable> object) {
		Comparator<V> comparator = new Comparator<V>() {
			@Override
			public int compare(V o1, V o2) {
				if (o1.equalsComplex(o2, true, false)) {
					return 0;
				} else {
					return 1;
				}
			}
		};
		return isRedundant(object, comparator);
	}

	public boolean isRedundant(PrismObject<? extends Objectable> object, Comparator<V> comparator) {
		Item<V,D> currentItem = (Item<V,D>) object.findItem(getPath());
		if (currentItem == null) {
			if (valuesToReplace != null) {
				return valuesToReplace.isEmpty();
			}
			return !hasAnyValue(valuesToAdd);
		} else {
			if (valuesToReplace != null) {
				return MiscUtil.unorderedCollectionCompare(valuesToReplace, currentItem.getValues(), comparator);
			}
			ItemDelta<V,D> narrowed = narrow(object, comparator);
			boolean narrowedNotEmpty = narrowed == null || narrowed.hasAnyValue(narrowed.valuesToAdd) || narrowed.hasAnyValue(narrowed.valuesToDelete);
			return narrowedNotEmpty;
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
    					(contextDescription == null ? "" : " in "+contextDescription) + "; values: "+valuesToAdd);
    		}
    		if (valuesToReplace != null && valuesToReplace.size() > 1) {
    			throw new SchemaException("Attempt to replace "+valuesToReplace.size()+" values to a single-valued item "+getPath() +
    					(contextDescription == null ? "" : " in "+contextDescription) + "; values: "+valuesToReplace);
    		}
    	}
    	if (definition.isMandatory()) {
    		if (valuesToReplace != null && valuesToReplace.isEmpty()) {
    			throw new SchemaException("Attempt to clear all values of a mandatory item "+getPath() +
    					(contextDescription == null ? "" : " in "+contextDescription));
    		}
    	}
    }


    public void validateValues(ItemDeltaValidator<V> validator) throws SchemaException {
    	validateValues(validator, getEstimatedOldValues());
    }

    public void validateValues(ItemDeltaValidator<V> validator, Collection<V> oldValues) throws SchemaException {
    	validateSet(valuesToAdd, PlusMinusZero.PLUS, validator);
    	validateSet(valuesToDelete, PlusMinusZero.MINUS, validator);
    	if (isReplace()) {
    		for (V val: getValuesToReplace()) {
    			if (oldValues != null && PrismValue.containsRealValue(oldValues, val)) {
    				validator.validate(PlusMinusZero.ZERO, val);
    			} else {
    				validator.validate(PlusMinusZero.PLUS, val);
    			}
    		}
    		if (oldValues != null) {
    			for (V val: getValuesToReplace()) {
    				if (!PrismValue.containsRealValue(getValuesToReplace(), val)) {
    					validator.validate(PlusMinusZero.MINUS, val);
    				}
    			}
    		}
    	}
    }

    private void validateSet(Collection<V> set, PlusMinusZero plusMinusZero,
			ItemDeltaValidator<V> validator) {
		if (set != null) {
			for (V val: set) {
				validator.validate(plusMinusZero, val);
			}
		}
	}

	public static void checkConsistence(Collection<? extends ItemDelta> deltas) {
        checkConsistence(deltas, ConsistencyCheckScope.THOROUGH);
    }

    public static void checkConsistence(Collection<? extends ItemDelta> deltas, ConsistencyCheckScope scope) {
		checkConsistence(deltas, false, false, scope);
	}

	public static void checkConsistence(Collection<? extends ItemDelta> deltas, boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope) {
		Map<ItemPath,ItemDelta<?,?>> pathMap = new HashMap<>();
		for (ItemDelta<?,?> delta : deltas) {
			delta.checkConsistence(requireDefinition, prohibitRaw, scope);
			int matches = 0;
			for (ItemDelta<?,?> other : deltas) {
				if (other == delta) {
					matches++;
				} else if (other.equals(delta)) {
					throw new IllegalStateException("Duplicate item delta: "+delta+" and "+other);
				}
			}
			if (matches > 1) {
				throw new IllegalStateException("The delta "+delta+" appears multiple times in the modification list");
			}
		}
	}

    public void checkConsistence() {
        checkConsistence(ConsistencyCheckScope.THOROUGH);
    }

	public void checkConsistence(ConsistencyCheckScope scope) {
		checkConsistence(false, false, scope);
	}

	public void checkConsistence(boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope) {
		if (scope.isThorough() && parentPath == null) {
			throw new IllegalStateException("Null parent path in " + this);
		}
		if (scope.isThorough() && requireDefinition && definition == null) {
			throw new IllegalStateException("Null definition in "+this);
		}
		if (scope.isThorough() && valuesToReplace != null && (valuesToAdd != null || valuesToDelete != null)) {
			throw new IllegalStateException(
					"The delta cannot be both 'replace' and 'add/delete' at the same time");
		}
		assertSetConsistence(valuesToReplace, "replace", requireDefinition, prohibitRaw, scope);
		assertSetConsistence(valuesToAdd, "add", requireDefinition, prohibitRaw, scope);
		assertSetConsistence(valuesToDelete, "delete", requireDefinition, prohibitRaw, scope);
	}

	private void assertSetConsistence(Collection<V> values, String type, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
		if (values == null) {
			return;
		}
		// This may be not be 100% correct but we can tolerate it now
		// if (values.isEmpty()) {
		// throw new
		// IllegalStateException("The "+type+" values set in "+this+" is not-null but it is empty");
		// }
		for (V val : values) {
            if (scope.isThorough()) {
                if (val == null) {
                    throw new IllegalStateException("Null value in the " + type + " values set in " + this);
                }
                if (val.getParent() != this) {
                    throw new IllegalStateException("Wrong parent for " + val + " in " + type + " values set in " + this + ": " + val.getParent());
                }
            }
            val.checkConsistenceInternal(this, requireDefinitions, prohibitRaw, scope);
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
	public void merge(ItemDelta<V,D> deltaToMerge) {
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
		// Do not remove replace set, even if it is empty.
		// Empty replace set is not the same as no replace set
		if (valuesToAdd != null && valuesToAdd.isEmpty()) {
			valuesToAdd = null;
		}
		if (valuesToDelete != null && valuesToDelete.isEmpty()) {
			valuesToDelete = null;
		}
	}

	public Collection<V> getValueChanges(PlusMinusZero mode) {
		Collection<V> out = new ArrayList<>();

		if (isReplace()) {
			switch (mode) {
				case PLUS:
					setSubtract(out, valuesToReplace, estimatedOldValues);
					break;
				case MINUS:
					setSubtract(out, estimatedOldValues, valuesToReplace);
					break;
				case ZERO:
					setIntersection(out, valuesToReplace, estimatedOldValues);
			}
		} else {
			switch (mode) {
				case PLUS:
					setAddAll(out, valuesToAdd);
					break;
				case MINUS:
					setAddAll(out, valuesToDelete);
					break;
				case ZERO:
					setAddAll(out, estimatedOldValues);
					break;
			}
		}

		return out;
	}

	private void setSubtract(Collection<V> out, Collection<V> subtrahend, Collection<V> minuend) {
		if (subtrahend == null) {
			return;
		}
		for (V sube: subtrahend) {
			if (minuend == null) {
				out.add(sube);
			} else {
				if (!minuend.contains(sube)) {
					out.add(sube);
				}
			}
		}
	}

	private void setIntersection(Collection<V> out, Collection<V> a, Collection<V> b) {
		if (a == null || b == null) {
			return;
		}
		for (V ae: a) {
			if (b.contains(ae)) {
				out.add(ae);
			}
		}
	}

	private void setAddAll(Collection<V> out, Collection<V> in) {
		if (in != null) {
			out.addAll(in);
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

	private void cleanupAllTheWayUp(Item<?,?> item) {
		if (item.isEmpty()) {
			PrismValue itemParent = item.getParent();
			if (itemParent != null) {
				if (itemParent instanceof PrismContainerValue<?>) {
					((PrismContainerValue<?>)itemParent).remove(item);
					if (itemParent.isEmpty()) {
						Itemable itemGrandparent = itemParent.getParent();
						if (itemGrandparent != null) {
							if (itemGrandparent instanceof Item<?,?>) {
								cleanupAllTheWayUp((Item<?,?>)itemGrandparent);
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

	public static void applyTo(Collection<? extends ItemDelta> deltas, PrismContainerValue propertyContainerValue)
			throws SchemaException {
		for (ItemDelta delta : deltas) {
			delta.applyTo(propertyContainerValue);
		}
	}
	
	public static void applyToMatchingPath(Collection<? extends ItemDelta> deltas, PrismContainer propertyContainer)
			throws SchemaException {
		for (ItemDelta delta : deltas) {
			delta.applyToMatchingPath(propertyContainer);
		}
	}

	public void applyTo(PrismContainerValue containerValue) throws SchemaException {
		ItemPath deltaPath = getPath();
		if (ItemPath.isNullOrEmpty(deltaPath)) {
			throw new IllegalArgumentException("Cannot apply empty-path delta " + this + " directly to a PrismContainerValue " + containerValue);
		}
		Item subItem = containerValue.findOrCreateItem(deltaPath, getItemClass(), getDefinition());
		applyToMatchingPath(subItem);
	}

	public void applyTo(Item item) throws SchemaException {
		ItemPath itemPath = item.getPath();
		ItemPath deltaPath = getPath();
		CompareResult compareComplex = itemPath.compareComplex(deltaPath);
		if (compareComplex == CompareResult.EQUIVALENT) {
			applyToMatchingPath(item);
			cleanupAllTheWayUp(item);
		} else if (compareComplex == CompareResult.SUBPATH) {
			if (item instanceof PrismContainer<?>) {
				PrismContainer<?> container = (PrismContainer<?>)item;
				ItemPath remainderPath = deltaPath.remainder(itemPath);
				Item subItem = container.findOrCreateItem(remainderPath, getItemClass(), getDefinition());
				applyToMatchingPath(subItem);
			} else {
				throw new SchemaException("Cannot apply delta "+this+" to "+item+" as delta path is below the item path and the item is not a container");
			}
		} else if (compareComplex == CompareResult.SUPERPATH) {
			throw new SchemaException("Cannot apply delta "+this+" to "+item+" as delta path is above the item path");
		} else if (compareComplex == CompareResult.NO_RELATION) {
			throw new SchemaException("Cannot apply delta "+this+" to "+item+" as paths do not match");
		}
	}

	/**
	 * Applies delta to item were path of the delta and path of the item matches (skips path checks).
	 */
	public void applyToMatchingPath(Item item) throws SchemaException {
		if (item == null) {
			return;
		}
		if (item.getDefinition() == null && getDefinition() != null){
			item.applyDefinition(getDefinition());
		}
		if (!getItemClass().isAssignableFrom(item.getClass())) {
			throw new SchemaException("Cannot apply delta "+this+" to "+item+" because the deltas is applicable only to "+getItemClass().getSimpleName());
		}
		if (valuesToReplace != null) {
			item.replaceAll(PrismValue.cloneCollection(valuesToReplace));
			// Application of delta might have removed values therefore leaving empty items.
			// Those needs to be cleaned-up (removed) as empty item is not a legal state.
			cleanupAllTheWayUp(item);
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
		// Application of delta might have removed values therefore leaving empty items.
		// Those needs to be cleaned-up (removed) as empty item is not a legal state.
		cleanupAllTheWayUp(item);
	}

	public ItemDelta<?,?> getSubDelta(ItemPath path) {
		return this;
	}

	public boolean isApplicableTo(Item item) {
		if (item == null) {
			return false;
		}
		if (!isApplicableToType(item)) {
			return false;
		}
		// TODO: maybe check path?
		return true;
	}

	protected abstract boolean isApplicableToType(Item item);

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

	/**
	 * Returns the "new" state of the property - the state that would be after
	 * the delta is applied.
	 */
	public Item<V,D> getItemNew() throws SchemaException {
		return getItemNew(null);
	}

	/**
	 * Returns the "new" state of the property - the state that would be after
	 * the delta is applied.
	 */
	public Item<V,D> getItemNew(Item<V,D> itemOld) throws SchemaException {
		if (definition == null) {
			throw new IllegalStateException("No definition in "+this);
		}
		Item<V,D> itemNew;
		if (itemOld == null) {
			if (isEmpty()) {
				return null;
			}
			itemNew = definition.instantiate(getElementName());
		} else {
			itemNew = itemOld.clone();
		}
		applyTo(itemNew);
		return itemNew;
	}

	public Item<V,D> getItemNewMatchingPath(Item<V,D> itemOld) throws SchemaException {
		if (definition == null) {
			throw new IllegalStateException("No definition in "+this);
		}
		Item<V,D> itemNew;
		if (itemOld == null) {
			if (isEmpty()) {
				return null;
			}
			itemNew = definition.instantiate(getElementName());
		} else {
			itemNew = itemOld.clone();
		}
		applyToMatchingPath(itemNew);
		return itemNew;
	}

	/**
	 * Returns true if the other delta is a complete subset of this delta.
	 * I.e. if all the statements of the other delta are already contained
	 * in this delta. As a consequence it also returns true if the two
	 * deltas are equal.
	 */
	public boolean contains(ItemDelta<V,D> other) {
		return contains(other, PrismConstants.EQUALS_DEFAULT_IGNORE_METADATA, PrismConstants.EQUALS_DEFAULT_IS_LITERAL);
	}
	/**
	 * Returns true if the other delta is a complete subset of this delta.
	 * I.e. if all the statements of the other delta are already contained
	 * in this delta. As a consequence it also returns true if the two
	 * deltas are equal.
	 */
	public boolean contains(ItemDelta<V,D> other, boolean ignoreMetadata, boolean isLiteral) {
		if (!this.getPath().equivalent(other.getPath())) {
			return false;
		}
		if (!PrismValue.containsAll(this.valuesToAdd, other.valuesToAdd, ignoreMetadata, isLiteral)) {
			return false;
		}
		if (!PrismValue.containsAll(this.valuesToDelete, other.valuesToDelete, ignoreMetadata, isLiteral)) {
			return false;
		}
		if (!PrismValue.containsAll(this.valuesToReplace, other.valuesToReplace, ignoreMetadata, isLiteral)) {
			return false;
		}
		return true;
	}

	public void filterValues(Function<V, Boolean> function) {
		filterValuesSet(this.valuesToAdd, function);
		filterValuesSet(this.valuesToDelete, function);
		filterValuesSet(this.valuesToReplace, function);
	}

	private void filterValuesSet(Collection<V> set, Function<V, Boolean> function) {
		if (set == null) {
			return;
		}
		Iterator<V> iterator = set.iterator();
		while (iterator.hasNext()) {
			Boolean keep = function.apply(iterator.next());
			if (keep == null || !keep) {
				iterator.remove();
			}
		}
	}

	public abstract ItemDelta<V,D> clone();

	protected void copyValues(ItemDelta<V,D> clone) {
		clone.definition = this.definition;
		clone.elementName = this.elementName;
		clone.parentPath = this.parentPath;
		clone.valuesToAdd = cloneSet(clone, this.valuesToAdd);
		clone.valuesToDelete = cloneSet(clone, this.valuesToDelete);
		clone.valuesToReplace = cloneSet(clone, this.valuesToReplace);
		clone.estimatedOldValues = cloneSet(clone, this.estimatedOldValues);
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

	public static <D extends ItemDelta<?,?>> Collection<D> cloneCollection(Collection<D> orig) {
		if (orig == null) {
			return null;
		}
		Collection<D> clone = new ArrayList<>(orig.size());
		for (D delta: orig) {
			clone.add((D)delta.clone());
		}
		return clone;
	}


	@Deprecated
	public static <IV extends PrismValue,ID extends ItemDefinition> PrismValueDeltaSetTriple<IV> toDeltaSetTriple(Item<IV,ID> item, ItemDelta<IV,ID> delta,
			boolean oldValuesValid, boolean newValuesValid) {
		if (item == null && delta == null) {
			return null;
		}
		if (!oldValuesValid && !newValuesValid) {
			return null;
		}
		if (oldValuesValid && !newValuesValid) {
			// There were values but they no longer are -> everything to minus set
			PrismValueDeltaSetTriple<IV> triple = new PrismValueDeltaSetTriple<IV>();
			if (item != null) {
				triple.addAllToMinusSet(item.getValues());
			}
			return triple;
		}
		if (item == null && delta != null) {
			return delta.toDeltaSetTriple(item);
		}
		if (delta == null || (!oldValuesValid && newValuesValid)) {
			PrismValueDeltaSetTriple<IV> triple = new PrismValueDeltaSetTriple<IV>();
			if (item != null) {
				triple.addAllToZeroSet(item.getValues());
			}
			return triple;
		}
		return delta.toDeltaSetTriple(item);
	}

	public static <IV extends PrismValue,ID extends ItemDefinition> PrismValueDeltaSetTriple<IV> toDeltaSetTriple(Item<IV,ID> item, ItemDelta<IV,ID> delta) {
		if (item == null && delta == null) {
			return null;
		}
		if (delta == null) {
			PrismValueDeltaSetTriple<IV> triple = new PrismValueDeltaSetTriple<IV>();
			triple.addAllToZeroSet(PrismValue.cloneCollection(item.getValues()));
			return triple;
		}
		return delta.toDeltaSetTriple(item);
	}

	public PrismValueDeltaSetTriple<V> toDeltaSetTriple() {
		return toDeltaSetTriple(null);
	}

	public PrismValueDeltaSetTriple<V> toDeltaSetTriple(Item<V,D> itemOld) {
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
				for (Item<?,?> item: cval.getItems()) {
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

	public void applyDefinition(D itemDefinition, boolean force) throws SchemaException {
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

	/**
	 * Deltas are equivalent if they have the same result when
	 * applied to an object. I.e. meta-data and other "decorations"
	 * such as old values are not considered in this comparison.
	 */
	public boolean equivalent(ItemDelta other) {
		if (elementName == null) {
			if (other.elementName != null)
				return false;
		} else if (!QNameUtil.match(elementName, elementName))
			return false;
		if (parentPath == null) {
			if (other.parentPath != null)
				return false;
		} else if (!parentPath.equivalent(other.parentPath))
			return false;
		if (!equivalentSetRealValue(this.valuesToAdd, other.valuesToAdd, false))
			return false;
		if (!equivalentSetRealValue(this.valuesToDelete, other.valuesToDelete, true))
			return false;
		if (!equivalentSetRealValue(this.valuesToReplace, other.valuesToReplace, false))
			return false;
		return true;
	}

	public static boolean hasEquivalent(Collection<? extends ItemDelta> col, ItemDelta delta) {
		for (ItemDelta colItem: col) {
			if (colItem.equivalent(delta)) {
				return true;
			}
		}
		return false;
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
		if (!equivalentSetRealValue(this.valuesToAdd, other.valuesToAdd, false))
			return false;
		if (!equivalentSetRealValue(this.valuesToDelete, other.valuesToDelete, true))		// TODO ok?
			return false;
		if (!equivalentSetRealValue(this.valuesToReplace, other.valuesToReplace, false))
			return false;
		if (!equivalentSetRealValue(this.estimatedOldValues, other.estimatedOldValues, false))
			return false;
		return true;
	}

	private boolean equivalentSetRealValue(Collection<V> thisValue, Collection<V> otherValues, boolean isDelete) {
		return MiscUtil.unorderedCollectionEquals(thisValue, otherValues,
				(v1, v2) -> {
					if (v1 != null && v2 != null) {
						if (!isDelete || !(v1 instanceof PrismContainerValue) || !(v2 instanceof PrismContainerValue)) {
							// Here it is questionable if we should consider adding "assignment id=1 (A)" and "assignment id=2 (A)"
							// - i.e. assignments with the same real value but different identifiers - the same delta.
							// Historically, we considered it as such. But the question is if it's correct.
							return v1.equalsRealValue(v2);
						} else {
							// But for container values to be deleted, they can be referred to either using IDs or values.
							// If content is used - but no IDs - the content must be equal.
							// If IDs are used - and are the same - the content is irrelevant.
							// The problem is if one side has content with ID, and the other has the same content without ID.
							// This might have the same or different effect, depending on the content it is applied to.
							// See MID-3828
							return (v1.equalsRealValue(v2) && !differentIds(v1, v2)) || v1.representsSameValue(v2, false);
						}
					} else {
						return false;
					}
				});
	}

	private boolean differentIds(PrismValue v1, PrismValue v2) {
		Long id1 = v1 instanceof PrismContainerValue ? ((PrismContainerValue) v1).getId() : null;
		Long id2 = v2 instanceof PrismContainerValue ? ((PrismContainerValue) v2).getId() : null;
		return id1 != null && id2 != null && id1.longValue() != id2.longValue();
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

		if (estimatedOldValues != null) {
			sb.append("\n");
			dumpValues(sb, "OLD", estimatedOldValues, indent + 1);
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

	public static void addAll(Collection<? extends ItemDelta> modifications, Collection<? extends ItemDelta> deltasToAdd) {
		if (deltasToAdd == null) {
			return;
		}
		for (ItemDelta deltaToAdd: deltasToAdd) {
			if (!modifications.contains(deltaToAdd)) {
				((Collection)modifications).add(deltaToAdd);
			}
		}
	}

	public static void merge(Collection<? extends ItemDelta> modifications, ItemDelta delta) {
		for (ItemDelta modification: modifications) {
			if (modification.getPath().equals(delta.getPath())) {
				modification.merge(delta);
				return;
			}
		}
		((Collection)modifications).add(delta);
	}

	public static void mergeAll(Collection<? extends ItemDelta> modifications, Collection<? extends ItemDelta> deltasToMerge) {
		if (deltasToMerge == null) {
			return;
		}
		for (ItemDelta deltaToMerge: deltasToMerge) {
			merge(modifications, deltaToMerge);
		}
	}

	public void addToReplaceDelta() {
		if (isReplace()) {
			throw new IllegalStateException("Delta is a REPLACE delta, not an ADD one");
		}
		valuesToReplace = valuesToAdd;
		if (valuesToReplace == null) {
			valuesToReplace = new ArrayList<>(0);
		}
		valuesToAdd = null;
	}

	public ItemDelta<V,D> createReverseDelta() {
		ItemDelta<V,D> reverseDelta = clone();
		Collection<V> cloneValuesToAdd = reverseDelta.valuesToAdd;
		Collection<V> cloneValuesToDelete = reverseDelta.valuesToDelete;
		Collection<V> cloneValuesToReplace = reverseDelta.valuesToReplace;
		Collection<V> cloneEstimatedOldValues = reverseDelta.estimatedOldValues;

		reverseDelta.valuesToAdd = cloneValuesToDelete;
		reverseDelta.valuesToDelete = cloneValuesToAdd;
		if (cloneValuesToReplace != null) {
			reverseDelta.valuesToReplace = cloneEstimatedOldValues;
			if (reverseDelta.valuesToReplace == null) {
				// We want to force replace delta here. Otherwise the reverse delta
				// may look like empty. We do not explicitly have old values here,
				// so this is a bit tricky and not entirely correct. But we can live
				// with that for now.
				reverseDelta.valuesToReplace = new ArrayList<>(0);
			}
			reverseDelta.estimatedOldValues = cloneValuesToReplace;
		} else {
			// TODO: what about estimatedOldValues here?
		}

		return reverseDelta;
	}

	public V findValueToAddOrReplace(V value) {
		V found = findValue(valuesToAdd, value);
		if (found == null) {
			found = findValue(valuesToReplace, value);
		}
		return found;
	}

	private V findValue(Collection<V> values, V value) {
		if (values == null) {
			return null;
		}
		return values.stream()
				.filter(v -> !differentIds(v, value) && v.equals(value, true))
				.findFirst().orElse(null);
	}

	/**
	 * Set origin type to all values and subvalues
	 */
	public void setOriginTypeRecursive(final OriginType originType) {
		accept((visitable) -> {
			if (visitable instanceof PrismValue) {
				((PrismValue)visitable).setOriginType(originType);
			}
		});
	}
}
