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

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

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
public class PrismContainer<V> extends Item<PrismContainerValue<V>> implements PrismContainerable {
    private static final long serialVersionUID = 5206821250098051028L;

    public PrismContainer(QName name) {
        super(name);
    }
    
    protected PrismContainer(QName name, PrismContainerDefinition definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }

    @Override
    public List<PrismContainerValue<V>> getValues() {
    	return (List<PrismContainerValue<V>>) super.getValues();
    }
   
    public PrismContainerValue<V> getValue() {
    	if (getValues().size() == 1) {
    		return getValues().get(0);
		}
    	if (getValues().size() > 1) {
    		throw new IllegalStateException("Attempt to get single value from a multivalued container "+getName());
    	}
    	if (getDefinition() != null) {
			if (getDefinition().isSingleValue()) {
				// Insert first empty value. This simulates empty single-valued container. It the container exists
		        // it is clear that it has at least one value (and that value is empty).
				PrismContainerValue<V> pValue = new PrismContainerValue<V>(null, null, this, null);
		        add(pValue);
		        return pValue;
			} else {
				throw new IllegalStateException("Attempt to get single value from a multivalued container "+getName());
			}
		} else {
			// Insert first empty value. This simulates empty single-valued container. It the container exists
	        // it is clear that it has at least one value (and that value is empty).
			PrismContainerValue<V> pValue = new PrismContainerValue<V>(null, null, this, null);
	        add(pValue);
	        return pValue;
		}
    }
    
	@Override
	public PrismContainerValue<V> getPreviousValue(PrismValue value) {
		return (PrismContainerValue<V>) super.getPreviousValue(value);
	}

	@Override
	public PrismContainerValue<V> getNextValue(PrismValue value) {
		return (PrismContainerValue<V>) super.getNextValue(value);
	}

	private boolean canAssumeSingleValue() {
		if (getDefinition() != null) {
			return getDefinition().isSingleValue();
		} else {
			if (getValues().size() <= 1) {
				return true;
			} else {
				return false;
			}
		}
	}
    
    public PrismContainerValue<V> getValue(String id) {
    	for (PrismContainerValue<V> pval: getValues()) {
    		if ((id == null && pval.getId() == null) ||
    				id.equals(pval.getId())) {
    			return pval;
    		}
    	}
    	return null;
    }
    
    public void setPropertyRealValue(QName propertyName, Object realValue) {
    	PrismProperty<?> property = findOrCreateProperty(propertyName);
    	property.setRealValue(realValue);
    }
    
    public <T> T getPropertyRealValue(QName propertyName, Class<T> type) {
    	PrismProperty<?> property = findProperty(propertyName);
    	if (property == null) {
    		return null;
    	}
    	return property.getRealValue(type);
    }
    
    public boolean add(PrismContainerValue<V> pValue) {
    	pValue.setParent(this);
    	return getValues().add(pValue);
    }
    
    /**
     * Convenience method. Works only on single-valued containers.
     */
    public void add(Item<?> item) {
    	getValue().add(item);
    }
    
    /**
     * Convenience method. Works only on single-valued containers.
     */
    public void remove(Item<?> item) {
    	getValue().remove(item);
    }
    
    public PrismContainerValue<V> createNewValue() {
    	PrismContainerValue<V> pValue = new PrismContainerValue<V>();
    	add(pValue);
    	return pValue;
    }
    
    public void mergeValues(PrismContainer<V> other) throws SchemaException {
    	mergeValues(other.getValues());
    }
    
    public void mergeValues(Collection<PrismContainerValue<V>> otherValues) throws SchemaException {
    	for (PrismContainerValue<V> otherValue : otherValues) {
    		mergeValue(otherValue);
    	}
    }
    
	public void mergeValue(PrismContainerValue<V> otherValue) throws SchemaException {
		Iterator<PrismContainerValue<V>> iterator = getValues().iterator();
		while (iterator.hasNext()) {
			PrismContainerValue<V> thisValue = iterator.next();
			if (thisValue.equals(otherValue)) {
				// Same values, nothing to merge
				return;
			}
			if (thisValue.getId() != null && thisValue.getId().equals(otherValue.getId())) {
				// Different value but same id. New value overwrites.
				iterator.remove();
			}
		}
		otherValue.setParent(this);
		if (getDefinition() != null) {
			otherValue.applyDefinition(getDefinition());
		}
		add(otherValue);
	}

	/**
     * Remove all empty values
     */
    public void trim() {
    	Iterator<PrismContainerValue<V>> iterator = getValues().iterator();
    	while (iterator.hasNext()) {
    		PrismContainerValue<V> pval = iterator.next();
    		if (pval.isEmpty()) {
    			iterator.remove();
    		}
    	}
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
    
    @Override
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
    	if (definition == null) {
    		return;
    	}
    	if (!(definition instanceof PrismContainerDefinition)) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to container " + this);
    	}
    	super.applyDefinition(definition);
	}

	public <I extends Item<?>> I findItem(QName itemQName, Class<I> type) {
    	return findCreateItem(itemQName, type, false);
    }
	
	public Item<?> findItem(QName itemQName) {
    	return findCreateItem(itemQName, Item.class, false);
    }
    
    <I extends Item<?>> I findCreateItem(QName itemQName, Class<I> type, boolean create) {
   		return getValue().findCreateItem(itemQName, type, null, create);
    }
        
    public <I extends Item<?>> I findItem(PropertyPath propPath, Class<I> type) {
    	return findCreateItem(propPath, type, null, false);
    }
    
    public Item<?> findItem(PropertyPath propPath) {
    	return findCreateItem(propPath, Item.class, null, false);
    }
    
    // Expects that "self" path IS present in propPath
    <I extends Item<?>> I findCreateItem(PropertyPath propPath, Class<I> type, ItemDefinition itemDefinition, boolean create) {
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
    			return (I) this;
    		} else {
    			if (create) {
    				throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because "
    						+ this.getClass().getSimpleName() + " with the same name exists"); 
    			} else {
    				return null;
    			}
    		}
    	}
    	// Otherwise descent to the correct value
    	if (first.getId() == null) {
    		if (canAssumeSingleValue()) {
    			return getValue().findCreateItem(rest, type, itemDefinition, create);
    		} else {
    			throw new IllegalArgumentException("Attempt to get segment "+first+" without an ID from a multi-valued container "+getName());
    		}
    	} else {
	        for (PrismContainerValue<V> pval : getValues()) {
	        	if (first.getId().equals(pval.getId())) {
	        		return pval.findCreateItem(rest, type, itemDefinition, create);
	        	}
	        }
	        return null;
    	}
    }
    
	public <T> PrismContainer<T> findContainer(PropertyPath path) {
        return findItem(path, PrismContainer.class);
    }
    
    public <T> PrismContainer<T> findContainer(QName containerName) {
        return findItem(containerName, PrismContainer.class);
    }

    public <T> PrismProperty<T> findProperty(PropertyPath path) {
        return findItem(path, PrismProperty.class);
    }
    
    public <T> PrismProperty<T> findProperty(QName propertyQName) {
    	return findItem(propertyQName, PrismProperty.class);
    }

    public PrismReference findReference(PropertyPath path) {
        return findItem(path, PrismReference.class);
    }
    
    public PrismReference findReference(QName referenceQName) {
    	return findItem(referenceQName, PrismReference.class);
    }
    
    public <T extends Item<?>> T findOrCreateItem(PropertyPath containerPath, Class<T> type) {
        return findCreateItem(containerPath, type, null, true);
    }
    
    // The "definition" parameter provides definition of item to create, in case that the container does not have
    // the definition (e.g. in case of "extension" containers)
    public <T extends Item<?>> T findOrCreateItem(PropertyPath containerPath, Class<T> type, ItemDefinition definition) {
        return findCreateItem(containerPath, type, definition, true);
    }
    
    public <T> PrismContainer<T> findOrCreateContainer(PropertyPath containerPath) {
        return findCreateItem(containerPath, PrismContainer.class, null, true);
    }
    
    public <T> PrismContainer<T> findOrCreateContainer(QName containerName) {
        return findCreateItem(containerName, PrismContainer.class, true);
    }
    
    public <T> PrismProperty<T> findOrCreateProperty(PropertyPath propertyPath) {
        return findCreateItem(propertyPath, PrismProperty.class, null, true);
    }
    
    public <T> PrismProperty<T> findOrCreateProperty(QName propertyName) {
        return findCreateItem(propertyName, PrismProperty.class, true);
    }

    public PrismReference findOrCreateReference(PropertyPath propertyPath) {
        return findCreateItem(propertyPath, PrismReference.class, null, true);
    }
    
    public PrismReference findOrCreateReference(QName propertyName) {
        return findCreateItem(propertyName, PrismReference.class, true);
    }

    // Expects that the "self" path segment is NOT included in the basePath
    void addItemPathsToList(PropertyPath basePath, Collection<PropertyPath> list) {
    	boolean addIds = true;
    	if (getDefinition() != null) {
    		if (getDefinition().isSingleValue()) {
    			addIds = false;
    		}
    	}
    	for (PrismContainerValue<V> pval: getValues()) {
    		PropertyPathSegment segment = null;
    		if (addIds) {
    			segment = new PropertyPathSegment(getName(), pval.getId());
    		} else {
    			segment = new PropertyPathSegment(getName());
    		}
    		pval.addItemPathsToList(basePath.subPath(segment), list);
    	}
    }
    
    @Override
	public ContainerDelta<V> createDelta(PropertyPath path) {
    	return new ContainerDelta<V>(path, getDefinition());
	}

    public Collection<? extends ItemDelta> diff(PrismContainer<V> other, PropertyPath pathPrefix) {
    	return diff(other, pathPrefix, true);
    }
    
    public Collection<? extends ItemDelta> diff(PrismContainer<V> other, PropertyPath pathPrefix, boolean ignoreMetadata) {
    	Collection<? extends ItemDelta> deltas = new ArrayList<ItemDelta>();
    	diffInternal(other, pathPrefix, deltas, ignoreMetadata);
    	return deltas;
    }
    
    @Override
	public void revive(PrismContext prismContext) {
		if (this.prismContext != null) {
			return;
		}
		super.revive(prismContext);
		for (PrismContainerValue<V> pval: getValues()) {
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
        for(PrismContainerValue<V> pval : getValues()) {
        	if (!pval.isEmpty()) {
        		return false;
        	}
        }
        return true;
    }

    @Override
    public PrismContainer<V> clone() {
    	PrismContainer<V> clone = new PrismContainer<V>(getName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismContainer<V> clone) {
        super.copyValues(clone);
        for (PrismContainerValue<V> pval : getValues()) {
            clone.add(pval.clone());
        }
    }

    @Override
	public int hashCode() {
		int result = super.hashCode();
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
		return true;
	}

    /**
     * this method ignores some part of the object during comparison (e.g. source demarkation in values)
     * These methods compare the "meaningful" parts of the objects.
     */
    public boolean equivalent(Object obj) {
        // Alibistic implementation for now. But should work well.
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        PrismContainer other = (PrismContainer) obj;

        //todo probably better comparation (ignore some part of object)
        return equals(other);
//        ObjectDelta<T> delta = compareTo(other);
//        return delta.isEmpty();
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
        Iterator<PrismContainerValue<V>> i = getValues().iterator();
        if (i.hasNext()) {
            sb.append("\n");
        }
        while (i.hasNext()) {
        	PrismContainerValue<V> pval = i.next();
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
