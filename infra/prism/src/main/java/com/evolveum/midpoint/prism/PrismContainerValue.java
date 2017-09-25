/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.marshaller.JaxbDomHack;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
 *
 */
public class PrismContainerValue<C extends Containerable> extends PrismValue implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerValue.class);

	// This is list. We need to maintain the order internally to provide consistent
    // output in DOM and other ordering-sensitive representations
	protected List<Item<?,?>> items = null;
    private Long id;

	private C containerable = null;

	// Definition of this value. Usually it is the same as CTD declared in the parent container.
	// However, in order to support polymorphism (as well as parent-less values) we distinguish between PC and PCV type definition.
	protected ComplexTypeDefinition complexTypeDefinition = null;

	public PrismContainerValue() {
	}

	public PrismContainerValue(C containerable) {
		this(containerable, null);
    }

	public PrismContainerValue(PrismContext prismContext) {
		this(null, prismContext);
	}

	public PrismContainerValue(C containerable, PrismContext prismContext) {
		super(prismContext);
		this.containerable = containerable;

		if (prismContext != null) {
			getComplexTypeDefinition();        // to determine CTD (could be also called with null prismContext, but non-null prismContext provides additional information in some cases)
		}
	}

    public PrismContainerValue(OriginType type, Objectable source, PrismContainerable container, Long id, ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext) {
		super(prismContext, type, source, container);
		this.id = id;
		this.complexTypeDefinition = complexTypeDefinition;
    }

	public static <T extends Containerable> T asContainerable(PrismContainerValue<T> value) {
    	return value != null ? value.asContainerable() : null;
	}

	@Override
    public PrismContext getPrismContext() {
        if (prismContext != null) {
            return prismContext;
        }
        if (complexTypeDefinition != null && complexTypeDefinition.getPrismContext() != null) {
			return complexTypeDefinition.getPrismContext();
		}
        if (getParent() != null) {
            return getParent().getPrismContext();
        }
        return null;
    }

	// Primarily for testing
	public PrismContext getPrismContextLocal() {
		return prismContext;
	}

	/**
     * Returns a set of items that the property container contains. The items may be properties or inner property containers.
     * <p>
     * The set may be null. In case there are no properties an empty set is
     * returned.
     * <p>
     * Returned set is mutable. Live object is returned.
     *
     * @return set of items that the property container contains.
     */

    public List<Item<?,?>> getItems() {
    	if (items == null) {
    		return null;
		}
		if (isImmutable()) {
			return Collections.unmodifiableList(items);
		} else {
			return items;
		}
    }

    @SuppressWarnings("unchecked")
	public <I extends Item<?,?>> List<I> getItems(Class<I> type) {
		List<I> rv = new ArrayList<>();
		for (Item<?, ?> item : CollectionUtils.emptyIfNull(items)) {
			if (type.isAssignableFrom(item.getClass())) {
				rv.add(((I) item));
			}
		}
		return rv;
	}

	public Item<?,?> getNextItem(Item<?,?> referenceItem) {
    	if (items == null) {
    		return null;
    	}
    	Iterator<Item<?,?>> iterator = items.iterator();
    	while (iterator.hasNext()) {
    		Item<?,?> item = iterator.next();
    		if (item == referenceItem) {
    			if (iterator.hasNext()) {
    				return iterator.next();
    			} else {
    				return null;
    			}
    		}
    	}
    	throw new IllegalArgumentException("Item "+referenceItem+" is not part of "+this);
    }

    public Item<?,?> getPreviousItem(Item<?,?> referenceItem) {
    	if (items == null){
    		return null;
    	}
    	Item<?,?> lastItem = null;
    	Iterator<Item<?,?>> iterator = items.iterator();
		//noinspection WhileLoopReplaceableByForEach
		while (iterator.hasNext()) {
    		Item<?,?> item = iterator.next();
    		if (item == referenceItem) {
    			return lastItem;
    		}
    		lastItem = item;
    	}
    	throw new IllegalArgumentException("Item "+referenceItem+" is not part of "+this);
    }


    /**
     * Returns a set of properties that the property container contains.
     * <p>
     * The set must not be null. In case there are no properties an empty set is
     * returned.
     * <p>
     * Returned set is immutable! Any change to it will be ignored.
     *
     * @return set of properties that the property container contains.
     */
    public Set<PrismProperty<?>> getProperties() {
        Set<PrismProperty<?>> properties = new HashSet<PrismProperty<?>>();
        if (items != null) {
            for (Item<?,?> item : getItems()) {
                if (item instanceof PrismProperty) {
                    properties.add((PrismProperty<?>) item);
                }
            }
        }
        return properties;
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		checkMutability();
		this.id = id;
	}

	@SuppressWarnings("unchecked")
	public PrismContainerable<C> getParent() {
		Itemable parent = super.getParent();
		if (parent == null) {
			return null;
		}
		if (!(parent instanceof PrismContainerable)) {
			throw new IllegalStateException("Expected that parent of "+PrismContainerValue.class.getName()+" will be "+
					PrismContainerable.class.getName()+", but it is "+parent.getClass().getName());
		}
		return (PrismContainerable<C>)parent;
	}

	@SuppressWarnings("unchecked")
	public PrismContainer<C> getContainer() {
		Itemable parent = super.getParent();
		if (parent == null) {
			return null;
		}
		if (!(parent instanceof PrismContainer)) {
			throw new IllegalStateException("Expected that parent of "+PrismContainerValue.class.getName()+" will be "+
					PrismContainer.class.getName()+", but it is "+parent.getClass().getName());
		}
		return (PrismContainer<C>)super.getParent();
	}

	@NotNull
	public ItemPath getPath() {
		Itemable parent = getParent();
		ItemPath parentPath = ItemPath.EMPTY_PATH;
		if (parent != null) {
			parentPath = parent.getPath();
		}
		if (getId() != null) {
			return ItemPath.subPath(parentPath, new IdItemPathSegment(getId()));
		} else {
			return parentPath;
		}
	}

	// For compatibility with other PrismValue types
	public C getValue() {
		return asContainerable();
	}

	@SuppressWarnings("unchecked")
	public C asContainerable() {
		if (containerable != null) {
			return containerable;
		}
		if (getParent() == null && complexTypeDefinition == null) {
			throw new IllegalStateException("Cannot represent container value without a parent and complex type definition as containerable; value: " + this);
		}
        return asContainerableInternal(resolveClass(null));
	}

	// returned class must be of type 'requiredClass' (or any of its subtypes)
    public C asContainerable(Class<C> requiredClass) {
		if (containerable != null) {
			return containerable;
		}
        return asContainerableInternal(resolveClass(requiredClass));
    }

	private Class<C> resolveClass(@Nullable Class<C> requiredClass) {
		if (complexTypeDefinition != null && complexTypeDefinition.getCompileTimeClass() != null) {
			Class<?> actualClass = complexTypeDefinition.getCompileTimeClass();
			if (requiredClass != null && !requiredClass.isAssignableFrom(actualClass)) {
				throw new IllegalStateException("asContainerable was called to produce " + requiredClass
						+ ", but the actual class in PCV is " + actualClass);
			} else {
				return (Class<C>) actualClass;
			}
		} else {
			PrismContainerable parent = getParent();
			if (parent != null) {
				Class<?> parentClass = parent.getCompileTimeClass();
				if (parentClass != null) {
					if (requiredClass != null && !requiredClass.isAssignableFrom(parentClass)) {
						// mismatch; but this can occur (see ShadowAttributesType vs ShadowIdentifiersType in ShadowAssociationType)
						// but TODO maybe this is only a workaround and the problem is in the schema itself (?)
						return requiredClass;
					} else {
						return (Class<C>) parentClass;
					}
				}
			}
		}
		return requiredClass;
	}

	private C asContainerableInternal(Class<C> clazz) {
		if (clazz == null) {
			String elementName = getParent() != null ? String.valueOf(getParent().getElementName()) : String.valueOf(this);
			throw new SystemException("Unknown compile time class of container value of '" + elementName + "'.");
		}
		if (Modifier.isAbstract(clazz.getModifiers())) {
			throw new SystemException("Can't create instance of class '" + clazz.getSimpleName() + "', it's abstract.");
		}
		try {
			if (prismContext != null) {
				containerable = clazz.getConstructor(PrismContext.class).newInstance(prismContext);
			} else {
				containerable = clazz.newInstance();
			}
            containerable.setupContainerValue(this);
            return containerable;
        } catch (SystemException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException("Couldn't create jaxb object instance of '" + clazz + "': "+ex.getMessage(), ex);
        }
    }

	public Collection<QName> getPropertyNames() {
		Collection<QName> names = new HashSet<>();
		for (PrismProperty<?> prop: getProperties()) {
			names.add(prop.getElementName());
		}
		return names;
	}

	public <IV extends PrismValue,ID extends ItemDefinition> boolean add(Item<IV,ID> item) throws SchemaException {
		return add(item, true);
	}

    /**
     * Adds an item to a property container.
     *
     * @param item item to add.
     * @throws SchemaException
     * @throws IllegalArgumentException an attempt to add value that already exists
     */
    public <IV extends PrismValue,ID extends ItemDefinition> boolean add(Item<IV,ID> item, boolean checkUniqueness) throws SchemaException {
		checkMutability();
    	if (item.getElementName() == null) {
    		throw new IllegalArgumentException("Cannot add item without a name to value of container "+getParent());
    	}
        if (checkUniqueness && findItem(item.getElementName(), Item.class) != null) {
            throw new IllegalArgumentException("Item " + item.getElementName() + " is already present in " + this.getClass().getSimpleName());
        }
        item.setParent(this);
        PrismContext prismContext = getPrismContext();
        if (prismContext != null) {
        	item.setPrismContext(prismContext);
        }
        if (getComplexTypeDefinition() != null && item.getDefinition() == null) {
        	item.applyDefinition((ID)determineItemDefinition(item.getElementName(), getComplexTypeDefinition()), false);
        }
        if (items == null) {
        	items = new ArrayList<>();
        }
        return items.add(item);
    }

    /**
     * Merges the provided item into this item. The values are joined together.
     * Returns true if new item or value was added.
     */
	public <IV extends PrismValue,ID extends ItemDefinition> boolean merge(Item<IV,ID> item) throws SchemaException {
		checkMutability();
		Item<IV, ID> existingItem = findItem(item.getElementName(), Item.class);
		if (existingItem == null) {
			return add(item);
		} else {
			boolean changed = false;
			for (IV newVal: item.getValues()) {
				if (existingItem.add((IV) newVal.clone())) {
					changed = true;
				}
			}
			return changed;
		}
	}

	/**
     * Subtract the provided item from this item. The values of the provided item are deleted
     * from this item.
     * Returns true if this item was changed.
     */
	public <IV extends PrismValue,ID extends ItemDefinition> boolean subtract(Item<IV,ID> item) throws SchemaException {
		checkMutability();
		Item<IV,ID> existingItem = findItem(item.getElementName(), Item.class);
		if (existingItem == null) {
			return false;
		} else {
			boolean changed = false;
			for (IV newVal: item.getValues()) {
				if (existingItem.remove(newVal)) {
					changed = true;
				}
			}
			return changed;
		}
	}

    /**
     * Adds an item to a property container. Existing value will be replaced.
     *
     * @param item item to add.
     */
    public <IV extends PrismValue,ID extends ItemDefinition> void addReplaceExisting(Item<IV,ID> item) throws SchemaException {
		checkMutability();
    	if (item == null) {
    		return;
    	}
        Item<IV,ID> existingItem = findItem(item.getElementName(), Item.class);
        if (existingItem != null && items != null) {
            items.remove(existingItem);
            existingItem.setParent(null);
        }
        add(item);
    }

    public <IV extends PrismValue,ID extends ItemDefinition> void remove(Item<IV,ID> item) {
        Validate.notNull(item, "Item must not be null.");
		checkMutability();

        Item<IV,ID> existingItem = findItem(item.getElementName(), Item.class);
        if (existingItem != null && items != null) {
            items.remove(existingItem);
            existingItem.setParent(null);
        }
    }

    public void removeAll() {
		checkMutability();
    	if (items == null){
    		return;
    	}
        Iterator<Item<?,?>> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item<?,?> item = iterator.next();
            item.setParent(null);
            iterator.remove();
        }
    }

    /**
     * Adds a collection of items to a property container.
     *
     * @param itemsToAdd items to add
     * @throws IllegalArgumentException an attempt to add value that already exists
     */
    public void addAll(Collection<? extends Item<?,?>> itemsToAdd) throws SchemaException {
        for (Item<?,?> item : itemsToAdd) {
        	add(item);
        }
    }

    /**
     * Adds a collection of items to a property container. Existing values will be replaced.
     *
     * @param itemsToAdd items to add
     */
    public void addAllReplaceExisting(Collection<? extends Item<?,?>> itemsToAdd) throws SchemaException {
		checkMutability();
        // Check for conflicts, remove conflicting values
        for (Item<?,?> item : itemsToAdd) {
            Item<?,?> existingItem = findItem(item.getElementName(), Item.class);
            if (existingItem != null && items != null) {
                items.remove(existingItem);
            }
        }
        addAll(itemsToAdd);
    }

	public <IV extends PrismValue,ID extends ItemDefinition> void replace(Item<IV,ID> oldItem, Item<IV,ID> newItem) throws SchemaException {
		remove(oldItem);
		add(newItem);
	}

    public void clear() {
		checkMutability();
    	if (items != null) {
    		items.clear();
    	}
    }

    public boolean contains(Item item) {
		return items != null && items.contains(item);
	}

    public boolean contains(QName itemName) {
        return findItem(itemName) != null;
    }

    public static <C extends Containerable> boolean containsRealValue(Collection<PrismContainerValue<C>> cvalCollection,
    		PrismContainerValue<C> cval) {
    	for (PrismContainerValue<C> colVal: cvalCollection) {
    		if (colVal.equalsRealValue(cval)) {
    			return true;
    		}
    	}
    	return false;
    }

    @Override
    public Object find(ItemPath path) {
    	if (path == null || path.isEmpty()) {
    		return this;
    	}
    	ItemPathSegment first = path.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	ItemPath rest = path.rest();
    	Item<?,?> subItem = findItem(subName);
    	if (subItem == null) {
    		return null;
    	}
    	return subItem.find(rest);
    }

    @Override
	public <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path) {
    	if (path == null || path.isEmpty()) {
    		// Incomplete path
    		return null;
    	}
    	ItemPathSegment first = path.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	ItemPath rest = path.rest();
    	Item<?,?> subItem = findItem(subName);
    	if (subItem == null) {
    		return null;
    	}
    	return subItem.findPartial(rest);
	}

	@SuppressWarnings("unchecked")
	public <X> PrismProperty<X> findProperty(QName propertyQName) {
        return findItem(propertyQName, PrismProperty.class);
    }

    public <X> PrismProperty<X> findProperty(ItemPath propertyPath) {
        return (PrismProperty) findItem(propertyPath);
    }

    /**
     * Finds a specific property in the container by definition.
     * <p>
     * Returns null if nothing is found.
     *
     * @param propertyDefinition property definition to find.
     * @return found property or null
     */
    public <X> PrismProperty<X> findProperty(PrismPropertyDefinition<X> propertyDefinition) {
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No property definition");
        }
        return findProperty(propertyDefinition.getName());
    }

    public <X extends Containerable> PrismContainer<X> findContainer(QName containerName) {
    	return findItem(containerName, PrismContainer.class);
    }

    public PrismReference findReference(QName elementName) {
    	return findItem(elementName, PrismReference.class);
    }

    public PrismReference findReferenceByCompositeObjectElementName(QName elementName) {
    	if (items == null){
    		return null;
    	}
    	for (Item item: items) {
    		if (item instanceof PrismReference) {
    			PrismReference ref = (PrismReference)item;
    			PrismReferenceDefinition refDef = ref.getDefinition();
    			if (refDef != null) {
    				if (elementName.equals(refDef.getCompositeObjectElementName())) {
    					return ref;
    				}
    			}
    		}
    	}
    	return null;
    }

    public <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findItem(QName itemName, Class<I> type) {
    	try {
			return findCreateItem(itemName, type, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItem(QName itemName) {
    	try {
			return findCreateItem(itemName, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItem(ItemPath itemPath) {
    	try {
			return findCreateItem(itemPath, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }

    @SuppressWarnings("unchecked")
    <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findCreateItem(QName itemName, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
    	Item<IV,ID> item = findItemByQName(itemName);
        if (item != null) {
            if (type.isAssignableFrom(item.getClass())) {
                return (I) item;
            } else {
                if (create) {
                    throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because "
                            + item.getClass().getSimpleName() + " with the same name exists ("+item.getElementName()+")");
                } else {
                    return null;
                }
            }
        }
    	if (create) {   // todo treat unqualified names
    		return createSubItem(itemName, type, itemDefinition);
    	} else {
    		return null;
    	}
    }

    public <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findItem(ItemDefinition itemDefinition, Class<I> type) {
        if (itemDefinition == null) {
            throw new IllegalArgumentException("No item definition");
        }
        return findItem(itemDefinition.getName(), type);
    }

    public boolean containsItem(ItemPath propPath, boolean acceptEmptyItem) throws SchemaException {
    	ItemPathSegment first = propPath.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+propPath+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	ItemPath rest = propPath.rest();
        Item item = findItemByQName(subName);
        if (item != null) {
            if (rest.isEmpty()) {
                return (acceptEmptyItem || !item.isEmpty());
            } else {
                // Go deeper
                if (item instanceof PrismContainer) {
                    return ((PrismContainer<?>)item).containsItem(rest, acceptEmptyItem);
                } else {
                	return (acceptEmptyItem || !item.isEmpty());
                }
            }
        }

        return false;
    }

    // Expects that "self" path is NOT present in propPath
    @SuppressWarnings("unchecked")
    <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findCreateItem(ItemPath propPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
    	ItemPathSegment first = propPath.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+propPath+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	ItemPath rest = propPath.rest();
        I item = (I) findItemByQName(subName);
        if (item != null) {
            if (rest.isEmpty()) {
                if (type.isAssignableFrom(item.getClass())) {
                    return (I)item;
                } else {
                    if (create) {
                        throw new SchemaException("The " + type.getSimpleName() + " cannot be created because "
                                + item.getClass().getSimpleName() + " with the same name exists ("+item.getElementName()+")");
                    } else {
                        return null;
                    }
                }
            } else {
                // Go deeper
                if (item instanceof PrismContainer) {
                    return ((PrismContainer<?>)item).findCreateItem(rest, type, itemDefinition, create);
                } else {
                    if (create) {
                        throw new SchemaException("The " + type.getSimpleName() + " cannot be created because "
                                + item.getClass().getSimpleName() + " with the same name exists ("+item.getElementName()+")");
                    } else {
                        // Return the item for non-container even if the path is non-empty
                        // FIXME: This is not the best solution but it is needed to be able to look inside properties
                        // such as PolyString
                        if (type.isAssignableFrom(item.getClass())) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                }
            }
        }

    	if (create) {       // todo treat unqualified names
    		if (rest.isEmpty()) {
    			return createSubItem(subName, type, itemDefinition);
    		} else {
	    		// Go deeper
    			PrismContainer<?> subItem = createSubItem(subName, PrismContainer.class, null);
	        	return subItem.findCreateItem(rest, type, itemDefinition, create);
    		}
    	} else {
    		return null;
    	}
    }

    private <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItemByQName(QName subName) throws SchemaException {
        if (items == null) {
            return null;
        }
        Item<IV,ID> matching = null;
        for (Item<?,?> item : items) {
            if (QNameUtil.match(subName, item.getElementName())) {
                if (matching != null) {
                    String containerName = getParent() != null ? DebugUtil.formatElementName(getParent().getElementName()) : "";
                    throw new SchemaException("More than one items matching " + subName + " in container " + containerName);
                } else {
                    matching = (Item<IV, ID>) item;
                }
            }
        }
        return matching;
    }

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I createDetachedSubItem(QName name,
			Class<I> type, ID itemDefinition, boolean immutable) throws SchemaException {
		I newItem = createDetachedNewItemInternal(name, type, itemDefinition);
		if (immutable) {
			newItem.setImmutable(true);
		}
		return newItem;
	}

	private <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I createSubItem(QName name, Class<I> type, ID itemDefinition) throws SchemaException {
		checkMutability();
		I newItem = createDetachedNewItemInternal(name, type, itemDefinition);
		add(newItem);
		return newItem;
    }

	private <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I createDetachedNewItemInternal(QName name, Class<I> type,
			ID itemDefinition) throws SchemaException {
		I newItem;
		if (itemDefinition == null) {
			ComplexTypeDefinition ctd = getComplexTypeDefinition();
			itemDefinition = determineItemDefinition(name, ctd);
			if (ctd != null && itemDefinition == null) {
				throw new SchemaException("No definition for item "+name+" in "+getParent());
			}
		}

		if (itemDefinition != null) {
			if (StringUtils.isNotBlank(name.getNamespaceURI())){
				newItem = (I) itemDefinition.instantiate(name);
			} else {
				QName computed = new QName(itemDefinition.getNamespace(), name.getLocalPart());
				newItem = (I) itemDefinition.instantiate(computed);
			}
			if (newItem instanceof PrismObject) {
				throw new IllegalStateException("PrismObject instantiated as a subItem in "+this+" from definition "+itemDefinition);
			}
		} else {
			newItem = Item.createNewDefinitionlessItem(name, type, prismContext);
			if (newItem instanceof PrismObject) {
				throw new IllegalStateException("PrismObject instantiated as a subItem in "+this+" as definitionless instance of class "+type);
			}
		}
		if (type.isAssignableFrom(newItem.getClass())) {
			return newItem;
		} else {
			throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because the item should be of type "
					+ newItem.getClass().getSimpleName() + " ("+newItem.getElementName()+")");
		}
	}

	public <T extends Containerable> PrismContainer<T> findOrCreateContainer(QName containerName) throws SchemaException {
    	return findCreateItem(containerName, PrismContainer.class, null, true);
    }

    public PrismReference findOrCreateReference(QName referenceName) throws SchemaException {
    	return findCreateItem(referenceName, PrismReference.class, null, true);
    }

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findOrCreateItem(QName containerName) throws SchemaException {
    	return findCreateItem(containerName, Item.class, null, true);
    }

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(QName containerName, Class<I> type) throws SchemaException {
    	return findCreateItem(containerName, type, null, true);
    }

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath path, Class<I> type, ID definition) throws SchemaException {
    	return findCreateItem(path, type, definition, true);
    }

    public <X> PrismProperty<X> findOrCreateProperty(QName propertyQName) throws SchemaException {
        PrismProperty<X> property = findItem(propertyQName, PrismProperty.class);
        if (property != null) {
            return property;
        }
        return createProperty(propertyQName);
    }

    public <X> PrismProperty<X> findOrCreateProperty(ItemPath propertyPath) throws SchemaException {
        return findOrCreateItem(propertyPath, PrismProperty.class, null);
    }

    public <X> PrismProperty<X> findOrCreateProperty(PrismPropertyDefinition propertyDef) throws SchemaException {
        PrismProperty<X> property = findItem(propertyDef.getName(), PrismProperty.class);
        if (property != null) {
            return property;
        }
        return createProperty(propertyDef);
    }

    public <X> PrismProperty<X> createProperty(QName propertyName) throws SchemaException {
		checkMutability();
        PrismPropertyDefinition propertyDefinition = determineItemDefinition(propertyName, getComplexTypeDefinition());
		if (propertyDefinition == null) {
			// container has definition, but there is no property definition. This is either runtime schema
			// or an error
			if (getParent() != null && getDefinition() != null && !getDefinition().isRuntimeSchema()) {		// TODO clean this up
				throw new IllegalArgumentException("No definition for property "+propertyName+" in "+complexTypeDefinition);
			}
		}
        PrismProperty<X> property;
        if (propertyDefinition == null) {
        	property = new PrismProperty<X>(propertyName, prismContext);		// Definitionless
        } else {
        	property = propertyDefinition.instantiate();
        }
        add(property, false);
        return property;
    }

    public <X> PrismProperty<X> createProperty(PrismPropertyDefinition propertyDefinition) throws SchemaException {
    	PrismProperty<X> property = propertyDefinition.instantiate();
    	add(property);
        return property;
    }

    public void removeProperty(QName propertyName) {
    	removeProperty(new ItemPath(propertyName));
    }

	public void removeProperty(ItemPath propertyPath) {
		removeItem(propertyPath, PrismProperty.class);
	}

	public void removeContainer(QName containerName) {
		removeContainer(new ItemPath(containerName));
    }

	public void removeContainer(ItemPath itemPath) {
		removeItem(itemPath, PrismContainer.class);
	}

    public void removeReference(QName name) {
        removeReference(new ItemPath(name));
    }

    public void removeReference(ItemPath path) {
        removeItem(path, PrismReference.class);
    }

    // Expects that "self" path is NOT present in propPath
	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> void removeItem(ItemPath propPath, Class<I> itemType) {
		checkMutability();
		if (items == null){
    		return;
    	}
		ItemPathSegment first = propPath.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to remove item using a non-name path "+propPath+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	ItemPath rest = propPath.rest();
    	Iterator<Item<?,?>> itemsIterator = items.iterator();
    	while(itemsIterator.hasNext()) {
    		Item<?,?> item = itemsIterator.next();
            if (subName.equals(item.getElementName())) {
            	if (!rest.isEmpty() && item instanceof PrismContainer) {
            		((PrismContainer<?>)item).removeItem(propPath, itemType);
            		return;
            	} else {
            		if (itemType.isAssignableFrom(item.getClass())) {
            			itemsIterator.remove();
            		} else {
           				throw new IllegalArgumentException("Attempt to remove item "+subName+" from "+this+
           						" of type "+itemType+" while the existing item is of incompatible type "+item.getClass());
            		}
            	}
            }
        }
    }

    public void setPropertyRealValue(QName propertyName, Object realValue, PrismContext prismContext) throws SchemaException {
		checkMutability();
    	PrismProperty<?> property = findOrCreateProperty(propertyName);
    	property.setRealValue(realValue);
        if (property.getPrismContext() == null) {
            property.setPrismContext(prismContext);
        }
    }

    public <T> T getPropertyRealValue(QName propertyName, Class<T> type) {
    	PrismProperty<T> property = findProperty(propertyName);
    	if (property == null) {          // when using sql repo, even non-existing properties do not have 'null' here
    		return null;
    	}
    	return property.getRealValue(type);
    }

    @Override
	public void recompute(PrismContext prismContext) {
		// Nothing to do. The subitems should be already recomputed as they are added to this container.
	}

    @Override
	public void accept(Visitor visitor) {
		super.accept(visitor);
		if (items != null) {
			for (Item<?,?> item : getItems()) {
				item.accept(visitor);
			}
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
			ItemPathSegment first = path.first();
	    	if (!(first instanceof NameItemPathSegment)) {
	    		throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
	    	}
	    	QName subName = ((NameItemPathSegment)first).getName();
	    	ItemPath rest = path.rest();
			if (items != null) {
				for (Item<?,?> item : items) {            // todo unqualified names!
					if (first.isWildcard() || subName.equals(item.getElementName())) {
						item.accept(visitor, rest, recursive);
					}
				}
			}
		}
	}

    public boolean hasCompleteDefinition() {
		if (items != null) {
			for (Item<?,?> item : getItems()) {
				if (!item.hasCompleteDefinition()) {
					return false;
				}
			}
		}
    	return true;
    }

	@Override
	public boolean representsSameValue(PrismValue other, boolean lax) {
		if (other instanceof PrismContainerValue) {
			return representsSameValue((PrismContainerValue<C>)other, lax);
		} else {
			return false;
		}
	}

	@SuppressWarnings("Duplicates")
	private boolean representsSameValue(PrismContainerValue<C> other, boolean lax) {
		if (lax && getParent() != null) {
			PrismContainerDefinition definition = getDefinition();
			if (definition != null) {
				if (definition.isSingleValue()) {
					// There is only one value, therefore it always represents the same thing
					return true;
				}
			}
		}
		if (lax && other.getParent() != null) {
			PrismContainerDefinition definition = other.getDefinition();
			if (definition != null) {
				if (definition.isSingleValue()) {
					// There is only one value, therefore it always represents the same thing
					return true;
				}
			}
		}
		if (this.getId() != null && other.getId() != null) {
			return this.getId().equals(other.getId());
		}
		return false;
	}


	@Override
	void diffMatchingRepresentation(PrismValue otherValue,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral) {
		if (otherValue instanceof PrismContainerValue) {
			diffRepresentation((PrismContainerValue)otherValue, deltas, ignoreMetadata, isLiteral);
		} else {
			throw new IllegalStateException("Comparing incompatible values "+this+" - "+otherValue);
		}
	}

	void diffRepresentation(PrismContainerValue<C> otherValue,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral) {
		diffItems(this, otherValue, deltas, ignoreMetadata, isLiteral);
	}

	@Override
	public boolean isRaw() {
		return false;
	}

	public boolean addRawElement(Object element) throws SchemaException {
		checkMutability();
		PrismContainerDefinition<C> definition = getDefinition();
		if (definition == null) {
			throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
		} else {
			// We have definition here, we can parse it right now
			Item<?,?> subitem = parseRawElement(element, definition);
			return merge(subitem);
		}
	}

	public boolean deleteRawElement(Object element) throws SchemaException {
		checkMutability();
		PrismContainerDefinition<C> definition = getDefinition();
		if (definition == null) {
			throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
		} else {
			// We have definition here, we can parse it right now
			Item<?,?> subitem = parseRawElement(element, definition);
			return subtract(subitem);
		}
	}


	public boolean removeRawElement(Object element) {
		checkMutability();
		throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
	}

	private <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> parseRawElement(Object element, PrismContainerDefinition<C> definition) throws SchemaException {
		JaxbDomHack jaxbDomHack = definition.getPrismContext().getJaxbDomHack();
		return jaxbDomHack.parseRawElement(element, definition);
	}

	private PrismContainerValue<C> parseRawElementsToNewValue(PrismContainerValue<C> origCVal, PrismContainerValue<C> definitionSource) throws SchemaException {
		throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void diffItems(PrismContainerValue<C> thisValue, PrismContainerValue<C> other,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral) {

		if (thisValue.getItems() !=  null) {
			for (Item<?,?> thisItem: thisValue.getItems()) {
				Item otherItem = other.findItem(thisItem.getElementName());
				if (!isLiteral) {
					ItemDefinition itemDef = thisItem.getDefinition();
					if (itemDef == null && other.getDefinition() != null) {
						itemDef = other.getDefinition().findItemDefinition(thisItem.getElementName());
					}
					if (isOperationalOnly(thisItem, itemDef)
							&& (otherItem == null || isOperationalOnly(otherItem, itemDef))) {
						continue;
					}
				}
				// The "delete" delta will also result from the following diff
				thisItem.diffInternal(otherItem, deltas, ignoreMetadata, isLiteral);
			}
		}

		if (other.getItems() != null) {
			for (Item otherItem: other.getItems()) {
				Item thisItem = thisValue.findItem(otherItem.getElementName());
				if (thisItem != null) {
					// Already processed in previous loop
					continue;
				}
				if (!isLiteral) {
					ItemDefinition itemDef = otherItem.getDefinition();
					if (itemDef == null && thisValue.getDefinition() != null) {
						itemDef = thisValue.getDefinition().findItemDefinition(otherItem.getElementName());
					}
					if (isOperationalOnly(otherItem, itemDef)) {
						continue;
					}
				}

				// Other has an item that we don't have, this must be an add
				ItemDelta itemDelta = otherItem.createDelta();
				itemDelta.addValuesToAdd(otherItem.getClonedValues());
				if (!itemDelta.isEmpty()) {
					((Collection)deltas).add(itemDelta);
				}
			}
		}
	}

	private boolean isOperationalOnly(Item item, ItemDefinition itemDef) {
		if (itemDef != null && itemDef.isOperational()) {
			return true;
		}
		if (item.isEmpty()) {
			return false;
		}
		if (!(item instanceof PrismContainer)) {
			return false;
		}
		PrismContainer<?> container = (PrismContainer)item;
		for (PrismContainerValue<?> cval: container.getValues()) {
			if (cval != null) {
				List<Item<?, ?>> subitems = cval.getItems();
				if (subitems != null) {
					for (Item<?, ?> subitem: subitems) {
						ItemDefinition subItemDef = subitem.getDefinition();
						if (subItemDef == null && itemDef != null) {
							subItemDef = ((PrismContainerDefinition)itemDef).findItemDefinition(subitem.getElementName());
						}
						if (subItemDef == null) {
							return false;
						}
						if (!subItemDef.isOperational()) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	protected PrismContainerDefinition<C> getDefinition() {
		return (PrismContainerDefinition<C>) super.getDefinition();
	}

	@Override
	public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
		checkMutability();
		if (!(definition instanceof PrismContainerDefinition)) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to container " + this);
    	}
		applyDefinition((PrismContainerDefinition<C>)definition, force);
	}

	public void applyDefinition(@NotNull PrismContainerDefinition<C> containerDef, boolean force) throws SchemaException {
		checkMutability();
		if (complexTypeDefinition != null && !force) {
			return;                // there's a definition already
		}
		replaceComplexTypeDefinition(containerDef.getComplexTypeDefinition());
		// we need to continue even if CTD is null or 'any' - e.g. to resolve definitions within object extension
		if (items != null) {
			for (Item item : items) {
				if (item.getDefinition() != null && !force) {
					// Item has a definition already, no need to apply it
					continue;
				}
				ItemDefinition itemDefinition = determineItemDefinition(item.getElementName(), complexTypeDefinition);
				if (itemDefinition == null && item.getDefinition() != null && item.getDefinition().isDynamic()) {
					// We will not apply the null definition here. The item has a dynamic definition that we don't
					// want to destroy as it cannot be reconstructed later.
				} else {
					item.applyDefinition(itemDefinition, force);
				}
			}
		}
	}

	/**
	 * This method can both return null and throws exception. It returns null in case there is no definition
	 * but it is OK (e.g. runtime schema). It throws exception if there is no definition and it is not OK.
	 */
	private <ID extends ItemDefinition> ID determineItemDefinition(QName itemName, @Nullable ComplexTypeDefinition ctd) throws SchemaException {
		ID itemDefinition = ctd != null ? ctd.findItemDefinition(itemName) : null;
		if (itemDefinition != null) {
			return itemDefinition;
		}
		if (ctd == null || ctd.isXsdAnyMarker() || ctd.isRuntimeSchema()) {
			// If we have prism context, try to locate global definition. But even if that is not
			// found it is still OK. This is runtime container. We tolerate quite a lot here.
			PrismContext prismContext = getPrismContext();
			if (prismContext != null) {
				return (ID) prismContext.getSchemaRegistry().resolveGlobalItemDefinition(itemName, ctd);
			} else {
				return null;
			}
		} else {
			throw new SchemaException("No definition for item " + itemName + " in " + getParent());
		}
	}

	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
        if (this.prismContext == null) {
            this.prismContext = prismContext;
        }
		super.revive(prismContext);
		if (items != null) {
			for (Item<?,?> item: items) {
				item.revive(prismContext);
			}
		}
	}

	@Override
    public boolean isEmpty() {
		if (id != null) {
			return false;
		}
		if (items == null) {
			return true;
		}
        return items.isEmpty();
    }

    @Override
	public void normalize() {
		checkMutability();
    	if (items != null) {
			for (Item<?, ?> item : items) {
				item.normalize();
			}
    	}
	}

	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        ItemPath myPath = getPath();
		if (getDefinition() == null) {
			throw new IllegalStateException("Definition-less container value " + this +" ("+myPath+" in "+rootItem+")");
		}
		if (items != null) {
			for (Item<?,?> item: items) {
                if (scope.isThorough()) {
                    if (item == null) {
                        throw new IllegalStateException("Null item in container value "+this+" ("+myPath+" in "+rootItem+")");
                    }
                    if (item.getParent() == null) {
                        throw new IllegalStateException("No parent for item "+item+" in container value "+this+" ("+myPath+" in "+rootItem+")");
                    }
                    if (item.getParent() != this) {
                        throw new IllegalStateException("Wrong parent for item "+item+" in container value "+this+" ("+myPath+" in "+rootItem+"), " +
                                "bad parent: "+ item.getParent());
                    }
                }
				item.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
			}
		}
	}

    public void assertDefinitions(String sourceDescription) throws SchemaException {
    	assertDefinitions(false, sourceDescription);
    }

	public void assertDefinitions(boolean tolerateRaw, String sourceDescription) throws SchemaException {
		if (getItems() == null){
			return;
		}
		for (Item<?,?> item: getItems()) {
			item.assertDefinitions(tolerateRaw, "value("+getId()+") in "+sourceDescription);
		}
	}

	public PrismContainerValue<C> clone() {	// TODO resolve also the definition?
    	PrismContainerValue<C> clone = new PrismContainerValue<>(getOriginType(), getOriginObject(), getParent(), getId(),
				this.complexTypeDefinition, this.prismContext);
    	copyValues(clone);
        return clone;
    }

	protected void copyValues(PrismContainerValue<C> clone) {
		super.copyValues(clone);
		clone.id = this.id;
		if (this.items != null) {
			for (Item<?,?> item : this.items) {
				Item<?,?> clonedItem = item.clone();
				clonedItem.setParent(clone);
				if (clone.items == null) {
					clone.items = new ArrayList<>(this.items.size());
				}
				clone.items.add(clonedItem);
			}
		}
	}

	protected void deepCloneDefinition(boolean ultraDeep, PrismContainerDefinition<C> clonedContainerDef) {
		// special treatment of CTD (we must not simply overwrite it with clonedPCD.CTD!)
		PrismContainerable parent = getParent();
		if (parent != null && complexTypeDefinition != null) {
			if (complexTypeDefinition == parent.getComplexTypeDefinition()) {
				replaceComplexTypeDefinition(clonedContainerDef.getComplexTypeDefinition());
			} else {
				replaceComplexTypeDefinition(complexTypeDefinition.deepClone(ultraDeep ? null : new HashMap<>(), new HashMap<>() ));		// OK?
			}
		}
		if (items != null) {
			for (Item<?,?> item: items) {
				deepCloneDefinitionItem(item, ultraDeep, clonedContainerDef);
			}
		}
	}

	private <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> void deepCloneDefinitionItem(I item, boolean ultraDeep, PrismContainerDefinition<C> clonedContainerDef) {
		PrismContainerDefinition<C> oldContainerDef = getDefinition();
		QName itemName = item.getElementName();
		ID oldItemDefFromContainer = oldContainerDef.findItemDefinition(itemName);
		ID oldItemDef = item.getDefinition();
		ID clonedItemDef;
		if (oldItemDefFromContainer == oldItemDef) {
			clonedItemDef = clonedContainerDef.findItemDefinition(itemName);
		} else {
			clonedItemDef = (ID) oldItemDef.deepClone(ultraDeep);
		}
		item.propagateDeepCloneDefinition(ultraDeep, clonedItemDef);		// propagate to items in values
		item.setDefinition(clonedItemDef);									// sets CTD in values only if null!
	}

	@Override
	public boolean equalsComplex(PrismValue other, boolean ignoreMetadata, boolean isLiteral) {
		if (other == null || !(other instanceof PrismContainerValue<?>)) {
			return false;
		}
		return equalsComplex((PrismContainerValue<?>)other, ignoreMetadata, isLiteral);
	}

	public boolean equalsComplex(PrismContainerValue<?> other, boolean ignoreMetadata, boolean isLiteral) {
		if (!super.equalsComplex(other, ignoreMetadata, isLiteral)) {
			return false;
		}
		if (!ignoreMetadata) {
			if (this.id == null) {
				if (other.id != null)
					return false;
			} else if (!this.id.equals(other.id))
				return false;
		}
		if (this.items == null) {
			if (other.items != null)
				return false;
		} else if (!this.equalsItems(this, (PrismContainerValue<C>) other, ignoreMetadata, isLiteral)) {
			return false;
		}
		return true;
	}

	boolean equalsItems(PrismContainerValue<C> other, boolean ignoreMetadata, boolean isLiteral) {
		return equalsItems(this, other, ignoreMetadata, isLiteral);
	}

	boolean equalsItems(PrismContainerValue<C> thisValue, PrismContainerValue<C> other,
			boolean ignoreMetadata, boolean isLiteral) {
		Collection<? extends ItemDelta<?,?>> deltas = new ArrayList<ItemDelta<?,?>>();
		// The EMPTY_PATH is a lie. We don't really care if the returned deltas have correct path or not
		// we only care whether some deltas are returned or not.
		diffItems(thisValue, other, deltas, ignoreMetadata, isLiteral);
		return deltas.isEmpty();
	}

	public boolean equivalent(PrismContainerValue<?> other) {
        return equalsRealValue(other);
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrismContainerValue<?> other = (PrismContainerValue<?>) obj;
		return equalsComplex(other, false, false);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		// Do not include id. containers with non-null id and null id may still be considered equivalent
		// We also need to make sure that container valus that contain only metadata will produce zero hashcode
		// so it will not ruin hashcodes of parent containers
		int itemsHash = 0;
		if (items != null) {
			itemsHash = MiscUtil.unorderedCollectionHashcode(items, item -> !item.isMetadata());
		}
		if (itemsHash != 0) {
			result = prime * result + itemsHash;
		}
		return result;
	}

	@Override
    public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PCV(");
		sb.append(getId());
		sb.append("):");
        sb.append(getItems());
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        boolean wasIndent = false;
        if (DebugUtil.isDetailedDebugDump()) {
        	DebugUtil.indentDebugDump(sb, indent);
            wasIndent = true;
			detailedDebugDumpStart(sb);
		}
        boolean multivalue = true;
        PrismContainerable<C> parent = getParent();
        if (parent != null && parent.getDefinition() != null) {
        	multivalue = parent.getDefinition().isMultiValue();
        }
        Long id = getId();
        if (multivalue || id != null || DebugUtil.isDetailedDebugDump()) {
        	if (!wasIndent) {
            	DebugUtil.indentDebugDump(sb, indent);
                wasIndent = true;
        	}
			debugDumpIdentifiers(sb);
		}
        appendOriginDump(sb);
		List<Item<?,?>> items = getItems();
		if (items == null || items.isEmpty()) {
			if (wasIndent) {
				sb.append("\n");
			}
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("(no items)");
		} else {
	        Iterator<Item<?,?>> i = getItems().iterator();
	        if (wasIndent && i.hasNext()) {
	            sb.append("\n");
	        }
	        while (i.hasNext()) {
	        	Item<?,?> item = i.next();
	            sb.append(item.debugDump(indent + 1));
	            if (i.hasNext()) {
	                sb.append("\n");
	            }
	        }
        }
        return sb.toString();
    }

	protected void debugDumpIdentifiers(StringBuilder sb) {
		sb.append("id=").append(PrettyPrinter.prettyPrint(getId()));
	}

	protected void detailedDebugDumpStart(StringBuilder sb) {
		sb.append("PCV").append(": ");
	}

	@Override
	public boolean match(PrismValue otherValue) {
		return equalsRealValue(otherValue);
	}

	@Override
	public String toHumanReadableString() {
		return "id="+id+": "+items.size()+" items";
	}

    // copies the definition from original to aClone (created outside of this method)
    // it has to (artifically) create a parent PrismContainer to hold the definition
    //
    // without having a definition, such containers cannot be serialized using
    // PrismJaxbProcessor.marshalContainerableToString (without definition, there is
    // no information on corresponding element name)
    //
    // todo review usefulness and appropriateness of this method and its placement
	@Deprecated
    public static void copyDefinition(Containerable aClone, Containerable original, PrismContext prismContext) {
        try {
            Validate.notNull(original.asPrismContainerValue().getParent(), "original PrismContainerValue has no parent");

            ComplexTypeDefinition definition = original.asPrismContainerValue().getComplexTypeDefinition();
            Validate.notNull(definition, "original PrismContainer definition is null");

            PrismContainer<?> aCloneParent = prismContext.getSchemaRegistry()
					.findContainerDefinitionByCompileTimeClass((Class<? extends Containerable>) definition.getCompileTimeClass())
					.instantiate();
            aCloneParent.add(aClone.asPrismContainerValue());
        } catch (SchemaException e) {
            throw new SystemException("Unexpected SchemaException when copying definition from original object to its clone", e);
        }
    }

    public QName getTypeName() {
        return getComplexTypeDefinition() != null ? getComplexTypeDefinition().getTypeName() : null;
    }

    @Nullable
    public ComplexTypeDefinition getComplexTypeDefinition() {
		if (complexTypeDefinition == null) {
			complexTypeDefinition = determineComplexTypeDefinition();
		}
		return complexTypeDefinition;
	}

	// will correctly work only if argument is not null (otherwise the CTD will be determined on next call to getCTD)
	void replaceComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
//		if (this.complexTypeDefinition != null && complexTypeDefinition != null && !this.complexTypeDefinition.getTypeName().equals(complexTypeDefinition.getTypeName())) {
//			System.out.println("Dangerous!");
//		}
		this.complexTypeDefinition = complexTypeDefinition;
	}


	private ComplexTypeDefinition determineComplexTypeDefinition() {
		PrismContainerable<C> parent = getParent();
		ComplexTypeDefinition parentCTD = parent != null && parent.getDefinition() != null ?
				parent.getDefinition().getComplexTypeDefinition() : null;
		if (containerable == null) {
			return parentCTD;
		}
		if (prismContext == null) {
			// check if parentCTD matches containerable
			if (parentCTD != null && containerable.getClass().equals(parentCTD.getCompileTimeClass())) {
				return parentCTD;
			} else {
				//throw new IllegalStateException("Cannot determine complexTypeDefinition for PrismContainerValue because prismContext is missing; PCV = " + this);
				return null;
			}
		}
		ComplexTypeDefinition def = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(containerable.getClass());
		return def;		// may be null at this place
    }

	public static <T extends Containerable> List<PrismContainerValue<T>> toPcvList(List<T> beans) {
		List<PrismContainerValue<T>> rv = new ArrayList<>(beans.size());
		for (T bean : beans) {
			rv.add(bean.asPrismContainerValue());
		}
		return rv;
	}

	@Override
	public void setImmutable(boolean immutable) {
		super.setImmutable(immutable);
		if (items != null) {
			for (Item item : items) {
				item.setImmutable(immutable);
			}
		}
	}

	@Override
	public Class<?> getRealClass() {
		if (containerable != null) {
			return containerable.getClass();
		}
		return resolveClass(null);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T getRealValue() {
		return (T) asContainerable();
	}

	/**
	 * Returns a single-valued container (with a single-valued definition) holding just this value.
	 * @param itemName Item name for newly-created container.
	 * @return
	 */
	public PrismContainer<C> asSingleValuedContainer(@NotNull QName itemName) throws SchemaException {
		PrismContext prismContext = getPrismContext();
		Validate.notNull(prismContext, "Prism context is null");

		PrismContainerDefinitionImpl<C> definition = new PrismContainerDefinitionImpl<>(itemName,
				getComplexTypeDefinition(), prismContext);
		definition.setMaxOccurs(1);

		PrismContainer<C> pc = definition.instantiate();
		pc.add(clone());
		return pc;
	}

	// EXPERIMENTAL. TODO write some tests
	// BEWARE, it expects that definitions for items are present. Otherwise definition-less single valued items will get overwritten.
	@SuppressWarnings("unchecked")
	public void mergeContent(PrismContainerValue<?> other, List<QName> overwrite) throws SchemaException {
		List<QName> remainingToOverwrite = new ArrayList<>(overwrite);
		if (other.getItems() != null) {
			for (Item<?, ?> otherItem : other.getItems()) {
				Item<?, ?> existingItem = findItem(otherItem.elementName);
				if (QNameUtil.remove(remainingToOverwrite, otherItem.getElementName())
						|| existingItem != null && existingItem.isSingleValue()) {
					remove(existingItem);
				}
				merge(otherItem.clone());
			}
		}
		remainingToOverwrite.forEach(name -> removeItem(new ItemPath(name), Item.class));
	}

	@Override
	public PrismContainerValue<?> getRootValue() {
		return (PrismContainerValue) super.getRootValue();
	}

	public static <C extends Containerable> List<PrismContainerValue<C>> asPrismContainerValues(List<C> containerables) {
		return containerables.stream().map(c -> (PrismContainerValue<C>) c.asPrismContainerValue()).collect(Collectors.toList());
	}

	public static <C extends Containerable> List<C> asContainerables(List<PrismContainerValue<C>> pcvs) {
		return pcvs.stream().map(c -> c.asContainerable()).collect(Collectors.toList());
	}

	public static <C extends Containerable> Collection<C> asContainerables(Collection<PrismContainerValue<C>> pcvs) {
		return pcvs.stream().map(c -> c.asContainerable()).collect(Collectors.toList());
	}

	/**
	 * Set origin type to all values and subvalues
	 */
	public void setOriginTypeRecursive(final OriginType originType) {
		accept((visitable) -> {
			if (visitable instanceof PrismValue) {
				((PrismValue)visitable).setOriginType(originType);
			}
		});
	}

	// TODO optimize a bit + test thoroughly
	public void keepPaths(List<ItemPath> keep) {
		if (items != null) {
			for (Iterator<Item<?, ?>> iterator = items.iterator(); iterator.hasNext(); ) {
				Item<?, ?> item = iterator.next();
				ItemPath itemPath = item.getPath().removeIdentifiers();
				if (!ItemPath.containsSuperpathOrEquivalent(keep, itemPath)) {
					iterator.remove();
				} else {
					if (item instanceof PrismContainer) {
						((PrismContainer<?>) item).getValues().forEach(v -> v.keepPaths(keep));
					} else {
						// TODO some additional checks here (e.g. when trying to keep 'name/xyz' - this is illegal)
					}
				}
			}
		}
	}

	// TODO optimize a bit + test thoroughly
	public void removePaths(List<ItemPath> remove) {
		if (items != null) {
			for (Iterator<Item<?, ?>> iterator = items.iterator(); iterator.hasNext(); ) {
				Item<?, ?> item = iterator.next();
				ItemPath itemPath = item.getPath().removeIdentifiers();
				if (ItemPath.containsEquivalent(remove, itemPath)) {
					iterator.remove();
				} else if (ItemPath.containsSuperpath(remove, itemPath)) {
					if (item instanceof PrismContainer) {
						((PrismContainer<?>) item).getValues().forEach(v -> v.removePaths(remove));
					}
				}
			}
		}
	}

	// Removes all unused definitions, in order to conserve heap. Assumes that the definition is not shared. Use with care!
	void trimItemsDefinitionsTrees(Collection<ItemPath> alwaysKeep) {
		// to play safe, we won't touch PCV-specific complexTypeDefinition
		for (Item<?, ?> item : CollectionUtils.emptyIfNull(items)) {
			if (item instanceof PrismContainer) {
				Collection<ItemPath> alwaysKeepInSub = ItemPath.remainder(CollectionUtils.emptyIfNull(alwaysKeep),
						new ItemPath(item.getElementName()), false);
				((PrismContainer<?>) item).trimDefinitionTree(alwaysKeepInSub);
			}
		}
	}
}
