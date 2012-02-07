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

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

import java.io.Serializable;

import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public class PropertyValue<T> implements Dumpable, DebugDumpable, Serializable {

    private T value;
    private SourceType type;
    private ObjectType source;

    public PropertyValue(T value) {
        this(value, null, null);
    }

    public PropertyValue(T value, SourceType type, ObjectType source) {
        if (value instanceof PropertyValue) {
            throw new IllegalArgumentException("Probably problem somewhere, encapsulating property " +
                    "value object to another property value.");
        }

        this.value = value;
        this.type = type;
        this.source = source;
    }

    public void setSource(ObjectType source) {
        this.source = source;
    }

    public void setType(SourceType type) {
        this.type = type;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public SourceType getType() {
        return type;
    }

    public ObjectType getSource() {
        return source;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PropertyValue[");
        if (getValue() != null) {
            builder.append(getValue().toString());
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
        int hash = 11;
        if (getValue() != null) {
            hash += getValue().hashCode();
        }

        if (getSource() != null) {
            hash = hash * 13 + getSource().hashCode();
        }
        if (getType() != null) {
            hash = hash * 17 + getType().hashCode();
        }

        return hash;
    }

    public boolean equalsRealValue(PropertyValue<T> pValueToCompare) {
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
        if (valueToCompare instanceof ObjectReferenceType && getValue() instanceof ObjectReferenceType) {
        	return ((ObjectReferenceType)valueToCompare).getOid().equals(((ObjectReferenceType)getValue()).getOid());
        }
        
        return getValue().equals(pValueToCompare.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyValue)) {
            return false;
        }
        PropertyValue other = (PropertyValue) o;

        return equals(getValue(), other.getValue())
                && equals(getSource(), other.getSource())
                && equals(getType(), other.getType());
    }

    private boolean equals(Object o1, Object o2) {
        return o1 != null ? o1.equals(o2) : o2 == null;
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
    public PropertyValue<T> clone() {
        return new PropertyValue(getValue(), getType(), getSource());
    }
}
