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
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
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
    
    public static <T> T toJavaValue(Object element, Class<T> type) throws SchemaException {
        if (element instanceof Element) {
            Element xmlElement = (Element) element;
            if (type.equals(Element.class)) {
                return (T) xmlElement;
            } else if (type.equals(QName.class)) {
                return (T) DOMUtil.getQNameValue(xmlElement);
            } else {
                String stringContent = xmlElement.getTextContent();
                if (stringContent == null) {
                    return null;
                }
                T javaValue = toJavaValue(stringContent, type);
                if (javaValue == null) {
                    throw new IllegalArgumentException("Unknown type for conversion: " + type + "(element " + JAXBUtil.getElementQName(element) + ")");
                }
                return javaValue;
            }
        } else {
            throw new IllegalArgumentException("Unsupported element type: " + element.getClass().getName() + ": " + element);
        }
    }

    public static <T> T toJavaValue(String stringContent, Class<T> type) {
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
        } else if (type.equals(Long.class)) {
            return (T) Long.valueOf(stringContent);
        } else if (type.equals(long.class)) {
            return (T) Long.valueOf(stringContent);
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
        } else {
            return null;
        }
    }


    public static Object toJavaValue(Object xmlElement, QName type) throws SchemaException {
        return toJavaValue(xmlElement, XsdTypeMapper.toJavaType(type));
    }

    /**
     * Expects type information in xsi:type
     *
     * @param xmlElement
     * @return
     * @throws JAXBException
     */
    public static Object toJavaValue(Object xmlElement) throws SchemaException {
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
    public static TypedValue toTypedJavaValueWithDefaultType(Object element, QName defaultType) throws SchemaException {
        if (element instanceof Element) {
            // DOM Element
            Element xmlElement = (Element) element;
            QName xsiType = DOMUtil.resolveXsiType(xmlElement, null);
            if (xsiType == null) {
                xsiType = defaultType;
                if (xsiType == null) {
                    QName elementQName = JAXBUtil.getElementQName(xmlElement);
                    throw new SchemaException("Cannot convert element " + elementQName + " to java, no type information available", elementQName);
                }
            }
            return new TypedValue(toJavaValue(xmlElement, xsiType), xsiType, DOMUtil.getQName(xmlElement));
        } else {
            throw new IllegalArgumentException("Unsupported element type " + element.getClass().getName() + " in " + XmlTypeConverter.class.getSimpleName());
        }
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
     * @throws JAXBException
     */
    public static Object toXsdElement(Object val, QName elementName, Document doc, boolean recordType) throws SchemaException {
        if (val == null) {
            // if no value is specified, do not create element
            return null;
        }
        Class type = XsdTypeMapper.getTypeFromClass(val.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No type mapping for conversion: " + val.getClass() + "(element " + elementName + ")");
        }
//        if (JAXBUtil.isJaxbClass(type)) {
//            JAXBElement jaxbElement = new JAXBElement(elementName, type, val);
//            return jaxbElement;
//        } else {
            if (doc == null) {
                doc = DOMUtil.getDocument();
            }
            Element element = doc.createElementNS(elementName.getNamespaceURI(), elementName.getLocalPart());
            //TODO: switch to global namespace prefixes map
//            element.setPrefix(MidPointNamespacePrefixMapper.getPreferredPrefix(elementName.getNamespaceURI()));
            if (type.equals(Element.class)) {
                return val;
            } else if (type.equals(String.class)) {
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
            } else if (type.equals(Boolean.class)) {
                Boolean bool = (Boolean) val;
                if (bool.booleanValue()) {
                    element.setTextContent(XsdTypeMapper.BOOLEAN_XML_VALUE_TRUE);
                } else {
                    element.setTextContent(XsdTypeMapper.BOOLEAN_XML_VALUE_FALSE);
                }
            } else if (type.equals(GregorianCalendar.class)) {
                XMLGregorianCalendar xmlCal = toXMLGregorianCalendar((GregorianCalendar) val);
                element.setTextContent(xmlCal.toXMLFormat());
            } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {
            	element.setTextContent(((XMLGregorianCalendar) val).toXMLFormat());
            } else if (type.equals(QName.class)) {
                QName qname = (QName) val;
                DOMUtil.setQNameValue(element, qname);
            } else {
                throw new IllegalArgumentException("Unknown type for conversion: " + type + "(element " + elementName + ")");
            }
            if (recordType) {
                QName xsdType = XsdTypeMapper.toXsdType(val.getClass());
                DOMUtil.setXsiType(element, xsdType);
            }
            return element;
//        }
    }

    public static boolean canConvert(Class<?> clazz) {
        return (XsdTypeMapper.getJavaToXsdMapping(clazz) != null);
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
}
