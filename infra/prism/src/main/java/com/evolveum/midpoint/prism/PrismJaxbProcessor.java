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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.ObjectType;
import com.evolveum.midpoint.schema.processor.ObjectTypes;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;

/**
 * @author semancik
 *
 */
public class PrismJaxbProcessor {
	
	private SchemaRegistry schemaRegistry;
	private JAXBContext context;
	private Marshaller marshaller;

	PrismJaxbProcessor(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	public void initialize() {
		StringBuilder sb = new StringBuilder();
		Iterator<Package> iterator = schemaRegistry.getJaxbPackages().iterator();
		while (iterator.hasNext()) {
			Package jaxbPackage = iterator.next();
			sb.append(jaxbPackage.getName());
			if (iterator.hasNext()) {
				sb.append(":");
			}
		}

		try {
			context = JAXBContext.newInstance(sb.toString());
		} catch (JAXBException ex) {
			throw new SystemException("Couldn't create JAXBContext for: " + sb.toString(), ex);
		}
	}

	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}

	public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
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
		for (Package jaxbPackage: schemaRegistry.getJaxbPackages()) {
			if (jaxbPackage.equals(clazz.getPackage())) {
				return true;
			}
		}
		return false;
	}
	
	private Marshaller createMarshaller(Map<String, Object> jaxbProperties) throws JAXBException {
		Marshaller marshaller = context.createMarshaller();
		// set default properties
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", schemaRegistry.getNamespacePrefixMapper());
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
		QName elementQName = determineElementQName(objectable);
		JAXBElement<?> jaxbElement = new JAXBElement<Object>(elementQName, (Class<Object>) objectable.getClass(), objectable);
		return marshalElementToString(jaxbElement);
	}
	
	public String marshalElementToString(JAXBElement<?> jaxbElement) throws JAXBException {
		StringWriter writer = new StringWriter();
		Marshaller marshaller = getMarshaller();
		marshaller.marshal(jaxbElement, writer);
		return writer.getBuffer().toString();
	}
	
	/**
	 * Serializes DOM or JAXB element to string
	 */
	public String marshalElementToString(Object element) throws JAXBException {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return DOMUtil.serializeDOMToString((Element) element);
		} else if (element instanceof JAXBElement) {
			return marshalElementToString((JAXBElement<?>)element);
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
		JAXBElement<?> jaxbElement = new JAXBElement<Object>(elementQName, (Class<Object>) objectable.getClass(), objectable);
		marshalElementToDom(jaxbElement, parentNode);
	}

	public void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
		getMarshaller().marshal(jaxbElement, parentNode);		
	}
	
	// Do we need this ??
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
	
	public <T> Element marshalElementToDom(JAXBElement<T> jaxbElement, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		Element element = doc.createElementNS(jaxbElement.getName().getNamespaceURI(), jaxbElement.getName().getLocalPart());
		marshalElementToDom(jaxbElement, element);

		return (Element) element.getFirstChild();
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


	public <T> JAXBElement<T> unmarshalElement(String xmlString) throws JAXBException {
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
			return unmarshalElement(reader);
		} finally {
			if (reader != null) {
				IOUtils.closeQuietly(reader);
			}
		}
	}
	
	public <T> JAXBElement<T> unmarshalElement(InputStream input) throws JAXBException {
		Object object = getUnmarshaller().unmarshal(input);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		return jaxbElement;
	}
	
	public <T> JAXBElement<T> unmarshalElement(Reader reader) throws JAXBException {
		Object object = getUnmarshaller().unmarshal(reader);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		return jaxbElement;
	}
	
	public <T> JAXBElement<T> unmarshalElement(Node node) throws JAXBException {
		Object object = createUnmarshaller().unmarshal(node);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		return jaxbElement;
	}
	
	public <T> JAXBElement<T> unmarshalElement(File file) throws JAXBException {
		if (file == null) {
			throw new IllegalArgumentException("File argument must not be null.");
		}

		InputStream is = null;
		try {
			is = new FileInputStream(file);
			return (JAXBElement<T>) getUnmarshaller().unmarshal(is);
		} catch (IOException ex) {
			throw new JAXBException("Couldn't parse file: " + file.getAbsolutePath(), ex);
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

	
	public <T> T fromElement(Object element, Class<T> type) {
		
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
				JAXBElement<T> unmarshalledElement = unmarshalElement((Element)element);
				return unmarshalledElement.getValue();
			} catch (JAXBException e) {
				throw new IllegalArgumentException("Unmarshall failed: " + e.getMessage(),e);
			}
		}
		
		throw new IllegalArgumentException("Unknown element type "+element.getClass().getName());
	}
	
	public <T extends Objectable> ObjectDefinition<T> findObjectDefinition(ObjectTypes objectType, Class<T> type) {
		return findContainerDefinitionByType(objectType.getTypeQName(),ObjectDefinition.class);
	}
	
	public <T extends Objectable> ObjectDefinition<T> findObjectDefinition(Class<T> type) {
		return findContainerDefinitionByType(ObjectTypes.getObjectType(type).getTypeQName(),ObjectDefinition.class);
	}

	public PropertyContainerDefinition findContainerDefinition(Class<? extends Objectable> type, PropertyPath path) {
		ObjectTypes objectType = ObjectTypes.getObjectType(type);
		ObjectDefinition objectDefinition = findObjectDefinitionByType(objectType.getTypeQName());
		if (objectDefinition == null) {
			throw new IllegalArgumentException("The definition of object type "+type.getSimpleName()+" not found in the schema");
		}
		return objectDefinition.findItemDefinition(path, PropertyContainerDefinition.class);
	}

	
	public <T extends Objectable> MidPointObject<T> parseObjectType(T objectType) throws SchemaException {
		ObjectDefinition<T> objectDefinition = (ObjectDefinition<T>) findObjectDefinition(objectType.getClass());
		if (objectDefinition == null) {
			throw new IllegalArgumentException("No definition for object type "+objectType);
		}
		return objectDefinition.parseObjectType(objectType);
	}
	
	public <T extends ObjectType> MidPointObject<T> parseObject(String stringXml, Class<T> type) throws SchemaException {
		ObjectDefinition<T> objectDefinition = findObjectDefinition(type);
		JAXBElement<T> jaxbElement;
		try {
			jaxbElement = JAXBUtil.unmarshal(type, stringXml);
		} catch (JAXBException e) {
			throw new SchemaException("Error parsing the XML: "+e.getMessage(),e);
		}
        T objectType = jaxbElement.getValue();
        MidPointObject<T> object = objectDefinition.parseObjectType(objectType);
        return object;
	}

	public <T extends ObjectType> MidPointObject<T> parseObject(File xmlFile, Class<T> type) throws SchemaException {
		ObjectDefinition<T> objectDefinition = findObjectDefinition(type);
		JAXBElement<T> jaxbElement;
		try {
			jaxbElement = JAXBUtil.unmarshal(xmlFile, type);
		} catch (JAXBException e) {
			throw new SchemaException("Error parsing the XML: "+e.getMessage(),e);
		}
        T objectType = jaxbElement.getValue();
        MidPointObject<T> object = objectDefinition.parseObjectType(objectType);
        return object;
	}


	
	private QName determineElementQName(Objectable objectable) {
		// TODO Auto-generated method stub
		dsfsfd
		return null;
	}


}
