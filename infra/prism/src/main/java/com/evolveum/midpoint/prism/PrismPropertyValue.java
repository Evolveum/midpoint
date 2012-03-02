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
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;
import java.util.Collection;

import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public class PrismPropertyValue<T> extends PrismValue implements Dumpable, DebugDumpable, Serializable {

    private T value;
    // The rawElement is set during a schema-less parsing, e.g. during a dumb JAXB parsing or XML parsing without a
    // definition.
    // We can't do anything smarter, as we don't have definition nor prism context. So we store the raw
    // elements here and process them later (e.g. during applyDefinition or getting a value with explicit type).
    private Object rawElement;

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
    	if (rawElement != null) {
    		ItemDefinition def = null;
    		Item parent = getParent();
    		if (parent != null && parent.getDefinition() != null) {
    			def = getParent().getDefinition();
    		}
    		if (def == null) {
        		// We are weak now. If there is no better definition for this we assume a default definitio and process
        		// the attribute now. But we should rather do this: TODO:
        		// throw new IllegalStateException("Attempt to get value withot a type from raw value of property "+getParent());
    			if (parent != null && parent.getPrismContext() != null) {
    				def = SchemaRegistry.createDefaultItemDefinition(parent.getName(), parent.getPrismContext());
    			} else {
    				if (rawElement instanceof Element) {
        				// Do the most stupid thing possible. Assume string value. And there will be no definition.
    					value = (T) ((Element)rawElement).getTextContent();
    				} else {
    					throw new IllegalStateException("No parent or prism context in property value "+this+", cannot create default definition." +
    							"The element is also not a DOM element but it is "+rawElement.getClass()+". Epic fail.");
    				}
    			}
    		}
    		if (def != null) {
				try {
					applyDefinition(def);
				} catch (SchemaException e) {
					throw new IllegalStateException(e.getMessage(),e);
				}
    		}
    	}
        return value;
    }
    
    public Object getRawElement() {
		return rawElement;
	}

	public void setRawElement(Object rawElement) {
		this.rawElement = rawElement;
	}
	
	@Override
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
		if (definition != null && rawElement !=null) {
			PrismDomProcessor domProcessor = definition.getPrismContext().getPrismDomProcessor();
			value = (T) domProcessor.parsePrismPropertyRealValue(rawElement, (PrismPropertyDefinition) definition);
			rawElement = null;
		}
	}

	@Override
	protected Element createDomElement() {
		return new ElementPrismPropertyImpl<T>(this);
	}

	@Override
	public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PPV(");
        // getValue() must not be here. getValue() contains exception that in turn causes a call to toString()
        if (value != null) {
        	builder.append(value.getClass().getSimpleName()).append(":");
            builder.append(DebugUtil.prettyPrint(value));
        } else {
            builder.append("null");
        }
        if (getType() != null) {
	        builder.append(", type: ");
	        builder.append(getType());
        }
        if (getSource() != null) {
	        builder.append(", source: ");
	        builder.append(getSource());
        }
        builder.append(")");

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
	
	public boolean equalsRealValue(PrismPropertyValue<T> other) {
        if (other == null) {
            return false;
        }
        
        if (this.rawElement != null && other.rawElement != null) {
        	return equalsRawElements(other);
        }
        if (this.rawElement != null || other.rawElement != null) {
        	return false;
        }
        
        T otherRealValue = other.getValue();
        if (otherRealValue == null && getValue() == null) {
        	return true;
        }
        if (otherRealValue == null || getValue() == null) {
        	return false;
        }

        // DOM elements cannot be compared directly. Use utility method instead.
        if (otherRealValue instanceof Element && getValue() instanceof Element) {
        	return DOMUtil.compareElement((Element)getValue(), (Element)otherRealValue, true);
        }
        
        // FIXME!! HACK!!
//        if (valueToCompare instanceof ObjectReferenceType && getValue() instanceof ObjectReferenceType) {
//        	return ((ObjectReferenceType)valueToCompare).getOid().equals(((ObjectReferenceType)getValue()).getOid());
//        }
        
        return getValue().equals(other.getValue());
    }

	private boolean equalsRawElements(PrismPropertyValue<T> other) {
		if (this.rawElement instanceof Element && other.rawElement instanceof Element) {
			return DOMUtil.compareElement((Element)this.rawElement, (Element)other.rawElement, false);
		}
		return this.rawElement.equals(other.rawElement);
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
