/*
 * Copyright (c) 2010-2014 Evolveum
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.parser.JaxbDomHack;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * @author semancik
 *
 */
public class PrismContainerValue<T extends Containerable> extends PrismValue implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerValue.class);

	// This is list. We need to maintain the order internally to provide consistent
    // output in DOM and other ordering-sensitive representations
    private List<Item<?>> items = null;
    private Long id;
    
    // XNode map of all sub-elements in this container value.
    private MapXNode rawXNode = null;
    // The elements are set during a schema-less parsing, e.g. during a dumb JAXB parsing of the object
    // We can't do anything smarter, as we don't have definition nor prism context. We cannot even convert
    // this to XNode because no prism context means no parser.
    // So we store the raw elements here and process them later (e.g. during applyDefinition).
    private List<Object> rawElements = null;
    
	private T containerable = null;

    /**
     * Concrete type of the containerable value. It is the declared container type or any of its subtypes.
     * If null, it is considered to be the declared type itself.
     *
     * (It is advisable to keep it null when the concrete type is the same as declared type
     * in order to prevent the serialized form from having unnecessary type QName declaration,
     * as currently this information is directly serialized into value's MapXNode representation
     * as type attribute.)
     *
     * Currently this feature is "half-baked" and experimental. If you need it, you have to set it up explicitly
     * when creating your container value.
     */
    private QName concreteType = null;

    private PrismContainerDefinition concreteTypeDefinition = null;      // lazily evaluated

    transient private PrismContext prismContext;

    public PrismContainerValue() {
    	super();
    	// Nothing to do
    }

    public PrismContainerValue(PrismContext prismContext) {
        this();
        setPrismContext(prismContext);
    }

    private void setPrismContext(PrismContext prismContext) {
        //Validate.notNull(prismContext, "No prismContext in PrismContainerValue");             // not yet
        //if (prismContext == null) {
        //    LOGGER.warn("No prismContext in PrismContainerValue");
        //}
        this.prismContext = prismContext;
    }

    private PrismContainerValue(T containerable) {
		super();
		this.containerable = containerable;
	}

    public PrismContainerValue(T containerable, PrismContext prismContext) {
        this(containerable);
        this.prismContext = prismContext;
    }
    
    private PrismContainerValue(OriginType type, Objectable source, PrismContainerable container, Long id, QName concreteType) {
		super(type, source, container);
		this.id = id;
        this.concreteType = concreteType;
	}

    public PrismContainerValue(OriginType type, Objectable source, PrismContainerable container, Long id, QName concreteType, PrismContext prismContext) {
        this(type, source, container, id, concreteType);
        setPrismContext(prismContext);
    }

    @Override
    public PrismContext getPrismContext() {
        if (prismContext != null) {
            return prismContext;
        }
        if (getParent() != null) {
            return getParent().getPrismContext();
        }
        return null;
    }

    /**
     * Returns a set of items that the property container contains. The items may be properties or inner property containers.
     * <p/>
     * The set may be null. In case there are no properties an empty set is
     * returned.
     * <p/>
     * Returned set is mutable. Live object is returned.
     *
     * @return set of items that the property container contains.
     */
    
    public List<Item<?>> getItems() {
        return items;
    }
    
    public Item<?> getNextItem(Item<?> referenceItem) {
    	if (items == null){
    		return null;
    	}
    	Iterator<Item<?>> iterator = items.iterator();
    	while (iterator.hasNext()) {
    		Item<?> item = iterator.next();
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

    public Item<?> getPreviousItem(Item<?> referenceItem) {
    	if (items == null){
    		return null;
    	}
    	Item<?> lastItem = null;
    	Iterator<Item<?>> iterator = items.iterator();
    	while (iterator.hasNext()) {
    		Item<?> item = iterator.next();
    		if (item == referenceItem) {
    			return lastItem;
    		}
    		lastItem = item;
    	}
    	throw new IllegalArgumentException("Item "+referenceItem+" is not part of "+this);
    }

    
    /**
     * Returns a set of properties that the property container contains.
     * <p/>
     * The set must not be null. In case there are no properties an empty set is
     * returned.
     * <p/>
     * Returned set is immutable! Any change to it will be ignored.
     *
     * @return set of properties that the property container contains.
     */
    public Set<PrismProperty<?>> getProperties() {
        Set<PrismProperty<?>> properties = new HashSet<PrismProperty<?>>();
        if (items == null){
        	return null;
        }
        for (Item<?> item : getItems()) {
            if (item instanceof PrismProperty) {
                properties.add((PrismProperty<?>) item);
            }
        }
        return properties;
    }
    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@SuppressWarnings("unchecked")
	public PrismContainerable<T> getParent() {
		Itemable parent = super.getParent();
		if (parent == null) {
			return null;
		}
		if (!(parent instanceof PrismContainerable)) {
			throw new IllegalStateException("Expected that parent of "+PrismContainerValue.class.getName()+" will be "+
					PrismContainerable.class.getName()+", but it is "+parent.getClass().getName());
		}
		return (PrismContainerable<T>)parent;
	}
	
	void setParent(PrismContainerable container) {
		super.setParent(container);
	}
	
	@SuppressWarnings("unchecked")
	public PrismContainer<T> getContainer() {
		Itemable parent = super.getParent();
		if (parent == null) {
			return null;
		}
		if (!(parent instanceof PrismContainer)) {
			throw new IllegalStateException("Expected that parent of "+PrismContainerValue.class.getName()+" will be "+
					PrismContainer.class.getName()+", but it is "+parent.getClass().getName());
		}
		return (PrismContainer<T>)super.getParent();
	}
	
	void setParent(PrismContainer<T> container) {
		super.setParent(container);
	}
	
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
	public T getValue() {
		return asContainerable();
	}
	
	private List<Object> createElement() {
		return new ArrayList<Object>();
	}

	public T asContainerable() {
		PrismContainerable parent = getParent();
		if (parent == null) {
			throw new IllegalStateException("Cannot represent container value without a parent as containerable; value: "+this);
		}

        Class<T> clazz = null;
        if (concreteType != null) {
            clazz = resolveConcreteClass(parent);
        }
        if (clazz == null) {
            clazz = parent.getCompileTimeClass();
        }
        if (clazz == null) {
            throw new SystemException("Unknown compile time class of container '" + parent.getElementName() + "'.");
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new SystemException("Can't create instance of class '" + clazz.getSimpleName() + "', it's abstract.");
        }
        return asContainerableInternal(clazz);
	}

    private Class<T> resolveConcreteClass(PrismContainerable parent) {
        Class<T> clazz;
        PrismContainerDefinition containerDefinition = parent.getPrismContext().getSchemaRegistry().findContainerDefinitionByType(concreteType);
        if (containerDefinition == null) {
            throw new IllegalStateException("A definition for an explicit type " + concreteType + " of a container " + parent.getElementName() + " value couldn't be found");
        }
        clazz = containerDefinition.getCompileTimeClass();
        if (clazz == null) {
            throw new IllegalStateException("A definition for an explicit type " + concreteType + " of a container " + parent.getElementName() + " value has no compile-time class specified");
        }
        return clazz;
    }

    public T asContainerable(Class<T> defaultClazz) {
        Class<T> clazz = defaultClazz;
        if (concreteType != null) {
            PrismContainerable parent = getParent();
            if (parent != null) {
                clazz = resolveConcreteClass(parent);
            } else {
                throw new IllegalStateException("Cannot determine compile-time class for concrete type " + concreteType + " because the respective prism container value has no parent");
            }
        }
        return asContainerableInternal(clazz);
    }

    private T asContainerableInternal(Class<T> clazz) {
	   if (containerable != null) {
		   return containerable ;
	   }
		try {
            containerable = clazz.newInstance();
            containerable.setupContainerValue(this);
            return containerable;
        } catch (SystemException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException("Couldn't create jaxb object instance of '" + clazz + "': "+ex.getMessage(), ex);
        }
    }

	public Collection<QName> getPropertyNames() {
		Collection<QName> names = new HashSet<QName>();
		for (PrismProperty<?> prop: getProperties()) {
			names.add(prop.getElementName());
		}
		return names;
	}
    
    /**
     * Adds an item to a property container.
     *
     * @param item item to add.
     * @throws SchemaException 
     * @throws IllegalArgumentException an attempt to add value that already exists
     */
    public boolean add(Item<?> item) throws SchemaException {
    	if (item.getElementName() == null) {
    		throw new IllegalArgumentException("Cannot add item without a name to value of container "+getParent());
    	}
        if (findItem(item.getElementName(), Item.class) != null) {
            throw new IllegalArgumentException("Item " + item.getElementName() + " is already present in " + this.getClass().getSimpleName());
        }
        item.setParent(this);
        PrismContext prismContext = getPrismContext();
        if (prismContext != null) {
        	item.setPrismContext(prismContext);
        }
        if (getActualDefinition() != null && item.getDefinition() == null) {
        	item.applyDefinition(determineItemDefinition(item.getElementName(), getActualDefinition()), false);
        }
        if (items == null) {
        	items = new ArrayList<Item<?>>();
        }
        return items.add(item);
    }

    /**
     * Merges the provided item into this item. The values are joined together.
     * Returns true if new item or value was added.
     */
	public <V extends PrismValue> boolean merge(Item<V> item) throws SchemaException {
		Item<V> exisingItem = findItem(item.getElementName(), Item.class);
		if (exisingItem == null) {
			return add(item);
		} else {
			boolean changed = false;
			for (V newVal: item.getValues()) {
				if (exisingItem.add((V) newVal.clone())) {
					changed = true;
				}
			}
			return changed;
		}
	}
	
	/**
     * Substract the provided item from this item. The values of the provided item are deleted
     * from this item.
     * Returns true if this item was changed.
     */
	public <V extends PrismValue> boolean substract(Item<V> item) throws SchemaException {
		Item<V> exisingItem = findItem(item.getElementName(), Item.class);
		if (exisingItem == null) {
			return false;
		} else {
			boolean changed = false;
			for (V newVal: item.getValues()) {
				if (exisingItem.remove(newVal)) {
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
    public void addReplaceExisting(Item<?> item) throws SchemaException {
    	if (item == null){
    		return;
    	}
        Item<?> existingItem = findItem(item.getElementName(), Item.class);
        if (existingItem != null && items != null) {
            items.remove(existingItem);
            existingItem.setParent(null);
        }
        add(item);
    }
    
    public void remove(Item<?> item) {
        Validate.notNull(item, "Item must not be null.");

        Item<?> existingItem = findItem(item.getElementName(),  Item.class);
        if (existingItem != null && items != null) {
            items.remove(existingItem);
            existingItem.setParent(null);
        }
    }
    
    public void removeAll() {
    	if (items == null){
    		return;
    	}
        Iterator<Item<?>> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item<?> item = iterator.next();
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
    public void addAll(Collection<? extends Item<?>> itemsToAdd) throws SchemaException {
        for (Item<?> item : itemsToAdd) {
        	add(item);
        }
    }

    /**
     * Adds a collection of items to a property container. Existing values will be replaced.
     *
     * @param itemsToAdd items to add
     */
    public void addAllReplaceExisting(Collection<? extends Item<?>> itemsToAdd) throws SchemaException {
        // Check for conflicts, remove conflicting values
        for (Item<?> item : itemsToAdd) {
            Item<?> existingItem = findItem(item.getElementName(), Item.class);
            if (existingItem != null && items != null) {
                items.remove(existingItem);
            }
        }
        addAll(itemsToAdd);
    }
    
	public void replace(Item<?> oldItem, Item<?> newItem) throws SchemaException {
		remove(oldItem);
		add(newItem);
	}

    // Expects that the "self" path segment is already included in the basePath
    // todo treat unqualified names
    void addItemPathsToList(ItemPath basePath, Collection<ItemPath> list) {
    	if (items != null) {
	    	for (Item<?> item: items) {
	    		if (item instanceof PrismProperty) {
	    			list.add(basePath.subPath(item.getElementName()));
	    		} else if (item instanceof PrismContainer) {
	    			((PrismContainer<?>)item).addItemPathsToList(basePath, list);
	    		}
	    	}
    	}
    }
    
    public void clear() {
    	if (items != null) {
    		items.clear();
    	}
    }
    
    public boolean contains(Item item) {
    	if (items != null) {
    		return items.contains(item);
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
    	Item<?> subItem = findItem(subName);
    	if (subItem == null) {
    		return null;
    	}
    	return subItem.find(rest);
    }
    
    @Override
	public <X extends PrismValue> PartiallyResolvedValue<X> findPartial(ItemPath path) {
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
    	Item<?> subItem = findItem(subName);
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
        return (PrismProperty<X>) findItem(propertyPath);
    }

    /**
     * Finds a specific property in the container by definition.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param propertyDefinition property definition to find.
     * @return found property or null
     */
    public <X> PrismProperty<X> findProperty(PrismPropertyDefinition propertyDefinition) {
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
    
    public <I extends Item<?>> I findItem(QName itemName, Class<I> type) {
    	try {
			return findCreateItem(itemName, type, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }
    
    public Item<?> findItem(QName itemName) {
    	try {
			return findCreateItem(itemName, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }
    
    public Item<?> findItem(ItemPath itemPath) {
    	try {
			return findCreateItem(itemPath, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
    }
    
    @SuppressWarnings("unchecked")
	<I extends Item<?>> I findCreateItem(QName itemName, Class<I> type, ItemDefinition itemDefinition, boolean create) throws SchemaException {
        Item<?> item = findItemByQName(itemName);
        if (item != null) {
            if (type.isAssignableFrom(item.getClass())) {
                return (I)item;
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
    
    public <I extends Item<?>> I findItem(ItemDefinition itemDefinition, Class<I> type) {
        if (itemDefinition == null) {
            throw new IllegalArgumentException("No item definition");
        }
        return findItem(itemDefinition.getName(), type);
    }


    // Expects that "self" path is NOT present in propPath
    @SuppressWarnings("unchecked")
	<I extends Item<?>> I findCreateItem(ItemPath propPath, Class<I> type, ItemDefinition itemDefinition, boolean create) throws SchemaException {
    	ItemPathSegment first = propPath.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+propPath+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	ItemPath rest = propPath.rest();
        Item<?> item = findItemByQName(subName);
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
                            return (I)item;
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

    private Item<?> findItemByQName(QName subName) throws SchemaException {
        if (items == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(subName.getNamespaceURI())) {
            // traditional search by fully qualified name
            for (Item<?> item : items) {
                if (subName.equals(item.getElementName())) {
                    return item;
                }
            }
            return null;
        } else {
            // approximate search using local part
            String localNameToFind = subName.getLocalPart();
            Item<?> matching = null;
            for (Item<?> item : items) {
                if (localNameToFind.equals(item.getElementName().getLocalPart())) {
                    if (matching != null) {
                        String containerName = getParent() != null ? DebugUtil.formatElementName(getParent().getElementName()) : "";
                        throw new SchemaException("Using ambiguous unqualified item name " + localNameToFind + " in container " + containerName);
                    } else {
                        matching = item;
                    }
                }
            }
            return matching;
        }
    }

    @SuppressWarnings("unchecked")
	private <I extends Item<?>> I createSubItem(QName name, Class<I> type, ItemDefinition itemDefinition) throws SchemaException {
    	// the item with specified name does not exist, create it now
		Item<?> newItem = null;
		
		if (itemDefinition == null && getActualDefinition() != null) {
			itemDefinition = determineItemDefinition(name, getActualDefinition());
			if (itemDefinition == null) {
				throw new SchemaException("No definition for item "+name+" in "+getParent());
			}
		}
		
		if (itemDefinition != null) {
			if (StringUtils.isNotBlank(name.getNamespaceURI())){
				newItem = itemDefinition.instantiate(name);
			} else {
				QName computed = new QName(itemDefinition.getNamespace(), name.getLocalPart());
				newItem = itemDefinition.instantiate(computed);
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
			add(newItem);
			return (I)newItem;
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
    
    public Item<?> findOrCreateItem(QName containerName) throws SchemaException {
    	return findCreateItem(containerName, Item.class, null, true);
    }
    
    public <X extends Item> X findOrCreateItem(QName containerName, Class<X> type) throws SchemaException {
    	return findCreateItem(containerName, type, null, true);
    }
    
    public <X extends Item> X findOrCreateItem(ItemPath path, Class<X> type, ItemDefinition definition) throws SchemaException {
    	return findCreateItem(path, type, definition, true);
    }

    public <X> PrismProperty<X> findOrCreateProperty(QName propertyQName) throws SchemaException {
        PrismProperty<X> property = findItem(propertyQName, PrismProperty.class);
        if (property != null) {
            return property;
        }
        return createProperty(propertyQName);
    }
    
    public <X> PrismProperty<X> findOrCreateProperty(PrismPropertyDefinition propertyDef) throws SchemaException {
        PrismProperty<X> property = findItem(propertyDef.getName(), PrismProperty.class);
        if (property != null) {
            return property;
        }
        return createProperty(propertyDef);
    }

//    public PrismProperty findOrCreateProperty(PropertyPath parentPath, QName propertyQName, Class<?> valueClass) {
//        PrismContainer container = findOrCreatePropertyContainer(parentPath);
//        if (container == null) {
//            throw new IllegalArgumentException("No container");
//        }
//        return container.findOrCreateProperty(propertyQName, valueClass);
//    }

    public <X extends Containerable> PrismContainer<X> createContainer(QName containerName) throws SchemaException {
        if (getActualDefinition() == null) {
            throw new IllegalStateException("No definition of container "+containerName);
        }
        PrismContainerDefinition containerDefinition = getActualDefinition().findContainerDefinition(containerName);
        if (containerDefinition == null) {
            throw new IllegalArgumentException("No definition of container '" + containerName + "' in " + getActualDefinition());
        }
        PrismContainer<X> container = containerDefinition.instantiate();
        add(container);
        return container;
    }

    public <X> PrismProperty<X> createProperty(QName propertyName) throws SchemaException {
        PrismPropertyDefinition propertyDefinition = null;
        if (getActualDefinition() != null) {
        	propertyDefinition = getActualDefinition().findPropertyDefinition(propertyName);
        	if (propertyDefinition == null) {
        		// container has definition, but there is no property definition. This is either runtime schema
        		// or an error
        		if (getParent().getDefinition().isRuntimeSchema) {
        			// TODO: create opportunistic runtime definition
            		//propertyDefinition = new PrismPropertyDefinition(propertyName, propertyName, typeName, container.prismContext);
        		} else {
                    throw new IllegalArgumentException("No definition for property "+propertyName+" in "+getActualDefinition());
        		}
        	}
        }
        PrismProperty<X> property = null;
        if (propertyDefinition == null) {
        	// Definitionless
        	property = new PrismProperty<X>(propertyName, prismContext);
        } else {
        	property = propertyDefinition.instantiate();
        }
        add(property);
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

	// Expects that "self" path is NOT present in propPath
	<I extends Item<?>> void removeItem(ItemPath propPath, Class<I> itemType) {
		if (items == null){
    		return;
    	}
		ItemPathSegment first = propPath.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to remove item using a non-name path "+propPath+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	ItemPath rest = propPath.rest();
    	Iterator<Item<?>> itemsIterator = items.iterator();
    	while(itemsIterator.hasNext()) {
    		Item<?> item = itemsIterator.next();
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
			for (Item<?> item : getItems()) {
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
				for (Item<?> item : items) {            // todo unqualified names!
					if (first.isWildcard() || subName.equals(item.getElementName())) {
						item.accept(visitor, rest, recursive);
					}
				}
			}
		}
	}
    
    public boolean hasCompleteDefinition() {
		if (items != null) {
			for (Item<?> item : getItems()) {
				if (!item.hasCompleteDefinition()) {
					return false;
				}
			}
		}
    	return true;
    }

	@Override
	public boolean representsSameValue(PrismValue other) {
		if (other instanceof PrismContainerValue) {
			return representsSameValue((PrismContainerValue<T>)other);
		} else {
			return false;
		}
	}
	
	public boolean representsSameValue(PrismContainerValue<T> other) {
		if (getParent() != null) {
			PrismContainerDefinition definition = getActualDefinition();
			if (definition != null) {
				if (definition.isSingleValue()) {
					// There is only one value, therefore it always represents the same thing
					return true;
				}
			}
		}
		if (other.getParent() != null) {
			PrismContainerDefinition definition = other.getActualDefinition();
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
	
	void diffRepresentation(PrismContainerValue<T> otherValue,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral) {
		PrismContainerValue<T> thisValue = this;
		if (this.isRaw() || otherValue.isRaw()) {
			try {
				if (this.isRaw()) {
					otherValue = parseRawElementsToNewValue(otherValue, thisValue);
				} else if (otherValue.isRaw()) {
					thisValue = parseRawElementsToNewValue(thisValue, otherValue);
				}
			} catch (SchemaException e) {
				// TODO: Maybe just return false?
				throw new IllegalArgumentException("Error parsing the value of container "+getParent()+" using the 'other' definition "+
						"during a compare: "+e.getMessage(),e);
			}
		} 
		diffItems(thisValue, otherValue, deltas, ignoreMetadata, isLiteral);
	}
	
	@Override
	public boolean isRaw() {
		return rawXNode != null || rawElements != null;
	}
	
	public MapXNode getRawXNode() {
		return rawXNode;
	}

	public List<Object> getRawElements() {
		return rawElements;
	}
		
	public boolean addRawElement(Object element) throws SchemaException {
		PrismContainerDefinition<T> definition = getDefinition();
		if (definition == null) {
			// We cannot do much better. We do not even have prism context here.
			if (rawElements == null) {
				if (items != null && !items.isEmpty()) {
					throw new IllegalStateException("Attempt to add raw element to prism container value which already has items: "+this);
				}
				rawElements = new ArrayList<Object>();
			}
			return rawElements.add(element);
		} else {
			// We have definition here, we can parse it right now
			Item<?> subitem = parseRawElement(element, definition);
			return merge(subitem);
		}
	}
	
	public boolean deleteRawElement(Object element) throws SchemaException {
		PrismContainerDefinition<T> definition = getDefinition();
		if (definition == null) {
			// We cannot do much better. We do not even have prism context here.
			if (rawElements == null) {
				rawElements = new ArrayList<Object>();
			}
			return rawElements.add(element);
		} else {
			// We have definition here, we can parse it right now
			Item<?> subitem = parseRawElement(element, definition);
			return substract(subitem);
		}
	}

	
	public boolean removeRawElement(Object element) {
		return rawElements.remove(element);
	}

	private <V extends PrismValue> Item<V> parseRawElement(Object element, PrismContainerDefinition<T> definition) throws SchemaException {
		JaxbDomHack jaxbDomHack = definition.getPrismContext().getJaxbDomHack();
		return jaxbDomHack.parseRawElement(element, definition);
	}
	
	private PrismContainerValue<T> parseRawElementsToNewValue(PrismContainerValue<T> origCVal, PrismContainerValue<T> definitionSource) throws SchemaException {
		if (definitionSource.getParent() == null || definitionSource.getActualDefinition() == null) {
			throw new IllegalArgumentException("Attempt to use container " + origCVal.getParent() + 
			" values in a raw parsing state (raw elements) with parsed value that has no definition");
		}
		PrismContainerDefinition<T> definition = definitionSource.getActualDefinition();
		
		XNode origRawXnode = origCVal.rawXNode;
		if (origRawXnode != null) {
			XNodeProcessor xnodeProcessor = definition.getPrismContext().getXnodeProcessor();
			PrismContainerValue<T> newCVal = xnodeProcessor.parsePrismContainerValue(origRawXnode, definition);
			return newCVal;
		}
		
		List<Object> origRawElements = origCVal.rawElements;
		if (origRawElements != null) {
			PrismContainerValue<T> newCVal = new PrismContainerValue<T>(prismContext);
			for (Object rawElement: origRawElements) {
				Item<?> subitem = parseRawElement(rawElement, definition);
				newCVal.merge(subitem);
			}
			return newCVal;
		}
		
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void diffItems(PrismContainerValue<T> thisValue, PrismContainerValue<T> other,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral) {
		
		if (thisValue.getItems() !=  null) {
			for (Item<?> thisItem: thisValue.getItems()) {
				Item otherItem = other.findItem(thisItem.getElementName());
				// The "delete" delta will also result from the following diff
				thisItem.diffInternal(otherItem, deltas, ignoreMetadata, isLiteral);
			}
		}
		
		if (other.getItems() != null) {
			for (Item otherItem: other.getItems()) {
				Item thisItem = thisValue.findItem(otherItem.getElementName());
				if (thisItem == null) {
					// Other has an item that we don't have, this must be an add
					ItemDelta itemDelta = otherItem.createDelta();
					itemDelta.addValuesToAdd(otherItem.getClonedValues());
					if (!itemDelta.isEmpty()) {
						((Collection)deltas).add(itemDelta);
					}
				}
			}
		}
	}	
	
	@Override
	protected PrismContainerDefinition<T> getDefinition() {
		return (PrismContainerDefinition<T>) super.getDefinition();
	}

	@Override
	public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
		if (!(definition instanceof PrismContainerDefinition)) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to container " + this);
    	}
		applyDefinition((PrismContainerDefinition<T>)definition, force);
	}

	public void applyDefinition(PrismContainerDefinition<T> definition, boolean force) throws SchemaException {
		if (definition.isWildcard()) {
			// No point in aplying this. Nothing will change and there may be phantom errors.
			return;
		}
		if (rawElements != null) {
			for (Object rawElement: rawElements) {
				Item<?> subitem = parseRawElement(rawElement, definition);
				merge(subitem);
			}
			rawElements = null;
		}
		if (rawXNode != null) {
			XNodeProcessor xnodeProcessor = definition.getPrismContext().getXnodeProcessor();
			PrismContainerValue<T> newCVal = xnodeProcessor.parsePrismContainerValue(rawXNode, definition);
			// Maybe we need to manually reset parent on items?
			addAll(newCVal.getItems());
			rawXNode = null;
		}
		if (items != null) {
			for (Item<?> item: items) {
				if (item.getDefinition() != null && !force) {
					// Item has a definition already, no need to apply it
					continue;
				}
				ItemDefinition itemDefinition = determineItemDefinition(item.getElementName(), definition);
				if (itemDefinition == null && item.getDefinition() != null && item.getDefinition().isDynamic()) {
					// We will not apply the null definition here. The item has a dynamic definition that we don't
					// want to destroy as it cannot be reconstructed later.
				} else {
					item.applyDefinition(itemDefinition);
				}
			}
		}
	}

	/**
	 * This method can both return null and throws exception. It returns null in case there is no definition
	 * but it is OK (e.g. runtime schema). It throws exception if there is no definition and it is not OK.
	 */
	private ItemDefinition determineItemDefinition(QName itemName, PrismContainerDefinition<T> containerDefinition) throws SchemaException {
		ItemDefinition itemDefinition = containerDefinition.findItemDefinition(itemName);
		if (itemDefinition == null) {
			if (containerDefinition.isRuntimeSchema()) {
				// If we have prism context, try to locate global definition. But even if that is not
				// found it is still OK. This is runtime container. We tolerate quite a lot here.
				PrismContext prismContext = getPrismContext();
				if (prismContext != null) {
					itemDefinition = prismContext.getSchemaRegistry().resolveGlobalItemDefinition(itemName);
				}
			} else {
				throw new SchemaException("No definition for item " + itemName + " in " + getParent());
			}
		}
		return itemDefinition;
	}

	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
        if (this.prismContext == null) {
            this.prismContext = prismContext;
        }
		super.revive(prismContext);
		if (items != null) {
			for (Item<?> item: items) {
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
    	if (items != null) {
	    	Iterator<Item<?>> iterator = items.iterator();
	    	while (iterator.hasNext()) {
	    		Item<?> item = iterator.next();
	    		item.normalize();
	    		if (item.isEmpty()) {
	    			iterator.remove();
	    		}
	    	}
    	}
	}

	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw) {
		ItemPath myPath = getPath();
		if (prohibitRaw && isRaw()) {
			throw new IllegalStateException("Raw elements in container value "+this+" ("+myPath+" in "+rootItem+")");
		}
		if (items == null && !isRaw()) {
			// This is normal empty container, isn't it?
//			throw new IllegalStateException("Neither items nor raw elements specified in container value "+this+" ("+myPath+" in "+rootItem+")");
		}
		if (items != null && isRaw()) {
			throw new IllegalStateException("Both items and raw elements specified in container value "+this+" ("+myPath+" in "+rootItem+")");
		}
		if (items != null) {
			for (Item<?> item: items) {
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
				item.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw);
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
		for (Item<?> item: getItems()) {
			item.assertDefinitions(tolerateRaw, "value("+getId()+") in "+sourceDescription);
		}
	}

	public PrismContainerValue<T> clone() {
    	PrismContainerValue<T> clone = new PrismContainerValue<T>(getOriginType(), getOriginObject(), getParent(), getId(), this.concreteType, this.prismContext);
    	copyValues(clone);
        return clone;
    }
    
	protected void copyValues(PrismContainerValue<T> clone) {
		super.copyValues(clone);
		clone.id = this.id;
		if (this.items != null) {
			for (Item<?> item : this.items) {
				Item<?> clonedItem = item.clone();
				clonedItem.setParent(clone);
				if (clone.items == null) {
					clone.items = new ArrayList<>(this.items.size());
				}
				clone.items.add(clonedItem);
			}
		}
        // TODO: deep clonning?
        clone.rawXNode = this.rawXNode;
        clone.rawElements = this.rawElements;
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
		} else if (!this.equalsItems(this, (PrismContainerValue<T>) other, ignoreMetadata, isLiteral)) {
			return false;
		}
		return true;
	}
	
	boolean equalsItems(PrismContainerValue<T> other, boolean ignoreMetadata, boolean isLiteral) {
		return equalsItems(this, other, ignoreMetadata, isLiteral);
	}
	
	boolean equalsItems(PrismContainerValue<T> thisValue, PrismContainerValue<T> other, 
			boolean ignoreMetadata, boolean isLiteral) {
		Collection<? extends ItemDelta<?>> deltas = new ArrayList<ItemDelta<?>>();
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((items == null) ? 0 : MiscUtil.unorderedCollectionHashcode(items));
		return result;
	}
		
	@Override
    public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PCV(");
		sb.append(getId());
		if (isRaw()) {
			sb.append(", raw");
		}
		sb.append("):");
        sb.append(getItems());
        return sb.toString();
    }

    @Override
    public String debugDump() {
    	return debugDump(0);
    }
    
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        boolean wasIndent = false;
        if (DebugUtil.isDetailedDebugDump()) {
        	DebugUtil.indentDebugDump(sb, indent);
            wasIndent = true;
        	sb.append("PCV").append(": ");
        }
        boolean multivalue = true;
        PrismContainerable<T> parent = getParent();
        if (parent != null && parent.getDefinition() != null) {
        	multivalue = parent.getDefinition().isMultiValue();
        }
        Long id = getId();
        if (multivalue || id != null || DebugUtil.isDetailedDebugDump()) {
        	if (!wasIndent) {
            	DebugUtil.indentDebugDump(sb, indent);
                wasIndent = true;
        	}
        	sb.append("id=").append(PrettyPrinter.prettyPrint(getId()));
        }
        appendOriginDump(sb);
        List<Item<?>> items = getItems();
        if (items != null) {
	        Iterator<Item<?>> i = getItems().iterator();
	        if (wasIndent && i.hasNext()) {
	            sb.append("\n");
	        }
	        while (i.hasNext()) {
	        	Item<?> item = i.next();
	            sb.append(item.debugDump(indent + 1));
	            if (i.hasNext()) {
	                sb.append("\n");
	            }
	        }
        } else {
        	if (isRaw()) {
        		if (wasIndent) {
        			sb.append("\n");
        		}
        		DebugUtil.indentDebugDump(sb, indent + 1);
        		sb.append("(raw)");
        	}
        }
        return sb.toString();
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
    public static void copyDefinition(Containerable aClone, Containerable original) {
        try {
            Validate.notNull(original.asPrismContainerValue().getParent(), "original PrismContainerValue has no parent");

            PrismContainerDefinition definition = original.asPrismContainerValue().getActualDefinition();
            Validate.notNull(definition, "original PrismContainer definition is null");

            PrismContainer<?> aCloneParent = definition.instantiate();
            aCloneParent.add(aClone.asPrismContainerValue());
        } catch (SchemaException e) {
            throw new SystemException("Unexpected SchemaException when copying definition from original object to its clone", e);
        }
    }

    public QName getConcreteType() {
        return concreteType;
    }

    public void setConcreteType(QName concreteType) {
        this.concreteType = concreteType;
        this.concreteTypeDefinition = null;
    }

    private PrismContainerDefinition getConcreteTypeDefinition() {
        if (concreteTypeDefinition != null) {
            return concreteTypeDefinition;
        }
        if (concreteType != null) {
            // First of all, if we have parent definition and it is applicable, just use that
            // (Besides performance aspects, the parent definition might be already converted
            // to contain ResourceAttributeContainerDefinition instead of PrismContainerDefinition
            // - in case of ShadowType.)
            if (getParent() != null && getParent().getDefinition() != null && concreteType.equals(getParent().getDefinition().getTypeName())) {
                concreteTypeDefinition = getParent().getDefinition();
            } else {
                PrismContext prismContext = getPrismContext();
                if (prismContext != null) {
                    concreteTypeDefinition = prismContext.getSchemaRegistry().findContainerDefinitionByType(concreteType);
                    if (concreteTypeDefinition == null) {
                        throw new IllegalStateException("Couldn't find a definition for concrete type " + concreteType);
                    }
                }
            }
        }
        return concreteTypeDefinition;      // may still be null at this moment
    }

    private PrismContainerDefinition<T> getActualDefinition() {
        if (getParent() != null) {
            PrismContainerDefinition concreteDef = getConcreteTypeDefinition();
            if (concreteDef != null) {
                return concreteDef;
            } else {
                return getParent().getDefinition();
            }
        } else {
            return null;
        }
    }


}
