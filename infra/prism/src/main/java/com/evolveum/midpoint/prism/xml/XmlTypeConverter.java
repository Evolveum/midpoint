/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.prism.xml;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.marshaller.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Simple implementation that converts XSD primitive types to Java (and vice
 * versa).
 * <p/>
 * It convert type names (xsd types to java classes) and also the values.
 * <p/>
 * The implementation is very simple now. In fact just a bunch of ifs. We don't
 * need much more now. If more complex thing will be needed, we will extend the
 * implementation later.
 *
 * @author Radovan Semancik
 */
public class XmlTypeConverter {
	
	private static DatatypeFactory datatypeFactory = null;

    private static final Trace LOGGER = TraceManager.getTrace(XmlTypeConverter.class);

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
    
    public static <T> T toJavaValue(Element xmlElement, Class<T> type) throws SchemaException {
        if (type.equals(Element.class)) {
            return (T) xmlElement;
        } else if (type.equals(QName.class)) {
            return (T) DOMUtil.getQNameValue(xmlElement);
        } else if (PolyString.class.isAssignableFrom(type)) {
        	return (T) polyStringToJava(xmlElement);
        } else {
            String stringContent = xmlElement.getTextContent();
            if (stringContent == null) {
                return null;
            }
            T javaValue = toJavaValue(stringContent, type);
            if (javaValue == null) {
                throw new IllegalArgumentException("Unknown type for conversion: " + type + "(element " + DOMUtil.getQName(xmlElement) + ")");
            }
            return javaValue;
        }
    }
    
    public static <T> T toJavaValue(String stringContent, Class<T> type) {
    	return toJavaValue(stringContent, type, false);
    }
    
    public static <T> T toJavaValue(String stringContent, QName typeQName) {
    	Class<T> javaClass = XsdTypeMapper.getXsdToJavaMapping(typeQName);
    	return toJavaValue(stringContent, javaClass, false);
    }

	@SuppressWarnings("unchecked")
	public static <T> T toJavaValue(String stringContent, Class<T> type, boolean exceptionOnUnknown) {
        if (type.equals(String.class)) {
            return (T) stringContent;
        } else if (type.equals(char.class)) {
            return (T) (new Character(stringContent.charAt(0)));
        } else if (type.equals(File.class)) {
            return (T) new File(stringContent);
        } else if (type.equals(Integer.class)) {
            return (T) Integer.valueOf(stringContent);
        } else if (type.equals(int.class)) {
            return (T) Integer.valueOf(stringContent);
        } else if (type.equals(Short.class) || type.equals(short.class)) {
            return (T) Short.valueOf(stringContent);
        } else if (type.equals(Long.class)) {
            return (T) Long.valueOf(stringContent);
        } else if (type.equals(long.class)) {
            return (T) Long.valueOf(stringContent);
        } else if (type.equals(Byte.class)) {
            return (T) Byte.valueOf(stringContent);
        } else if (type.equals(byte.class)) {
            return (T) Byte.valueOf(stringContent);
        } else if (type.equals(float.class)) {
            return (T) Float.valueOf(stringContent);
        } else if (type.equals(Float.class)) {
            return (T) Float.valueOf(stringContent);
        } else if (type.equals(double.class)) {
            return (T) Double.valueOf(stringContent);
        } else if (type.equals(Double.class)) {
            return (T) Double.valueOf(stringContent);
        } else if (type.equals(BigInteger.class)) {
            return (T) new BigInteger(stringContent);
        } else if (type.equals(BigDecimal.class)) {
            return (T) new BigDecimal(stringContent);
        } else if (type.equals(byte[].class)) {
            byte[] decodedData = Base64.decodeBase64(stringContent);
            return (T) decodedData;
        } else if (type.equals(boolean.class) || Boolean.class.isAssignableFrom(type)) {
            // TODO: maybe we will need more inteligent conversion, e.g. to trim spaces, case insensitive, etc.
            return (T) Boolean.valueOf(stringContent);
        } else if (type.equals(GregorianCalendar.class)) {
            return (T) getDatatypeFactory().newXMLGregorianCalendar(stringContent).toGregorianCalendar();
        } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {
        	return (T) getDatatypeFactory().newXMLGregorianCalendar(stringContent);
        } else if (Duration.class.isAssignableFrom(type)) {
        	return (T) getDatatypeFactory().newDuration(stringContent);
        } else if (type.equals(PolyString.class)) {
        	return (T) new PolyString(stringContent);
        } else if (type.equals(ItemPath.class)) {
        	throw new UnsupportedOperationException("Path conversion not supported yet");
        } else {
        	if (exceptionOnUnknown) {
        		throw new IllegalArgumentException("Unknown conversion type "+type);
        	} else {
        		return null;
        	}
        }
    }


    public static Object toJavaValue(Element xmlElement, QName type) throws SchemaException {
        return toJavaValue(xmlElement, XsdTypeMapper.getXsdToJavaMapping(type));
    }

    /**
     * Expects type information in xsi:type
     *
     * @param xmlElement
     * @return
     * @throws JAXBException
     */
    public static Object toJavaValue(Element xmlElement) throws SchemaException {
        return toTypedJavaValueWithDefaultType(xmlElement, null).getValue();
    }

    /**
     * Try to locate element type from xsi:type, fall back to specified default
     * type.
     *
     * @param element
     * @param defaultType
     * @return converted java value
     * @throws JAXBException
     * @throws SchemaException if no xsi:type or default type specified
     */
    public static TypedValue toTypedJavaValueWithDefaultType(Element xmlElement, QName defaultType) throws SchemaException {
        QName xsiType = DOMUtil.resolveXsiType(xmlElement);
        if (xsiType == null) {
            xsiType = defaultType;
            if (xsiType == null) {
                QName elementQName = JAXBUtil.getElementQName(xmlElement);
                throw new SchemaException("Cannot convert element " + elementQName + " to java, no type information available", elementQName);
            }
        }
        return new TypedValue(toJavaValue(xmlElement, xsiType), xsiType, DOMUtil.getQName(xmlElement));
    }

    public static Object toXsdElement(Object val, QName typeName, QName elementName, Document doc, boolean recordType) throws SchemaException {
        // Just ignore the typeName for now. The java type will determine the conversion
        Object createdObject = toXsdElement(val, elementName, doc, false);
        if (createdObject instanceof Element) {
            Element createdElement = (Element) createdObject;
            // But record the correct type is asked to
            if (recordType) {
                if (typeName == null) {
                    // if no type was specified, just record the one that was used for automatic conversion
                    typeName = XsdTypeMapper.toXsdType(val.getClass());
                }
                DOMUtil.setXsiType(createdElement, typeName);
            }
        }
        return createdObject;
    }

    public static Object toXsdElement(Object val, QName elementName, Document doc) throws SchemaException {
        return toXsdElement(val, elementName, doc, false);
    }

    /**
     * @param val
     * @param elementName
     * @param doc
     * @param recordType
     * @return created element
     * @throws SchemaException
     */
    public static Element toXsdElement(Object val, QName elementName, Document doc, boolean recordType) throws SchemaException {
        if (val == null) {
            // if no value is specified, do not create element
            return null;
        }
        Class type = XsdTypeMapper.getTypeFromClass(val.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No type mapping for conversion: " + val.getClass() + "(element " + elementName + ")");
        }
        if (doc == null) {
            doc = DOMUtil.getDocument();
        }
        Element element = doc.createElementNS(elementName.getNamespaceURI(), elementName.getLocalPart());
        //TODO: switch to global namespace prefixes map
//            element.setPrefix(MidPointNamespacePrefixMapper.getPreferredPrefix(elementName.getNamespaceURI()));
        toXsdElement(val, element, recordType);
        return element;
    }
    
    public static void toXsdElement(Object val, Element element, boolean recordType) throws SchemaException {
        if (val instanceof Element) {
            return;
        } else if (val instanceof QName) {
            QName qname = (QName) val;
            DOMUtil.setQNameValue(element, qname);
        } else if (val instanceof PolyString) {
        	polyStringToElement(element, (PolyString)val);
        } else {
            element.setTextContent(toXmlTextContent(val, DOMUtil.getQName(element)));
        }
        if (recordType) {
            QName xsdType = XsdTypeMapper.toXsdType(val.getClass());
            DOMUtil.setXsiType(element, xsdType);
        }
    }
    
	public static String toXmlTextContent(Object val, QName elementName) {
        if (val == null) {
            // if no value is specified, do not create element
            return null;
        }
        Class type = XsdTypeMapper.getTypeFromClass(val.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No type mapping for conversion: " + val.getClass() + "(element " + elementName + ")");
        }
        if (type.equals(String.class)) {
            return (String) val;
        } if (type.equals(PolyString.class)){
        	return ((PolyString) val).getNorm();
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return ((Character) val).toString();
        } else if (type.equals(File.class)) {
            return ((File) val).getPath();
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return ((Integer) val).toString();
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return ((Long) val).toString();
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return ((Byte) val).toString();
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return ((Float) val).toString();
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return ((Double) val).toString();
        } else if (type.equals(byte[].class)) {
            byte[] binaryData = (byte[]) val;
            return Base64.encodeBase64String(binaryData);
        } else if (type.equals(Boolean.class)) {
            Boolean bool = (Boolean) val;
            if (bool.booleanValue()) {
                return XsdTypeMapper.BOOLEAN_XML_VALUE_TRUE;
            } else {
                return XsdTypeMapper.BOOLEAN_XML_VALUE_FALSE;
            }
        } else if (type.equals(BigInteger.class)) {
            return ((BigInteger) val).toString();
        } else if (type.equals(BigDecimal.class)) {
            return ((BigDecimal) val).toString();
        } else if (type.equals(GregorianCalendar.class)) {
            XMLGregorianCalendar xmlCal = createXMLGregorianCalendar((GregorianCalendar) val);
            return xmlCal.toXMLFormat();
        } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {
        	return ((XMLGregorianCalendar) val).toXMLFormat();
        } else if (Duration.class.isAssignableFrom(type)) {
        	return ((Duration) val).toString();
        } else if (type.equals(ItemPath.class)){
        	XPathHolder xpath = new XPathHolder((ItemPath)val);
        	return xpath.getXPath();
        } else {
            throw new IllegalArgumentException("Unknown type for conversion: " + type + "(element " + elementName + ")");
        }
    }

    public static boolean canConvert(Class<?> clazz) {
        return (XsdTypeMapper.getJavaToXsdMapping(clazz) != null);
    }
    
    public static boolean canConvert(QName xsdType) {
        return (XsdTypeMapper.getXsdToJavaMapping(xsdType) != null);
    }

    public static <T> T convertValueElementAsScalar(Element valueElement, Class<T> type) throws SchemaException {
        return toJavaValue(valueElement, type);
    }

    public static Object convertValueElementAsScalar(Element valueElement, QName xsdType) throws SchemaException {
        return toJavaValue(valueElement, xsdType);
    }

    public static List<Object> convertValueElementAsList(Element valueElement) throws SchemaException {
        return convertValueElementAsList(valueElement.getChildNodes(), Object.class);
    }

    public static <T> List<T> convertValueElementAsList(Element valueElement, Class<T> type) throws SchemaException {
        if (type.equals(Object.class)) {
            if (DOMUtil.hasXsiType(valueElement)) {
                Object scalarValue = convertValueElementAsScalar(valueElement, DOMUtil.resolveXsiType(valueElement));
                List<Object> list = new ArrayList<Object>(1);
                list.add(scalarValue);
                return (List<T>) list;
            }
        }
        return convertValueElementAsList(valueElement.getChildNodes(), type);
    }

    public static List<?> convertValueElementAsList(Element valueElement, QName xsdType) throws SchemaException {
        Class<?> type = XsdTypeMapper.toJavaType(xsdType);
        return convertValueElementAsList(valueElement.getChildNodes(), type);
    }

    public static <T> List<T> convertValueElementAsList(NodeList valueNodes, Class<T> type) throws SchemaException {
        // We need to determine whether this is single value or multi value
        // no XML elements = single (primitive) value
        // XML elements = multi value

        List<T> values = new ArrayList<T>();
        if (valueNodes == null) {
            return values;
        }

        T scalarAttempt = tryConvertPrimitiveScalar(valueNodes, type);
        if (scalarAttempt != null) {
            values.add(scalarAttempt);
            return values;
        }

        for (int i = 0; i < valueNodes.getLength(); i++) {
            Node node = valueNodes.item(i);
            if (DOMUtil.isJunk(node)) {
                continue;
            }
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                T value = null;
                if (type.equals(Object.class)) {
                    Class<?> overrideType = String.class;
                    if (DOMUtil.hasXsiType(element)) {
                        QName xsiType = DOMUtil.resolveXsiType(element);
                        overrideType = XsdTypeMapper.toJavaType(xsiType);
                    }
                    value = (T) XmlTypeConverter.toJavaValue(element, overrideType);
                } else {
                    value = XmlTypeConverter.toJavaValue(element, type);
                }
                values.add(value);
            }
        }
        return values;
    }

    private static <T> T tryConvertPrimitiveScalar(NodeList valueNodes, Class<T> type) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valueNodes.getLength(); i++) {
            Node node = valueNodes.item(i);
            if (DOMUtil.isJunk(node)) {
                continue;
            }
            if (node.getNodeType() == Node.TEXT_NODE) {
                sb.append(node.getTextContent());
            } else {
                // We have failed
                return null;
            }
        }
        if (type.equals(Object.class)) {
            // Try string as default type
            return (T) XmlTypeConverter.toJavaValue(sb.toString(), String.class);
        }
        return XmlTypeConverter.toJavaValue(sb.toString(), type);
    }

    public static void appendBelowNode(Object val, QName xsdType, QName name, Node parentNode,
                                       boolean recordType) throws SchemaException {
        Object xsdElement = toXsdElement(val, xsdType, name, parentNode.getOwnerDocument(), recordType);
        if (xsdElement == null) {
            return;
        }
        if (xsdElement instanceof Element) {
            parentNode.appendChild((Element) xsdElement);
//        } else if (xsdElement instanceof JAXBElement) {
//            try {
//                JAXBUtil.marshal(xsdElement, parentNode);
//            } catch (JAXBException e) {
//                throw new SchemaException("Error marshalling element " + xsdElement + ": " + e.getMessage(), e);
//            }
        } else {
            throw new IllegalStateException("The XSD type converter returned unknown element type: " + xsdElement + " (" + xsdElement.getClass().getName() + ")");
        }
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(long timeInMillis) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(timeInMillis);
        return createXMLGregorianCalendar(gregorianCalendar);
    }
    
	public static XMLGregorianCalendar createXMLGregorianCalendar(Date date) {
        if (date == null) {
            return null;
        }
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.setTime(date);
		return createXMLGregorianCalendar(gregorianCalendar);
	}
	
	public static XMLGregorianCalendar createXMLGregorianCalendar(String string) {
		return getDatatypeFactory().newXMLGregorianCalendar(string);
	}
    
    public static XMLGregorianCalendar createXMLGregorianCalendar(GregorianCalendar cal) {
        return getDatatypeFactory().newXMLGregorianCalendar(cal);
    }

    // in some environments, XMLGregorianCalendar.clone does not work
    public static XMLGregorianCalendar createXMLGregorianCalendar(XMLGregorianCalendar cal) {
        if (cal == null) {
            return null;
        }
        return getDatatypeFactory().newXMLGregorianCalendar(cal.toGregorianCalendar()); // TODO find a better way
    }
    
    public static XMLGregorianCalendar createXMLGregorianCalendar(int year, int month, int day, int hour, int minute,
    		int second, int millisecond, int timezone) {
        return getDatatypeFactory().newXMLGregorianCalendar(year, month, day, hour, minute, second, millisecond, timezone);
    }
    
    public static XMLGregorianCalendar createXMLGregorianCalendar(int year, int month, int day, int hour, int minute,
    		int second) {
        return getDatatypeFactory().newXMLGregorianCalendar(year, month, day, hour, minute, second, 0, 0);
    }

    public static long toMillis(XMLGregorianCalendar xmlCal) {
    	if (xmlCal == null){
    		return 0;
    	}
        return xmlCal.toGregorianCalendar().getTimeInMillis();
    }

	public static Date toDate(XMLGregorianCalendar xmlCal) {
		return xmlCal != null ? new Date(xmlCal.toGregorianCalendar().getTimeInMillis()) : null;
	}
    
    public static Duration createDuration(long durationInMilliSeconds) {
    	return getDatatypeFactory().newDuration(durationInMilliSeconds);
    }
    
    public static Duration createDuration(String lexicalRepresentation) {
    	return lexicalRepresentation != null ? getDatatypeFactory().newDuration(lexicalRepresentation) : null;
    }

    public static Duration createDuration(boolean isPositive, int years, int months, int days, int hours, int minutes, int seconds) {
    	return getDatatypeFactory().newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }
    
    /**
     * Parse PolyString from DOM element.
     */
	private static PolyString polyStringToJava(Element polyStringElement) throws SchemaException {
		Element origElement = DOMUtil.getChildElement(polyStringElement, PrismConstants.POLYSTRING_ELEMENT_ORIG_QNAME);
		if (origElement == null) {
			// Check for a special syntactic short-cut. If the there are no child elements use the text content of the
			// element as the value of orig
			if (DOMUtil.hasChildElements(polyStringElement)) {
				throw new SchemaException("Missing element "+PrismConstants.POLYSTRING_ELEMENT_ORIG_QNAME+" in polystring element "+
						DOMUtil.getQName(polyStringElement));
			}
			String orig = polyStringElement.getTextContent();
			return new PolyString(orig);
		}
		String orig = origElement.getTextContent();
		String norm = null;
		Element normElement = DOMUtil.getChildElement(polyStringElement, PrismConstants.POLYSTRING_ELEMENT_NORM_QNAME);
		if (normElement != null) {
			norm = normElement.getTextContent();
		}
		return new PolyString(orig, norm);
	}
	
	/**
	 * Serialize PolyString to DOM element.
	 */
	private static void polyStringToElement(Element polyStringElement, PolyString polyString) {
		if (polyString.getOrig() != null) {
			Element origElement = DOMUtil.createSubElement(polyStringElement, PrismConstants.POLYSTRING_ELEMENT_ORIG_QNAME);
			origElement.setTextContent(polyString.getOrig());
		}
		if (polyString.getNorm() != null) {
			Element origElement = DOMUtil.createSubElement(polyStringElement, PrismConstants.POLYSTRING_ELEMENT_NORM_QNAME);
			origElement.setTextContent(polyString.getNorm());
		}		
	}
	
	public static <T> T toXmlEnum(Class<T> expectedType, String stringValue) {
		if (stringValue == null) {
			return null;
		}
		for (T enumConstant: expectedType.getEnumConstants()) {
			Field field;
			try {
				field = expectedType.getField(((Enum)enumConstant).name());
			} catch (SecurityException e) {
				throw new IllegalArgumentException("Error getting field from '"+enumConstant+"' in "+expectedType, e);
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Error getting field from '"+enumConstant+"' in "+expectedType, e);
			}
			XmlEnumValue annotation = field.getAnnotation(XmlEnumValue.class);
			if (annotation.value().equals(stringValue)) {
				return enumConstant;
			}
		}
		throw new IllegalArgumentException("No enum value '"+stringValue+"' in "+expectedType);
	}
	
	public static <T> String fromXmlEnum(T enumValue) {
		if (enumValue == null) {
			return null;
		}
		String fieldName = ((Enum)enumValue).name();
		Field field;
		try {
			field = enumValue.getClass().getField(fieldName);
		} catch (SecurityException e) {
			throw new IllegalArgumentException("Error getting field from "+enumValue, e);
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException("Error getting field from "+enumValue, e);
		}
		XmlEnumValue annotation = field.getAnnotation(XmlEnumValue.class);
		return annotation.value();
	}

	public static XMLGregorianCalendar addDuration(XMLGregorianCalendar now, Duration duration) {
		XMLGregorianCalendar later = createXMLGregorianCalendar(toMillis(now));
		later.add(duration);
		return later;
	}
	
	public static XMLGregorianCalendar addDuration(XMLGregorianCalendar now, String duration) {
		XMLGregorianCalendar later = createXMLGregorianCalendar(toMillis(now));
		later.add(createDuration(duration));
		return later;
	}

	public static XMLGregorianCalendar addMillis(XMLGregorianCalendar now, long duration) {
		return createXMLGregorianCalendar(toMillis(now) + duration);
	}

	public static String formatDateXml(Date date) {
		return createXMLGregorianCalendar(date).toXMLFormat();
	}

}
