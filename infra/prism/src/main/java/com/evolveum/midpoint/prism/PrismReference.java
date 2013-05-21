/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Object Reference is a property that describes reference to an object. It is
 * used to represent association between objects. For example reference from
 * User object to Account objects that belong to the user. The reference is a
 * simple uni-directional link using an OID as an identifier.
 * 
 * This type should be used for all object references so the implementations can
 * detect them and automatically resolve them.
 * 
 * @author semancik
 * 
 */
public class PrismReference extends Item<PrismReferenceValue> {
	private static final long serialVersionUID = 1872343401395762657L;
	
	public PrismReference(QName name) {
        super(name);
    }
	
	PrismReference(QName name, PrismReferenceDefinition definition, PrismContext prismContext) {
		super(name, definition, prismContext);
	}

	/**
	 * {@inheritDoc}
	 */
	public PrismReferenceDefinition getDefinition() {
		return (PrismReferenceDefinition) super.getDefinition();
	}
		
	/**
     * Returns reference values.
     * <p/>
     * The values are returned as set. The order of values is not significant.
     *
     * @return property values
     */
	@Override
    public List<PrismReferenceValue> getValues() {
        return (List<PrismReferenceValue>) super.getValues();
    }

    public PrismReferenceValue getValue() {
    	// We are not sure about multiplicity if there is no definition or the definition is dynamic
    	if (getDefinition() != null && !getDefinition().isDynamic()) {
    		if (getDefinition().isMultiValue()) {
    			throw new IllegalStateException("Attempt to get single value from property " + name
                        + " with multiple values");
    		}
    	}
        if (getValues().size() > 1) {
            throw new IllegalStateException("Attempt to get single value from property " + name
                    + " with multiple values");
        }
        if (getValues().isEmpty()) {
        	// Insert first empty value. This simulates empty single-valued reference. It the reference exists
	        // it is clear that it has at least one value (and that value is empty).
        	PrismReferenceValue rval = new PrismReferenceValue();
            add(rval);
            return rval;
        }
        return getValues().iterator().next();
    }
    
	private PrismReferenceValue getValue(String oid) {
		// We need to tolerate null OIDs here. Because of JAXB.
		for (PrismReferenceValue val: getValues()) {
			if (MiscUtil.equals(oid, val.getOid())) {
				return val;
			}
		}
		return null;
	}

    
    public boolean add(PrismReferenceValue value) {
    	value.setParent(this);
    	return getValues().add(value);
    }
    
    public boolean merge(PrismReferenceValue value) {
    	String newOid = value.getOid();
    	// We need to tolerate null OIDs here. Because of JAXB.
    	PrismReferenceValue existingValue = getValue(newOid);
		if (existingValue == null) {
			return add(value);
		}

		// in the value.getObject() is not null, it it probably only resolving
		// of refenrence, so only change oid to object
		if (value.getObject() != null) {
			existingValue.setObject(value.getObject());
			return true;
		}  
		
		// in the case, if the existing value and new value are not equal, add
		// also another reference alhtrough one with the same oid exist. It is
		// needed for parent org refs, becasue there can exist more than one
		// reference with the same oid, but they should be different (e.g. user
		// is member and also manager of the org. unit.)
		if (!value.equalsComplex(existingValue, false, false)) {
			return add(value);
		}
		
		if (value.getTargetType() != null) {
			existingValue.setTargetType(value.getTargetType());
//			return true;
		} 
    	// No need to copy OID as OIDs match
    	return true;
    }
    

	public String getOid() {
    	return getValue().getOid();
    }
    
    public PrismReferenceValue findValueByOid(String oid) {
    	for (PrismReferenceValue pval: getValues()) {
    		if (oid.equals(pval.getOid())) {
    			return pval;
    		}
    	}
    	return null;
    }
    
    @Override
	public Object find(ItemPath path) {
    	if (path == null || path.isEmpty()) {
			return this;
		}
		if (!isSingleValue()) {
    		throw new IllegalStateException("Attempt to resolve sub-path '"+path+"' on multi-value reference " + getName());
    	}
		PrismReferenceValue value = getValue();
		return value.find(path);
	}

    
    
	@Override
	public <X extends PrismValue> PartiallyResolvedValue<X> findPartial(ItemPath path) {
		if (path == null || path.isEmpty()) {
			return new PartiallyResolvedValue<X>((Item<X>)this, null);
		}
		if (!isSingleValue()) {
    		throw new IllegalStateException("Attempt to resolve sub-path '"+path+"' on multi-value reference " + getName());
    	}
		PrismReferenceValue value = getValue();
		return value.findPartial(path);
	}

	@Override
	public ReferenceDelta createDelta() {
    	return new ReferenceDelta(getPath(), getDefinition());
	}
	
	@Override
	public ReferenceDelta createDelta(ItemPath path) {
    	return new ReferenceDelta(path, getDefinition());
	}

	@Override
	protected void checkDefinition(ItemDefinition def) {
		if (!(def instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply definition "+def+" to reference "+this);
		}
	}

	@Override
    public PrismReference clone() {
    	PrismReference clone = new PrismReference(getName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismReference clone) {
        super.copyValues(clone);
        for (PrismReferenceValue value : getValues()) {
            clone.add(value.clone());
        }
    }
			
	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + PrettyPrinter.prettyPrint(getName()) + "):" + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        PrismReferenceDefinition definition = getDefinition();
        boolean isComposite = false;
        if (definition != null) {
        	isComposite = definition.isComposite();
        }
        sb.append(getDebugDumpClassName()).append(": ").append(PrettyPrinter.prettyPrint(getName()));
        if (getValues() == null) {
            sb.append(" = null");
        } else {
        	if (isComposite) {
        		// Longer output for composite references, we want to see embedded object
        		if (definition != null) {
                    sb.append(" def,composite");
                }
        		// Display the full object
        		for (PrismReferenceValue value : getValues()) {
        			sb.append("\n");
	                sb.append(value.debugDump(indent + 1, true));
	            }
        	} else {
	            sb.append(" = [ ");
	            for (PrismReferenceValue value : getValues()) {
	                sb.append(PrettyPrinter.prettyPrint(value));
	                sb.append(", ");
	            }
	            sb.append(" ]");
	            if (definition != null) {
	                sb.append(" def");
	            }
        	}
        }
        
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PR";
    }
}
