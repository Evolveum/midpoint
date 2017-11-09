/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author semancik
 *
 */
public abstract class PrismValue implements IPrismValue {

	private OriginType originType;
    private Objectable originObject;
    private Itemable parent;
    private transient Map<String,Object> userData = new HashMap<>();
	protected boolean immutable;

	transient protected PrismContext prismContext;

	PrismValue() {
	}

	PrismValue(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

    PrismValue(OriginType type, Objectable source) {
		this(null, type, source);
	}

    PrismValue(PrismContext prismContext, OriginType type, Objectable source) {
		this.prismContext = prismContext;
		this.originType = type;
		this.originObject = source;
	}

    PrismValue(PrismContext prismContext, OriginType type, Objectable source, Itemable parent) {
		this.prismContext = prismContext;
		this.originType = type;
		this.originObject = source;
		this.parent = parent;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public void setOriginObject(Objectable source) {
        this.originObject = source;
    }

    public void setOriginType(OriginType type) {
        this.originType = type;
    }

    @Override
	public OriginType getOriginType() {
        return originType;
    }

    @Override
	public Objectable getOriginObject() {
        return originObject;
    }

    public Map<String, Object> getUserData() {
        return userData;
    }

    @Override
	public Object getUserData(@NotNull String key) {
        return userData.get(key);
    }

    @Override
	public void setUserData(@NotNull String key, Object value) {
        userData.put(key, value);
    }

    @Override
	public Itemable getParent() {
		return parent;
	}

	@Override
	public void setParent(Itemable parent) {
		if (this.parent != null && parent != null && this.parent != parent) {
			throw new IllegalStateException("Attempt to reset value parent from "+this.parent+" to "+parent);
		}
		this.parent = parent;
	}

	@NotNull
	@Override
	public ItemPath getPath() {
		Itemable parent = getParent();
		if (parent == null) {
			throw new IllegalStateException("No parent, cannot create value path for "+this);
		}
		return parent.getPath();
	}

	/**
	 * Used when we are removing the value from the previous parent.
	 * Or when we know that the previous parent will be discarded and we
	 * want to avoid unnecessary cloning.
	 */
	@Override
	public void clearParent() {
		parent = null;
	}

	public static <T> void clearParent(List<PrismPropertyValue<T>> values) {
		if (values == null) {
			return;
		}
		for (PrismPropertyValue<T> val: values) {
			val.clearParent();
		}
	}

	@Override
	public PrismContext getPrismContext() {
		if (prismContext != null) {
			return prismContext;
		}
		if (parent != null) {
			prismContext = parent.getPrismContext();
			return prismContext;
		}
		return null;
	}

	protected ItemDefinition getDefinition() {
		Itemable parent = getParent();
    	if (parent == null) {
    		return null;
    	}
    	return parent.getDefinition();
    }

	@Override
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
		checkMutability();		// TODO reconsider
		applyDefinition(definition, true);
	}

	@Override
	public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
		checkMutability();		// TODO reconsider
		// Do nothing by default
	}

	public void revive(PrismContext prismContext) throws SchemaException {
		if (this.prismContext == null) {
			this.prismContext = prismContext;
		}
		if (!immutable) {
			recompute(prismContext);
		}
	}

	/**
	 * Recompute the value or otherwise "initialize" it before adding it to a prism tree.
	 * This may as well do nothing if no recomputing or initialization is needed.
	 */
	@Override
	public void recompute() {
		recompute(getPrismContext());
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
		// This implementation is supposed to only work for non-hierarchical values, such as properties and references.
		// hierarchical values must override it.
		if (recursive) {
			accept(visitor);
		} else {
			visitor.visit(this);
		}
	}

    public abstract void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope);

	/**
	 * Returns true if this and other value represent the same value.
	 * E.g. if they have the same IDs, OIDs or it is otherwise know
	 * that they "belong together" without a deep examination of the
	 * values.
	 *
	 * @param lax If we can reasonably assume that the two values belong together even if they don't have the same ID,
	 *            e.g. if they both belong to single-valued parent items. This is useful e.g. when comparing
	 *            multi-valued containers. But can cause problems when we want to be sure we are removing the correct
	 *            value.
	 */
	public boolean representsSameValue(PrismValue other, boolean lax) {
		return false;
	}

	public static <V extends PrismValue> boolean containsRealValue(Collection<V> collection, V value) {
		return containsRealValue(collection, value, Function.identity());
	}

	public static <X, V extends PrismValue> boolean containsRealValue(Collection<X> collection, V value, Function<X, V> valueExtractor) {
		if (collection == null) {
			return false;
		}

		for (X colVal: collection) {
			if (colVal == null) {
				return value == null;
			}
		
			if (valueExtractor.apply(colVal).equalsRealValue(value)) {

				return true;
			}
		}
		return false;
	}

	public static <V extends PrismValue> boolean equalsRealValues(Collection<V> collection1, Collection<V> collection2) {
		return MiscUtil.unorderedCollectionEquals(collection1, collection2, (v1, v2) -> v1.equalsRealValue(v2));
	}

	public static <V extends PrismValue> boolean containsAll(Collection<V> thisSet, Collection<V> otherSet, boolean ignoreMetadata, boolean isLiteral) {
		if (thisSet == null && otherSet == null) {
			return true;
		}
		if (otherSet == null) {
			return true;
		}
		if (thisSet == null) {
			return false;
		}
		for (V otherValue: otherSet) {
			if (!contains(thisSet, otherValue, ignoreMetadata, isLiteral)) {
				return false;
			}
		}
		return true;
	}

	public static <V extends PrismValue> boolean contains(Collection<V> thisSet, V otherValue, boolean ignoreMetadata, boolean isLiteral) {
		for (V thisValue: thisSet) {
			if (thisValue.equalsComplex(otherValue, ignoreMetadata, isLiteral)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void normalize() {
		// do nothing by default
	}

	public static <X extends PrismValue> Collection<X> cloneValues(Collection<X> values) {
		Collection<X> clonedCollection = new ArrayList<X>(values.size());
		for (X val: values) {
			clonedCollection.add((X) val.clone());
		}
		return clonedCollection;
	}

	public abstract PrismValue clone();

	protected void copyValues(PrismValue clone) {
		clone.originType = this.originType;
		clone.originObject = this.originObject;
		// Do not clone parent. The clone will most likely go to a different prism
		// and setting the parent will make it difficult to add it there.
		clone.parent = null;
		// Do not clone immutable flag.
		if (clone.prismContext == null) {
			clone.prismContext = this.prismContext;
		}
	}

	@NotNull
	public static <T extends PrismValue> Collection<T> cloneCollection(Collection<T> values) {
		Collection<T> clones = new ArrayList<T>();
		if (values != null) {
			for (T value : values) {
				clones.add((T) value.clone());
			}
		}
		return clones;
	}

	/**
     * Sets all parents to null. This is good if the items are to be "transplanted" into a
     * different Containerable.
     */
	public static <T extends PrismValue> Collection<T> resetParentCollection(Collection<T> values) {
    	for (T value: values) {
    		value.setParent(null);
    	}
    	return values;
	}

	@Override
	public int hashCode() {
		int result = 0;
		return result;
	}

	public boolean equalsComplex(PrismValue other, boolean ignoreMetadata, boolean isLiteral) {
		// parent is not considered at all. it is not relevant.
		// neither the immutable flag
		if (!ignoreMetadata) {
			if (originObject == null) {
				if (other.originObject != null)
					return false;
			} else if (!originObject.equals(other.originObject))
				return false;
			if (originType != other.originType)
				return false;
		}
		return true;
	}

	@Override
	public boolean equals(PrismValue otherValue, boolean ignoreMetadata) {
		return equalsComplex(otherValue, ignoreMetadata, false);
	}

	public boolean equals(PrismValue thisValue, PrismValue otherValue) {
		if (thisValue == null && otherValue == null) {
			return true;
		}
		if (thisValue == null || otherValue == null) {
			return false;
		}
		return thisValue.equalsComplex(otherValue, false, false);
	}

	public boolean equalsRealValue(PrismValue otherValue) {
		return equalsComplex(otherValue, true, false);
	}

	public boolean equalsRealValue(PrismValue thisValue, PrismValue otherValue) {
		if (thisValue == null && otherValue == null) {
			return true;
		}
		if (thisValue == null || otherValue == null) {
			return false;
		}
		return thisValue.equalsComplex(otherValue, true, false);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrismValue other = (PrismValue) obj;
		return equalsComplex(other, false, false);
	}

	/**
	 * Assumes matching representations. I.e. it assumes that both this and otherValue represent the same instance of item.
	 * E.g. the container with the same ID.
	 */
	@Override
	public Collection<? extends ItemDelta> diff(PrismValue otherValue) {
		return diff(otherValue, true, false);
	}

	/**
	 * Assumes matching representations. I.e. it assumes that both this and otherValue represent the same instance of item.
	 * E.g. the container with the same ID.
	 */
	@Override
	public Collection<? extends ItemDelta> diff(PrismValue otherValue, boolean ignoreMetadata, boolean isLiteral) {
		Collection<? extends ItemDelta> itemDeltas = new ArrayList<ItemDelta>();
		diffMatchingRepresentation(otherValue, itemDeltas, ignoreMetadata, isLiteral);
		return itemDeltas;
	}

	void diffMatchingRepresentation(PrismValue otherValue,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral) {
		// Nothing to do by default
	}

	protected void appendOriginDump(StringBuilder builder) {
		if (DebugUtil.isDetailedDebugDump()) {
	        if (getOriginType() != null || getOriginObject() != null) {
		        builder.append(", origin: ");
		        builder.append(getOriginType());
		        builder.append(":");
		        builder.append(getOriginObject());
	        }
		}
	}

    public static <T> Set<T> getRealValuesOfCollection(Collection<PrismPropertyValue<T>> collection) {
        Set<T> retval = new HashSet<T>(collection.size());
        for (PrismPropertyValue<T> value : collection) {
            retval.add(value.getValue());
        }
        return retval;
    }


	public static <V extends PrismValue> boolean collectionContainsEquivalentValue(Collection<V> collection, V value) {
		if (collection == null) {
			return false;
		}
		for (V collectionVal: collection) {
			if (collectionVal.equals(value, true)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public boolean isImmutable() {
		return immutable;
	}

	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}

	protected void checkMutability() {
		if (immutable) {
			throw new IllegalStateException("An attempt to modify an immutable value of " + toHumanReadableString());
		}
	}

	@Nullable
	abstract public Class<?> getRealClass();

	@Nullable
	abstract public <T> T getRealValue();

	// Returns a root of PrismValue tree. For example, if we have a AccessCertificationWorkItemType that has a parent (owner)
	// of AccessCertificationCaseType, which has a parent of AccessCertificationCampaignType, this method returns the PCV
	// of AccessCertificationCampaignType.
	//
	// Generally, this method returns either "this" (PrismValue) or a PrismContainerValue.
	public PrismValue getRootValue() {
		PrismValue current = this;
		for (;;) {
			PrismContainerValue<?> parent = getParentContainerValue(current);
			if (parent == null) {
				return current;
			}
			current = parent;
		}
	}

	public static PrismContainerValue<?> getParentContainerValue(PrismValue value) {
		Itemable parent = value.getParent();
		if (parent instanceof Item) {
			PrismValue parentParent = ((Item) parent).getParent();
			return parentParent instanceof PrismContainerValue ? (PrismContainerValue) parentParent : null;
		} else {
			return null;
		}
	}

	public PrismContainerValue<?> getParentContainerValue() {
		return getParentContainerValue(this);
	}

	public QName getTypeName() {
		ItemDefinition definition = getDefinition();
		return definition != null ? definition.getTypeName() : null;
	}

	public static PrismValue fromRealValue(Object realValue) {
		if (realValue instanceof Containerable) {
			return ((Containerable) realValue).asPrismContainerValue();
		} else if (realValue instanceof Referencable) {
			return ((Referencable) realValue).asReferenceValue();
		} else {
			return new PrismPropertyValue<>(realValue);
		}
	}
}
