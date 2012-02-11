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

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * <p>
 * Property container groups properties into logical blocks.The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * </p><p>
 * Property Container contains a set of (potentially multi-valued) properties or inner property containers.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * </p><p>
 * Property Container is mutable.
 * </p>
 *
 * @author Radovan Semancik
 */
public class PrismContainer extends Item {
    private static final long serialVersionUID = 5206821250098051028L;

    List<PrismContainerValue> values;

    public PrismContainer(QName name, PrismContainerDefinition definition, PrismContext prismContext) {
        super(name, definition, prismContext);
        values = new ArrayList<PrismContainerValue>();
        
        // Insert first empty value. This simulates empty single-valued container. It the container exists
        // it is clear that it has at least one value (and that value is empty).
        PrismContainerValue pValue = new PrismContainerValue(null, null, this, null);
        values.add(pValue);
    }

    public List<PrismContainerValue> getValues() {
    	return values;
    }
   
    public PrismContainerValue getValue() {
    	if (isSingleValue()) {
    		return values.get(0);
    	} else {
    		throw new IllegalStateException("Attempt to get single value from a multivalued container "+getName());
    	}
    }
    
    public PrismContainerValue getValue(String id) {
    	for (PrismContainerValue pval: values) {
    		if ((id == null && pval.getId() == null) ||
    				id.equals(pval.getId())) {
    			return pval;
    		}
    	}
    	return null;
    }
    
    public void add(PrismContainerValue pValue) {
    	values.add(pValue);
    }
    
    /**
     * Remove all empty values
     */
    public void trim() {
    	Iterator<PrismContainerValue> iterator = values.iterator();
    	while (iterator.hasNext()) {
    		PrismContainerValue pval = iterator.next();
    		if (pval.isEmpty()) {
    			iterator.remove();
    		}
    	}
    }
    
	private boolean isSingleValue() {
		if (getDefinition() != null) {
			return getDefinition().isSingleValue();
		}
		return values.size() == 1;
	}

    /**
     * Returns applicable property container definition.
     * <p/>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property container definition
     */
    public PrismContainerDefinition getDefinition() {
        return (PrismContainerDefinition) definition;
    }

    /**
     * Sets applicable property container definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PrismContainerDefinition definition) {
        this.definition = definition;
    }
    
    public <T extends Item> T findItem(QName itemQName, Class<T> type) {
    	return findCreateItem(itemQName, type, false);
    }
    
    <T extends Item> T findCreateItem(QName itemQName, Class<T> type, boolean create) {
    	if (isSingleValue()) {
    		return values.get(0).findCreateItem(itemQName, type, create);
    	} else {
    		throw new IllegalStateException("Attempt to get find item by QName in a multivalued container "+getName());
    	}
    }
        
    public <T extends Item> T findItem(PropertyPath propPath, Class<T> type) {
    	return findCreateItem(propPath, type, false);
    }
    
    // Expects that "self" path IS present in propPath
    <T extends Item> T findCreateItem(PropertyPath propPath, Class<T> type, boolean create) {
    	if (propPath == null || propPath.isEmpty()) {
    		throw new IllegalArgumentException("Empty path specified");
    	}
    	PropertyPathSegment first = propPath.first();
    	if (!first.getName().equals(getName())) {
    		throw new IllegalArgumentException("Expected path with first segment name "+getName()+", but got "+first);
    	}
    	PropertyPath rest = propPath.rest();
    	if (rest.isEmpty()) {
    		// This is the end ...
    		if (type.isAssignableFrom(getClass())) {
    			return (T) this;
    		} else {
    			if (create) {
    				throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because "
    						+ this.getClass().getSimpleName() + " with the same name exists"); 
    			} else {
    				return null;
    			}
    		}
    	}
    	// Othewise descent to the correct value
    	if (first.getId() == null) {
    		if (values.size() == 1) {
    			return values.get(0).findCreateItem(rest, type, create);
    		} else {
    			throw new IllegalArgumentException("Attempt to get segment "+first+" without an ID from a multi-valued container "+getName());
    		}
    	} else {
	        for (PrismContainerValue pval : values) {
	        	if (first.getId().equals(pval.getId())) {
	        		return pval.findCreateItem(rest, type, create);
	        	}
	        }
	        return null;
    	}
    }
    
    public PrismContainer findPropertyContainer(PropertyPath path) {
        return findItem(path, PrismContainer.class);
    }
    
    public PrismContainer findPropertyContainer(QName containerName) {
        return findItem(containerName, PrismContainer.class);
    }

    public PrismProperty findProperty(PropertyPath path) {
        return findItem(path, PrismProperty.class);
    }
    
    public PrismProperty findProperty(QName propertyQName) {
    	return findItem(propertyQName, PrismProperty.class);
    }

    
    public PrismContainer findOrCreatePropertyContainer(PropertyPath containerPath) {
        return findCreateItem(containerPath, PrismContainer.class, true);
    }
    
    public PrismProperty findOrCreateProperty(PropertyPath propertyPath) {
        return findCreateItem(propertyPath, PrismProperty.class, true);
    }
    
    public PrismProperty findOrCreateProperty(QName propertyName) {
        return findCreateItem(propertyName, PrismProperty.class, true);
    }
 
    // Expects that the "self" path segment is NOT included in the basePath
    void addPropertyPathsToList(PropertyPath basePath, Collection<PropertyPath> list) {
    	boolean addIds = true;
    	if (getDefinition() != null) {
    		if (getDefinition().isSingleValue()) {
    			addIds = false;
    		}
    	}
    	for (PrismContainerValue pval: values) {
    		PropertyPathSegment segment = null;
    		if (addIds) {
    			segment = new PropertyPathSegment(getName(), pval.getId());
    		} else {
    			segment = new PropertyPathSegment(getName());
    		}
    		pval.addPropertyPathsToList(basePath.subPath(segment), list);
    	}
    }

    @Override
	public void revive(PrismContext prismContext) {
		if (this.prismContext != null) {
			return;
		}
		super.revive(prismContext);
		for (PrismContainerValue pval: values) {
			pval.revive(prismContext);
		}
	}

//	@Override
//    public void serializeToDom(Node parentNode) throws SchemaException {
//        if (parentNode == null) {
//            throw new IllegalArgumentException("No parent node specified");
//        }
//        Element containerElement = DOMUtil.getDocument(parentNode).createElementNS(name.getNamespaceURI(), name.getLocalPart());
//        parentNode.appendChild(containerElement);
//        for (Item item : items) {
//            item.serializeToDom(containerElement);
//        }
//    }


    public boolean isEmpty() {
        for(PrismContainerValue pval : values) {
        	if (!pval.isEmpty()) {
        		return false;
        	}
        }
        return true;
    }

    @Override
    public PrismContainer clone() {
        PrismContainer clone = new PrismContainer(getName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismContainer clone) {
        super.copyValues(clone);
        for (PrismContainerValue pval : values) {
            clone.values.add(pval.clone());
        }
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((values == null) ? 0 : values.hashCode());
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
		PrismContainer other = (PrismContainer) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getName() + "):"
                + getValues();
    }

    @Override
    public String dump() {
        return debugDump();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ").append(DebugUtil.prettyPrint(getName()));
        sb.append(additionalDumpDescription());
        if (getDefinition() != null) {
            sb.append(" def");
        }
        Iterator<PrismContainerValue> i = getValues().iterator();
        if (i.hasNext()) {
            sb.append("\n");
        }
        while (i.hasNext()) {
        	PrismContainerValue pval = i.next();
            sb.append(pval.debugDump(indent + 1));
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    protected String additionalDumpDescription() {
        return "";
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PC";
    }

}
