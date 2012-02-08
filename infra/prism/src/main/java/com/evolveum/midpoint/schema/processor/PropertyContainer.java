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

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.XsdTypeConverter;
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
public class PropertyContainer extends Item {
    private static final long serialVersionUID = 5206821250098051028L;

    private Set<Item> items = new HashSet<Item>();

//    public PropertyContainer() {
//        super();
//    }
//
//    public PropertyContainer(QName name) {
//        super(name);
//    }
//
//    public PropertyContainer(QName name, PropertyContainerDefinition definition) {
//        super(name, definition);
//    }

    public PropertyContainer(QName name, PropertyContainerDefinition definition, PrismContext prismContext, PropertyPath parentPath) {
        super(name, definition, prismContext, parentPath);
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
    public Set<Item> getItems() {
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
    public Set<Property> getProperties() {
        Set<Property> properties = new HashSet<Property>();
        for (Item item : items) {
            if (item instanceof Property) {
                properties.add((Property) item);
            }
        }
        return properties;
    }
    
	public Collection<QName> getPropertyNames() {
		Collection<QName> names = new HashSet<QName>();
		for (Property prop: getProperties()) {
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
    public void add(Item item) {
        if (findItem(item.getName()) != null) {
            throw new IllegalArgumentException("Item " + item.getName() + " is already present in " + this.getClass().getSimpleName());
        }
        items.add(item);
    }

    /**
     * Adds an item to a property container. Existing value will be replaced.
     *
     * @param item item to add.
     */
    public void addReplaceExisting(Item item) {
        Item existingItem = findItem(item.getName());
        if (existingItem != null) {
            items.remove(existingItem);
        }
        items.add(item);
    }

    /**
     * Adds a collection of items to a property container.
     *
     * @param itemsToAdd items to add
     * @throws IllegalArgumentException an attempt to add value that already exists
     */
    public void addAll(Collection<? extends Item> itemsToAdd) {
        // Check for conflicts
        for (Item item : itemsToAdd) {
            if (findItem(item.getName()) != null) {
                throw new IllegalArgumentException("Item " + item.getName() + " is already present in " + this.getClass().getSimpleName());
            }
        }
        items.addAll(itemsToAdd);
    }

    /**
     * Adds a collection of items to a property container. Existing values will be replaced.
     *
     * @param itemsToAdd items to add
     */
    public void addAllReplaceExisting(Collection<? extends Item> itemsToAdd) {
        // Check for conflicts, remove conflicting values
        for (Item item : itemsToAdd) {
            Item existingItem = findItem(item.getName());
            if (existingItem != null) {
                items.remove(existingItem);
            }
        }
        items.addAll(itemsToAdd);
    }


    /**
     * Returns applicable property container definition.
     * <p/>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property container definition
     */
    public PropertyContainerDefinition getDefinition() {
        return (PropertyContainerDefinition) definition;
    }

    /**
     * Sets applicable property container definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PropertyContainerDefinition definition) {
        this.definition = definition;
    }
    
    Collection<PropertyPath> listPropertyPaths() {
    	return listPropertyPaths(null);
    }
    
    Collection<PropertyPath> listPropertyPaths(PropertyPath basePath) {
    	Collection<PropertyPath> list = new HashSet<PropertyPath>();
    	for (Item item: items) {
    		PropertyPath subPath = null;
    		if (basePath == null) {
    			subPath = new PropertyPath(item.getName());
			} else {
				subPath = basePath.subPath(item.getName());
			}
    		if (item instanceof Property) {
    			list.add(subPath);
    		} else if (item instanceof PropertyContainer) {
    			list.addAll(((PropertyContainer)item).listPropertyPaths(subPath));
    		}
    	}
    	return list;
    }

    /**
     * Finds a specific property in the container by name.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param propertyQName property name to find.
     * @return found property or null
     */
    public Property findProperty(QName propertyQName) {
        for (Item item : items) {
            if (item instanceof Property && propertyQName.equals(item.getName())) {
                return (Property) item;
            }
        }
        return null;
    }

    public PropertyContainer findPropertyContainer(QName name) {
        return findItem(name, PropertyContainer.class);
    }

    public PropertyContainer findPropertyContainer(PropertyPath parentPath) {
        if (parentPath == null || parentPath.isEmpty()) {
            return this;
        }
        PropertyContainer subContainer = findItem(parentPath.first(), PropertyContainer.class);
        if (subContainer == null) {
            return null;
        }
        return subContainer.findPropertyContainer(parentPath.rest());
    }

    public Property findProperty(PropertyPath parentPath, QName propertyQName) {
        PropertyContainer pc = findPropertyContainer(parentPath);
        return pc.findProperty(propertyQName);
    }

    public Property findProperty(PropertyPath propertyPath) {
        if (propertyPath.size() == 0) {
            return null;
        }
        if (propertyPath.size() == 1) {
            return findProperty(propertyPath.first());
        }
        PropertyContainer pc = findPropertyContainer(propertyPath.allExceptLast());
        if (pc == null) {
            return null;
        }
        return pc.findProperty(propertyPath.last());
    }

    /**
     * Finds a specific property in the container by name.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param itemQName property name to find.
     * @return found property or null
     */
    public Item findItem(QName itemQName) {
        return findItem(itemQName, Item.class);
    }

    private <T extends Item> T findItem(QName itemQName, Class<T> type) {
        for (Item item : items) {
            if (type.isAssignableFrom(item.getClass()) &&
                    itemQName.equals(item.getName())) {
                return (T) item;
            }
        }
        return null;
    }

    /**
     * Finds a specific property in the container by definition.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param itemDefinition property definition to find.
     * @return found property or null
     */
    public Item findItem(ItemDefinition itemDefinition) {
        if (itemDefinition == null) {
            throw new IllegalArgumentException("No item definition");
        }
        return findItem(itemDefinition.getName());
    }

    /**
     * Finds a specific property in the container by definition.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param propertyDefinition property definition to find.
     * @return found property or null
     */
    public Property findProperty(PropertyDefinition propertyDefinition) {
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No property definition");
        }
        return findProperty(propertyDefinition.getName());
    }

    public PropertyContainer findOrCreatePropertyContainer(QName containerName) {
        PropertyContainer container = findItem(containerName, PropertyContainer.class);
        if (container != null) {
            return container;
        }
        return createPropertyContainer(containerName);
    }

    public PropertyContainer findOrCreatePropertyContainer(PropertyPath containerPath) {
        if (containerPath.size() == 0) {
            return this;
        }
        PropertyContainer container = findOrCreatePropertyContainer(containerPath.first());
        return container.findOrCreatePropertyContainer(containerPath.rest());
    }

    // The valueClass is kind of a hack
    public Property findOrCreateProperty(QName propertyQName, Class<?> valueClass) {
        Property property = findItem(propertyQName, Property.class);
        if (property != null) {
            return property;
        }
        return createProperty(propertyQName, valueClass);
    }

    public Property findOrCreateProperty(PropertyPath parentPath, QName propertyQName, Class<?> valueClass) {
        PropertyContainer container = findOrCreatePropertyContainer(parentPath);
        if (container == null) {
            throw new IllegalArgumentException("No container");
        }
        return container.findOrCreateProperty(propertyQName, valueClass);
    }

    public PropertyContainer createPropertyContainer(QName containerName) {
        if (getDefinition() == null) {
            throw new IllegalStateException("No definition of container "+containerName);
        }
        PropertyContainerDefinition containerDefinition = getDefinition().findPropertyContainerDefinition(containerName);
        if (containerDefinition == null) {
            throw new IllegalArgumentException("No definition of container '" + containerName + "' in " + getDefinition());
        }
        PropertyContainer container = containerDefinition.instantiate(this.getPath());
        add(container);
        return container;
    }

    public Property createProperty(QName propertyName, Class<?> valueClass) {
        if (getDefinition() == null) {
            throw new IllegalStateException("No definition");
        }
        PropertyDefinition propertyDefinition = getDefinition().findPropertyDefinition(propertyName);
        if (propertyDefinition == null) {
        	// HACK: sometimes we don't know if the definition is runtime or not (e.g. applying a patch)
        	// therefore pretend that everything without a definition is runtime (for now)
//        	if (this.getDefinition().isRuntimeSchema) {
        		// HACK: create the definition "on demand" based on the property java type.
        		QName typeName = XsdTypeConverter.toXsdType(valueClass);
        		propertyDefinition = new PropertyDefinition(propertyName, propertyName, typeName, prismContext);
//        	} else {
//        		throw new IllegalArgumentException("No definition of property '" + propertyName + "' in " + getDefinition());
//        	}
        }
        Property property = propertyDefinition.instantiate(this.getPath());
        add(property);
        return property;
    }

    @Override
	public void revive(PrismContext prismContext) {
		if (this.prismContext != null) {
			return;
		}
		super.revive(prismContext);
		for (Item item: items) {
			item.revive(prismContext);
		}
	}

	@Override
    public void serializeToDom(Node parentNode) throws SchemaException {
        if (parentNode == null) {
            throw new IllegalArgumentException("No parent node specified");
        }
        Element containerElement = DOMUtil.getDocument(parentNode).createElementNS(name.getNamespaceURI(), name.getLocalPart());
        parentNode.appendChild(containerElement);
        for (Item item : items) {
            item.serializeToDom(containerElement);
        }
    }

    /**
     * Serialize properties to DOM or JAXB Elements.
     * <p/>
     * The properties are serialized to DOM and returned as a list.
     * The property container element is not serialized.
     *
     * @param doc DOM Document
     * @return list of serialized properties
     * @throws SchemaException the schema definition is missing or is inconsistent
     */
    public List<Object> serializePropertiesToJaxb(Document doc) throws SchemaException {
        List<Object> elements = new ArrayList<Object>();
        // This is not really correct. We should follow the ordering of elements
        // in the schema so we produce valid XML
        // TODO: FIXME
        for (Item item : items) {
            if (item instanceof Property) {
                Property prop = (Property) item;
                if (prop.getDefinition() != null) {
                    elements.addAll(prop.serializeToJaxb(doc));
                } else {
                    elements.addAll(prop.serializeToJaxb(doc, getDefinition().findPropertyDefinition(prop.getName())));
                }
            }
        }
        return elements;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public PropertyContainer clone() {
        PropertyContainer clone = new PropertyContainer(getName(), getDefinition(), prismContext, getParentPath());
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PropertyContainer clone) {
        super.copyValues(clone);
        for (Item item : items) {
            clone.items.add(item.clone());
        }
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
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
		PropertyContainer other = (PropertyContainer) obj;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		return true;
	}

	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getName() + "):"
                + getItems();
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
        Iterator<Item> i = getItems().iterator();
        if (i.hasNext()) {
            sb.append("\n");
        }
        while (i.hasNext()) {
            Item item = i.next();
            sb.append(item.debugDump(indent + 1));
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
        return "PrC";
    }

}
