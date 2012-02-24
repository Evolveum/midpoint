/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import org.apache.commons.lang.Validate;

/**
 * @author semancik
 *
 */
public class PrismContainerValue<T> extends PrismValue implements Dumpable, DebugDumpable {
	
	// This is list. We need to maintain the order internally to provide consistent
    // output in DOM and other ordering-sensitive representations
    private List<Item<?>> items = new ArrayList<Item<?>>();
    private String id;
    
    public PrismContainerValue() {
    	super();
    	// Nothing to do
    }
    
    public PrismContainerValue(SourceType type, Objectable source, PrismContainer container, String id) {
		super(type, source, container);
		this.id = id;
	}

	/**
     * Returns a set of items that the property container contains. The items may be properties or inner property containers.
     * <p/>
     * The set must not be null. In case there are no properties an empty set is
     * returned.
     * <p/>
     * Returned set is mutable. Live object is returned.
     *
     * @return set of items that the property container contains.
     */
    public Collection<Item<?>> getItems() {
    	return items;
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
    public Set<PrismProperty> getProperties() {
        Set<PrismProperty> properties = new HashSet<PrismProperty>();
        for (Item<?> item : getItems()) {
            if (item instanceof PrismProperty) {
                properties.add((PrismProperty) item);
            }
        }
        return properties;
    }
    
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public PrismContainer<T> getParent() {
		return (PrismContainer<T>)super.getParent();
	}
	
	public PropertyPath getPath(PropertyPath pathPrefix) {
		PropertyPath parentPath = getParent().getPath(pathPrefix);
		if (parentPath == null || parentPath.isEmpty()) {
			return parentPath;
		}
		PropertyPathSegment mySegment = new PropertyPathSegment(getParent().getName(), getId());
		return parentPath.allExceptLast().subPath(mySegment);
	}

	void setParent(PrismContainer<T> container) {
		super.setParent(container);
	}
	
	// For compatibility with other PrismValue types
	public T getValue() {
		return asCompileTimeObject();
	}
	
	public T asCompileTimeObject() {
    	// TODO
    	throw new UnsupportedOperationException();
    }

	public Collection<QName> getPropertyNames() {
		Collection<QName> names = new HashSet<QName>();
		for (PrismProperty prop: getProperties()) {
			names.add(prop.getName());
		}
		return names;
	}
    
    /**
     * Adds an item to a property container.
     *
     * @param item item to add.
     * @throws IllegalArgumentException an attempt to add value that already exists
     */
    public void add(Item<?> item) {
        if (findItem(item.getName(), Item.class) != null) {
            throw new IllegalArgumentException("Item " + item.getName() + " is already present in " + this.getClass().getSimpleName());
        }
        item.setParent(this);
        items.add(item);
    }

    /**
     * Adds an item to a property container. Existing value will be replaced.
     *
     * @param item item to add.
     */
    public void addReplaceExisting(Item<?> item) {
        Item existingItem = findItem(item.getName(), Item.class);
        if (existingItem != null) {
            items.remove(existingItem);
            existingItem.setParent(null);
        }
        add(item);
    }
    
    public void remove(Item<?> item) {
        Validate.notNull(item, "Item must not be null.");

        Item existingItem = findItem(item.getName(),  Item.class);
        if (item != null) {
            items.remove(existingItem);
            existingItem.setParent(null);
        }
    }
    
    public void removeAll() {
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
    public void addAll(Collection<? extends Item<?>> itemsToAdd) {
        for (Item<?> item : itemsToAdd) {
        	add(item);
        }
    }

    /**
     * Adds a collection of items to a property container. Existing values will be replaced.
     *
     * @param itemsToAdd items to add
     */
    public void addAllReplaceExisting(Collection<? extends Item<?>> itemsToAdd) {
        // Check for conflicts, remove conflicting values
        for (Item item : itemsToAdd) {
            Item existingItem = findItem(item.getName(), Item.class);
            if (existingItem != null) {
                items.remove(existingItem);
            }
        }
        items.addAll(itemsToAdd);
    }

    // Expects that the "self" path segment is already included in the basePath
    void addItemPathsToList(PropertyPath basePath, Collection<PropertyPath> list) {
    	for (Item<?> item: items) {
    		if (item instanceof PrismProperty) {
    			list.add(basePath.subPath(item.getName()));
    		} else if (item instanceof PrismContainer) {
    			((PrismContainer<?>)item).addItemPathsToList(basePath, list);
    		}
    	}
    }
    
    public void clear() {
    	items.clear();
    }

    public PrismProperty findProperty(QName propertyQName) {
        return findItem(propertyQName, PrismProperty.class);
    }

    /**
     * Finds a specific property in the container by definition.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param propertyDefinition property definition to find.
     * @return found property or null
     */
    public PrismProperty findProperty(PrismPropertyDefinition propertyDefinition) {
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No property definition");
        }
        return findProperty(propertyDefinition.getName());
    }
    
    public <I extends Item<?>> I findItem(QName itemName, Class<I> type) {
    	return findCreateItem(itemName, type, false);
    }
    
    public Item<?> findItem(QName itemName) {
    	return findCreateItem(itemName, Item.class, false);
    }
    
    <I extends Item<?>> I findCreateItem(QName itemName, Class<I> type, boolean create) {
    	for (Item<?> item : items) {
            if (itemName.equals(item.getName())) {
            	if (type.isAssignableFrom(item.getClass())) {
            		return (I)item;
            	} else {
            		if (create) {
            			throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because "
        						+ item.getClass().getSimpleName() + " with the same name exists ("+item.getName()+")");
            		} else {
            			return null;
            		}
            	}
            }
        }
    	if (create) {
    		return createSubItem(itemName, type);
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
    <I extends Item<?>> I findCreateItem(PropertyPath propPath, Class<I> type, boolean create) {
    	PropertyPathSegment first = propPath.first();
    	PropertyPath rest = propPath.rest();
    	for (Item<?> item : items) {
            if (first.getName().equals(item.getName())) {
            	if (type.isAssignableFrom(item.getClass())) {
            		if (rest.isEmpty()) {
            			return (I)item;
            		}
            		// Go deeper
	            	if (item instanceof PrismContainer) {
	            		return ((PrismContainer<?>)item).findCreateItem(propPath, type, create);
	            	} else {
            			if (create) {
            				throw new IllegalStateException("Cannot create " + type.getSimpleName() + " under a "
            						+ item.getClass().getSimpleName() + " ("+item.getName()+")");
            			} else {
            				return null;
            			}
	            	}
            	} else {
            		if (create) {
            			throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because "
        						+ item.getClass().getSimpleName() + " with the same name exists ("+item.getName()+")");
            		} else {
            			return null;
            		}
            	}
                
            }
        }
    	if (create) {
    		I subItem = createSubItem(first.getName(), type);
    		if (rest.isEmpty()) {
    			return (I)subItem;
    		}
    		// Go deeper
        	if (subItem instanceof PrismContainer) {
        		return ((PrismContainer<?>)subItem).findCreateItem(propPath, type, create);
        	} else {
				throw new IllegalStateException("Cannot create " + type.getSimpleName() + " under a "
						+ subItem.getClass().getSimpleName() + " ("+subItem.getName()+")");
        	}
    	} else {
    		return null;
    	}
    }
    
    private <I extends Item<?>> I createSubItem(QName name, Class<I> type) {
    	// the item with specified name does not exist, create it now
		Item<?> newItem = null;
		if (getParent().getDefinition() != null) {
			ItemDefinition itemDefinition = getParent().getDefinition().findItemDefinition(name);
			newItem = itemDefinition.instantiate(name);
		} else {
			newItem = Item.createNewDefinitionlessItem(name, type);
		}
		
		if (type.isAssignableFrom(newItem.getClass())) {
			add(newItem);
			return (I)newItem;
    	} else {
			throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because the item should be of type "
					+ newItem.getClass().getSimpleName() + " ("+newItem.getName()+")");
    	}
    }

    public PrismContainer<?> findOrCreateContainer(QName containerName) {
    	return findCreateItem(containerName, PrismContainer.class, true);
    }

    public PrismProperty findOrCreateProperty(QName propertyQName) {
        PrismProperty property = findItem(propertyQName, PrismProperty.class);
        if (property != null) {
            return property;
        }
        return createProperty(propertyQName);
    }

//    public PrismProperty findOrCreateProperty(PropertyPath parentPath, QName propertyQName, Class<?> valueClass) {
//        PrismContainer container = findOrCreatePropertyContainer(parentPath);
//        if (container == null) {
//            throw new IllegalArgumentException("No container");
//        }
//        return container.findOrCreateProperty(propertyQName, valueClass);
//    }

    public PrismContainer<?> createContainer(QName containerName) {
        if (getParent().getDefinition() == null) {
            throw new IllegalStateException("No definition of container "+containerName);
        }
        PrismContainerDefinition containerDefinition = getParent().getDefinition().findContainerDefinition(containerName);
        if (containerDefinition == null) {
            throw new IllegalArgumentException("No definition of container '" + containerName + "' in " + getParent().getDefinition());
        }
        PrismContainer<?> container = containerDefinition.instantiate();
        add(container);
        return container;
    }

    public PrismProperty createProperty(QName propertyName) {
        PrismPropertyDefinition propertyDefinition = null;
        if (getParent() != null && getParent().getDefinition() != null) {
        	propertyDefinition = getParent().getDefinition().findPropertyDefinition(propertyName);
        	if (propertyDefinition == null) {
        		// container has definition, but there is no property definition. This is either runtime schema
        		// or an error
        		if (getParent().getDefinition().isRuntimeSchema) {
        			// TODO: create opportunistic runtime definition
            		//propertyDefinition = new PrismPropertyDefinition(propertyName, propertyName, typeName, container.prismContext);
        		} else {
        			throw new IllegalArgumentException("No definition for property "+propertyName+" in "+getParent());
        		}
        	}
        }
        PrismProperty property = null;
        if (propertyDefinition == null) {
        	// Definitionless
        	property = new PrismProperty(propertyName);
        } else {
        	property = propertyDefinition.instantiate();
        }
        add(property);
        return property;
    }
    
    public boolean equivalent(PrismContainerValue<?> other) {
        return equalsRealValue(other);
    }
    
	@Override
	public boolean equalsRealValue(PrismValue value) {
		if (value instanceof PrismPropertyValue) {
			return equalsRealValue((PrismContainerValue<T>)value);
		} else {
			return false;
		}
	}
	
	public boolean equalsRealValue(PrismContainerValue<T> other) {
		if (this.getId() != null && other.getId() != null) {
			if (!(this.getId().equals(other.getId()))) {
				return false;
			}
		}
		if (this.getId() != null || other.getId() != null) {
			return false;
		}
		return diffItems(other, true);
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
			PrismContainerDefinition definition = getParent().getDefinition();
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
	void diffMatchingRepresentation(PrismValue otherValue, PropertyPath pathPrefix,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata) {
		if (otherValue instanceof PrismContainerValue) {
			diffRepresentation((PrismContainerValue)otherValue, pathPrefix, deltas, ignoreMetadata);
		} else {
			throw new IllegalStateException("Comparing incompatible values "+this+" - "+otherValue);
		}		
	}
	
	void diffRepresentation(PrismContainerValue<T> otherValue, PropertyPath pathPrefix,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata) {
		// TODO 
		diffItems(otherValue, pathPrefix, deltas, ignoreMetadata);
	}
	
	boolean diffItems(PrismContainerValue<T> other, boolean ignoreMetadata) {
		Collection<? extends ItemDelta> deltas = new ArrayList<ItemDelta>();
		diffItems(other, null, deltas, ignoreMetadata);
		return deltas.isEmpty();
	}
	
	void diffItems(PrismContainerValue<T> other, PropertyPath pathPrefix,
			Collection<? extends ItemDelta> deltas, boolean ignoreMetadata) {
		
		for (Item thisItem: this.getItems()) {
			Item otherItem = other.findItem(thisItem.getName());
			// The "delete" delta will also result from the following diff
			thisItem.diffInternal(otherItem, getPath(pathPrefix), deltas, ignoreMetadata);
		}
		
		for (Item otherItem: other.getItems()) {
			Item thisItem = this.findItem(otherItem.getName());
			if (thisItem == null) {
				// Other has an item that we don't have, this must be an add
				ItemDelta itemDelta = otherItem.createDelta(otherItem.getPath(pathPrefix));
				itemDelta.addValuesToAdd(otherItem.getValues());
				if (!itemDelta.isEmpty()) {
					((Collection)deltas).add(itemDelta);
				}
			}
		}
	}
	
	

	public void applyDefinition(PrismContainerDefinition definition) {
		for (Item<?> item: items) {
			ItemDefinition itemDefinition = definition.findItemDefinition(item.getName());
			item.applyDefinition(itemDefinition);
		}
	}

    public void revive(PrismContext prismContext) {
		for (Item<?> item: items) {
			item.revive(prismContext);
		}
	}
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public PrismContainerValue<T> clone() {
    	PrismContainerValue<T> clone = new PrismContainerValue<T>(getType(), getSource(), getParent(), getId());
        for (Item<?> item: this.items) {
        	clone.items.add(item.clone());
        }
        return clone;
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((items == null) ? 0 : items.hashCode());
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
		PrismContainerValue<?> other = (PrismContainerValue<?>) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		return true;
	}

	@Override
    public String toString() {
        return "PCV[" + getId() + "]:"
                + getItems();
    }

    @Override
    public String dump() {
        return debugDump();
    }

    @Override
    public String debugDump() {
    	return debugDump(0);
    }
    
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append("PCV").append(": ").append(DebugUtil.prettyPrint(getId()));
        Iterator<Item<?>> i = getItems().iterator();
        if (i.hasNext()) {
            sb.append("\n");
        }
        while (i.hasNext()) {
        	Item<?> item = i.next();
            sb.append(item.debugDump(indent + 1));
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
