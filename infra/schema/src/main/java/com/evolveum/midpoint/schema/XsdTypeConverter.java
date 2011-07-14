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

import com.evolveum.midpoint.xml.schema.SchemaConstants;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Element;

/**
 * Simple implementation that converts XSD primitive types to Java (and vice versa).
 * 
 * It convert type names (xsd types to java classes) and also the values.
 * 
 * The implementation is very simple now. In fact just a bunch of ifs. We
 * don't need much more now. If more complex thing will be needed, we will
 * extend the implementation later.
 * 
 * @author Radovan Semancik
 */
public class XsdTypeConverter {
	
	private static Map<Class,QName> javaToXsdTypeMap;
	private static Map<QName,Class> xsdToJavaTypeMap;
	private static DatatypeFactory datatypeFactory = null;
		
	private static void initTypeMap() {
		
        javaToXsdTypeMap = new HashMap();
		xsdToJavaTypeMap = new HashMap();
        addMapping(String.class, SchemaConstants.XSD_STRING,true);
        addMapping(int.class, SchemaConstants.XSD_INTEGER,true);
        addMapping(Integer.class, SchemaConstants.XSD_INTEGER,false);
        addMapping(long.class, SchemaConstants.XSD_INTEGER,false);
        addMapping(Long.class, SchemaConstants.XSD_INTEGER,false);
        addMapping(boolean.class, SchemaConstants.XSD_BOOLEAN,true);
		addMapping(byte[].class, SchemaConstants.XSD_BASE64BINARY,true);
		addMapping(GregorianCalendar.class, SchemaConstants.XSD_DATETIME,true);
    }
	
	private static void addMapping(Class javaClass, QName xsdType,boolean both) {
		javaToXsdTypeMap.put(javaClass, xsdType);
		if (both) {
			xsdToJavaTypeMap.put(xsdType, javaClass);
		}
	}
	
    public static QName toXsdType(Class javaClass) {
        QName xsdType = javaToXsdTypeMap.get(javaClass);
        if (xsdType==null) {
            throw new IllegalArgumentException("No XSD mapping for Java type "+javaClass.getCanonicalName());
        }
        return xsdType;
    }

	public static Class toJavaType(QName xsdType) {
        Class javaType = xsdToJavaTypeMap.get(xsdType);
        if (javaType==null) {
            throw new IllegalArgumentException("No type mapping for XSD type "+xsdType);
        }
        return javaType;
    }

	public static Object toJavaValue(Element xmlElement, Class type) {
		String stringContent = xmlElement.getTextContent();
		if (type.equals(String.class)) {
			return stringContent;
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
		} else if (type.equals(boolean.class) || Boolean.class.isAssignableFrom(type)){
			return Boolean.parseBoolean(stringContent);
		} else if (type.equals(GregorianCalendar.class)){
			return getDatatypeFactory().newXMLGregorianCalendar(stringContent).toGregorianCalendar();
		} else {
			throw new IllegalArgumentException("Unknown type for conversion: " + type);
		}
	}

	public static Object toJavaValue(Element xmlElement, QName type) {
		return toJavaValue(xmlElement,toJavaType(type));
	}
	
	public static void toXsdElement(Object val, QName typeName, Element element) {
		// Just ignore the typeName for now. The java type will determine the conversion
		toXsdElement(val,element);
	}
	
	public static void toXsdElement(Object val, Element element) {
		Class type = val.getClass();
		if (type.equals(String.class)) {
			element.setTextContent((String)val);
		} else if (type.equals(int.class) || type.equals(Integer.class)) {
			element.setTextContent(((Integer)val).toString());
		} else if (type.equals(long.class) || type.equals(Long.class)) {
			element.setTextContent(((Long)val).toString());
		} else if (type.equals(byte[].class)) {
			byte[] binaryData = (byte[]) val;
			element.setTextContent(Base64.encodeBase64String(binaryData));
		} else if (type.equals(GregorianCalendar.class)) {
			XMLGregorianCalendar xmlCal = toXMLGregorianCalendar((GregorianCalendar)val);
			element.setTextContent(xmlCal.toXMLFormat());
		} else {
			throw new IllegalArgumentException("Unknown type for conversion: " + type);
		}
	}
	
	public static boolean canConvert(Class clazz) {
		return javaToXsdTypeMap.get(clazz)!=null;
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
		if (datatypeFactory==null) {
			try {
				datatypeFactory = DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException ex) {
				throw new IllegalStateException("Cannot construct DatatypeFactory: "+ex.getMessage(),ex);
			}
		}
		return datatypeFactory;
	}
	
	static {
		initTypeMap();
	}

}
