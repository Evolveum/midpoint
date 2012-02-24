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
package com.evolveum.midpoint.prism.dom;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class PrismDomProcessor {
	
	private static final QName DEFAULT_XSD_TYPE = DOMUtil.XSD_STRING;
	
	private SchemaRegistry schemaRegistry;
	private PrismContext prismContext;
	
	public PrismDomProcessor(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
	
	private PrismJaxbProcessor getJaxbProcessor() {
		return getPrismContext().getPrismJaxbProcessor();
	}

	public <T extends Objectable> PrismObject<T> parseObject(File file, Class<T> type) throws SchemaException {
		PrismObject<T> object = parseObject(file);
		if (!object.canRepresent(type)) {
			throw new SchemaException("Requested object of type "+type+", but parsed type "+object.getCompileTimeClass());
		}
		return object;
	}
	
	public <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException {
		Document parsedDocument = DOMUtil.parseFile(file);
		Element element = DOMUtil.getFirstChildElement(parsedDocument);
		if (element == null) {
			return null;
		}
		return parseObject(element);
	}
	
	public <T extends Objectable> PrismObject<T> parseObject(String objectString, Class<T> type) throws SchemaException {
		PrismObject<T> object = parseObject(objectString);
		if (!object.canRepresent(type)) {
			throw new SchemaException("Requested object of type "+type+", but parsed type "+object.getCompileTimeClass());
		}
		return object;
	}
	
	public <T extends Objectable> PrismObject<T> parseObject(String objectString) throws SchemaException {
		Document parsedDocument = DOMUtil.parseDocument(objectString);
		Element element = DOMUtil.getFirstChildElement(parsedDocument);
		if (element == null) {
			return null;
		}
		return parseObject(element);
	}

	public <T extends Objectable> PrismObject<T> parseObject(Node domNode) throws SchemaException {
		if (domNode instanceof Element) {
			return parseObject((Element)domNode);
		}
		return parseObject(DOMUtil.getFirstChildElement(domNode));
	}
	
	public <T extends Objectable> PrismObject<T> parseObject(Element objectElement) throws SchemaException {
		QName elementName = DOMUtil.getQName(objectElement);
		PrismSchema schema = schemaRegistry.findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			throw new SchemaException("No schema for namespace "+elementName.getNamespaceURI());
		}
		PrismObjectDefinition<T> objectDefinition = schema.findObjectDefinitionByElementName(elementName);
		if (objectDefinition == null) {
			throw new SchemaException("No object definition for element "+elementName+" in schema "+schema);
		}
		PrismObject<T> object = (PrismObject<T>) parsePrismContainer(objectElement, objectDefinition);
		String oid = getOid(objectElement);
		object.setOid(oid);
		String version = objectElement.getAttribute(PrismConstants.ATTRIBUTE_VERSION_LOCAL_NAME);
		object.setVersion(version);
		return object;
	}
	
	public PrismContainer parsePrismContainer(Element domElement) throws SchemaException {
		// locate appropriate definition based on the element name
		QName elementName = DOMUtil.getQName(domElement);
		PrismSchema schema = schemaRegistry.findSchemaByNamespace(elementName.getNamespaceURI());
		PrismContainerDefinition propertyContainerDefinition = schema.findItemDefinition(elementName,
				PrismContainerDefinition.class);
		if (propertyContainerDefinition == null) {
			throw new SchemaException("No definition for element " + elementName);
		}
		return parsePrismContainer(domElement, propertyContainerDefinition);
	}

	private PrismContainer parsePrismContainer(Element domElement, PrismContainerDefinition propertyContainerDefinition) throws SchemaException {
		List<Element> valueElements = new ArrayList<Element>(1);
		valueElements.add(domElement);
		return parsePrismContainer(valueElements, propertyContainerDefinition);
	}

	private PrismContainer parsePrismContainer(List<Element> valueElements, PrismContainerDefinition containerDefinition) throws SchemaException {
		QName elementQName = DOMUtil.getQName(valueElements.get(0));
		PrismContainer container = containerDefinition.instantiate(elementQName);
        for (Element element: valueElements) {
        	String id = getContainerId(element);
        	PrismContainerValue pval = new PrismContainerValue(null, null, container, id);
            List<Element> childElements = DOMUtil.listChildElements(element);
            pval.addAll(parsePrismContainerItems(childElements, containerDefinition));
            container.add(pval);
        }
        return container;
	}
	
	private String getOid(Element element) {
		return element.getAttribute(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME);
	}
	
	private String getContainerId(Element element) {
		String id = element.getAttribute(PrismConstants.ATTRIBUTE_ID_LOCAL_NAME);
		if (StringUtils.isNotBlank(id)) {
			return id;
		}
		id = element.getAttributeNS(element.getNamespaceURI(), PrismConstants.ATTRIBUTE_ID_LOCAL_NAME);
		if (StringUtils.isNotBlank(id)) {
			return id;
		}
		id = element.getAttributeNS(DOMUtil.XML_ID_ATTRIBUTE.getNamespaceURI(), DOMUtil.XML_ID_ATTRIBUTE.getLocalPart());
		if (StringUtils.isNotBlank(id)) {
			return id;
		}
		return null;
	}

	private Collection<? extends Item> parsePrismContainerItems(List<Element> childElements, PrismContainerDefinition containerDefinition) throws SchemaException {
		return parsePrismContainerItems(childElements, containerDefinition, null);
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
    		Collection<? extends ItemDefinition> selection) throws SchemaException {

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
            
            Item item = parseItem(valueElements, def);
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
    
    public PrismProperty parsePrismProperty(List<Element> valueElements, PrismPropertyDefinition propertyDefinition) throws SchemaException {
        if (valueElements == null || valueElements.isEmpty()) {
            return null;
        }
        QName propName = DOMUtil.getQName(valueElements.get(0));
        PrismProperty prop = propertyDefinition.instantiate(propName);

        if (!propertyDefinition.isMultiValue() && valueElements.size() > 1) {
            throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
        }

        for (Element element : valueElements) {
        	Object value = null;
        	PrismJaxbProcessor jaxbProcessor = getJaxbProcessor();
        	QName typeName = propertyDefinition.getTypeName();
        	if (XmlTypeConverter.canConvert(typeName)) {
            	value = XmlTypeConverter.toJavaValue(element, typeName);
        	} else if (jaxbProcessor.canConvert(typeName)) {
        		try {
					value = jaxbProcessor.toJavaValue(element, typeName);
				} catch (JAXBException e) {
					throw new SchemaException("Attempt to unmarshal value of property "+propName+" failed: "+e.getMessage(), e);
				}
        	} else {
        		// fallback to DOM
        		value = element;
        	}
            prop.getValues().add(new PrismPropertyValue(value));
        }
        return prop;
    }
    
	public PrismProperty parsePropertyFromValueElement(Element valueElement, PrismPropertyDefinition propertyDefinition) throws SchemaException {
    	PrismProperty prop = propertyDefinition.instantiate();
        if (propertyDefinition.isSingleValue()) {
            prop.addValue(new PrismPropertyValue(XmlTypeConverter.convertValueElementAsScalar(valueElement, propertyDefinition.getTypeName())));
        } else {
            List list = XmlTypeConverter.convertValueElementAsList(valueElement, propertyDefinition.getTypeName());
            for (Object object : list) {
                prop.getValues().add(new PrismPropertyValue(object));
            }
        }
        return prop;
    }
    
    public PrismReference parsePrismReference(List<Element> valueElements, PrismReferenceDefinition referenceDefinition) throws SchemaException {
        if (valueElements == null || valueElements.isEmpty()) {
            return null;
        }
        QName propName = DOMUtil.getQName(valueElements.get(0));
        PrismReference ref = referenceDefinition.instantiate(propName);

        if (!referenceDefinition.isMultiValue() && valueElements.size() > 1) {
            throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
        }

        for (Element element : valueElements) {
            ref.getValues().add(parseReferenceValue(element));
        }
        return ref;
    }

	private PrismReferenceValue parseReferenceValue(Element element) {
		String oid = getOid(element);
		QName type = DOMUtil.getQNameAttribute(element, PrismConstants.ATTRIBUTE_REF_TYPE_LOCAL_NAME);
		PrismReferenceValue refVal = new PrismReferenceValue(oid);
		refVal.setTargetType(type);
		return refVal;
	}

	/**
     * This gets definition of an unspecified type. It has to find the right method to call.
     * Value elements have the same element name. They may be elements of a property or a container. 
     */
	private Item parseItem(List<Element> valueElements, ItemDefinition def) throws SchemaException {
		if (def instanceof PrismContainerDefinition) {
			return parsePrismContainer(valueElements, (PrismContainerDefinition)def);
		} else if (def instanceof PrismPropertyDefinition) {
			return parsePrismProperty(valueElements, (PrismPropertyDefinition)def);
		} if (def instanceof PrismReferenceDefinition) {
			return parsePrismReference(valueElements, (PrismReferenceDefinition)def);
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

	public <T extends Objectable> String serializeObjectToString(PrismObject<T> object) {
		throw new UnsupportedOperationException();
	}

}
