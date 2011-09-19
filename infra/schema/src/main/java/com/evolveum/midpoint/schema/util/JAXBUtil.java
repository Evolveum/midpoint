/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.namespace.PrefixMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author lazyman
 * 
 */
public final class JAXBUtil {

	private static final Trace LOGGER = TraceManager.getTrace(JAXBUtil.class);
	private static final JAXBContext context;
	private static final JAXBIntrospector introspector;

	static {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < SchemaConstants.JAXB_PACKAGES.length; i++) {
			sb.append(SchemaConstants.JAXB_PACKAGES[i]);
			if (i != SchemaConstants.JAXB_PACKAGES.length - 1) {
				sb.append(":");
			}
		}

		try {
			context = JAXBContext.newInstance(sb.toString());
			introspector = context.createJAXBIntrospector();
		} catch (JAXBException ex) {
			LOGGER.error("Couldn't create JAXBContext for: " + ObjectFactory.class.getPackage().getName(), ex);
			throw new IllegalStateException("Couldn't create JAXBContext for: "
					+ ObjectFactory.class.getPackage().getName(), ex);
		}
	}

	public static JAXBContext getContext() {
		return context;
	}

	public static JAXBIntrospector getIntrospector() {
		return introspector;
	}

	public static boolean isJaxbClass(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("No class, no fun");
		}
		if (clazz.getPackage() == null) {
			// No package: this is most likely a primitive type and definitely
			// not a JAXB class
			return false;
		}
		for (int i = 0; i < SchemaConstants.JAXB_PACKAGES.length; i++) {
			if (SchemaConstants.JAXB_PACKAGES[i] == null) {
				throw new IllegalStateException("Entry #" + i + " in SchemaConstants.JAXB_PACKAGES is null");
			}
			if (SchemaConstants.JAXB_PACKAGES[i].equals(clazz.getPackage().getName())) {
				return true;
			}
		}
		return false;
	}

	public static String getSchemaNamespace(Package pkg) {
		XmlSchema xmlSchemaAnn = pkg.getAnnotation(XmlSchema.class);
		if (xmlSchemaAnn == null) {
			return null;
		}
		return xmlSchemaAnn.namespace();
	}

	public static <T> String getTypeLocalName(Class<T> type) {
		XmlType xmlTypeAnn = type.getAnnotation(XmlType.class);
		if (xmlTypeAnn == null) {
			return null;
		}
		return xmlTypeAnn.name();
	}

	public static <T> QName getTypeQName(Class<T> type) {
		String namespace = getSchemaNamespace(type.getPackage());
		String localPart = getTypeLocalName(type);
		if (localPart == null) {
			return null;
		}
		return new QName(namespace, localPart);
	}

	private static Marshaller createMarshaller(Map<String, Object> jaxbProperties) throws JAXBException {
		Marshaller marshaller = context.createMarshaller();
		// set default properties
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new PrefixMapper());
		// set custom properties
		if (jaxbProperties != null) {
			for (Entry<String, Object> property : jaxbProperties.entrySet()) {
				marshaller.setProperty(property.getKey(), property.getValue());
			}
		}

		return marshaller;
	}

	public static Unmarshaller createUnmarshaller() throws JAXBException {
		return context.createUnmarshaller();
	}

	@SuppressWarnings("unchecked")
	public static ObjectType clone(ObjectType object) throws JAXBException {
		if (object == null) {
			return null;
		}
		ObjectFactory of = new ObjectFactory();
		JAXBElement<ObjectType> obj = of.createObject(object);
		obj = (JAXBElement<ObjectType>) unmarshal(marshal(obj));

		return obj.getValue();
	}

	public static String marshal(Object object) throws JAXBException {
		return marshal(null, object);
	}

	public static String marshal(Map<String, Object> jaxbProperties, Object object) throws JAXBException {
		if (object == null) {
			return "";
		}

		StringWriter writer = new StringWriter();
		Marshaller marshaller = createMarshaller(jaxbProperties);
		marshaller.marshal(object, writer);

		return writer.getBuffer().toString();
	}

	public static String marshalWrap(Object object) throws JAXBException {
		JAXBElement<Object> element = new JAXBElement<Object>(new QName(SchemaConstants.NS_C, "object"),
				Object.class, object);
		return marshal(element);
	}

	@SuppressWarnings("unchecked")
	public static <T> String marshalWrap(T jaxbObject, QName elementQName) throws JAXBException {
		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
				jaxbObject);
		return marshal(jaxbElement);
	}

	@SuppressWarnings("unchecked")
	public static <T> String marshalWrap(Map<String, Object> jaxbProperties, T jaxbObject, QName elementQName)
			throws JAXBException {
		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
				jaxbObject);
		return marshal(jaxbProperties, jaxbElement);
	}

	public static String silentMarshal(Object xmlObject) {
		try {
			return marshal(xmlObject);
		} catch (JAXBException ex) {
			LOGGER.debug("Failed to marshal object {}", xmlObject, ex);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> String silentMarshalWrap(T jaxbObject, QName elementQName) {
		if (jaxbObject == null) {
			return null;
		}

		try {
			JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
					jaxbObject);
			return marshal(jaxbElement);
		} catch (JAXBException ex) {
			LOGGER.trace("Failed to marshal object {}", jaxbObject, ex);
			return null;
		}
	}

	public static String silentMarshalWrap(Object object) {
		return silentMarshalWrap(object, new QName(SchemaConstants.NS_C, "object"));
	}

	public static void marshal(Object xmlObject, Node parentNode) throws JAXBException {
		createMarshaller(null).marshal(xmlObject, parentNode);
	}

	public static void marshal(Map<String, Object> properties, Object xmlObject, OutputStream stream)
			throws JAXBException {
		createMarshaller(properties).marshal(xmlObject, stream);
	}

	public static void silentMarshal(Object xmlObject, Element element) {
		try {
			marshal(xmlObject, element);
		} catch (JAXBException ex) {
			LOGGER.debug("Failed to marshal object {}", xmlObject, ex);
		}
	}

	public static Object unmarshal(String xmlString) throws JAXBException {
		return unmarshal(Object.class, xmlString);
	}

	public static <T> JAXBElement<T> unmarshal(Class<T> type, String xmlString) throws JAXBException {
		if (xmlString == null) {
			return null;
			// throw new
			// IllegalArgumentException("Can't parse null xml string.");
		}

		xmlString = xmlString.trim();
		if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
			return null;
			// throw new
			// IllegalArgumentException("Not an xml string (doesn't start with < and finish with >.");
		}

		InputStream stream = null;
		try {
			stream = IOUtils.toInputStream(xmlString, "utf-8");
			return unmarshal(type, stream);
		} catch (IOException ex) {
			throw new JAXBException(ex);
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
		}
	}

	public static Object unmarshal(InputStream input) throws JAXBException {
		return unmarshal(Object.class, input);
	}

	@SuppressWarnings("unchecked")
	public static <T> JAXBElement<T> unmarshal(Class<T> type, InputStream input) throws JAXBException {
		Object object = createUnmarshaller().unmarshal(input);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		return jaxbElement;
	}

	@SuppressWarnings("unchecked")
	public static <T> JAXBElement<T> unmarshal(Node node) throws JAXBException {
		Object object = createUnmarshaller().unmarshal(node);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		return jaxbElement;
	}

	@SuppressWarnings("unchecked")
	public static <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType) throws JAXBException {
		Object object = createUnmarshaller().unmarshal(node, declaredType);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		return jaxbElement;
	}

	public static Object silentUnmarshal(String xmlString) {
		try {
			return unmarshal(xmlString);
		} catch (JAXBException ex) {
			LOGGER.debug("Failed to unmarshal xml string {}", xmlString, ex);
			return null;
		}
	}

	public static Object silentUnmarshal(File file) {
		try {
			return unmarshal(file);
		} catch (JAXBException ex) {
			LOGGER.debug("Failed to unmarshal file {}", file, ex);
			return null;
		}
	}

	public static Object unmarshal(File file) throws JAXBException {
		if (file == null) {
			throw new IllegalArgumentException("File argument can't be null.");
		}

		Reader reader = null;
		try {
			reader = new InputStreamReader(new FileInputStream(file), "utf-8");
			return createUnmarshaller().unmarshal(reader);
		} catch (IOException ex) {
			throw new JAXBException("Couldn't parse file: " + file.getAbsolutePath(), ex);
		} finally {
			if (reader != null) {
				IOUtils.closeQuietly(reader);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Element jaxbToDom(T jaxbObject, QName elementQName, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
				jaxbObject);
		Element element = doc.createElementNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
		marshal(jaxbElement, element);

		return (Element) element.getFirstChild();
	}

	public static <T> Element jaxbToDom(JAXBElement<T> jaxbElement, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		Element element = doc.createElementNS(jaxbElement.getName().getNamespaceURI(), jaxbElement.getName()
				.getLocalPart());
		marshal(jaxbElement, element);

		return (Element) element.getFirstChild();
	}

	public static <T extends ObjectType> Element objectTypeToDom(T jaxbObject, Document doc)
			throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}
		QName qname = ObjectTypes.getObjectType(jaxbObject.getClass()).getQName();
		if (qname == null) {
			throw new IllegalArgumentException("Cannot find element for class " + jaxbObject.getClass());
		}
		return jaxbToDom(jaxbObject, qname, doc);
	}

	/**
	 * Serializes DOM or JAXB element to string
	 * 
	 * @param element
	 * @return
	 * @throws JAXBException
	 */
	public static String serializeElementToString(Object element) throws JAXBException {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return DOMUtil.serializeDOMToString((Element) element);
		} else {
			return marshal(element);
		}
	}

	@SuppressWarnings("rawtypes")
	public static QName getElementQName(Object element) {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return DOMUtil.getQName((Element) element);
		} else if (element instanceof JAXBElement) {
			return ((JAXBElement) element).getName();
		} else {
			throw new IllegalArgumentException("Not an element: " + element);
		}
	}

	@SuppressWarnings("rawtypes")
	public static String getElementLocalName(Object element) {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return ((Element) element).getLocalName();
		} else if (element instanceof JAXBElement) {
			return ((JAXBElement) element).getName().getLocalPart();
		} else {
			throw new IllegalArgumentException("Not an element: " + element);
		}
	}

	public static Element toDomElement(Object element) throws JAXBException {
		return toDomElement(element, DOMUtil.getDocument());
	}

	public static Element toDomElement(Object jaxbElement, Document doc) throws JAXBException {
		return toDomElement(jaxbElement,doc,false,false,false);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Element toDomElement(Object jaxbElement, Document doc, boolean adopt, boolean clone, boolean deep) throws JAXBException {
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
			return jaxbToDom((JAXBElement) jaxbElement, doc);
		} else {
			throw new IllegalArgumentException("Not an element: " + jaxbElement + " ("
					+ jaxbElement.getClass().getName() + ")");
		}
	}

	/**
	 * Returns short description of element content for diagnostics use (logs,
	 * dumps).
	 * 
	 * Works with DOM and JAXB elements.
	 * 
	 * @param element
	 *            DOM or JAXB element
	 * @return short description of element content
	 */
	public static String getTextContentDump(Object element) {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return ((Element) element).getTextContent();
		} else {
			return element.toString();
		}
	}

	/**
	 * @param firstElement
	 * @return
	 */
	public static Document getDocument(Object element) {
		if (element instanceof Element) {
			return ((Element) element).getOwnerDocument();
		} else {
			return DOMUtil.getDocument();
		}
	}

	/**
	 * Looks for an element with specified name. Considers both DOM and JAXB
	 * elements. Assumes single element instance in the list.
	 * 
	 * @param elements
	 * @param elementName
	 */
	public static Object findElement(List<Object> elements, QName elementName) {
		if (elements == null) {
			return null;
		}
		for (Object element : elements) {
			if (elementName.equals(getElementQName(element))) {
				return element;
			}
		}
		return null;
	}

	/**
	 * @param configurationPropertiesElement
	 * @return
	 */
	public static List<Object> listChildElements(Object parentElement) {
		if (parentElement == null) {
			return null;
		}
		List<Object> childElements = new ArrayList<Object>();
		if (parentElement instanceof Element) {
			Element parentEl = (Element) parentElement;
			NodeList childNodes = parentEl.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node item = childNodes.item(i);
				if (item.getNodeType() == Node.ELEMENT_NODE) {
					childElements.add(item);
				}
			}
		} else if (parentElement instanceof JAXBElement) {
			// TODO: implement this
			// Look for @XsdAnyElement annotation and return the list
			throw new UnsupportedOperationException("Not implemented yet");
		} else {
			throw new IllegalArgumentException("Not an element: " + parentElement + " ("
					+ parentElement.getClass().getName() + ")");
		}
		return childElements;
	}

	public static boolean compareAny(List<Object> a, List<Object> b) {
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
	private static boolean compareElement(Object a, Object b) {
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
				ae = jaxbToDom((JAXBElement) a, doc);
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
				be = jaxbToDom((JAXBElement) a, doc);
			} catch (JAXBException e) {
				throw new IllegalStateException("Failed to marshall element " + b, e);
			}
		} else {
			throw new IllegalArgumentException("Got unexpected type " + b.getClass().getName() + ": " + b);
		}

		return DOMUtil.compareElement(ae, be);
	}

}
