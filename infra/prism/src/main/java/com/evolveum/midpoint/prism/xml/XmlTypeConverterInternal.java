/*
 * Copyright (c) 2010-2018 Evolveum
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
import com.evolveum.midpoint.prism.marshaller.ItemPathSerializerTemp;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

/**
 *  This is to be cleaned up later. Ideally, this functionality should be accessed by prism serializer interface.
 */
public class XmlTypeConverterInternal {

	private static DatatypeFactory datatypeFactory = null;

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

	private static void toXsdElement(Object val, Element element, boolean recordType) throws SchemaException {
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

	// TODO move to DOM/XML serializer
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
            XMLGregorianCalendar xmlCal = XmlTypeConverter.createXMLGregorianCalendar((GregorianCalendar) val);
            return xmlCal.toXMLFormat();
        } else if (type.equals(ZonedDateTime.class)) {
        	GregorianCalendar gregorianCalendar = GregorianCalendar.from((ZonedDateTime)val);
            XMLGregorianCalendar xmlCal = XmlTypeConverter.createXMLGregorianCalendar(gregorianCalendar);
            return xmlCal.toXMLFormat();
        } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {
        	return ((XMLGregorianCalendar) val).toXMLFormat();
        } else if (Duration.class.isAssignableFrom(type)) {
        	return ((Duration) val).toString();
        } else if (type.equals(UniformItemPath.class) || type.equals(ItemPath.class)) {
        	return ItemPathSerializerTemp.serializeWithDeclarations((ItemPath) val);
        } else {
            throw new IllegalArgumentException("Unknown type for conversion: " + type + "(element " + elementName + ")");
        }
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

	public static Object toJavaValue(Element xmlElement, QName type) throws SchemaException {
	    return toJavaValue(xmlElement, XsdTypeMapper.getXsdToJavaMapping(type));
	}

	public static <T> T toJavaValue(String stringContent, QName typeQName) {
		Class<T> javaClass = XsdTypeMapper.getXsdToJavaMapping(typeQName);
		return toJavaValue(stringContent, javaClass, false);
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

	private static <T> T toJavaValue(String stringContent, Class<T> type) {
		return toJavaValue(stringContent, type, false);
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
	    } else if (type.equals(ZonedDateTime.class)) {
	        return (T) ZonedDateTime.parse(stringContent);
	    } else if (Duration.class.isAssignableFrom(type)) {
		    return (T) getDatatypeFactory().newDuration(stringContent);
	    } else if (type.equals(PolyString.class)) {
		    return (T) new PolyString(stringContent);
	    } else if (type.equals(UniformItemPath.class) || type.equals(ItemPath.class)) {
		    throw new UnsupportedOperationException("Path conversion not supported yet");
	    } else {
		    if (exceptionOnUnknown) {
			    throw new IllegalArgumentException("Unknown conversion type "+type);
		    } else {
			    return null;
		    }
	    }
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
}
