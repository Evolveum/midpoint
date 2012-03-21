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

import java.util.Collection;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class PrismValue implements Visitable {
	
	private SourceType type;
    private Objectable source;
    private Itemable parent;
    protected Element domElement = null;
    
    PrismValue() {
		super();
	}
    
    PrismValue(SourceType type, Objectable source) {
		super();
		this.type = type;
		this.source = source;
	}
    
    PrismValue(SourceType type, Objectable source, Itemable parent) {
		super();
		this.type = type;
		this.source = source;
		this.parent = parent;
	}

	public void setSource(Objectable source) {
        this.source = source;
    }

    public void setType(SourceType type) {
        this.type = type;
    }
    
    public SourceType getType() {
        return type;
    }

    public Objectable getSource() {
        return source;
    }
    
	public Itemable getParent() {
		return parent;
	}

	public void setParent(Itemable parent) {
		this.parent = parent;
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
	
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
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
	
	/**
	 * Returns true if this and other value represent the same value.
	 * E.g. if they have the same IDs, OIDs or it is otherwise know
	 * that they "belong together" without a deep examination of the
	 * values.
	 */
	public boolean representsSameValue(PrismValue other) {
		return false;
	}

	public boolean equals(PrismValue otherValue, boolean ignoreMetadata) {
		return equals(this, otherValue, ignoreMetadata);
	}
	
	public boolean equals(PrismValue thisValue, PrismValue otherValue, boolean ignoreMetadata) {
		if (ignoreMetadata) {
			return equalsRealValue(thisValue, otherValue);
		} else {
			return equals(thisValue, otherValue);
		}
	}
	
	public boolean equalsRealValue(PrismValue otherValue) {
		return equalsRealValue(this, otherValue);
	}
	
	public abstract boolean equalsRealValue(PrismValue thisValue, PrismValue otherValue);
	
	public abstract PrismValue clone();
	
	protected void copyValues(PrismValue clone) {
		clone.type = this.type;
		clone.source = this.source;
		clone.parent = this.parent;
	}
	
	@Override
	public int hashCode() {
		int result = 1;
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
		PrismValue other = (PrismValue) obj;
		return equals(this, other);
	}
	
	public boolean equals(PrismValue thisValue, PrismValue otherValue) {
		// parent is not considered at all. it is not relevant.
		if (source == null) {
			if (otherValue.source != null)
				return false;
		} else if (!source.equals(otherValue.source))
			return false;
		if (type != otherValue.type)
			return false;
		return true;
	}

	void diffMatchingRepresentation(PrismValue otherValue, PropertyPath pathPrefix,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata) {
		// Nothing to do by default
	}

	
}
