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
package com.evolveum.midpoint.common.jaxb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public final class JAXBUtil {

	private static final Trace TRACE = TraceManager.getTrace(JAXBUtil.class);
	private static final JAXBContext context;

	static {
		JAXBContext ctx = null;
		try {
			ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		} catch (JAXBException ex) {
			TRACE.error("Couldn't create JAXBContext for: " + ObjectFactory.class.getPackage().getName(), ex);
			throw new IllegalStateException("Couldn't create JAXBContext for: "
					+ ObjectFactory.class.getPackage().getName(), ex);
		}

		context = ctx;
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

	private static Unmarshaller createUnmarshaller() throws JAXBException {
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
			TRACE.debug("Failed to marshal object {}", xmlObject, ex);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> String silentMarshalWrap(T jaxbObject, QName elementQName) {
		try {
			JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
					jaxbObject);
			return marshal(jaxbElement);
		} catch (JAXBException ex) {
			TRACE.debug("Failed to marshal object {}", jaxbObject, ex);
			return null;
		}
	}

	public static void marshal(Object xmlObject, Element element) throws JAXBException {
		createMarshaller(null).marshal(xmlObject, element);
	}

	public static void marshal(Map<String, Object> properties, Object xmlObject, OutputStream stream)
			throws JAXBException {
		createMarshaller(properties).marshal(xmlObject, stream);
	}

	public static void silentMarshal(Object xmlObject, Element element) {
		try {
			marshal(xmlObject, element);
		} catch (JAXBException ex) {
			TRACE.debug("Failed to marshal object {}", xmlObject, ex);
		}
	}

	public static Object unmarshal(String xmlString) throws JAXBException {
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
			return unmarshal(stream);
		} catch (IOException ex) {
			throw new JAXBException(ex);
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
		}
	}

	public static Object unmarshal(InputStream input) throws JAXBException {
		return createUnmarshaller().unmarshal(input);
	}

	public static Object silentUnmarshal(String xmlString) {
		try {
			return unmarshal(xmlString);
		} catch (JAXBException ex) {
			TRACE.debug("Failed to unmarshal xml string {}", xmlString, ex);
			return null;
		}
	}

	public static Object silentUnmarshal(File file) {
		try {
			return unmarshal(file);
		} catch (JAXBException ex) {
			TRACE.debug("Failed to unmarshal file {}", file, ex);
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

	public static <T> Element objectTypeToDom(T jaxbObject, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}
		QName qname = SchemaConstants.getElementByObjectType(jaxbObject.getClass());
		if (qname == null) {
			throw new IllegalArgumentException("Cannot find element for class " + jaxbObject.getClass());
		}
		return jaxbToDom(jaxbObject, qname, doc);
	}
}
