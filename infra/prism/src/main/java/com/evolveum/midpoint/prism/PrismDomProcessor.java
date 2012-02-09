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
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;

/**
 * @author semancik
 *
 */
class PrismDomProcessor {
	
	private static final QName DEFAULT_XSD_TYPE = DOMUtil.XSD_STRING;
	
	private SchemaRegistry schemaRegistry;
	private PrismContext prismContext;
	
	PrismDomProcessor(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public <T extends Objectable> PrismObject<T> parseObject(Element objectElement) throws SchemaException {
		QName elementName = DOMUtil.getQName(objectElement);
		Schema schema = schemaRegistry.findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			throw new SchemaException("No schema for namespace "+elementName.getNamespaceURI());
		}
		PrismObjectDefinition<T> objectDefinition = schema.findObjectDefinitionByElementName(elementName);
		if (objectDefinition == null) {
			throw new SchemaException("No object definition for element "+elementName+" in schema "+schema);
		}
		PrismObject<T> object = parsePrismContainer(objectElement, objectDefinition, null);
		String oid = objectElement.getAttribute("oid");
		object.setOid(oid);
		String version = objectElement.getAttribute("version");
		object.setVersion(version);
		return object;
	}
	
	public PrismContainer parsePropertyContainer(Element domElement) throws SchemaException {
		// locate appropriate definition based on the element name
		QName elementName = DOMUtil.getQName(domElement);
		Schema schema = schemaRegistry.findSchemaByNamespace(elementName.getNamespaceURI());
		PrismContainerDefinition propertyContainerDefinition = schema.findItemDefinition(elementName,
				PrismContainerDefinition.class);
		if (propertyContainerDefinition == null) {
			throw new SchemaException("No definition for element " + elementName);
		}
		return parsePrismContainer(domElement, propertyContainerDefinition, null);
	}

	private <T extends PrismContainer> T parsePrismContainer(Element element, PrismContainerDefinition containerDefinition, PropertyPath parentPath) throws SchemaException {
		QName elementQName = DOMUtil.getQName(element);
        T container = (T) containerDefinition.instantiate(elementQName, parentPath);
        List<Element> childElements = DOMUtil.listChildElements(element);
        container.getItems().addAll(parsePrismContainerItems(childElements, containerDefinition, container.getPath()));
        return container;
	}
	
	private <T extends PrismContainer> T parsePrismContainer(List<Element> valueElements, PrismContainerDefinition containerDefinition, PropertyPath parentPath) throws SchemaException {
		if (valueElements.size() == 1) {
			return parsePrismContainer(valueElements.get(0), containerDefinition, parentPath);
		} else {
			throw new UnsupportedOperationException("We don't support multi-valued prism containers yet");
		}
	}

	private Collection<? extends Item> parsePrismContainerItems(List<Element> childElements, PrismContainerDefinition containerDefinition, PropertyPath parentPath) throws SchemaException {
		return parsePrismContainerItems(childElements, containerDefinition, parentPath, null);
	}
	
    /**
     * Parses items of PRISM CONTAINER from a list of elements.
     * <p/>
     * The elements must describe properties or property container as defined by this
     * PropertyContainerDefinition. Serializes all the elements from the provided list.
     * <p/>
     * Internal parametric method. This does the real work.
     * <p/>
     * min/max constraints are not checked now
     * TODO: maybe we need to check them
     */
    protected Collection<? extends Item> parsePrismContainerItems(List<Element> elements, PrismContainerDefinition containerDefinition, 
    		PropertyPath parentPath, Collection<? extends ItemDefinition> selection) throws SchemaException {

        // TODO: more robustness in handling schema violations (min/max constraints, etc.)

        Collection<Item> props = new HashSet<Item>();

        // Iterate over all the XML elements there. Each element is
        // an attribute.
        for (int i = 0; i < elements.size(); i++) {
        	Element propElement = elements.get(i);
            QName elementQName = DOMUtil.getQName(propElement);
            // Collect all elements with the same QName
            List<Element> valueElements = new ArrayList<Element>();
            valueElements.add(propElement);
            while (i + 1 < elements.size()
                    && elementQName.equals(DOMUtil.getQName(elements.get(i + 1)))) {
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
            ItemDefinition def = containerDefinition.findItemDefinition(elementQName);
            
            if (def == null) {
            	// Try to locate xsi:type definition in the element
            	def = resolveDynamicItemDefinition(containerDefinition, valueElements, prismContext);
            }
            
            if (def == null && containerDefinition.isRuntimeSchema()) {
            	// Kindof hack. Create default definition for this.
            	def = createDefaultItemDefinition(containerDefinition, valueElements, prismContext);
            }
            
            if (def == null) {
                throw new SchemaException("Item " + elementQName + " has no definition", elementQName);
            }
            
            Item item = parseItem(valueElements, def, parentPath);
            props.add(item);
        }
        return props;
    }
    
    /**
	 * Try to locate xsi:type definition in the elements and return appropriate ItemDefinition.
	 */
	private ItemDefinition resolveDynamicItemDefinition(ItemDefinition parentDefinition, List<Element> valueElements, 
			PrismContext prismContext) {
		QName typeName = null;
		QName elementName = null;
		for (Object element: valueElements) {
			if (elementName == null) {
				elementName = JAXBUtil.getElementQName(element);
			}
			// TODO: try JAXB types
			if (element instanceof Element) {
				Element domElement = (Element)element;
				if (DOMUtil.hasXsiType(domElement)) {
					typeName = DOMUtil.resolveXsiType(domElement);
					if (typeName != null) {
						break;
					}
				}
			}
		}
		// FIXME: now the definition assumes property, may also be property container?
		if (typeName == null) {
			return null;
		}
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(elementName, elementName, typeName, prismContext);
		// Set it to multi-value to be on the safe side
		propDef.setMaxOccurs(-1);
		// TODO: set "dynamic" flag
		return propDef;
	}
	
	/**
	 * Create default ItemDefinition. Used as a last attempt to provide some useful definition. Kind of a hack.
	 */
	private ItemDefinition createDefaultItemDefinition(ItemDefinition parentDefinition, List<Element> valueElements,
			PrismContext prismContext) {
		QName elementName = null;
		for (Object element: valueElements) {
			if (elementName == null) {
				elementName = JAXBUtil.getElementQName(element);
				break;
			}
		}
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(elementName, elementName, DEFAULT_XSD_TYPE, prismContext);
		// Set it to multi-value to be on the safe side
		propDef.setMaxOccurs(-1);
		// TODO: set "dynamic" flag
		return propDef;
	}
    
    public PrismProperty parsePrismProperty(List<Element> valueElements, PrismPropertyDefinition propertyDefinition, PropertyPath parentPath) throws SchemaException {
        if (valueElements == null || valueElements.isEmpty()) {
            return null;
        }
        QName propName = DOMUtil.getQName(valueElements.get(0));
        PrismProperty prop = null;
        if (valueElements.size() == 1) {
            prop = propertyDefinition.instantiate(propName, parentPath);
        } else {
            // In-place modification not supported for multi-valued properties
            prop = propertyDefinition.instantiate(propName, null);
        }

        if (!propertyDefinition.isMultiValue() && valueElements.size() > 1) {
            throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
        }

        for (Object element : valueElements) {
            Object value = XsdTypeConverter.toJavaValue(element, propertyDefinition.getTypeName());
            prop.getValues().add(new PropertyValue(value));
        }
        return prop;
    }

    /**
     * This gets definition of an unspecified type. It has to find the right method to call.
     * Value elements have the same element name. They may be elements of a property or a container. 
     */
	private Item parseItem(List<Element> valueElements, ItemDefinition def, PropertyPath parentPath) throws SchemaException {
		if (def instanceof PrismContainerDefinition) {
			return parsePrismContainer(valueElements, (PrismContainerDefinition)def, parentPath);
		} if (def instanceof PrismPropertyDefinition) {
			return parsePrismProperty(valueElements, (PrismPropertyDefinition)def, parentPath);
		} else {
			throw new IllegalArgumentException("Attempt to parse unknown definition type "+def.getClass().getName());
		}
	}

	// Property:
	
//    public Property parseFromValueElement(Element valueElement, PropertyPath parentPath) throws SchemaException {
//        Property prop = this.instantiate(parentPath);
//        if (isSingleValue()) {
//            prop.getValues().add(new PropertyValue(XsdTypeConverter.convertValueElementAsScalar(valueElement, getTypeName())));
//        } else {
//            List list = XsdTypeConverter.convertValueElementAsList(valueElement, getTypeName());
//            for (Object object : list) {
//                prop.getValues().add(new PropertyValue(object));
//            }
//        }
//        return prop;
//    }

	

}
