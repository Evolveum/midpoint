/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.dom.ElementPrismContainerImpl;
import com.evolveum.midpoint.prism.dom.ElementPrismReferenceImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * @author Radovan Semancik
 */
public class PrismReferenceValue extends PrismValue implements Dumpable, DebugDumpable, Serializable {

    private String oid = null;
    private PrismObject object = null;
    private QName targetType = null;
    private String description = null;
    private Element filter = null;
    
    public PrismReferenceValue() {
        this(null,null,null);
    }

    public PrismReferenceValue(String oid) {
        this(oid, null, null);
    }

    public PrismReferenceValue(String oid, SourceType type, Objectable source) {
    	super(type,source);
        this.oid = oid;
    }

	/**
	 * OID of the object that this reference refers to (reference target).
	 * 
	 * May return null, but the reference is in that case incomplete and
	 * unusable.
	 * 
	 * @return the target oid
	 */
    public String getOid() {
		if (oid != null) {
			return oid;
		}
		if (object != null) {
			return object.getOid();
		}
		return null;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}
	
	public PrismObject getObject() {
		return object;
	}

	public void setObject(PrismObject object) {
		this.object = object;
	}

	/**
	 * Returns XSD type of the object that this reference refers to. It may be
	 * used in XPath expressions and similar filters.
	 * 
	 * May return null if the type name is not set.
	 * 
	 * @return the target type name
	 */
	public QName getTargetType() {
		if (targetType != null) {
			return targetType;
		}
		if (object != null && object.getDefinition() != null) {
			return object.getDefinition().getTypeName();
		}
		return null;
	}

	public void setTargetType(QName targetType) {
		// Null value is OK
		if (targetType != null) {
			// But non-empty is not ..
			Itemable item = getParent();
			DOMUtil.validateNonEmptyQName(targetType, " in target type in reference "+ (item == null ? "(unknown)" : item.getName()));
		}
		this.targetType = targetType;
	}

    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Element getFilter() {
		return filter;
	}

	public void setFilter(Element filter) {
		this.filter = filter;
	}
	
	
	@Override
	public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply "+definition+" to a reference value");
		}
		applyDefinition((PrismReferenceDefinition)definition, force);
	}

	public void applyDefinition(PrismReferenceDefinition definition, boolean force) throws SchemaException {
		super.applyDefinition(definition, force);
		if (object == null) {
			return;
		}
		if (object.getDefinition() != null && !force) {
			return;
		}
		PrismContext prismContext = definition.getPrismContext();
		QName targetTypeName = definition.getTargetTypeName();
		if (targetTypeName == null) {
			throw new SchemaException("Cannot apply definition to composite object in reference "+getParent()
					+": the target type name is not specified in the reference schema");
		}
		PrismObjectDefinition<Objectable> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(targetTypeName);
		if (objectDefinition == null) {
			throw new SchemaException("Cannot apply definition to composite object in reference "+getParent()
					+": no definition for object type "+targetTypeName);
		}
		// this should do it
		object.applyDefinition(objectDefinition, force);
	}

	@Override
	protected Element createDomElement() {
		return new ElementPrismReferenceImpl(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((oid == null) ? 0 : oid.hashCode());
		result = prime * result + ((targetType == null) ? 0 : targetType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrismReferenceValue other = (PrismReferenceValue) obj;
		return equals(this, other);
	}
	
	public boolean equals(PrismValue thisValue, PrismValue otherValue) {
		if (thisValue instanceof PrismReferenceValue && otherValue instanceof PrismReferenceValue) {
			return equals((PrismReferenceValue)thisValue, (PrismReferenceValue)otherValue);
		}
		return false;
	}
	
	public boolean equals(PrismReferenceValue thisValue, PrismReferenceValue otherValue) {
		if (thisValue.oid == null) {
			if (otherValue.oid != null)
				return false;
		} else if (!thisValue.oid.equals(otherValue.oid))
			return false;
		if (thisValue.targetType == null) {
			if (otherValue.targetType != null)
				return false;
		} else if (!thisValue.targetType.equals(otherValue.targetType))
			return false;
		return true;
	}
	
	@Override
	public boolean equalsRealValue(PrismValue thisValue, PrismValue otherValue) {
		if (thisValue instanceof PrismReferenceValue && otherValue instanceof PrismReferenceValue) {
			return equalsRealValue((PrismReferenceValue)thisValue, (PrismReferenceValue)otherValue);
		} else {
			return false;
		}
	}

	public boolean equalsRealValue(PrismReferenceValue thisValue, PrismReferenceValue otherValue) {
        if (otherValue == null) {
            return false;
        }
        
        String valueToCompare = otherValue.getOid();
        if (valueToCompare == null && thisValue.getOid() == null) {
        	return true;
        }
        if (valueToCompare == null || thisValue.getOid() == null) {
        	return false;
        }

        return thisValue.getOid().equals(otherValue.getOid());
    }
	
	@Override
	public boolean representsSameValue(PrismValue other) {
		if (other instanceof PrismPropertyValue) {
			return representsSameValue((PrismReferenceValue)other);
		} else {
			return false;
		}
	}
	
	public boolean representsSameValue(PrismReferenceValue other) {
		if (this.getOid() != null && other.getOid() != null) {
			return this.getOid().equals(other.getOid());
		}
		return false;
	}

	@Override
	public String toString() {
		if (object == null) {
			return "PRV[oid=" + oid + ", targetType=" + targetType + ", type=" + getType()
				+ ", source=" + getSource() + "]";
		} else {
			return "PRV[object=" + object + ", source=" + getSource() + "]";
		}
	}

	@Override
    public String debugDump() {
        return toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(toString());

        return sb.toString();
    }

    @Override
    public String dump() {
        return toString();
    }

    @Override
    public PrismReferenceValue clone() {
        PrismReferenceValue clone = new PrismReferenceValue(getOid(), getType(), getSource());
        copyValues(clone);
        return clone;
    }

	protected void copyValues(PrismReferenceValue clone) {
		super.copyValues(clone);
		clone.targetType = this.targetType;
	}
    
}
