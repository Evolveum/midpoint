/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;


import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * A set of ugly hacks that are needed for prism and "real" JAXB to coexist. We hate it be we need it.
 * This is a mix of DOM and JAXB code that allows the use of "any" methods on JAXB-generated objects.
 * Prism normally does not use of of that. But JAXB code (such as JAX-WS) can invoke it and therefore
 * it has to return correct DOM/JAXB elements as expected.
 *  
 * @author Radovan Semancik
 */
public class JaxbDomHack {
	
	private static final Trace LOGGER = TraceManager.getTrace(JaxbDomHack.class);
	
	private PrismContext prismContext;
	private DomParser domParser;
//	private JAXBContext jaxbContext;

	public JaxbDomHack(DomParser domParser, PrismContext prismContext) {
		super();
		this.domParser = domParser;
		this.prismContext = prismContext;
//		initializeJaxbContext();
	}

//	private void initializeJaxbContext() {
//		StringBuilder sb = new StringBuilder();
//		Iterator<Package> iterator = prismContext.getSchemaRegistry().getCompileTimePackages().iterator();
//		while (iterator.hasNext()) {
//			Package jaxbPackage = iterator.next();
//			sb.append(jaxbPackage.getName());
//			if (iterator.hasNext()) {
//				sb.append(":");
//			}
//		}
//		String jaxbPaths = sb.toString();
//		if (jaxbPaths.isEmpty()) {
//			LOGGER.debug("No JAXB paths, skipping creation of JAXB context");
//		} else {
//			try {
//				jaxbContext = JAXBContext.newInstance(jaxbPaths);
//			} catch (JAXBException ex) {
//				throw new SystemException("Couldn't create JAXBContext for: " + jaxbPaths, ex);
//			}
//		}
//	}
	
	private <T extends Containerable> ItemDefinition locateItemDefinition(
			PrismContainerDefinition<T> containerDefinition, QName elementQName, Object valueElements)
			throws SchemaException {
		ItemDefinition def = containerDefinition.findItemDefinition(elementQName);
		if (def != null) {
			return def;
		}

		if (valueElements instanceof Element) {
			// Try to locate xsi:type definition in the element
			def = resolveDynamicItemDefinition(containerDefinition, elementQName, (Element) valueElements,
					prismContext);
		} 
		
		if (valueElements instanceof List){
			List elements = (List) valueElements;
			if (elements.size() == 1){
				Object element = elements.get(0);
				if (element instanceof JAXBElement){
					Object val = ((JAXBElement) element).getValue();
					if (val.getClass().isPrimitive()){
						QName typeName = XsdTypeMapper.toXsdType(val.getClass());
						PrismPropertyDefinition propDef = new PrismPropertyDefinition(elementQName, typeName, prismContext);
//						propDef.setMaxOccurs(maxOccurs);
						propDef.setDynamic(true);
						return propDef;
					}
				}
			}
		}
		if (def != null) {
			return def;
		}

		if (containerDefinition.isRuntimeSchema()) {
			// Try to locate global definition in any of the schemas
			def = resolveGlobalItemDefinition(containerDefinition, elementQName);
		}
		return def;
	}
	private ItemDefinition resolveDynamicItemDefinition(ItemDefinition parentDefinition, QName elementName,
			Element element, PrismContext prismContext) throws SchemaException {
		QName typeName = null;
		// QName elementName = null;
		// Set it to multi-value to be on the safe side
		int maxOccurs = -1;
//		for (Object element : valueElements) {
			// if (elementName == null) {
			// elementName = JAXBUtil.getElementQName(element);
			// }
			// TODO: try JAXB types
			if (element instanceof Element) {
				Element domElement = (Element) element;
				if (DOMUtil.hasXsiType(domElement)) {
					typeName = DOMUtil.resolveXsiType(domElement);
					if (typeName != null) {
						String maxOccursString = domElement.getAttributeNS(
								PrismConstants.A_MAX_OCCURS.getNamespaceURI(),
								PrismConstants.A_MAX_OCCURS.getLocalPart());
						if (!StringUtils.isBlank(maxOccursString)) {
							// TODO
//							maxOccurs = parseMultiplicity(maxOccursString, elementName);
						}
//						break;
					}
				}
			}
//		}
		// FIXME: now the definition assumes property, may also be property
		// container?
		if (typeName == null) {
			return null;
		}
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(elementName, typeName, prismContext);
		propDef.setMaxOccurs(maxOccurs);
		propDef.setDynamic(true);
		return propDef;
	}


	private <T extends Containerable> ItemDefinition resolveGlobalItemDefinition(
			PrismContainerDefinition<T> containerDefinition, QName elementQName)
			throws SchemaException {
		// Object firstElement = valueElements.get(0);
		// QName elementQName = JAXBUtil.getElementQName(firstElement);
		return prismContext.getSchemaRegistry().resolveGlobalItemDefinition(elementQName);
	}
	
	/**
	 * This is used in a form of "fromAny" to parse elements from a JAXB getAny method to prism. 
	 */
	public <V extends PrismValue,C extends Containerable> Item<V> parseRawElement(Object element, PrismContainerDefinition<C> definition) throws SchemaException {
		Validate.notNull(definition, "Attempt to parse raw element in a container without definition");
		
		QName elementName = JAXBUtil.getElementQName(element);
		ItemDefinition itemDefinition = definition.findItemDefinition(elementName);
		
		if (itemDefinition == null) {
			itemDefinition = locateItemDefinition(definition, elementName, element);
			if (itemDefinition == null) {
	            throw new SchemaException("No definition for item "+elementName);
			}
		}
		
		PrismContext prismContext = definition.getPrismContext();
		Item<V> subItem;
		if (element instanceof Element) {
			// DOM Element
			DomParser domParser = prismContext.getParserDom();
			XNode xnode = domParser.parseElementContent((Element)element);
			subItem = prismContext.getXnodeProcessor().parseItem(xnode, elementName, itemDefinition);
		} else if (element instanceof JAXBElement<?>) {
			// JAXB Element
			JAXBElement<?> jaxbElement = (JAXBElement<?>)element;
			Object jaxbBean = jaxbElement.getValue();
			if (itemDefinition == null) {
				throw new SchemaException("No definition for item "+elementName+" in container "+definition+" (parsed from raw element)", elementName);
			}
			if (itemDefinition instanceof PrismPropertyDefinition<?>) {
				// property
				PrismProperty<?> property = ((PrismPropertyDefinition<?>)itemDefinition).instantiate();
				property.setRealValue(jaxbBean);
				subItem = (Item<V>) property;
			} else if (itemDefinition instanceof PrismContainerDefinition<?>) {
				if (jaxbBean instanceof Containerable) {
					PrismContainer<?> container = ((PrismContainerDefinition<?>)itemDefinition).instantiate();
					PrismContainerValue subValue = ((Containerable)jaxbBean).asPrismContainerValue();
					container.add(subValue);
					subItem = (Item<V>) container;
				} else {
					throw new IllegalArgumentException("Unsupported JAXB bean "+jaxbBean.getClass());
				}
			} else if (itemDefinition instanceof PrismReferenceDefinition) {
				// TODO
				if (jaxbBean instanceof Referencable){
					PrismReference reference = ((PrismReferenceDefinition)itemDefinition).instantiate();
					PrismReferenceValue refValue = ((Referencable) jaxbBean).asReferenceValue();
					reference.merge(refValue);
					subItem = (Item<V>) reference;
				} else{
					throw new IllegalArgumentException("Unsupported JAXB bean" + jaxbBean);
				}
				
			} else {
				throw new IllegalArgumentException("Unsupported definition type "+itemDefinition.getClass());
			}
		} else {
			throw new IllegalArgumentException("Unsupported element type "+element.getClass());
		}	
		return subItem;
	}
	
//	public <V extends PrismValue, C extends Containerable> Collection<Item<V>> fromAny(List<Object> anyObjects, PrismContainerDefinition<C> definition) throws SchemaException {
//		Collection<Item<V>> items = new ArrayList<>();
//		for (Object anyObject: anyObjects) {
//			Item<V> newItem = parseRawElement(anyObject, definition);
//			boolean merged = false;
//			for (Item<V> existingItem: items) {
//				if (newItem.getElementName().equals(existingItem.getElementName())) {
//					existingItem.merge(newItem);
//					merged = true;
//					break;
//				}
//			}
//			if (!merged) {
//				items.add(newItem);
//			}
//		}
//		return items;
//	}

	/**
	 * Serializes prism value to JAXB "any" format as returned by JAXB getAny() methods. 
	 */
	public Object toAny(PrismValue value) throws SchemaException {
		Document document = DOMUtil.getDocument();
		if (value == null) {
			return value;
		}
		QName elementName = value.getParent().getElementName();
		Object xmlValue;
		if (value instanceof PrismPropertyValue) {
			PrismPropertyValue<Object> pval = (PrismPropertyValue)value;
			if (pval.isRaw() && (pval.getParent() == null || pval.getParent().getDefinition() == null)) {
				Object rawElement = pval.getRawElement();
				if (rawElement instanceof Element) {
					return ((Element)rawElement).cloneNode(true);
				} else if (rawElement instanceof MapXNode) {
					return domParser.serializeXMapToElement((MapXNode)rawElement, elementName);
				} else if (rawElement instanceof PrimitiveXNode<?>) {
					PrimitiveXNode<?> xprim = (PrimitiveXNode<?>)rawElement;
					String stringValue = xprim.getStringValue();
					Element element = DOMUtil.createElement(document, elementName);
					element.setTextContent(stringValue);
                    DOMUtil.setNamespaceDeclarations(element, xprim.getRelevantNamespaceDeclarations());
					return element;
				} else {
					throw new IllegalArgumentException("Cannot convert raw element "+rawElement+" to xsd:any");
				}
			}
			Object realValue = pval.getValue();
        	xmlValue = realValue;
        	if (XmlTypeConverter.canConvert(realValue.getClass())) {
        		// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
        		xmlValue = XmlTypeConverter.toXsdElement(realValue, elementName, document, true);
        	}
		} else if (value instanceof PrismReferenceValue) {
			PrismReferenceValue rval = (PrismReferenceValue)value;
			xmlValue = prismContext.serializeValueToDom(rval, elementName, document);
		} else if (value instanceof PrismContainerValue<?>) {
			PrismContainerValue<?> pval = (PrismContainerValue<?>)value;
			if (pval.getParent().getCompileTimeClass() == null) {
				// This has to be runtime schema without a compile-time representation.
				// We need to convert it to DOM
				xmlValue = prismContext.serializeValueToDom(pval, elementName, document);
			} else {
				xmlValue = pval.asContainerable();
			}
		} else {
			throw new IllegalArgumentException("Unknown type "+value);
		}
		if (!(xmlValue instanceof Element) && !(xmlValue instanceof JAXBElement)) {
    		xmlValue = new JAXBElement(elementName, xmlValue.getClass(), xmlValue);
    	}
        return xmlValue;
	}

//	public <O extends Objectable> PrismObject<O> parseObjectFromJaxb(Object objectElement) throws SchemaException {
//		if (objectElement instanceof Element) {
//			// DOM
//			XNode objectXNode = domParser.parseElementContent((Element)objectElement);
//			return prismContext.getXnodeProcessor().parseObject(objectXNode);
//		} else if (objectElement instanceof JAXBElement<?>) {
//			O jaxbValue = ((JAXBElement<O>)objectElement).getValue();
//			prismContext.adopt(jaxbValue);
//			return jaxbValue.asPrismObject();
//		} else {
//			throw new IllegalArgumentException("Unknown element type "+objectElement.getClass());
//		}
//	}
		
//	public <O extends Objectable> Element serializeObjectToJaxb(PrismObject<O> object) throws SchemaException {
//		RootXNode xroot = prismContext.getXnodeProcessor().serializeObject(object);
//		return domParser.serializeXRootToElement(xroot);
//	}
	
//	public <T> Element marshalJaxbObjectToDom(T jaxbObject, QName elementQName) throws JAXBException {
//        return marshalJaxbObjectToDom(jaxbObject, elementQName, (Document) null);
//    }
	
//	public <T> Element marshalJaxbObjectToDom(T jaxbObject, QName elementQName, Document doc) throws JAXBException {
//		if (doc == null) {
//			doc = DOMUtil.getDocument();
//		}
//
//		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
//				jaxbObject);
//		Element element = doc.createElementNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
//		marshalElementToDom(jaxbElement, element);
//
//		return (Element) element.getFirstChild();
//	}
	
//	private void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
//		createMarshaller(null).marshal(jaxbElement, parentNode);
//	}
	
//	private Marshaller createMarshaller(Map<String, Object> jaxbProperties) throws JAXBException {
//		Marshaller marshaller = jaxbContext.createMarshaller();
//		// set default properties
//		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
//		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//		DynamicNamespacePrefixMapper namespacePrefixMapper = prismContext.getSchemaRegistry().getNamespacePrefixMapper().clone();
//		namespacePrefixMapper.setAlwaysExplicit(true);
//		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper);
//		// set custom properties
//		if (jaxbProperties != null) {
//			for (Entry<String, Object> property : jaxbProperties.entrySet()) {
//				marshaller.setProperty(property.getKey(), property.getValue());
//			}
//		}
//
//		return marshaller;
//	}

//    public <T> T toJavaValue(Element element, Class<T> typeClass) throws JAXBException {
//        QName type = JAXBUtil.getTypeQName(typeClass);
//        return (T) toJavaValue(element, type);
//    }

    /**
     * Used to convert property values from DOM
     */
//    private Object toJavaValue(Element element, QName xsdType) throws JAXBException {
//        Class<?> declaredType = prismContext.getSchemaRegistry().getCompileTimeClass(xsdType);
//        if (declaredType == null) {
//            // This may happen if the schema is runtime and there is no associated compile-time class
//            throw new SystemException("Cannot determine Java type for "+xsdType);
//        }
//        JAXBElement<?> jaxbElement = createUnmarshaller().unmarshal(element, declaredType);
//        Object object = jaxbElement.getValue();
//        return object;
//    }

//    private Unmarshaller createUnmarshaller() throws JAXBException {
//        return jaxbContext.createUnmarshaller();
//    }

//    public JAXBElement unmarshalJaxbElement(File input) throws JAXBException, IOException {
//        return (JAXBElement) createUnmarshaller().unmarshal(input);
//    }

//    public <T> T unmarshalObject(InputStream input) throws JAXBException, SchemaException {
//        Object object = createUnmarshaller().unmarshal(input);
//        JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
//        adopt(jaxbElement);
//
//        if (jaxbElement == null) {
//            return null;
//        } else {
//            return jaxbElement.getValue();
//        }
//    }

//    private void adopt(Object object) throws SchemaException {
//        if (object instanceof JAXBElement) {
//            adopt(((JAXBElement)object).getValue());
//        } else if (object instanceof Objectable) {
//            prismContext.adopt(((Objectable) (object)));
//        }
//    }

    public String silentMarshalObject(Object object, Trace logger) {
        String xml = null;
        try {
            QName fakeQName=new QName(PrismConstants.NS_PREFIX + "debug", "debugPrintObject");
            if (object instanceof Objectable) {
                xml = prismContext.serializeObjectToString(((Objectable) object).asPrismObject(), PrismContext.LANG_XML);
            } else if (object instanceof Containerable) {
                xml = prismContext.serializeContainerValueToString(((Containerable) object).asPrismContainerValue(),
                        fakeQName, PrismContext.LANG_XML);
            } else {
                xml = prismContext.serializeAnyData(object, fakeQName, PrismContext.LANG_XML);
            }
        } catch (Exception ex) {
            Trace log = logger != null ? logger : LOGGER;
            LoggingUtils.logException(log, "Couldn't marshal element to string {}", ex, object);
        }
        return xml;
    }

//    public String marshalElementToString(JAXBElement<?> jaxbElement) throws JAXBException {
//        return marshalElementToString(jaxbElement, new HashMap<String, Object>());
//    }

//    public String marshalElementToString(JAXBElement<?> jaxbElement, Map<String, Object> properties) throws JAXBException {
//        StringWriter writer = new StringWriter();
//        Marshaller marshaller = createMarshaller(null);
//        for (Entry<String, Object> entry : properties.entrySet()) {
//            marshaller.setProperty(entry.getKey(), entry.getValue());
//        }
//        marshaller.marshal(jaxbElement, writer);
//        return writer.getBuffer().toString();
//    }

//    public boolean isJaxbClass(Class<?> clazz) {
//        if (clazz == null) {
//            throw new IllegalArgumentException("No class, no fun");
//        }
//        if (clazz.getPackage() == null) {
//            // No package: this is most likely a primitive type and definitely
//            // not a JAXB class
//            return false;
//        }
//        for (Package jaxbPackage: prismContext.getSchemaRegistry().getCompileTimePackages()) {
//            if (jaxbPackage.equals(clazz.getPackage())) {
//                return true;
//            }
//        }
//        return false;
//    }

//    public boolean canConvert(Class<?> clazz) {
//        return isJaxbClass(clazz);
//    }

}
