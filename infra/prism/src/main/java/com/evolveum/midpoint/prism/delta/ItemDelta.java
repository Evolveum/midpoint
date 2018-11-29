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
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Foreachable;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Radovan Semancik
 *
 */
public interface ItemDelta<V extends PrismValue,D extends ItemDefinition> extends Itemable, DebugDumpable, Visitable, PathVisitable, Foreachable<V>, Serializable {

	ItemName getElementName();

	void setElementName(QName elementName);

	UniformItemPath getParentPath();

	void setParentPath(ItemPath parentPath);

	@NotNull
	@Override
	UniformItemPath getPath();

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

	static void applyDefinitionIfPresent(Collection<? extends ItemDelta> deltas,
			PrismObjectDefinition definition, boolean tolerateNoDefinition) throws SchemaException {
		for (ItemDelta itemDelta : deltas) {
			UniformItemPath path = itemDelta.getPath();
			ItemDefinition itemDefinition = ((ItemDefinition) definition).findItemDefinition(path, ItemDefinition.class);
            if (itemDefinition != null) {
				itemDelta.applyDefinition(itemDefinition);
			} else if (!tolerateNoDefinition) {
				throw new SchemaException("Object type " + definition.getTypeName() + " doesn't contain definition for path " + path.serializeWithDeclarations());
			}
		}
	}

	boolean hasCompleteDefinition();

	PrismContext getPrismContext();

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

	static boolean isEmpty(ItemDeltaType itemDeltaType) {
		if (itemDeltaType == null) {
			return true;
		}
		if (itemDeltaType.getModificationType() == ModificationTypeType.REPLACE) {
			return false;
		}
		return !itemDeltaType.getValue().isEmpty();
	}

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

    static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath) {
    	return findItemDelta(deltas, propertyPath, PropertyDelta.class, false);
    }

    static <X extends Containerable> ContainerDelta<X> findContainerDelta(Collection<? extends ItemDelta> deltas,
		    UniformItemPath propertyPath) {
    	return findItemDelta(deltas, propertyPath, ContainerDelta.class, false);
    }

    // 'strict' means we avoid returning deltas that only partially match. This is NOT a definite solution, see MID-4689
    static <DD extends ItemDelta> DD findItemDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath,
		    Class<DD> deltaType, boolean strict) {
        if (deltas == null) {
            return null;
        }
        for (ItemDelta<?,?> delta : deltas) {
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equivalent(propertyPath)) {
                return (DD) delta;
            }
            // e.g. when deleting credentials we match also deletion of credentials/password (is that correct?)
            if (!strict && delta instanceof ContainerDelta<?> && delta.getPath().isSubPath(propertyPath)) {
	            //noinspection unchecked
	            return (DD) ((ContainerDelta)delta).getSubDelta(UniformItemPathImpl.fromItemPath(propertyPath).remainder(delta.getPath()));  // todo
            }
        }
        return null;
    }

    static Collection<? extends ItemDelta<?,?>> findItemDeltasSubPath(Collection<? extends ItemDelta<?, ?>> deltas,
		    ItemPath itemPath) {
    	Collection<ItemDelta<?,?>> foundDeltas = new ArrayList<>();
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

    static <D extends ItemDelta> void removeItemDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath,
		    Class<D> deltaType) {
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

    static <D extends ItemDelta> void removeItemDelta(Collection<? extends ItemDelta> deltas, ItemDelta deltaToRemove) {
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
    ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object);

	/**
     * Filters out all delta values that are meaningless to apply. E.g. removes all values to add that the property already has,
     * removes all values to delete that the property does not have, etc.
     * Returns null if the delta is not needed at all.
     */
	ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object, Comparator<V> comparator);

	/**
	 * Checks if the delta is redundant w.r.t. current state of the object.
	 * I.e. if it changes the current object state.
	 */
	boolean isRedundant(PrismObject<? extends Objectable> object);

	boolean isRedundant(PrismObject<? extends Objectable> object, Comparator<V> comparator);

	void validate() throws SchemaException;

	void validate(String contextDescription) throws SchemaException;

	void validateValues(ItemDeltaValidator<V> validator) throws SchemaException;

	void validateValues(ItemDeltaValidator<V> validator, Collection<V> oldValues) throws SchemaException;

	static void checkConsistence(Collection<? extends ItemDelta> deltas) {
        checkConsistence(deltas, ConsistencyCheckScope.THOROUGH);
    }

    static void checkConsistence(Collection<? extends ItemDelta> deltas, ConsistencyCheckScope scope) {
		checkConsistence(deltas, false, false, scope);
	}

	static void checkConsistence(Collection<? extends ItemDelta> deltas, boolean requireDefinition, boolean prohibitRaw,
			ConsistencyCheckScope scope) {
		Map<UniformItemPath,ItemDelta<?,?>> pathMap = new HashMap<>();
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

	static void applyTo(Collection<? extends ItemDelta> deltas, PrismContainer propertyContainer)
			throws SchemaException {
		for (ItemDelta delta : deltas) {
			delta.applyTo(propertyContainer);
		}
	}

	static void applyTo(Collection<? extends ItemDelta> deltas, PrismContainerValue propertyContainerValue)
			throws SchemaException {
		for (ItemDelta delta : deltas) {
			delta.applyTo(propertyContainerValue);
		}
	}
	
	static void applyToMatchingPath(Collection<? extends ItemDelta> deltas, PrismContainer propertyContainer)
			throws SchemaException {
		for (ItemDelta delta : deltas) {
			delta.applyToMatchingPath(propertyContainer);
		}
	}

	void applyTo(PrismContainerValue containerValue) throws SchemaException;

	void applyTo(Item item) throws SchemaException;

	/**
	 * Applies delta to item were path of the delta and path of the item matches (skips path checks).
	 */
	void applyToMatchingPath(Item item) throws SchemaException;

	ItemDelta<?,?> getSubDelta(ItemPath path);

	boolean isApplicableTo(Item item);

	static void accept(Collection<? extends ItemDelta> modifications, Visitor visitor, ItemPath simplePath,
			boolean recursive) {
		UniformItemPath path = UniformItemPathImpl.fromItemPath(simplePath);    // todo fixme
		for (ItemDelta modification: modifications) {
			UniformItemPath modPath = modification.getPath();
			ItemPath.CompareResult rel = modPath.compareComplex(path);
			if (rel == ItemPath.CompareResult.EQUIVALENT) {
				modification.accept(visitor, null, recursive);
			} else if (rel == ItemPath.CompareResult.SUBPATH) {
				modification.accept(visitor, path.remainder(modPath), recursive);
			}
		}
	}

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
	boolean contains(ItemDelta<V, D> other, boolean ignoreMetadata, boolean isLiteral);

	void filterValues(Function<V, Boolean> function);

	ItemDelta<V,D> clone();

	ItemDelta<V,D> cloneWithChangedParentPath(UniformItemPath newParentPath);

	static <D extends ItemDelta<?,?>> Collection<D> cloneCollection(Collection<D> orig) {
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
	static <IV extends PrismValue,ID extends ItemDefinition> PrismValueDeltaSetTriple<IV> toDeltaSetTriple(Item<IV, ID> item,
			ItemDelta<IV, ID> delta,
			boolean oldValuesValid, boolean newValuesValid) {
		if (item == null && delta == null) {
			return null;
		}
		if (!oldValuesValid && !newValuesValid) {
			return null;
		}
		if (oldValuesValid && !newValuesValid) {
			// There were values but they no longer are -> everything to minus set
			PrismValueDeltaSetTriple<IV> triple = new PrismValueDeltaSetTripleImpl<>();
			if (item != null) {
				triple.addAllToMinusSet(item.getValues());
			}
			return triple;
		}
		if (item == null && delta != null) {
			return delta.toDeltaSetTriple(item);
		}
		if (delta == null || (!oldValuesValid && newValuesValid)) {
			PrismValueDeltaSetTriple<IV> triple = new PrismValueDeltaSetTripleImpl<>();
			if (item != null) {
				triple.addAllToZeroSet(item.getValues());
			}
			return triple;
		}
		return delta.toDeltaSetTriple(item);
	}

	static <IV extends PrismValue,ID extends ItemDefinition> PrismValueDeltaSetTriple<IV> toDeltaSetTriple(Item<IV, ID> item,
			ItemDelta<IV, ID> delta) {
		if (item == null && delta == null) {
			return null;
		}
		if (delta == null) {
			PrismValueDeltaSetTriple<IV> triple = new PrismValueDeltaSetTripleImpl<>();
			triple.addAllToZeroSet(PrismValue.cloneCollection(item.getValues()));
			return triple;
		}
		return delta.toDeltaSetTriple(item);
	}

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

	static boolean hasEquivalent(Collection<? extends ItemDelta> col, ItemDelta delta) {
		for (ItemDelta colItem: col) {
			if (colItem.equivalent(delta)) {
				return true;
			}
		}
		return false;
	}

	@Override
	boolean equals(Object obj);

	@Override
	String toString();

	@Override
	String debugDump(int indent);

	static void addAll(Collection<? extends ItemDelta> modifications, Collection<? extends ItemDelta> deltasToAdd) {
		if (deltasToAdd == null) {
			return;
		}
		for (ItemDelta deltaToAdd: deltasToAdd) {
			if (!modifications.contains(deltaToAdd)) {
				((Collection)modifications).add(deltaToAdd);
			}
		}
	}

	static void merge(Collection<? extends ItemDelta> modifications, ItemDelta delta) {
		for (ItemDelta modification: modifications) {
			if (modification.getPath().equals(delta.getPath())) {
				modification.merge(delta);
				return;
			}
		}
		((Collection)modifications).add(delta);
	}

	static void mergeAll(Collection<? extends ItemDelta<?, ?>> modifications, Collection<? extends ItemDelta<?, ?>> deltasToMerge) {
		if (deltasToMerge == null) {
			return;
		}
		for (ItemDelta deltaToMerge: deltasToMerge) {
			merge(modifications, deltaToMerge);
		}
	}

	void addToReplaceDelta();

	ItemDelta<V,D> createReverseDelta();

	V findValueToAddOrReplace(V value);

	/**
	 * Set origin type to all values and subvalues
	 */
	void setOriginTypeRecursive(final OriginType originType);

	// TODO move to Item
	static <V extends PrismValue, D extends ItemDefinition> ItemDelta<V, D> createAddDeltaFor(Item<V, D> item) {
		ItemDelta<V, D> rv = item.createDelta(item.getPath());
		rv.addValuesToAdd(item.getClonedValues());
		return rv;
	}

	// TODO move to Item
	@SuppressWarnings("unchecked")
	static <V extends PrismValue, D extends ItemDefinition> ItemDelta<V, D> createAddDeltaFor(Item<V, D> item, PrismValue value) {
		ItemDelta<V, D> rv = item.createDelta(item.getPath());
		rv.addValueToAdd((V) CloneUtil.clone(value));
		return rv;
	}

	static boolean pathMatches(@NotNull Collection<? extends ItemDelta<?, ?>> deltas, @NotNull UniformItemPath path, int segmentsToSkip,
			boolean exactMatch) {
		for (ItemDelta<?, ?> delta : deltas) {
			UniformItemPath modifiedPath = delta.getPath().rest(segmentsToSkip).removeIds();   // because of extension/cities[2]/name (in delta) vs. extension/cities/name (in spec)
			if (exactMatch) {
				if (path.equivalent(modifiedPath)) {
					return true;
				}
			} else {
				if (path.isSubPathOrEquivalent(modifiedPath)) {
					return true;
				}
			}
		}
		return false;
	}
}
