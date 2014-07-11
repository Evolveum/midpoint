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

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

import java.lang.reflect.Modifier;
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
public class PrismContainer<V extends Containerable> extends Item<PrismContainerValue<V>> implements PrismContainerable<V> {
    private static final long serialVersionUID = 5206821250098051028L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainer.class);

    protected Class<V> compileTimeClass;

    public PrismContainer(QName name) {
        super(name);
    }

    public PrismContainer(QName name, PrismContext prismContext) {
        super(name, prismContext);
    }

    public PrismContainer(QName name, Class<V> compileTimeClass) {
        super(name);
		if (Modifier.isAbstract(compileTimeClass.getModifiers())) {
            throw new IllegalArgumentException("Can't use class '" + compileTimeClass.getSimpleName() + "' as compile-time class for "+name+"; the class is abstract.");
        }
        this.compileTimeClass = compileTimeClass;
    }

    public PrismContainer(QName name, Class<V> compileTimeClass, PrismContext prismContext) {
        this(name, compileTimeClass);
        this.prismContext = prismContext;
    }


    protected PrismContainer(QName name, PrismContainerDefinition<V> definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }
    
    public Class<V> getCompileTimeClass() {
		if (this.compileTimeClass != null) {
			return compileTimeClass;
		}
		if (getDefinition() != null) {
			return getDefinition().getCompileTimeClass();
		}
		return null;
	}
    
    /**
	 * Returns true if this object can represent specified compile-time class.
	 * I.e. this object can be presented in the compile-time form that is an
	 * instance of a specified class.
	 */
	public boolean canRepresent(Class<?> compileTimeClass) {
		return (compileTimeClass.isAssignableFrom(getCompileTimeClass()));
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
    		throw new IllegalStateException("Attempt to get single value from a multivalued container "+ getElementName());
    	}
    	// We are not sure about multiplicity if there is no definition or the definition is dynamic
    	if (getDefinition() != null && !getDefinition().isDynamic()) {
			if (getDefinition().isSingleValue()) {
				// Insert first empty value. This simulates empty single-valued container. It the container exists
		        // it is clear that it has at least one value (and that value is empty).
				PrismContainerValue<V> pValue = new PrismContainerValue<V>(null, null, this, null, null, prismContext);
		        try {
					add(pValue);
				} catch (SchemaException e) {
					// This should not happen
					throw new SystemException("Internal Error: "+e.getMessage(),e);
				}
		        return pValue;
			} else {
				throw new IllegalStateException("Attempt to get single value from a multivalued container "+ getElementName());
			}
		} else {
			// Insert first empty value. This simulates empty single-valued container. It the container exists
	        // it is clear that it has at least one value (and that value is empty).
			PrismContainerValue<V> pValue = new PrismContainerValue<V>(null, null, this, null, null, prismContext);
	        try {
				add(pValue);
			} catch (SchemaException e) {
				// This should not happen
				throw new SystemException("Internal Error: "+e.getMessage(),e);
			}
	        return pValue;
		}
    }
    
    public void setValue(PrismContainerValue<V> value) throws SchemaException {
    	if (getDefinition() != null) {
			if (getDefinition().isSingleValue()) {
				clear();
		        add(value);
			} else {
				throw new IllegalStateException("Attempt to set single value to a multivalued container "+ getElementName());
			}
		} else {
			clear();
	        add(value);
		}
    }

    @Override
    public boolean add(PrismContainerValue newValue) throws SchemaException {
        // when a context-less item is added to a contextful container, it is automatically adopted
        if (newValue.getPrismContext() == null && this.prismContext != null) {
            prismContext.adopt(newValue);
        }
        return super.add(newValue);
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
    
    public PrismContainerValue<V> getValue(Long id) {
    	for (PrismContainerValue<V> pval: getValues()) {
    		if ((id == null && pval.getId() == null) ||
    				id.equals(pval.getId())) {
    			return pval;
    		}
    	}
    	return null;
    }
    
    public void setPropertyRealValue(QName propertyName, Object realValue) throws SchemaException {
    	PrismProperty<?> property = findOrCreateProperty(propertyName);
    	property.setRealValue(realValue);
    }
    
    public <T> T getPropertyRealValue(QName propertyName, Class<T> type) {
    	PrismProperty<T> property = findProperty(propertyName);
    	if (property == null) {          // when using sql repo, even non-existing properties do not have 'null' here
    		return null;
    	}
    	return property.getRealValue(type);
    }
    
    /**
     * Convenience method. Works only on single-valued containers.
     */
    public void add(Item<?> item) throws SchemaException {
    	getValue().add(item);
    }
    
    public PrismContainerValue<V> createNewValue() {
    	PrismContainerValue<V> pValue = new PrismContainerValue<V>(prismContext);
    	try {
			add(pValue);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
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
		PrismContainerValue<V> clonedOtherValue = otherValue.clone();
		if (getDefinition() != null) {
			clonedOtherValue.applyDefinition(getDefinition());
		}
		add(clonedOtherValue);
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
    public PrismContainerDefinition<V> getDefinition() {
        return (PrismContainerDefinition<V>) definition;
    }

    /**
     * Sets applicable property container definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PrismContainerDefinition<V> definition) {
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
    	this.compileTimeClass = ((PrismContainerDefinition<V>)definition).getCompileTimeClass();
    	super.applyDefinition(definition);
	}

	public <I extends Item<?>> I findItem(QName itemQName, Class<I> type) {
    	try {
			return findCreateItem(itemQName, type, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }
	
	/**
	 * Returns true if the object and all contained prisms have proper definition.
	 */
	@Override
	public boolean hasCompleteDefinition() {
		if (getDefinition() == null) {
			return false;
		}
		for (PrismContainerValue<V> cval: getValues()) {
			if (!cval.hasCompleteDefinition()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public Object find(ItemPath path) {
		if (path == null || path.isEmpty()) {
    		return this;
    	}
    	
    	IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(path);
    	PrismContainerValue<V> cval = findValue(idSegment);
    	if (cval == null) {
    		return null;
    	}
    	// descent to the correct value
    	ItemPath rest = ItemPath.pathRestStartingWithName(path);
    	return cval.find(rest);
	}

	@Override
	public <X extends PrismValue> PartiallyResolvedValue<X> findPartial(ItemPath path) {
		if (path == null || path.isEmpty()) {
    		return new PartiallyResolvedValue<X>((Item<X>)this, null);
    	}
    	
    	IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(path);
    	PrismContainerValue<V> cval = findValue(idSegment);
    	if (cval == null) {
    		return null;
    	}
    	// descent to the correct value
    	ItemPath rest = ItemPath.pathRestStartingWithName(path);
    	return cval.findPartial(rest);
	}

	public Item<?> findItem(QName itemQName) {
    	try {
			return findCreateItem(itemQName, Item.class, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }
    
    <I extends Item<?>> I findCreateItem(QName itemQName, Class<I> type, boolean create) throws SchemaException {
        return getValue().findCreateItem(itemQName, type, null, create);
    }
        
    public <I extends Item<?>> I findItem(ItemPath propPath, Class<I> type) {
    	try {
			return findCreateItem(propPath, type, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }
    
    public <X extends PrismValue> Item<X> findItem(ItemPath propPath) {
    	try {
			return findCreateItem(propPath, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }
    
    <I extends Item<?>> I findCreateItem(ItemPath itemPath, Class<I> type, ItemDefinition itemDefinition, boolean create) throws SchemaException {
    	if (itemPath == null || itemPath.isEmpty()) {
    		throw new IllegalArgumentException("Empty path specified");
    	}
    	
    	if (itemPath.isEmpty()) {
    		// This is the end ...
    		if (type.isAssignableFrom(getClass())) {
    			return (I) this;
    		} else {
    			if (create) {
    				throw new IllegalStateException("The " + type.getSimpleName() + " " + getElementName() + " cannot be created because "
    						+ this.getClass().getSimpleName() + " with the same name exists"); 
    			} else {
    				return null;
    			}
    		}
    	}
    	
    	IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(itemPath);
    	PrismContainerValue<V> cval = findValue(idSegment);
    	if (cval == null) {
    		return null;
    	}
    	// descent to the correct value
    	ItemPath rest = ItemPath.pathRestStartingWithName(itemPath);
    	return cval.findCreateItem(rest, type, itemDefinition, create);
    }
    
    public PrismContainerValue<V> findValue(long id) {
        for (PrismContainerValue<V> pval : getValues()) {
        	if (id == pval.getId()) {
        		return pval;
        	}
        }
        return null;
    }

	private PrismContainerValue<V> findValue(IdItemPathSegment idSegment) {
		Long id = null;
    	if (idSegment != null) {
    		id = idSegment.getId();
    	}
    	// Otherwise descent to the correct value
    	if (id == null) {
    		if (canAssumeSingleValue()) {
    			return getValue();
    		} else {
    			throw new IllegalArgumentException("Attempt to get segment without an ID from a multi-valued container "+ getElementName());
    		}
    	} else {
	        for (PrismContainerValue<V> pval : getValues()) {
	        	if (id.equals(pval.getId())) {
	        		return pval;
	        	}
	        }
	        return null;
    	}
    }

	public <T extends Containerable> PrismContainer<T> findContainer(ItemPath path) {
        return findItem(path, PrismContainer.class);
    }
    
    public <T extends Containerable> PrismContainer<T> findContainer(QName containerName) {
        return findItem(containerName, PrismContainer.class);
    }

    public <T> PrismProperty<T> findProperty(ItemPath path) {
        return findItem(path, PrismProperty.class);
    }
    
    public <T> PrismProperty<T> findProperty(QName propertyQName) {
    	return findItem(propertyQName, PrismProperty.class);
    }

    public PrismReference findReference(ItemPath path) {
        return findItem(path, PrismReference.class);
    }
    
    public PrismReference findReference(QName referenceQName) {
    	return findItem(referenceQName, PrismReference.class);
    }
    
    public PrismReference findReferenceByCompositeObjectElementName(QName elementName) {
    	return getValue().findReferenceByCompositeObjectElementName(elementName);
    }
            
    public <T extends Item<?>> T findOrCreateItem(ItemPath containerPath, Class<T> type) throws SchemaException {
        return findCreateItem(containerPath, type, null, true);
    }
    
    // The "definition" parameter provides definition of item to create, in case that the container does not have
    // the definition (e.g. in case of "extension" containers)
    public <T extends Item<?>> T findOrCreateItem(ItemPath containerPath, Class<T> type, ItemDefinition definition) throws SchemaException {
    	if (PrismObject.class.isAssignableFrom(type)) {
    		throw new IllegalArgumentException("It makes no sense to find object in a container (class)");
    	}
    	if (definition instanceof PrismObjectDefinition<?>) {
    		throw new IllegalArgumentException("It makes no sense to find object in a container (definition)");
    	}
        return findCreateItem(containerPath, type, definition, true);
    }
    
    public <T extends Containerable> PrismContainer<T> findOrCreateContainer(ItemPath containerPath) throws SchemaException {
        return findCreateItem(containerPath, PrismContainer.class, null, true);
    }
    
    public <T extends Containerable> PrismContainer<T> findOrCreateContainer(QName containerName) throws SchemaException {
        return findCreateItem(containerName, PrismContainer.class, true);
    }
    
    public <T> PrismProperty<T> findOrCreateProperty(ItemPath propertyPath) throws SchemaException {
        return findCreateItem(propertyPath, PrismProperty.class, null, true);
    }
    
    public <T> PrismProperty<T> findOrCreateProperty(QName propertyName) throws SchemaException {
        return findCreateItem(propertyName, PrismProperty.class, true);
    }

    public PrismReference findOrCreateReference(ItemPath propertyPath) throws SchemaException {
        return findCreateItem(propertyPath, PrismReference.class, null, true);
    }
    
    public PrismReference findOrCreateReference(QName propertyName) throws SchemaException {
        return findCreateItem(propertyName, PrismReference.class, true);
    }
    
    /**
     * Convenience method. Works only on single-valued containers.
     */
    public void remove(Item<?> item) {
    	getValue().remove(item);
    }
    
    public void removeProperty(QName propertyQName) {
    	removeItem(new ItemPath(propertyQName), PrismProperty.class);
    }
    
    public void removeProperty(ItemPath path) {
        removeItem(path, PrismProperty.class);
    }
    
    public void removeContainer(QName containerQName) {
    	removeItem(new ItemPath(containerQName), PrismContainer.class);
    }
    
    public void removeContainer(ItemPath path) {
        removeItem(path, PrismContainer.class);
    }
    
    public void removeReference(QName referenceQName) {
    	removeItem(new ItemPath(referenceQName), PrismReference.class);
    }
    
    public void removeReference(ItemPath path) {
        removeItem(path, PrismReference.class);
    }

    public <I extends Item<?>> void removeItem(ItemPath path, Class<I> itemType) {
    	IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(path);
    	PrismContainerValue<V> cval = findValue(idSegment);
    	if (cval == null) {
    		return;
    	}
    	cval.removeItem(ItemPath.pathRestStartingWithName(path.rest()), itemType);
    }

    // Expects that the "self" path segment is NOT included in the basePath
    void addItemPathsToList(ItemPath basePath, Collection<ItemPath> list) {
    	boolean addIds = true;
    	if (getDefinition() != null) {
    		if (getDefinition().isSingleValue()) {
    			addIds = false;
    		}
    	}
    	for (PrismContainerValue<V> pval: getValues()) {
    		ItemPath subpath = null;
    		ItemPathSegment segment = null;
    		if (addIds) {
    			subpath = basePath.subPath(new IdItemPathSegment(pval.getId())).subPath(new NameItemPathSegment(getElementName()));
    		} else {
    			subpath = basePath.subPath(new NameItemPathSegment(getElementName()));
    		}
    		pval.addItemPathsToList(subpath, list);
    	}
    }
    
    @Override
	public ContainerDelta<V> createDelta() {
    	return new ContainerDelta<V>(getPath(), getDefinition(), getPrismContext());
	}
    
    @Override
	public ContainerDelta<V> createDelta(ItemPath path) {
    	return new ContainerDelta<V>(path, getDefinition(), getPrismContext());
	}

    public boolean isEmpty() {
        for(PrismContainerValue<V> pval : getValues()) {
        	if (!pval.isEmpty()) {
        		return false;
        	}
        }
        return true;
    }
    
	@Override
	protected void checkDefinition(ItemDefinition def) {
		if (!(def instanceof PrismContainerDefinition<?>)) {
			throw new IllegalArgumentException("Definition "+def+" cannot be applied to container "+this);
		}
	}
		
	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions,
			boolean prohibitRaw) {
		// Containers that are from run-time schema cannot have compile-time class.
		if (getDefinition() != null && !getDefinition().isRuntimeSchema()) {
			if (getCompileTimeClass() == null) {
				throw new IllegalStateException("No compile-time class in "+this+" ("+getPath()+" in "+rootItem+")");
			}
		}
		super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw);
	}

	@Override
	public void assertDefinitions(boolean tolarateRaw, String sourceDescription) throws SchemaException {
		super.assertDefinitions(tolarateRaw, sourceDescription);
		for (PrismContainerValue<V> val: getValues()) {
			val.assertDefinitions(tolarateRaw, this.toString()+" in "+sourceDescription);
		}
	}

	@Override
    public PrismContainer<V> clone() {
    	PrismContainer<V> clone = new PrismContainer<V>(getElementName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismContainer<V> clone) {
        super.copyValues(clone);
        clone.compileTimeClass = this.compileTimeClass;
        for (PrismContainerValue<V> pval : getValues()) {
            try {
				clone.add(pval.clone());
			} catch (SchemaException e) {
				// This should not happen
				throw new SystemException("Internal Error: "+e.getMessage(),e);
			}
        }
    }

    @Override
	public boolean containsEquivalentValue(PrismContainerValue<V> value) {
    	if (value.getId() == null) {
    		return super.contains(value, true);
    	} else {
    		return super.contains(value, false);
    	}
	}
    
    @Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
		if (path == null || path.isEmpty()) {
			// We are at the end of path, continue with regular visits all the way to the bottom
			if (recursive) {
    			accept(visitor);
    		} else {
    			visitor.visit(this);
    		}
		} else {
			IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(path);
			ItemPath rest = ItemPath.pathRestStartingWithName(path);
			if (idSegment == null || idSegment.isWildcard()) {
				// visit all values
				for (PrismContainerValue<V> cval: getValues()) {
					cval.accept(visitor, rest, recursive);
				}
			} else {
				PrismContainerValue<V> cval = findValue(idSegment);
		    	if (cval != null) {
		    		cval.accept(visitor, rest, recursive);
		    	}
			}
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
        return getDebugDumpClassName() + "(" + getElementName() + "):"
                + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        if (DebugUtil.isDetailedDebugDump()) {
        	sb.append(getDebugDumpClassName()).append(": ");
        }
        sb.append(DebugUtil.formatElementName(getElementName()));
        sb.append(": ");
        sb.append(additionalDumpDescription());
        PrismContainerDefinition<V> def = getDefinition();
        if (DebugUtil.isDetailedDebugDump()) {
	        if (def != null) {
	            sb.append(" def(");
	            sb.append(PrettyPrinter.prettyPrint(def.getTypeName()));
	            if (def.isRuntimeSchema()) {
	            	sb.append(",runtime");
	            }
	            if (def.isDynamic()) {
	            	sb.append(",dyn");
	            }
	            sb.append(")");
	        }
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

    public static <V extends Containerable> PrismContainer<V> newInstance(PrismContext prismContext, QName type) throws SchemaException {
        PrismContainerDefinition<V> definition = prismContext.getSchemaRegistry().findContainerDefinitionByType(type);
        if (definition == null) {
            throw new SchemaException("Definition for " + type + " couldn't be found");
        }
        return definition.instantiate();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PC";
    }

}
