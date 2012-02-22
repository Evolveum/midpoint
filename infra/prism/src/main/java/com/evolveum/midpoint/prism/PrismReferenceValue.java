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

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;

import java.io.Serializable;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public class PrismReferenceValue extends PrismValue implements Dumpable, DebugDumpable, Serializable {

    private String oid = null;
    private PrismObject object = null;
    private QName targetType = null;
    
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
		return targetType;
	}

	public void setTargetType(QName targetType) {
		this.targetType = targetType;
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
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		if (targetType == null) {
			if (other.targetType != null)
				return false;
		} else if (!targetType.equals(other.targetType))
			return false;
		return true;
	}
	
	@Override
	public boolean equalsRealValue(PrismValue value) {
		if (value instanceof PrismReferenceValue) {
			return equalsRealValue((PrismReferenceValue)value);
		} else {
			return false;
		}
	}

	public boolean equalsRealValue(PrismReferenceValue pValueToCompare) {
        if (pValueToCompare == null) {
            return false;
        }
        
        String valueToCompare = pValueToCompare.getOid();
        if (valueToCompare == null && getOid() == null) {
        	return true;
        }
        if (valueToCompare == null || getOid() == null) {
        	return false;
        }

        return getOid().equals(pValueToCompare.getOid());
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
		return "PRV[oid=" + oid + ", targetType=" + targetType + ", type=" + getType()
				+ ", source=" + getSource() + "]";
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
        clone.targetType = this.targetType;
        return clone;
    }
}
