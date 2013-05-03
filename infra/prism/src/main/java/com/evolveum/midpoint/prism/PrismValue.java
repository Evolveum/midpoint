/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class PrismValue implements Visitable, PathVisitable, Serializable, Dumpable, DebugDumpable {
	
	private OriginType originType;
    private Objectable originObject;
    private Itemable parent;
    protected Element domElement = null;
    
    PrismValue() {
		super();
	}
    
    PrismValue(OriginType type, Objectable source) {
		super();
		this.originType = type;
		this.originObject = source;
	}
    
    PrismValue(OriginType type, Objectable source, Itemable parent) {
		super();
		this.originType = type;
		this.originObject = source;
		this.parent = parent;
	}

	public void setOriginObject(Objectable source) {
        this.originObject = source;
    }

    public void setOriginType(OriginType type) {
        this.originType = type;
    }
    
    public OriginType getOriginType() {
        return originType;
    }

    public Objectable getOriginObject() {
        return originObject;
    }
    
	public Itemable getParent() {
		return parent;
	}

	public void setParent(Itemable parent) {
		if (this.parent != null && parent != null && this.parent != parent) {
			throw new IllegalStateException("Attempt to reset value parent from "+this.parent+" to "+parent);
		}
		this.parent = parent;
	}
	
	public ItemPath getPath() {
		Itemable parent = getParent();
		if (parent == null) {
			throw new IllegalStateException("No parent, cannot create value path for "+this); 
		}
		return parent.getPath();
	}
	
	public PrismContext getPrismContext() {
		if (parent != null) {
			return parent.getPrismContext();
		}
		return null;
	}
	
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
		applyDefinition(definition, true);
	}
	
	public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
		// Do nothing by default
	}
	
	public void revive(PrismContext prismContext) {
		recompute(prismContext);
	}
	
	/**
	 * Recompute the value or otherwise "initialize" it before adding it to a prism tree.
	 * This may as well do nothing if no recomputing or initialization is needed.
	 */
	public void recompute() {
		recompute(getPrismContext());
	}
	
	public abstract void recompute(PrismContext prismContext);
	
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

	public Element asDomElement() {
		if (domElement == null) {
			domElement = createDomElement();
		}
		return domElement;
	}

	protected abstract Element createDomElement();
	
	protected void clearDomElement() {
		domElement = null;
	}
	
	public abstract void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw);
		
	/**
	 * Returns true if this and other value represent the same value.
	 * E.g. if they have the same IDs, OIDs or it is otherwise know
	 * that they "belong together" without a deep examination of the
	 * values.
	 */
	public boolean representsSameValue(PrismValue other) {
		return false;
	}
	
	public static <V extends PrismValue> boolean containsRealValue(Collection<V> collection, V value) {
		if (collection == null) {
			return false;
		}
		for (V colVal: collection) {
			if (colVal.equalsRealValue(value)) {
				return true;
			}
		}
		return false;
	}
	
	public abstract boolean isEmpty();
	
	public void normalize() {
		// do nothing by default
	}
	
	/**
	 * Returns true if the value is raw. Raw value is a semi-parsed value.
	 * A value for which we don't have a full definition yet and therefore
	 * the parsing could not be finished until the defintion is supplied.
	 */
	public abstract boolean isRaw();

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
	}
	
	public static <T extends PrismValue> Collection<T> cloneCollection(Collection<T> values) {
		Collection<T> clones = new ArrayList<T>();
		for (T value: values) {
			clones.add((T)value.clone());
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
	
	public abstract Object find(ItemPath path);
	
	public abstract <X extends PrismValue> PartiallyResolvedValue<X> findPartial(ItemPath path);
	
	@Override
	public int hashCode() {
		int result = 1;
		return result;
	}
	
	public boolean equalsComplex(PrismValue other, boolean ignoreMetadata, boolean isLiteral) {
		// parent is not considered at all. it is not relevant.
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
	public Collection<? extends ItemDelta> diff(PrismValue otherValue) {
		return diff(otherValue, true, false);
	}
	
	/**
	 * Assumes matching representations. I.e. it assumes that both this and otherValue represent the same instance of item.
	 * E.g. the container with the same ID. 
	 */
	public Collection<? extends ItemDelta> diff(PrismValue otherValue, boolean ignoreMetadata, boolean isLiteral) {
		Collection<? extends ItemDelta> itemDeltas = new ArrayList<ItemDelta>();
		diffMatchingRepresentation(otherValue, itemDeltas, ignoreMetadata, isLiteral);
		return itemDeltas;
	}
	
	void diffMatchingRepresentation(PrismValue otherValue,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral) {
		// Nothing to do by default
	}

	public abstract boolean match(PrismValue otherValue);
	
	/**
	 * Returns a short (one-line) representation of the real value stored in this object.
	 * The value is returned without any decorations or type demarcations (such as PPV, PRV, etc.)
	 */
	public abstract String toHumanReadableString();
	
}
