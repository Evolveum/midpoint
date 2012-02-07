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

import com.evolveum.midpoint.schema.TypedValue;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import org.apache.cxf.common.util.StringUtils;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;

/**
 * Definition of a property container.
 * <p/>
 * Property container groups properties into logical blocks. The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * <p/>
 * Property Container contains a set of (potentially multi-valued) properties.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * <p/>
 * This class represents schema definition for property container. See
 * {@link Definition} for more details.
 *
 * @author Radovan Semancik
 */
public class PropertyContainerDefinition extends ItemDefinition {

    private static final long serialVersionUID = -5068923696147960699L;
    private static final String ANY_GETTER_NAME = "getAny";
    protected ComplexTypeDefinition complexTypeDefinition;
    protected Schema schema;
    /**
     * This means that the property container is not defined by fixed (compile-time) schema.
     * This in fact means that we need to use getAny in a JAXB types. It does not influence the
     * processing of DOM that much, as that does not really depend on compile-time/run-time distinction.
     */
    protected boolean isRuntimeSchema;

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    PropertyContainerDefinition(QName name, ComplexTypeDefinition complexTypeDefinition) {
        super(name, determineDefaultName(complexTypeDefinition), determineTypeName(complexTypeDefinition));
        this.complexTypeDefinition = complexTypeDefinition;
        if (complexTypeDefinition == null) {
            isRuntimeSchema = true;
        } else {
            isRuntimeSchema = false;
        }
    }

    private static QName determineTypeName(ComplexTypeDefinition complexTypeDefinition) {
        if (complexTypeDefinition == null) {
            // Property container without type: xsd:any
            // FIXME: this is kind of hack, but it works now
            return DOMUtil.XSD_ANY;
        }
        return complexTypeDefinition.getTypeName();
    }

    private static QName determineDefaultName(ComplexTypeDefinition complexTypeDefinition) {
        if (complexTypeDefinition == null) {
            return null;
        }
        return complexTypeDefinition.getDefaultName();
    }

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    PropertyContainerDefinition(Schema schema, QName name, ComplexTypeDefinition complexTypeDefinition) {
        super(name, determineDefaultName(complexTypeDefinition), determineTypeName(complexTypeDefinition));
        this.complexTypeDefinition = complexTypeDefinition;
        if (schema == null) {
            throw new IllegalArgumentException("Schema can't be null.");
        }
        this.schema = schema;
    }

    protected String getSchemaNamespace() {
        return schema.getNamespace();
    }

    public ComplexTypeDefinition getComplexTypeDefinition() {
        return complexTypeDefinition;
    }

    public void setComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
        this.complexTypeDefinition = complexTypeDefinition;
    }

    public boolean isWildcard() {
        return (complexTypeDefinition == null);
    }

    public <T extends ItemDefinition> T findItemDefinition(QName name, Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("type not specified while searching for " + name + " in " + this);
        }
        if (name == null) {
            throw new IllegalArgumentException("name not specified while searching in " + this);
        }

        if (isItemValid(this, name, clazz)) {
            return (T) this;
        }

        for (ItemDefinition def : getDefinitions()) {
            if (isItemValid(def, name, clazz)) {
                return (T) def;
            }
        }
        return null;
    }

    private <T extends ItemDefinition> boolean isItemValid(ItemDefinition def, QName name, Class<T> clazz) {
        return clazz.isAssignableFrom(def.getClass()) && name.equals(def.getName());
    }

    public <T extends ItemDefinition> T findItemDefinition(PropertyPath path, Class<T> clazz) {
        if (path.isEmpty()) {
            return (T) this;
        }
        QName first = path.first();
        for (ItemDefinition def : getDefinitions()) {
            if (first.equals(def.getName())) {
                return def.findItemDefinition(path.rest(), clazz);
            }
        }
        return null;
    }

    public ItemDefinition findItemDefinition(QName name) {
        return findItemDefinition(name, ItemDefinition.class);
    }

    /**
     * Finds a PropertyDefinition by looking at the property name.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param name property definition name
     * @return found property definition or null
     */
    public PropertyDefinition findPropertyDefinition(QName name) {
        return findItemDefinition(name, PropertyDefinition.class);
    }

    public PropertyDefinition findPropertyDefinition(PropertyPath propertyPath) {
        if (propertyPath.isEmpty()) {
            throw new IllegalArgumentException("Property path is empty while searching for property definition in " + this);
        }
        if (propertyPath.size() == 1) {
            return findPropertyDefinition(propertyPath.first());
        }
        PropertyContainerDefinition pcd = findPropertyContainerDefinition(propertyPath.first());
        return pcd.findPropertyDefinition(propertyPath.rest());
    }

    /**
     * Finds an inner PropertyContainerDefinition by looking at the property container name.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param name property container definition name
     * @return found property container definition or null
     */
    public PropertyContainerDefinition findPropertyContainerDefinition(QName name) {
        return findItemDefinition(name, PropertyContainerDefinition.class);
    }

    /**
     * Finds an inner PropertyContainerDefinition by following the property container path.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param path property container path
     * @return found property container definition or null
     */
    public PropertyContainerDefinition findPropertyContainerDefinition(PropertyPath path) {
        return findItemDefinition(path, PropertyContainerDefinition.class);
    }

    /**
     * Returns set of property definitions.
     * <p/>
     * WARNING: This may return definitions from the associated complex type.
     * Therefore changing the returned set may influence also the complex type definition.
     * <p/>
     * The set contains all property definitions of all types that were parsed.
     * Order of definitions is insignificant.
     *
     * @return set of definitions
     */
    public Collection<ItemDefinition> getDefinitions() {
        if (complexTypeDefinition == null) {
            // e.g. for xsd:any containers
            // FIXME
            return new HashSet<ItemDefinition>();
        }
        return complexTypeDefinition.getDefinitions();
    }

    /**
     * Returns set of property definitions.
     * <p/>
     * The set contains all property definitions of all types that were parsed.
     * Order of definitions is insignificant.
     * <p/>
     * The returned set is immutable! All changes may be lost.
     *
     * @return set of definitions
     */
    public Set<PropertyDefinition> getPropertyDefinitions() {
        Set<PropertyDefinition> props = new HashSet<PropertyDefinition>();
        for (ItemDefinition def : complexTypeDefinition.getDefinitions()) {
            if (def instanceof PropertyDefinition) {
                props.add((PropertyDefinition) def);
            }
        }
        return props;
    }


    public boolean isRuntimeSchema() {
        return isRuntimeSchema;
    }

    public void setRuntimeSchema(boolean isRuntimeSchema) {
        this.isRuntimeSchema = isRuntimeSchema;
    }

    /**
     * Create property container instance with a default name.
     * <p/>
     * This is a preferred way how to create property container.
     */
    @Override
    public PropertyContainer instantiate(PropertyPath parentPath) {
        return instantiate(getNameOrDefaultName(), null, parentPath);
    }

    /**
     * Create property container instance with a specified name.
     * <p/>
     * This is a preferred way how to create property container.
     */
    @Override
    public PropertyContainer instantiate(QName name, PropertyPath parentPath) {
        return new PropertyContainer(name, this, null, parentPath);
    }

    /**
     * Create property container instance with a specified name and element.
     * <p/>
     * This is a preferred way how to create property container.
     */
    @Override
    public PropertyContainer instantiate(QName name, Object element, PropertyPath parentPath) {
        return new PropertyContainer(name, this, element, parentPath);
    }

    /**
     * Shallow clone
     */
    @Override
    public PropertyContainerDefinition clone() {
        PropertyContainerDefinition clone = new PropertyContainerDefinition(name, complexTypeDefinition);
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(PropertyContainerDefinition clone) {
        super.copyDefinitionData(clone);
        clone.complexTypeDefinition = this.complexTypeDefinition;
        clone.schema = this.schema;
        clone.isRuntimeSchema = this.isRuntimeSchema;
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param name     name of the property (element name)
     * @param typeName XSD type of the property
     * @return created property definition
     */
    public PropertyDefinition createPropertyDefinition(QName name, QName typeName) {
        PropertyDefinition propDef = new PropertyDefinition(name, typeName);
        getDefinitions().add(propDef);
        return propDef;
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param name      name of the property (element name)
     * @param typeName  XSD type of the property
     * @param minOccurs minimal number of occurrences
     * @param maxOccurs maximal number of occurrences (-1 means unbounded)
     * @return created property definition
     */
    public PropertyDefinition createPropertyDefinition(QName name, QName typeName,
            int minOccurs, int maxOccurs) {
        PropertyDefinition propDef = new PropertyDefinition(name, typeName);
        propDef.setMinOccurs(minOccurs);
        propDef.setMaxOccurs(maxOccurs);
        getDefinitions().add(propDef);
        return propDef;
    }

    // Creates reference to other schema
    // TODO: maybe check if the name is in different namespace
    // TODO: maybe create entirely new concept of property reference?
    public PropertyDefinition createPropertyDefinition(QName name) {
        PropertyDefinition propDef = new PropertyDefinition(name);
        getDefinitions().add(propDef);
        return propDef;
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param localName name of the property (element name) relative to the schema namespace
     * @param typeName  XSD type of the property
     * @return created property definition
     */
    public PropertyDefinition createPropertyDefinition(String localName, QName typeName) {
        QName name = new QName(getSchemaNamespace(), localName);
        return createPropertyDefinition(name, typeName);
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param localName     name of the property (element name) relative to the schema namespace
     * @param localTypeName XSD type of the property
     * @return created property definition
     */
    public PropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
        QName name = new QName(getSchemaNamespace(), localName);
        QName typeName = new QName(getSchemaNamespace(), localTypeName);
        return createPropertyDefinition(name, typeName);
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param localName     name of the property (element name) relative to the schema namespace
     * @param localTypeName XSD type of the property
     * @param minOccurs     minimal number of occurrences
     * @param maxOccurs     maximal number of occurrences (-1 means unbounded)
     * @return created property definition
     */
    public PropertyDefinition createPropertyDefinition(String localName, String localTypeName,
            int minOccurs, int maxOccurs) {
        QName name = new QName(getSchemaNamespace(), localName);
        QName typeName = new QName(getSchemaNamespace(), localTypeName);
        PropertyDefinition propertyDefinition = createPropertyDefinition(name, typeName);
        propertyDefinition.setMinOccurs(minOccurs);
        propertyDefinition.setMaxOccurs(maxOccurs);
        return propertyDefinition;
    }

    /**
     * Creates new property container from DOM or JAXB representation (single element).
     *
     * @param element DOM representation of property container
     * @param parentPath 
     * @return created property container parsed from the element
     * @throws SchemaException error parsing the element
     */
    public PropertyContainer parseItem(Object element, PropertyPath parentPath) throws SchemaException {
        List<Object> elements = new ArrayList<Object>();
        elements.add(element);
        return parseItem(elements, parentPath);
    }
    
    /**
     *  Assumes top-level element (null parentPath)
     */
    public PropertyContainer parseItem(Object element) throws SchemaException {
    	return parseItem(element,null);
    }

    /**
     * Creates new property container from DOM or JAXB representation (multiple elements).
     *
     * @param elements DOM or JAXB representation of property container
     * @return created property container parsed from the elements
     * @throws SchemaException error parsing the elements
     */
    @Override
    public PropertyContainer parseItem(List<Object> elements, PropertyPath parentPath) throws SchemaException {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        if (elements.size() > 1) {
            throw new IllegalArgumentException("Cannot parse container from more than one element");
        }
        return parseItem(elements.get(0), PropertyContainer.class, parentPath);
    }

    /**
     * Creates new property container from DOM or JAXB representation (multiple elements).
     * <p/>
     * Internal parametric method.
     *
     * @param <T>     subclass of property container to return
     * @param element JAXB or DOM element representing the container
     * @param type    subclass of property container to return
     * @return created new property container (or subclass)
     * @throws SchemaException error parsing the elements
     */
    protected <T extends PropertyContainer> T parseItem(Object element, Class<T> type, PropertyPath parentPath) throws SchemaException {
        if (element instanceof JAXBElement) {
        	return parseItemFromJaxbElement((JAXBElement)element, type, parentPath);
        }
        QName elementQName = JAXBUtil.getElementQName(element);
        T container = (T) this.instantiate(elementQName, element, parentPath);
        List<Object> childElements = JAXBUtil.listChildElements(element);
        container.getItems().addAll(parseItems(childElements, container.getPath()));
        return container;
    }

    public PropertyContainer parseAsContent(QName name, List<Object> contentElements, PropertyPath parentPath) throws SchemaException {
        return parseAsContent(name, contentElements, PropertyContainer.class, parentPath);
    }

    protected <T extends PropertyContainer> T parseAsContent(QName name, List<Object> contentElements,
            Class<T> type, PropertyPath parentPath) throws SchemaException {
        T container = (T) this.instantiate(name, parentPath);
        container.getItems().addAll(parseItems(contentElements, container.getPath()));
        return container;
    }

    /**
     * Parses items from a list of elements.
     * <p/>
     * The elements must describe properties or property container as defined by this
     * PropertyContainerDefinition. Serializes all the elements from the provided list.
     *
     * @param elements list of elements with serialized properties
     * @return set of deserialized items
     * @throws SchemaException error parsing the elements
     */
    public Collection<? extends Item> parseItems(List<Object> elements, PropertyPath parentPath) throws SchemaException {
        return parseItems(elements, parentPath, null);
    }

    /**
     * Parses items from a list of elements.
     * <p/>
     * The elements must describe properties or property container as defined by this
     * PropertyContainerDefinition. Serializes all the elements from the provided list.
     * <p/>
     * Internal parametric method. This does the real work.
     * <p/>
     * min/max constraints are not checked now
     * TODO: maybe we need to check them
     */
    protected Collection<? extends Item> parseItems(List<Object> elements, PropertyPath parentPath,
            Collection<? extends ItemDefinition> selection) throws SchemaException {

        // TODO: more robustness in handling schema violations (min/max constraints, etc.)

        Collection<Item> props = new HashSet<Item>();

        // Iterate over all the XML elements there. Each element is
        // an attribute.
        for (int i = 0; i < elements.size(); i++) {
            Object propElement = elements.get(i);
            QName elementQName = JAXBUtil.getElementQName(propElement);
            // Collect all elements with the same QName
            List<Object> valueElements = new ArrayList<Object>();
            valueElements.add(propElement);
            while (i + 1 < elements.size()
                    && elementQName.equals(JAXBUtil.getElementQName(elements.get(i + 1)))) {
                i++;
                valueElements.add(elements.get(i));
            }

            // If there was a selection filter specified, filter out the
            // properties that are not in the filter.

            // Quite an ugly code. TODO: clean it up
            if (selection != null) {
                boolean selected = false;
                for (ItemDefinition selProdDef : selection) {
                    if (selProdDef.getNameOrDefaultName().equals(elementQName)) {
                        selected = true;
                    }
                }
                if (!selected) {
                    continue;
                }
            }

            // Find item definition from the schema
            ItemDefinition def = findItemDefinition(elementQName);
            
            if (def == null) {
            	// Try to locate xsi:type definition in the element
            	def = Schema.resolveDynamicItemDefinition(this, valueElements);
            }
            
            if (def == null && isRuntimeSchema) {
            	// Kindof hack. Create default definition for this.
            	def = Schema.createDefaultItemDefinition(this, valueElements);
            }
            
            if (def == null) {
                throw new SchemaException("Item " + elementQName + " has no definition", elementQName);
            }
            
            Item item = def.parseItem(valueElements, parentPath);
            props.add(item);
        }
        return props;
    }

    public PropertyContainer parseItemFromJaxbObject(Object jaxbObject, PropertyPath parentPath) throws SchemaException {
        return parseItemFromJaxbObject(jaxbObject, PropertyContainer.class, parentPath);
    }

    protected <T extends PropertyContainer> T parseItemFromJaxbElement(JAXBElement jaxbElement, Class<T> type, PropertyPath parentPath) throws
    		SchemaException {
    	QName elementQName = JAXBUtil.getElementQName(jaxbElement);
    	Object jaxbObject = jaxbElement.getValue();
    	return parseItemFromJaxbObject(jaxbObject, elementQName, type, parentPath);
    }
    protected <T extends PropertyContainer> T parseItemFromJaxbObject(Object jaxbObject, Class<T> type, PropertyPath parentPath) throws
    		SchemaException {
    	return parseItemFromJaxbObject(jaxbObject, null, type, parentPath);
    }
    
    protected <T extends PropertyContainer> T parseItemFromJaxbObject(Object jaxbObject, QName elementName, Class<T> type, PropertyPath parentPath) throws
            SchemaException {

        if (isRuntimeSchema()) {
            return parseItemFromJaxbObjectDynamic(jaxbObject, type, parentPath);
        } else {
            return parseItemFromJaxbObjectStatic(jaxbObject, type, parentPath);
        }
    }

    private <T extends PropertyContainer> T parseItemFromJaxbObjectStatic(Object jaxbObject, Class<T> type, PropertyPath parentPath) throws
            SchemaException {

        Class clazz = jaxbObject.getClass();
        T propertyContainer = (T) this.instantiate(parentPath);

        if (complexTypeDefinition == null) {
            throw new IllegalStateException("Cannot parse object as the complexType definition for " + getName() + " is missing.");
        }

        for (ItemDefinition itemDef : complexTypeDefinition.getDefinitions()) {
            QName itemName = itemDef.getName();
            if (itemName == null) {
                throw new IllegalArgumentException("No definition name in " + getName() + ", definition: " + itemDef);
            }
            String getterName = null;
            if (itemDef.getTypeName().equals(DOMUtil.XSD_BOOLEAN)) {
                getterName = "is" + StringUtils.capitalize(itemName.getLocalPart());
            } else {
                getterName = "get" + StringUtils.capitalize(itemName.getLocalPart());
            }

            Object value = null;
            try {
                Method method = clazz.getMethod(getterName);
                value = method.invoke(jaxbObject);
            } catch (SecurityException e) {
                throw new SchemaException("Access denied while trying to execute getter " + getterName + " in " + getName() + ": " + e.getMessage(), e, itemName);
            } catch (NoSuchMethodException e) {
                throw new SchemaException("Metod not found while trying to execute getter " + getterName + " in " + getName() + ": " + e.getMessage(), e, itemName);
            } catch (IllegalArgumentException e) {
                throw new SchemaException("Illegal argument while trying to execute getter " + getterName + " in " + getName() + ": " + e.getMessage(), e, itemName);
            } catch (IllegalAccessException e) {
                throw new SchemaException("Illegal access while trying to execute getter " + getterName + " in " + getName() + ": " + e.getMessage(), e, itemName);
            } catch (InvocationTargetException e) {
                throw new SchemaException("Bad invocation target while trying to execute getter " + getterName + " in " + getName() + ": " + e.getMessage(), e, itemName);
            }

            if (value != null) {
                if (value instanceof Collection) {
                    if (((Collection) value).isEmpty()) {
                        continue;
                    }
                }
                Item item = itemDef.parseItemFromJaxbObject(value, propertyContainer.getPath());
                propertyContainer.add(item);
            }
        }

        return propertyContainer;
    }

    private <T extends PropertyContainer> T parseItemFromJaxbObjectDynamic(Object jaxbObject, Class<T> type, PropertyPath parentPath) throws
            SchemaException {

        Class clazz = jaxbObject.getClass();
        T propertyContainer = (T) this.instantiate(parentPath);

        Object value = null;
        try {
            Method method = clazz.getMethod(ANY_GETTER_NAME);
            value = method.invoke(jaxbObject);
        } catch (SecurityException e) {
            throw new SchemaException("Access denied while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new SchemaException("Method not found while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new SchemaException("Illegal argument while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new SchemaException("Illegal access while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new SchemaException("Bad invocation target while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        }

        if (value == null) {
            throw new IllegalStateException("Unexpected null result from " + ANY_GETTER_NAME + " in " + getName());
        }

        List<Object> elementList = null;
        try {
            elementList = (List<Object>) value;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Unexpected result from " + ANY_GETTER_NAME + " in " + getName() + ", expected List<Object> but got " + value.getClass());
        }
        
        Collection<? extends Item> items = parseItems(elementList, propertyContainer.getPath());
        propertyContainer.addAll(items);
        
//
//        Property property = null;
//        for (Object element : elementList) {
//        	
//            QName elementName = JAXBUtil.getElementQName(element);
//
//            if (property == null || !property.getName().equals(elementName)) {
//                PropertyDefinition propertyDefinition = findPropertyDefinition(elementName);
//                if (propertyDefinition != null) {
//                    property = propertyDefinition.instantiate(elementName);
//                } else {
//                	propertyDefinition = Schema.resolveDynamicItemDefinition(this, valueElements);
//                	dfs
//                    property = new Property(elementName);
//                }
//                propertyContainer.add(property);
//            }
//
//            try {
//                if (property.getDefinition() != null) {
//                    Object elementValue = XsdTypeConverter.toJavaValue(element, property.getDefinition().getTypeName());
//                    property.getValues().add(new PropertyValue(elementValue));
//                } else {
//                	
//                	// FIXME: The default type should not be here (strictly speaking)
//                	// But it is necessary for use in repository. we don't have the schema for account attributes there.
//                	// This somehow mimicks the behavior of XML diff/patch routines
//                	TypedValue typedValue = XsdTypeConverter.toTypedJavaValueWithDefaultType(element, DOMUtil.XSD_STRING);
//                    //TypedValue typedValue = XsdTypeConverter.toTypedJavaValueWithDefaultType(element, null);
//                	
//                    Object elementValue = typedValue.getValue();
//                    property.getValues().add(new PropertyValue(elementValue));
//                    if (property.getDefinition() == null) {
//                        PropertyDefinition def = new PropertyDefinition(elementName, typedValue.getXsdType());
//                        property.setDefinition(def);
//                    }
//                }
//            } catch (SchemaException e) {
//                // add more context. As exception is immutable, we need to wrap it once again
//                throw new SchemaException(e.getMessage() + ", in " + getName(), e, elementName);
//            }
//
//        }

        return propertyContainer;
    }

    /**
     * used for MidPoint -> JAXB conversion
     */
    protected void fillProperties(Object instance, PropertyContainer source) throws SchemaException {
        if (isJaxbAny(instance.getClass())) {
            fillPropertiesDynamic(instance, source);
        } else {
            fillPropertiesStatic(instance, source);
        }
    }

    /**
     * This is lame, but it works.
     */
    private boolean isJaxbAny(Class clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(ANY_GETTER_NAME)) {
                return true;
            }
        }
        return false;
    }

    private void fillPropertiesStatic(Object jaxbObject, PropertyContainer source) throws SchemaException {

        Class<? extends Object> clazz = jaxbObject.getClass();

        if (complexTypeDefinition == null) {
            throw new IllegalStateException("Cannot parse object as the complexType definition for " + getName() + " is missing.");
        }
        
        for (Item item : source.getItems()) {
            QName itemName = item.getName();
            Definition itemDef = item.getDefinition();
            if (itemDef == null) {
                throw new IllegalArgumentException("No definition for item " + getName());
            }

            Method method = null;
            Object argument = null;

            try {

                if (item instanceof Property) {
                    Property property = (Property) item;
                    if (((PropertyDefinition) itemDef).isMultiValue()) {
                        method = findGetter(clazz, itemName.getLocalPart());
                        Collection collection = (Collection) method.invoke(jaxbObject);

                        Set<PropertyValue<Object>> values = property.getValues();
                        for (PropertyValue<Object> value : values) {
                            collection.add(value.getValue());
                        }
                    } else {
                        PropertyValue<Object> propertyValue = property.getValue();
                        if (propertyValue != null) {
                            method = findSetter(clazz, itemName.getLocalPart());
                            argument = propertyValue.getValue();
                            argument = convertArgument(method,argument);
                            
                            method.invoke(jaxbObject, argument);
                        }
                    }
                } else if (item instanceof PropertyContainer) {
                    method = findSetter(clazz, itemName.getLocalPart());
                    Class<?> subClass = method.getParameterTypes()[0];
                    Object subInstance = instantiateJaxbClass(subClass);
                    ((PropertyContainerDefinition) itemDef).fillProperties(subInstance, (PropertyContainer) item);
                    argument = subInstance;
                    argument = convertArgument(method, argument);
                    method.invoke(jaxbObject, argument);
                }

            } catch (SecurityException e) {
                throw new SchemaException("Security error while trying to execute setter " + clazz.getName() + "." + method + " for " + itemName + " in " + getName() + " with argument "+argument+": " + e.getMessage(), e, itemName);
            } catch (NoSuchMethodException e) {
                throw new SchemaException("Schema error while trying to execute setter " + clazz.getName() + "." + method + " for " + itemName + " in " + getName() + " with argument "+argument+": " + e.getMessage(), e, itemName);
            } catch (IllegalArgumentException e) {
                throw new SchemaException("Illegal argument while trying to execute setter " + clazz.getName() + "." + method + " for " + itemName + " in " + getName() + " with argument "+argument+": " + e.getMessage(), e, itemName);
            } catch (IllegalAccessException e) {
                throw new SchemaException("Illgal access while trying to execute setter " + clazz.getName() + "." + method + " for " + itemName + " in " + getName() + " with argument "+argument+": " + e.getMessage(), e, itemName);
            } catch (InvocationTargetException e) {
                throw new SchemaException("Invocation target error while trying to execute setter " + clazz.getName() + "." + method + " for " + itemName + " in " + getName() + " with argument "+argument+": " + e.getMessage(), e, itemName);
            }
        }
    }

    /**
     * Kind of hack. The JAXB classes want XMLGregorianCalendar and BigInteger. Later we need to find a systematic way how to do this.
	 */
	private Object convertArgument(Method method, Object argument) {
		Class<?> parameterType = method.getParameterTypes()[0];
		if (argument instanceof GregorianCalendar) {
        	argument = XsdTypeConverter.toXMLGregorianCalendar((GregorianCalendar)argument);
        }
		if (BigInteger.class.equals(parameterType)) {
			if (argument instanceof Integer) {
				argument = BigInteger.valueOf((Integer)argument);
			}
		}
		return argument;
	}

	private Method findGetter(Class<? extends Object> clazz, String propName) throws SecurityException,
            NoSuchMethodException {
        String getterName = "get" + StringUtils.capitalize(propName);
        return clazz.getMethod(getterName);
    }

    private Method findSetter(Class<? extends Object> clazz, String propName) {
        String setterName = "set" + StringUtils.capitalize(propName);
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName)) {
                return method;
            }
        }
        return null;
    }

    private void fillPropertiesDynamic(Object jaxbObject, PropertyContainer source) throws SchemaException {

        Class clazz = jaxbObject.getClass();

        Object getterValue = null;
        try {
            Method method = clazz.getMethod(ANY_GETTER_NAME);
            getterValue = method.invoke(jaxbObject);
        } catch (SecurityException e) {
            throw new SchemaException("Access denied while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new SchemaException("Metod not found while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new SchemaException("Illegal argument while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new SchemaException("Illegal access while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new SchemaException("Bad invocation target while trying to execute getter " + ANY_GETTER_NAME + " in " + getName() + ": " + e.getMessage(), e);
        }

        if (getterValue == null) {
            throw new IllegalStateException("Unexpected null result from " + ANY_GETTER_NAME + " in " + getName());
        }

        List<Object> elementList = null;
        try {
            elementList = (List<Object>) getterValue;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Unexpected result from " + ANY_GETTER_NAME + " in " + getName() + ", expected List<Object> but got " + getterValue.getClass());
        }

        Property property = null;

        Document doc = DOMUtil.getDocument();

        for (Property prop : source.getProperties()) {
            QName propName = prop.getName();
            Definition itemDef = findPropertyDefinition(propName);
            boolean recordType = false;
            if (itemDef == null) {
                itemDef = prop.getDefinition();
                recordType = true;
                if (itemDef == null) {
                    throw new SchemaException("No definition for item " + propName + " in " + this);
                }
            }

            for (PropertyValue<Object> value : prop.getValues()) {
                try {
                    Object element = XsdTypeConverter.toXsdElement(value.getValue(), itemDef.getTypeName(), propName, doc, recordType);
                    elementList.add(element);
                } catch (SchemaException e) {
                    // add more context. As exception is immutable, we need to wrap it once again
                    throw new SchemaException(e.getMessage() + ", in " + getName(), e, propName);
                }
            }
        }
    }


    protected <T> T instantiateJaxbClass(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getConstructor();
            T instance = constructor.newInstance();
            return instance;
        } catch (SecurityException e) {
            throw new SystemException("Error instantiating JAXB object of type " + clazz + ": " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new SystemException("Error instantiating JAXB object of type " + clazz + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new SystemException("Error instantiating JAXB object of type " + clazz + ": " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new SystemException("Error instantiating JAXB object of type " + clazz + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new SystemException("Error instantiating JAXB object of type " + clazz + ": " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new SystemException("Error instantiating JAXB object of type " + clazz + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(DebugDumpable.INDENT_STRING);
        }
        sb.append(toString());
        if (isRuntimeSchema()) {
            sb.append(" dynamic");
        }
        sb.append("\n");
        for (Definition def : getDefinitions()) {
            sb.append(def.debugDump(indent + 1));
        }
        return sb.toString();
    }


    /**
     * @return
     */
    public boolean isEmpty() {
        return complexTypeDefinition.isEmpty();
    }

}
