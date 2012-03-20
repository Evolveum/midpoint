/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.util.DebugUtil;
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
public class PrismReference extends Item {
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
	
	public void setDefinition(PrismPropertyDefinition definition) {
        this.definition = definition;
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
    	if (getDefinition() != null) {
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
            return null;
        }
        return getValues().iterator().next();
    }
    
    public boolean add(PrismReferenceValue value) {
    	value.setParent(this);
    	return getValues().add(value);
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
	public ReferenceDelta createDelta(PropertyPath path) {
    	return new ReferenceDelta(path, getDefinition());
	}

	void applyDefinition(ItemDefinition definition) throws SchemaException {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply "+definition+" to reference");
		}
		super.applyDefinition(definition);
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
        return getClass().getSimpleName() + "(" + DebugUtil.prettyPrint(getName()) + "):" + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ").append(DebugUtil.prettyPrint(getName())).append(" = ");
        if (getValues() == null) {
            sb.append("null");
        } else {
            sb.append("[ ");
            for (Object value : getValues()) {
                sb.append(DebugUtil.prettyPrint(value));
                sb.append(", ");
            }
            sb.append(" ]");
        }
        if (getDefinition() != null) {
            sb.append(" def");
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
