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

import com.evolveum.midpoint.prism.dom.ElementPrismPropertyImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

import java.io.Serializable;

import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public class PrismPropertyValue<T> extends PrismValue implements Dumpable, DebugDumpable, Serializable {

    private T value;

    public PrismPropertyValue(T value) {
        this(value, null, null);
    }

    public PrismPropertyValue(T value, SourceType type, Objectable source) {
    	super(type,source);
        if (value instanceof PrismPropertyValue) {
            throw new IllegalArgumentException("Probably problem somewhere, encapsulating property " +
                    "value object to another property value.");
        }
        this.value = value;
    }

    public void setValue(T value) {
        this.value = value;
        // To make sure there is no stale value
        clearDomElement();
    }

    public T getValue() {
        return value;
    }
    
    @Override
	protected Element createDomElement() {
		return new ElementPrismPropertyImpl<T>(this);
	}

	public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PPV[");
        if (getValue() != null) {
        	builder.append(getValue().getClass().getSimpleName()).append(":");
            builder.append(DebugUtil.prettyPrint(getValue()));
        } else {
            builder.append("null");
        }
        builder.append(", type: ");
        builder.append(getType());
        builder.append(", source: ");
        builder.append(getSource());
        builder.append("]");

        return builder.toString();
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		PrismPropertyValue other = (PrismPropertyValue) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public boolean equalsRealValue(PrismValue value) {
		if (value instanceof PrismPropertyValue) {
			return equalsRealValue((PrismPropertyValue<T>)value);
		} else {
			return false;
		}
	}
	
	public boolean equalsRealValue(PrismPropertyValue<T> pValueToCompare) {
        if (pValueToCompare == null) {
            return false;
        }
        
        T valueToCompare = pValueToCompare.getValue();
        if (valueToCompare == null && getValue() == null) {
        	return true;
        }
        if (valueToCompare == null || getValue() == null) {
        	return false;
        }

        // DOM elements cannot be compared directly. Use utility method instead.
        if (valueToCompare instanceof Element && getValue() instanceof Element) {
        	return DOMUtil.compareElement((Element)getValue(), (Element)valueToCompare, true);
        }
        
        // FIXME!! HACK!!
//        if (valueToCompare instanceof ObjectReferenceType && getValue() instanceof ObjectReferenceType) {
//        	return ((ObjectReferenceType)valueToCompare).getOid().equals(((ObjectReferenceType)getValue()).getOid());
//        }
        
        return getValue().equals(pValueToCompare.getValue());
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
    public PrismPropertyValue<T> clone() {
        return new PrismPropertyValue(getValue(), getType(), getSource());
    }
}
