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
package com.evolveum.midpoint.prism.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class PrismJaxbProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PrismJaxbProcessor.class);
	
	private PrismContext prismContext;
	private JAXBContext context;

	public PrismJaxbProcessor(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	private SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}
	
	private PrismDomProcessor getPrismDomProcessor() {
		return prismContext.getPrismDomProcessor();
	}
	
	public void initialize() {
		StringBuilder sb = new StringBuilder();
		Iterator<Package> iterator = getSchemaRegistry().getCompileTimePackages().iterator();
		while (iterator.hasNext()) {
			Package jaxbPackage = iterator.next();
			sb.append(jaxbPackage.getName());
			if (iterator.hasNext()) {
				sb.append(":");
			}
		}
		String jaxbPaths = sb.toString();
		if (jaxbPaths.isEmpty()) {
			LOGGER.warn("No JAXB paths, skipping creation of JAXB context");
		} else {
			try {
				context = JAXBContext.newInstance(jaxbPaths);
			} catch (JAXBException ex) {
				throw new SystemException("Couldn't create JAXBContext for: " + jaxbPaths, ex);
			}
		}
	}

	public JAXBContext getContext() {
		return context;
	}

	public void setContext(JAXBContext context) {
		this.context = context;
	}
	
	public boolean isJaxbClass(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("No class, no fun");
		}
		if (clazz.getPackage() == null) {
			// No package: this is most likely a primitive type and definitely
			// not a JAXB class
			return false;
		}
		for (Package jaxbPackage: getSchemaRegistry().getCompileTimePackages()) {
			if (jaxbPackage.equals(clazz.getPackage())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canConvert(Class<?> clazz) {
		return isJaxbClass(clazz);
	}
	
	public boolean canConvert(QName xsdType) {
		SchemaDescription schemaDesc = getSchemaRegistry().findSchemaDescriptionByNamespace(xsdType.getNamespaceURI());
		if (schemaDesc == null) {
			return false;
		}
		if (schemaDesc.getCompileTimeClassesPackage() == null) {
			return false;
		}
		// We may be answering "yes" to a broader set of types that we can really convert.
		// But that does not matter that much. If the type is in the correct namespace
		// then either we can convert it or nobody can.
		return true;
		// Following code is not really correct. There are XSD types that we can convert and there is
		// no complexTypeDefinition for then in our parsed schema. E.g. all the property JAXB types.
//		ComplexTypeDefinition complexTypeDefinition = schema.findComplexTypeDefinition(xsdType);
//		return complexTypeDefinition != null;
	}

	public Class<?> getCompileTimeClass(QName xsdType) {
		SchemaDescription desc = getSchemaRegistry().findSchemaDescriptionByNamespace(xsdType.getNamespaceURI());
		if (desc == null) {
			return null;
		}
		Map<QName, Class<?>> map = desc.getXsdTypeTocompileTimeClassMap();
		if (map == null) {
			return null;
		}
		return map.get(xsdType);
	}
	
	public <T> T toJavaValue(Element element, Class<T> typeClass) throws JAXBException {
		QName type = JAXBUtil.getTypeQName(typeClass);
		return (T) toJavaValue(element, type);
	}
	
	/**
	 * Used to convert property values from DOM 
	 */
	public Object toJavaValue(Element element, QName xsdType) throws JAXBException {
		Class<?> declaredType = getCompileTimeClass(xsdType);
		if (declaredType == null) {
			// This may happen if the schema is runtime and there is no associated compile-time class
			throw new SystemException("Cannot determine Java type for "+xsdType);
		}
		JAXBElement<?> jaxbElement = createUnmarshaller().unmarshal(element, declaredType);
		Object object = jaxbElement.getValue();
		return object;
	}

	private Marshaller createMarshaller(Map<String, Object> jaxbProperties) throws JAXBException {
		Marshaller marshaller = context.createMarshaller();
		// set default properties
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		DynamicNamespacePrefixMapper namespacePrefixMapper = getSchemaRegistry().getNamespacePrefixMapper().clone();
		namespacePrefixMapper.setAlwaysExplicit(true);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper);
		// set custom properties
		if (jaxbProperties != null) {
			for (Entry<String, Object> property : jaxbProperties.entrySet()) {
				marshaller.setProperty(property.getKey(), property.getValue());
			}
		}

		return marshaller;
	}
	
	/**
	 * Allow for pooling and other fancy stuff. Now it dumb, creates in every call.
	 */
	private Marshaller getMarshaller() throws JAXBException {
		return createMarshaller(null);
	}

	private Unmarshaller createUnmarshaller() throws JAXBException {
		return context.createUnmarshaller();
	}
	
	/**
	 * Allow for pooling and other fancy stuff. Now it dumb, creates in every call.
	 */
	private Unmarshaller getUnmarshaller() throws JAXBException {
		return createUnmarshaller();
	}

    public String marshalToString(Objectable objectable) throws JAXBException {
        return marshalToString(objectable, new HashMap<String, Object>());
    }
	
	public String marshalToString(Objectable objectable, Map<String, Object> properties) throws JAXBException {
		QName elementQName = determineElementQName(objectable);
		JAXBElement<Object> jaxbElement = new JAXBElement<Object>(elementQName, (Class) objectable.getClass(), objectable);
		return marshalElementToString(jaxbElement, properties);
	}

    public String marshalElementToString(JAXBElement<?> jaxbElement) throws JAXBException {
        return marshalElementToString(jaxbElement, new HashMap<String, Object>());
    }
	
	public String marshalElementToString(JAXBElement<?> jaxbElement, Map<String, Object> properties) throws JAXBException {
		StringWriter writer = new StringWriter();
		Marshaller marshaller = getMarshaller();
        for (Entry<String, Object> entry : properties.entrySet()) {
            marshaller.setProperty(entry.getKey(), entry.getValue());
        }
		marshaller.marshal(jaxbElement, writer);
		return writer.getBuffer().toString();
	}

    public String marshalElementToString(Object element) throws JAXBException {
        return marshalElementToString(element, new HashMap<String, Object>());
    }
	
	/**
	 * Serializes DOM or JAXB element to string
	 */
	public String marshalElementToString(Object element, Map<String, Object> properties) throws JAXBException {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return DOMUtil.serializeDOMToString((Element) element);
		} else if (element instanceof JAXBElement) {
			return marshalElementToString((JAXBElement<?>)element, properties);
		} else {
			throw new IllegalArgumentException("Unsupported element type "+element.getClass().getName());
		}
	}

	/**
	 * Serializes DOM or JAXB element to string, using specified elementName if needed.
	 */
	public String marshalElementToString(Object element, QName elementName) throws JAXBException {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return DOMUtil.serializeDOMToString((Element) element);
		} else if (element instanceof JAXBElement) {
			return marshalElementToString((JAXBElement<?>)element);
		} else {
			JAXBElement<Object> jaxbElement = new JAXBElement<Object>(elementName, Object.class, element);
			return marshalElementToString(jaxbElement);
		}
	}
	
	public void marshalToDom(Objectable objectable, Node parentNode) throws JAXBException {
		QName elementQName = determineElementQName(objectable);
		JAXBElement<Object> jaxbElement = new JAXBElement<Object>(elementQName, (Class) objectable.getClass(), objectable);
		marshalElementToDom(jaxbElement, parentNode);
	}

	public void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
		getMarshaller().marshal(jaxbElement, parentNode);		
	}
		
	public <T> Element marshalElementToDom(JAXBElement<T> jaxbElement, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		Element element = doc.createElementNS(jaxbElement.getName().getNamespaceURI(), jaxbElement.getName().getLocalPart());
		marshalElementToDom(jaxbElement, element);

		return (Element) element.getFirstChild();
	}
	
	public <T> Element marshalObjectToDom(T jaxbObject, QName elementQName, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
				jaxbObject);
		Element element = doc.createElementNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
		marshalElementToDom(jaxbElement, element);

		return (Element) element.getFirstChild();
	}
	
	public <T> void marshalObjectToDom(T jaxbObject, QName elementQName, Element parentElement) throws JAXBException {

		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
				jaxbObject);
		marshalElementToDom(jaxbElement, parentElement);
	}
	
	public Element toDomElement(Object element) throws JAXBException {
		return toDomElement(element, DOMUtil.getDocument());
	}

	public Element toDomElement(Object jaxbElement, Document doc) throws JAXBException {
		return toDomElement(jaxbElement,doc,false,false,false);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Element toDomElement(Object jaxbElement, Document doc, boolean adopt, boolean clone, boolean deep) throws JAXBException {
		if (jaxbElement == null) {
			return null;
		}
		if (jaxbElement instanceof Element) {
			Element domElement = (Element) jaxbElement;
			if (clone) {
				domElement = (Element) domElement.cloneNode(deep);
			}
			if (domElement.getOwnerDocument().equals(doc)) {
				return domElement;
			}
			if (adopt) {
				doc.adoptNode(domElement);
			}
			return domElement;
		} else if (jaxbElement instanceof JAXBElement) {
			return marshalElementToDom((JAXBElement) jaxbElement, doc);
		} else {
			throw new IllegalArgumentException("Not an element: " + jaxbElement + " ("
					+ jaxbElement.getClass().getName() + ")");
		}
	}


	public <T> JAXBElement<T> unmarshalElement(String xmlString, Class<T> type) throws JAXBException, SchemaException {
		if (xmlString == null) {
			return null;
		}

		xmlString = xmlString.trim();
		if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
			throw new IllegalArgumentException("Provided string is unlikely to be an XML");
		}

		StringReader reader = null;
		try {
			reader = new StringReader(xmlString); 
			JAXBElement<T> element = unmarshalElement(reader, type);
			adopt(element);
			return element;
		} finally {
			if (reader != null) {
				IOUtils.closeQuietly(reader);
			}
		}
	}
	
	public <T> JAXBElement<T> unmarshalElement(InputStream input, Class<T> type) throws JAXBException, SchemaException {
		Object object = getUnmarshaller().unmarshal(input);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		adopt(jaxbElement);
		return jaxbElement;
	}
	
	public <T> JAXBElement<T> unmarshalElement(Reader reader, Class<T> type) throws JAXBException, SchemaException {
		Object object = getUnmarshaller().unmarshal(reader);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		adopt(jaxbElement);
		return jaxbElement;
	}
	
	public <T> T unmarshalToObject(Node node, Class<T> type) throws JAXBException, SchemaException {
		JAXBElement<T> element = unmarshalElement(node, type);
		if (element == null) {
			return null;
		}
		adopt(element);
		return element.getValue();
	}
	
	public <T> JAXBElement<T> unmarshalElement(Node node, Class<T> type) throws JAXBException, SchemaException {
		Object object = createUnmarshaller().unmarshal(node);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		adopt(jaxbElement);
		return jaxbElement;
	}
	
	public <T> T unmarshalObject(File file, Class<T> type) throws JAXBException, SchemaException, FileNotFoundException {
		JAXBElement<T> element = unmarshalElement(file, type);
		if (element == null) {
			return null;
		}
		T value = element.getValue();
		// adopt not needed, already adopted in unmarshalElement call above
		return value;
	}
	
	public <T> T unmarshalObject(String stringXml, Class<T> type) throws JAXBException, SchemaException {
		JAXBElement<T> element = unmarshalElement(stringXml, type);
		if (element == null) {
			return null;
		}
		T value = element.getValue();
		adopt(value, type);
		return value;
	}
	
	public <T> T unmarshalObject(Object domOrJaxbElement, Class<T> type) throws SchemaException {
		JAXBElement<T> element;
		if (domOrJaxbElement instanceof JAXBElement<?>) {
			element = (JAXBElement<T>) domOrJaxbElement;
		} else if (domOrJaxbElement instanceof Node) {
			try {
				element = unmarshalElement((Node)domOrJaxbElement, type);
			} catch (JAXBException e) {
				throw new SchemaException(e.getMessage(),e);
			}
		} else {
			throw new IllegalArgumentException("Unknown element type "+domOrJaxbElement);
		}
		if (element == null) {
			return null;
		}
		T value = element.getValue();
		adopt(value, type);
		return value;
	}
	
	public <T> JAXBElement<T> unmarshalElement(File file, Class<T> type) throws SchemaException, FileNotFoundException, JAXBException {
		if (file == null) {
			throw new IllegalArgumentException("File argument must not be null.");
		}

		InputStream is = null;
		try {
			is = new FileInputStream(file);
			JAXBElement<T> element = (JAXBElement<T>) getUnmarshaller().unmarshal(is);
			adopt(element);
			return element;
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
	}
    
    public <T> T unmarshalRootObject(File file, Class<T> type) throws JAXBException, FileNotFoundException, SchemaException {
        Validate.notNull(file, "File must not be null.");
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            T object = (T) getUnmarshaller().unmarshal(is);
            adopt(object);
            return object;
        } finally {
            if (is != null) {
                IOUtils.closeQuietly(is);
            }
        }
    }
	
	public boolean compareAny(List<Object> a, List<Object> b) {
		if (a == b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!compareElement(a.get(i), b.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private boolean compareElement(Object a, Object b) {
		if (a == b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		Document doc = null;
		Element ae = null;
		Element be = null;

		if (a instanceof Element) {
			ae = (Element) a;
		} else if (a instanceof JAXBElement) {
			if (doc == null) {
				doc = DOMUtil.getDocument();
			}
			try {
				ae = marshalElementToDom((JAXBElement) a, doc);
			} catch (JAXBException e) {
				throw new IllegalStateException("Failed to marshall element " + a, e);
			}
		} else {
			throw new IllegalArgumentException("Got unexpected type " + a.getClass().getName() + ": " + a);
		}

		if (b instanceof Element) {
			be = (Element) b;
		} else if (a instanceof JAXBElement) {
			if (doc == null) {
				doc = DOMUtil.getDocument();
			}
			try {
				be = marshalElementToDom((JAXBElement) a, doc);
			} catch (JAXBException e) {
				throw new IllegalStateException("Failed to marshall element " + b, e);
			}
		} else {
			throw new IllegalArgumentException("Got unexpected type " + b.getClass().getName() + ": " + b);
		}

		return DOMUtil.compareElement(ae, be, true);
	}

	
	public <T> T fromElement(Object element, Class<T> type) throws SchemaException {
		
		if (element == null) {
			return null;
		}
		
		if (type.isAssignableFrom(element.getClass())) {
			return (T) element;
		}
		
		if (element instanceof JAXBElement) {
			if (((JAXBElement) element).getValue() == null) {
				return null;
			}
			if (type.isAssignableFrom(((JAXBElement) element).getValue().getClass())) {
				return (T) ((JAXBElement) element).getValue();
			}
		}
		
		if (element instanceof Element) {
			try {
				JAXBElement<T> unmarshalledElement = unmarshalElement((Element)element, type);
				return unmarshalledElement.getValue();
			} catch (JAXBException e) {
				throw new IllegalArgumentException("Unmarshall failed: " + e.getMessage(),e);
			}
		}
		
		throw new IllegalArgumentException("Unknown element type "+element.getClass().getName());
	}
	
	public Object toAny(PrismValue value, Document document) throws SchemaException {
		if (value == null) {
			return value;
		}
		QName elementName = value.getParent().getName();
		Object xmlValue;
		if (value instanceof PrismPropertyValue) {
			PrismPropertyValue<Object> pval = (PrismPropertyValue)value;
			Object realValue = pval.getValue();
        	xmlValue = realValue;
        	if (XmlTypeConverter.canConvert(realValue.getClass())) {
        		// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
        		xmlValue = XmlTypeConverter.toXsdElement(realValue, elementName, document, true);
        	}
		} else if (value instanceof PrismReferenceValue) {
			PrismReferenceValue rval = (PrismReferenceValue)value;
			xmlValue =  getPrismDomProcessor().serializeValueToDom(rval, elementName, document);
		} else if (value instanceof PrismContainerValue<?>) {
			PrismContainerValue<?> pval = (PrismContainerValue<?>)value;
			if (pval.getParent().getCompileTimeClass() == null) {
				// This has to be runtime schema without a compile-time representation.
				// We need to convert it to DOM
				xmlValue =  getPrismDomProcessor().serializeValueToDom(pval, elementName, document);
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
	
	private QName determineElementQName(Objectable objectable) {
		PrismObject<?> prismObject = objectable.asPrismObject();
		if (prismObject.getName() != null) {
			return prismObject.getName();
		}
		PrismObjectDefinition<?> definition = prismObject.getDefinition();
		if (definition != null) {
			if (definition.getNameOrDefaultName() != null) {
				return definition.getNameOrDefaultName();
			}
		}
		throw new IllegalStateException("Cannot determine element name of "+objectable);
	}
	
	private boolean isObjectable(Class type) {
		return Objectable.class.isAssignableFrom(type);
	}
	
	private <T> void adopt(T object, Class<T> type) throws SchemaException {
		if (object instanceof Objectable) {
			getPrismContext().adopt(((Objectable)object));
		}
	}
	
    private void adopt(Object object) throws SchemaException {
        if (object instanceof JAXBElement) {
            adopt(((JAXBElement)object).getValue());
        } else if (object instanceof Objectable) {
            getPrismContext().adopt(((Objectable)(object)));
        }
    }

}
