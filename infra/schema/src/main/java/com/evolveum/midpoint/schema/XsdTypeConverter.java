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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Simple implementation that converts XSD primitive types to Java (and vice
 * versa).
 * 
 * It convert type names (xsd types to java classes) and also the values.
 * 
 * The implementation is very simple now. In fact just a bunch of ifs. We don't
 * need much more now. If more complex thing will be needed, we will extend the
 * implementation later.
 * 
 * @author Radovan Semancik
 */
public class XsdTypeConverter {

	private static Map<Class, QName> javaToXsdTypeMap;
	private static Map<QName, Class> xsdToJavaTypeMap;
	private static DatatypeFactory datatypeFactory = null;

	private static final Trace LOGGER = TraceManager.getTrace(XsdTypeConverter.class);

	private static void initTypeMap() throws IOException, ClassNotFoundException {

		javaToXsdTypeMap = new HashMap<Class, QName>();
		xsdToJavaTypeMap = new HashMap<QName, Class>();
		addMapping(String.class, DOMUtil.XSD_STRING, true);
		addMapping(char.class, DOMUtil.XSD_STRING, false);
		addMapping(File.class, DOMUtil.XSD_STRING, false);
		addMapping(int.class, DOMUtil.XSD_INTEGER, true);
		addMapping(Integer.class, DOMUtil.XSD_INTEGER, false);
		addMapping(long.class, DOMUtil.XSD_INTEGER, false);
		addMapping(Long.class, DOMUtil.XSD_INTEGER, false);
		addMapping(boolean.class, DOMUtil.XSD_BOOLEAN, true);
		addMapping(byte[].class, DOMUtil.XSD_BASE64BINARY, true);
		addMapping(GregorianCalendar.class, DOMUtil.XSD_DATETIME, true);
		addMapping(QName.class, DOMUtil.XSD_QNAME, true);

		for (int i = 0; i < SchemaConstants.JAXB_PACKAGES.length; i++) {
			String packageName = SchemaConstants.JAXB_PACKAGES[i];
			Set<Class> classes = ClassPathUtil.listClasses(packageName);
			if (classes.isEmpty()) {
				LOGGER.warn("No classes found in the JAXB package " + packageName);
			}
			for (Class jaxbClass : classes) {
				QName typeQName = JAXBUtil.getTypeQName(jaxbClass);
				if (typeQName != null) {
					addMapping(jaxbClass, typeQName, true);
				}
			}
		}
	}

	private static void addMapping(Class javaClass, QName xsdType, boolean both) {
		LOGGER.trace("Adding XSD type mapping {} {} {} ", new Object[] { javaClass, both ? "<->" : " ->",
				xsdType });
		javaToXsdTypeMap.put(javaClass, xsdType);
		if (both) {
			xsdToJavaTypeMap.put(xsdType, javaClass);
		}
	}

	public static QName toXsdType(Class javaClass) {
		QName xsdType = javaToXsdTypeMap.get(javaClass);
		if (xsdType == null) {
			throw new IllegalArgumentException("No XSD mapping for Java type " + javaClass.getCanonicalName());
		}
		return xsdType;
	}

	public static Class toJavaType(QName xsdType) {
		Class javaType = xsdToJavaTypeMap.get(xsdType);
		if (javaType == null) {
			throw new IllegalArgumentException("No type mapping for XSD type " + xsdType);
		}
		return javaType;
	}

	public static Object toJavaValue(Element xmlElement, Class type) throws JAXBException {
		String stringContent = xmlElement.getTextContent();
		if (type.equals(String.class)) {
			return stringContent;
		} else if (type.equals(char.class)) {
			return stringContent.charAt(0);
		} else if (type.equals(File.class)) {
			return new File(stringContent);
		} else if (type.equals(Integer.class)) {
			return Integer.valueOf(stringContent);
		} else if (type.equals(int.class)) {
			return Integer.parseInt(stringContent);
		} else if (type.equals(Long.class)) {
			return Long.valueOf(stringContent);
		} else if (type.equals(long.class)) {
			return Long.parseLong(stringContent);
		} else if (type.equals(byte[].class)) {
			byte[] decodedData = Base64.decodeBase64(xmlElement.getTextContent());
			return decodedData;
		} else if (type.equals(boolean.class) || Boolean.class.isAssignableFrom(type)) {
			return Boolean.parseBoolean(stringContent);
		} else if (type.equals(GregorianCalendar.class)) {
			return getDatatypeFactory().newXMLGregorianCalendar(stringContent).toGregorianCalendar();
		} else if (type.equals(QName.class)) {
			return DOMUtil.getQNameValue(xmlElement);
		} else if (isJaxbClass(type)) {
			return JAXBUtil.unmarshal(xmlElement).getValue();
		} else {
			throw new IllegalArgumentException("Unknown type for conversion: " + type);
		}
	}

	public static Object toJavaValue(Element xmlElement, QName type) throws JAXBException {
		return toJavaValue(xmlElement, toJavaType(type));
	}

	/**
	 * Expects type information in xsi:type
	 * 
	 * @param xmlElement
	 * @return
	 * @throws JAXBException
	 */
	public static Object toJavaValue(Element xmlElement) throws JAXBException {
		return toTypedJavaValueWithDefaultType(xmlElement, null).getValue();
	}

	/**
	 * Try to locate element type from xsi:type, fall back to specified default
	 * type.
	 * 
	 * @param xmlElement
	 * @param defaultType
	 * @return converted java value
	 * @throws JAXBException
	 * @throws IllegalStateException
	 *             if no xsi:type or default type specified
	 */
	public static TypedValue toTypedJavaValueWithDefaultType(Element xmlElement, QName defaultType)
			throws JAXBException {
		QName xsiType = DOMUtil.resolveXsiType(xmlElement, null);
		if (xsiType == null) {
			xsiType = defaultType;
			if (xsiType == null) {
				throw new IllegalStateException("Cannot conver element " + xmlElement
						+ " to java, no type information available");
			}
		}

		return new TypedValue(toJavaValue(xmlElement, xsiType), xsiType);
	}

	public static void toXsdElement(Object val, QName typeName, Element element, boolean recordType)
			throws JAXBException {
		// Just ignore the typeName for now. The java type will determine the
		// conversion
		toXsdElement(val, element, false);
		// But record the correct type is asked to
		if (recordType) {
			if (typeName == null) {
				// if no type was specified, just record the one that was used
				// for automatic conversion
				typeName = toXsdType(val.getClass());
			}
			DOMUtil.setXsiType(element, typeName);
		}
	}

	public static void toXsdElement(Object val, Element element) throws JAXBException {
		toXsdElement(val, element, false);
	}

	public static void toXsdElement(Object val, Element element, boolean recordType) throws JAXBException {
		if (val == null) {
			// if no value is specified, do not create element
			return;
		}
		Class type = val.getClass();
		if (type.equals(String.class)) {
			element.setTextContent((String) val);
		} else if (type.equals(char.class) || type.equals(Character.class)) {
			element.setTextContent(((Character) val).toString());
		} else if (type.equals(File.class)) {
			element.setTextContent(((File) val).getPath());
		} else if (type.equals(int.class) || type.equals(Integer.class)) {
			element.setTextContent(((Integer) val).toString());
		} else if (type.equals(long.class) || type.equals(Long.class)) {
			element.setTextContent(((Long) val).toString());
		} else if (type.equals(byte[].class)) {
			byte[] binaryData = (byte[]) val;
			element.setTextContent(Base64.encodeBase64String(binaryData));
		} else if (type.equals(GregorianCalendar.class)) {
			XMLGregorianCalendar xmlCal = toXMLGregorianCalendar((GregorianCalendar) val);
			element.setTextContent(xmlCal.toXMLFormat());
		} else if (type.equals(QName.class)) {
			QName qname = (QName) val;
			DOMUtil.setQNameValue(element, qname);
		} else if (isJaxbClass(type)) {
			JAXBUtil.marshal(val, element);
		} else {
			throw new IllegalArgumentException("Unknown type for conversion: " + type);
		}
		if (recordType) {
			QName xsdType = toXsdType(val.getClass());
			DOMUtil.setXsiType(element, xsdType);
		}
	}

	public static boolean canConvert(Class clazz) {
		return javaToXsdTypeMap.get(clazz) != null;
	}

	private static boolean isJaxbClass(Class clazz) {
		for (int i = 0; i < SchemaConstants.JAXB_PACKAGES.length; i++) {
			if (SchemaConstants.JAXB_PACKAGES[i].equals(clazz.getPackage().getName())) {
				return true;
			}
		}
		return false;
	}

	public static XMLGregorianCalendar toXMLGregorianCalendar(long timeInMillis) {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.setTimeInMillis(timeInMillis);
		return toXMLGregorianCalendar(gregorianCalendar);
	}

	public static XMLGregorianCalendar toXMLGregorianCalendar(GregorianCalendar cal) {
		return getDatatypeFactory().newXMLGregorianCalendar(cal);
	}

	public static long toMillis(XMLGregorianCalendar xmlCal) {
		return xmlCal.toGregorianCalendar().getTimeInMillis();
	}

	private static DatatypeFactory getDatatypeFactory() {
		if (datatypeFactory == null) {
			try {
				datatypeFactory = DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException ex) {
				throw new IllegalStateException("Cannot construct DatatypeFactory: " + ex.getMessage(), ex);
			}
		}
		return datatypeFactory;
	}

	static {
		try {
			initTypeMap();
		} catch (Exception e) {
			LOGGER.error("Cannot initialize XSD type mapping: " + e.getMessage(), e);
			throw new IllegalStateException("Cannot initialize XSD type mapping: " + e.getMessage(), e);
		}
	}

}
