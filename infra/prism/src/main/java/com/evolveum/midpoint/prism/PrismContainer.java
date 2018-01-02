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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

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
public class PrismContainer<C extends Containerable> extends Item<PrismContainerValue<C>,PrismContainerDefinition<C>> implements PrismContainerable<C> {
    private static final long serialVersionUID = 5206821250098051028L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainer.class);

    protected Class<C> compileTimeClass;

    public PrismContainer(QName name) {
        super(name);
    }

    public PrismContainer(QName name, PrismContext prismContext) {
        super(name, prismContext);
    }

    public PrismContainer(QName name, Class<C> compileTimeClass) {
        super(name);
		if (Modifier.isAbstract(compileTimeClass.getModifiers())) {
            throw new IllegalArgumentException("Can't use class '" + compileTimeClass.getSimpleName() + "' as compile-time class for "+name+"; the class is abstract.");
        }
        this.compileTimeClass = compileTimeClass;
    }

    public PrismContainer(QName name, Class<C> compileTimeClass, PrismContext prismContext) {
        this(name, compileTimeClass);
        this.prismContext = prismContext;
		if (prismContext != null) {
			try {
				prismContext.adopt(this);
			} catch (SchemaException e) {
				throw new SystemException("Schema exception when adopting freshly created PrismContainer: " + this);
			}
		}
    }


    public PrismContainer(QName name, PrismContainerDefinition<C> definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }

    public Class<C> getCompileTimeClass() {
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

	@NotNull
    @Override
    public Collection<C> getRealValues() {
	    List<C> realValues = new ArrayList<>(getValues().size());
		for (PrismContainerValue<C> value : getValues()) {
			realValues.add(value.asContainerable());
		}

		return realValues;

	}

    @Override
    public C getRealValue() {
    	if (getValue() == null) {
    		return null;
    	}
    	return getValue().asContainerable();
    }

    public PrismContainerValue<C> getOrCreateValue() {
		if (getValues().size() == 0) {
			return createNewValue();
		} else {
			return getValue();
		}
    }

    public PrismContainerValue<C> getValue() {
    	if (getValues().size() == 1) {
    		return getValues().get(0);
		}
    	if (getValues().size() > 1) {
    		throw new IllegalStateException("Attempt to get single value from a multivalued container "+ getElementName());
    	}
    	// We are not sure about multiplicity if there is no definition or the definition is dynamic
		// TODO why testing for isDynamic? consider Item.isSingleValue (we already removed this condition from there); see MID-3922
    	if (getDefinition() != null && !getDefinition().isDynamic()) {
			if (getDefinition().isSingleValue()) {
				// Insert first empty value. This simulates empty single-valued container. It the container exists
		        // it is clear that it has at least one value (and that value is empty).
				PrismContainerValue<C> pValue = new PrismContainerValue<C>(null, null, this, null, null, prismContext);
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
			PrismContainerValue<C> pValue = new PrismContainerValue<C>(null, null, this, null, null, prismContext);
	        try {
				add(pValue);
			} catch (SchemaException e) {
				// This should not happen
				throw new SystemException("Internal Error: "+e.getMessage(),e);
			}
	        return pValue;
		}
    }

    public void setValue(@NotNull PrismContainerValue<C> value) throws SchemaException {
		checkMutability();
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
    public boolean add(@NotNull PrismContainerValue newValue, boolean checkUniqueness) throws SchemaException {
		checkMutability();
        // when a context-less item is added to a contextful container, it is automatically adopted
        if (newValue.getPrismContext() == null && this.prismContext != null) {
            prismContext.adopt(newValue);
        }
        if (newValue.getId() != null) {
            for (PrismContainerValue existingValue : getValues()) {
                if (existingValue.getId() != null && existingValue.getId().equals(newValue.getId())) {
                    throw new IllegalStateException("Attempt to add a container value with an id that already exists: " + newValue.getId());
                }
            }
        }
        return super.add(newValue, checkUniqueness);
    }

	@Override
	public PrismContainerValue<C> getPreviousValue(PrismValue value) {
		return (PrismContainerValue<C>) super.getPreviousValue(value);
	}

	@Override
	public PrismContainerValue<C> getNextValue(PrismValue value) {
		return (PrismContainerValue<C>) super.getNextValue(value);
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

    public PrismContainerValue<C> getValue(Long id) {
    	for (PrismContainerValue<C> pval: getValues()) {
    		if ((id == null && pval.getId() == null) ||
    				id.equals(pval.getId())) {
    			return pval;
    		}
    	}
    	return null;
    }

    public void setPropertyRealValue(QName propertyName, Object realValue) throws SchemaException {
		checkMutability();
    	PrismProperty<?> property = findOrCreateProperty(propertyName);
    	property.setRealValue(realValue);
    }

    public <T> T getPropertyRealValue(QName propertyName, Class<T> type) {
    	return getPropertyRealValue(new ItemPath(propertyName), type);
    }

	public <T> T getPropertyRealValue(ItemPath propertyPath, Class<T> type) {
		PrismProperty<T> property = findProperty(propertyPath);
		if (property == null) {
			return null;
		}
		return property.getRealValue(type);
	}

    /**
     * Convenience method. Works only on single-valued containers.
     */
    public void add(Item<?,?> item) throws SchemaException {
		checkMutability();
    	getValue().add(item);
    }

    public PrismContainerValue<C> createNewValue() {
		checkMutability();
    	PrismContainerValue<C> pValue = new PrismContainerValue<>(prismContext);
    	try {
    		// No need to check uniqueness, we know that this value is new and therefore
    		// it will change anyway and therefore the check is pointless.
    		// However, the check is expensive (especially if there are many values).
    		// So we are happy to avoid it.
			add(pValue, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    	return pValue;
    }

    public void mergeValues(PrismContainer<C> other) throws SchemaException {
    	mergeValues(other.getValues());
    }

    public void mergeValues(Collection<PrismContainerValue<C>> otherValues) throws SchemaException {
    	for (PrismContainerValue<C> otherValue : otherValues) {
    		mergeValue(otherValue);
    	}
    }

	public void mergeValue(PrismContainerValue<C> otherValue) throws SchemaException {
		checkMutability();
		Iterator<PrismContainerValue<C>> iterator = getValues().iterator();
		while (iterator.hasNext()) {
			PrismContainerValue<C> thisValue = iterator.next();
			if (thisValue.equals(otherValue)) {
				// Same values, nothing to merge
				return;
			}
			if (thisValue.getId() != null && thisValue.getId().equals(otherValue.getId())) {
				// Different value but same id. New value overwrites.
				iterator.remove();
			}
		}
		PrismContainerValue<C> clonedOtherValue = otherValue.clone();
		if (getDefinition() != null) {
			clonedOtherValue.applyDefinition(getDefinition());
		}
		add(clonedOtherValue);
	}

	/**
     * Remove all empty values
     */
    public void trim() {
		Iterator<PrismContainerValue<C>> iterator = getValues().iterator();
    	while (iterator.hasNext()) {
    		PrismContainerValue<C> pval = iterator.next();
    		if (pval.isEmpty()) {
				checkMutability();
    			iterator.remove();
    		}
    	}
    }

    /**
     * Returns applicable property container definition.
     * <p>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property container definition
     */
    public PrismContainerDefinition<C> getDefinition() {
        return (PrismContainerDefinition<C>) definition;
    }

    /**
     * Sets applicable property container definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PrismContainerDefinition<C> definition) {
		checkMutability();
		if (definition != null) {
			for (PrismContainerValue<C> value : getValues()) {
				// TODO reconsider this - sometimes we want to change CTDs, sometimes not
				boolean safeToOverwrite =
						value.getComplexTypeDefinition() == null
						|| this.definition == null								// TODO highly dangerous (the definition might be simply unknown)
						|| this.definition.getComplexTypeDefinition() == null
						|| this.definition.getComplexTypeDefinition().getTypeName().equals(value.getComplexTypeDefinition().getTypeName());
				if (safeToOverwrite) {
					value.replaceComplexTypeDefinition(definition.getComplexTypeDefinition());
				}
			}
		}
		this.definition = definition;
    }

    @Override
	public void applyDefinition(PrismContainerDefinition<C> definition) throws SchemaException {
		checkMutability();
    	if (definition == null) {
    		return;
    	}
		this.compileTimeClass = definition.getCompileTimeClass();
    	super.applyDefinition(definition);
	}

	public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findItem(QName itemQName, Class<I> type) {
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
		for (PrismContainerValue<C> cval: getValues()) {
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
    	PrismContainerValue<C> cval = findValue(idSegment);
    	if (cval == null) {
    		return null;
    	}
    	// descent to the correct value
    	ItemPath rest = ItemPath.pathRestStartingWithName(path);
    	return cval.find(rest);
	}

	@Override
	public <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path) {
		if (path == null || path.isEmpty()) {
    		return new PartiallyResolvedItem<IV,ID>((Item<IV,ID>) this, null);
    	}

    	IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(path);
    	PrismContainerValue<C> cval = findValue(idSegment);
    	if (cval == null) {
    		return null;
    	}
    	// descent to the correct value
    	ItemPath rest = ItemPath.pathRestStartingWithName(path);
    	return cval.findPartial(rest);
	}

	public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItem(QName itemQName) {
    	try {
			return findCreateItem(itemQName, Item.class, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findCreateItem(QName itemQName, Class<I> type, boolean create) throws SchemaException {
        return getValue().findCreateItem(itemQName, type, null, create);
    }

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findItem(ItemPath path, Class<I> type) {
    	try {
			return findCreateItem(path, type, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error:(path="+path+",type="+type+"): "+e.getMessage(),e);
		}
    }

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItem(ItemPath path) {
    	try {
			return findCreateItem(path, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error:(path="+path+"): "+e.getMessage(),e);
		}
    }

    public boolean containsItem(ItemPath itemPath, boolean acceptEmptyItem) throws SchemaException {
    	if (itemPath == null || itemPath.isEmpty()) {
    		throw new IllegalArgumentException("Empty path specified");
    	}

    	ItemPath rest = ItemPath.pathRestStartingWithName(itemPath);
    	for (PrismContainerValue<C> value: getValues()) {
    		if (value.containsItem(rest, acceptEmptyItem)) {
    			return true;
    		}
    	}

    	return false;
    }

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findCreateItem(ItemPath itemPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
    	if (itemPath == null || itemPath.isEmpty()) {
    		throw new IllegalArgumentException("Empty path specified");
    	}
    	
    	IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(itemPath);
    	PrismContainerValue<C> cval = findValue(idSegment);
    	if (cval == null) {
    		return null;
    	}
    	// descent to the correct value
    	ItemPath rest = ItemPath.pathRestStartingWithName(itemPath);
    	return cval.findCreateItem(rest, type, itemDefinition, create);
    }

    public PrismContainerValue<C> findValue(long id) {
        for (PrismContainerValue<C> pval : getValues()) {
        	if (id == pval.getId()) {
        		return pval;
        	}
        }
        return null;
    }

	private PrismContainerValue<C> findValue(IdItemPathSegment idSegment) {
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
	        for (PrismContainerValue<C> pval : getValues()) {
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

    public <I extends Item<?,?>> List<I> getItems(Class<I> type) {
		return getValue().getItems(type);
    }

    @SuppressWarnings("unchecked")
    public List<PrismContainer<?>> getContainers() {
	    return (List) getItems(PrismContainer.class);
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

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath containerPath, Class<I> type) throws SchemaException {
        return findCreateItem(containerPath, type, null, true);
    }

    // The "definition" parameter provides definition of item to create, in case that the container does not have
    // the definition (e.g. in case of "extension" containers)
    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath containerPath, Class<I> type, ID definition) throws SchemaException {
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
    public void remove(Item<?,?> item) {
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

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> void removeItem(ItemPath path, Class<I> itemType) {
    	IdItemPathSegment idSegment = ItemPath.getFirstIdSegment(path);
    	PrismContainerValue<C> cval = findValue(idSegment);
    	if (cval == null) {
    		return;
    	}
    	cval.removeItem(ItemPath.pathRestStartingWithName(path.rest()), itemType);
    }

    // Expects that the "self" path segment is NOT included in the basePath
    // is this method used anywhere?
//    void addItemPathsToList(ItemPath basePath, Collection<ItemPath> list) {
//    	boolean addIds = true;
//    	if (getDefinition() != null) {
//    		if (getDefinition().isSingleValue()) {
//    			addIds = false;
//    		}
//    	}
//    	for (PrismContainerValue<V> pval: getValues()) {
//    		ItemPath subpath = null;
//    		ItemPathSegment segment = null;
//    		if (addIds) {
//    			subpath = basePath.subPath(new IdItemPathSegment(pval.getId())).subPath(new NameItemPathSegment(getElementName()));
//    		} else {
//    			subpath = basePath.subPath(new NameItemPathSegment(getElementName()));
//    		}
//    		pval.addItemPathsToList(subpath, list);
//    	}
//    }

    @Override
	public ContainerDelta<C> createDelta() {
    	return new ContainerDelta<C>(getPath(), getDefinition(), getPrismContext());
	}

    @Override
	public ContainerDelta<C> createDelta(ItemPath path) {
    	return new ContainerDelta<C>(path, getDefinition(), getPrismContext());
	}

    public boolean isEmpty() {
        for (PrismContainerValue<C> pval : getValues()) {
        	if (!pval.isEmpty()) {
        		return false;
        	}
        }
        return true;
    }

	@Override
	protected void checkDefinition(PrismContainerDefinition<C> def) {
		if (def == null) {
			throw new IllegalArgumentException("Null definition cannot be applied to container "+this);
		}
	}

	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions,
			boolean prohibitRaw, ConsistencyCheckScope scope) {
        checkIds();
        if (scope.isThorough()) {
            // Containers that are from run-time schema cannot have compile-time class.
            if (getDefinition() != null && !getDefinition().isRuntimeSchema()) {
                if (getCompileTimeClass() == null) {
                    throw new IllegalStateException("No compile-time class in "+this+" ("+getPath()+" in "+rootItem+")");
                }
            }
        }
		super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
	}

    private void checkIds() {
        Set<Long> oidsUsed = new HashSet<>();
        for (PrismContainerValue value : getValues()) {
            Long id = value.getId();
            if (id != null) {
                if (oidsUsed.contains(id)) {
                    throw new IllegalArgumentException("There are more container values with the id of " + id + " in " + getElementName());
                } else {
                    oidsUsed.add(id);
                }
            }
        }
    }

    @Override
	public void assertDefinitions(boolean tolarateRaw, String sourceDescription) throws SchemaException {
		super.assertDefinitions(tolarateRaw, sourceDescription);
		for (PrismContainerValue<C> val: getValues()) {
			val.assertDefinitions(tolarateRaw, this.toString()+" in "+sourceDescription);
		}
	}

	public ContainerDelta<C> diff(PrismContainer<C> other) {
		return (ContainerDelta<C>) super.diff(other);
    }

    public ContainerDelta<C> diff(PrismContainer<C> other, boolean ignoreMetadata, boolean isLiteral) {
    	return (ContainerDelta<C>) super.diff(other, true, false);
    }

    public List<? extends ItemDelta> diffModifications(PrismContainer<C> other) {
    	return diffModifications(other, true, false);
    }

    public List<? extends ItemDelta> diffModifications(PrismContainer<C> other, boolean ignoreMetadata, boolean isLiteral) {
    	List<? extends ItemDelta> itemDeltas = new ArrayList<>();
		diffInternal(other, itemDeltas, ignoreMetadata, isLiteral);
		return itemDeltas;
    }

	@Override
    public PrismContainer<C> clone() {
    	PrismContainer<C> clone = new PrismContainer<C>(getElementName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismContainer<C> clone) {
        super.copyValues(clone);
        clone.compileTimeClass = this.compileTimeClass;
        for (PrismContainerValue<C> pval : getValues()) {
            try {
				clone.add(pval.clone());
			} catch (SchemaException e) {
				// This should not happen
				throw new SystemException("Internal Error: "+e.getMessage(),e);
			}
        }
    }

    public PrismContainerDefinition<C> deepCloneDefinition(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
    	PrismContainerDefinition<C> clonedDef = (PrismContainerDefinition<C>) getDefinition().deepClone(ultraDeep, postCloneAction);
		propagateDeepCloneDefinition(ultraDeep, clonedDef, postCloneAction);
		setDefinition(clonedDef);
		return clonedDef;
	}

    @Override
    protected void propagateDeepCloneDefinition(boolean ultraDeep, PrismContainerDefinition<C> clonedDef, Consumer<ItemDefinition> postCloneAction) {
		for(PrismContainerValue<C> cval: getValues()) {
			cval.deepCloneDefinition(ultraDeep, clonedDef, postCloneAction);
		}
	}

    @Override
	public boolean containsEquivalentValue(PrismContainerValue<C> value) {
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
				for (PrismContainerValue<C> cval: getValues()) {
					cval.accept(visitor, rest, recursive);
				}
			} else {
				PrismContainerValue<C> cval = findValue(idSegment);
		    	if (cval != null) {
		    		cval.accept(visitor, rest, recursive);
		    	}
			}
		}
	}

	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost precisely equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent().
	 */
	@Override
	public int hashCode() {
		int result = super.hashCode();
		return result;
	}

	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost precisely equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent().
	 */
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
     * This method ignores some part of the object during comparison (e.g. source demarcation in values)
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
        DebugUtil.indentDebugDump(sb, indent);
        if (DebugUtil.isDetailedDebugDump()) {
        	sb.append(getDebugDumpClassName()).append(": ");
        }
        sb.append(DebugUtil.formatElementName(getElementName()));
        sb.append(": ");
        appendDebugDumpSuffix(sb);
        PrismContainerDefinition<C> def = getDefinition();
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
	            if (def.isElaborate()) {
	            	sb.append(",elaborate");
	            }
	            sb.append(")");
	        }
        } else {
        	if (def != null) {
	        	if (def.isElaborate()) {
	            	sb.append(",elaborate");
	            }
        	}
        }
		Iterator<PrismContainerValue<C>> i = getValues().iterator();
        if (i.hasNext()) {
            sb.append("\n");
        }
        while (i.hasNext()) {
        	PrismContainerValue<C> pval = i.next();
    		sb.append(pval.debugDump(indent + 1));
            if (i.hasNext()) {
        		sb.append("\n");
            }
        }
        return sb.toString();
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

	public static <V extends PrismContainerValue> void createParentIfNeeded(V value, ItemDefinition definition) throws SchemaException {
		if (value.getParent() != null) {
			return;
		}
		if (!(definition instanceof PrismContainerDefinition)) {
			throw new SchemaException("Missing or invalid definition for a PrismContainer: " + definition);
		}
		PrismContainer<?> rv = (PrismContainer) definition.instantiate();
		rv.add(value);
	}

	/**
	 * Optimizes (trims) definition tree by removing any definitions not corresponding to items in this container.
	 * Works recursively by sub-containers of this one.
	 * USE WITH CARE. Make sure the definitions are not shared by other objects!
	 */
	public void trimDefinitionTree(Collection<ItemPath> alwaysKeep) {
		PrismContainerDefinition<C> def = getDefinition();
		if (def == null || def.getComplexTypeDefinition() == null) {
			return;
		}
		Set<ItemPath> allPaths = getAllItemPaths(alwaysKeep);
		def.getComplexTypeDefinition().trimTo(allPaths);
		values.forEach(v -> v.trimItemsDefinitionsTrees(alwaysKeep));
	}

	// TODO implement more efficiently
	private Set<ItemPath> getAllItemPaths(Collection<ItemPath> alwaysKeep) {
		Set<ItemPath> paths = new HashSet<>();
		paths.addAll(CollectionUtils.emptyIfNull(alwaysKeep));
		this.accept(v -> {
			if (v instanceof PrismValue) {
				paths.add(((PrismValue) v).getPath());
			}
		});
		return paths;
	}
}
