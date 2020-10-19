/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.xml;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathSerializerTemp;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 *  This is to be cleaned up later. Ideally, this functionality should be accessed by prism serializer interface.
 */
public class XmlTypeConverterInternal {

    /**
     * @return created element
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
            if (bool) {
                return XsdTypeMapper.BOOLEAN_XML_VALUE_TRUE;
            } else {
                return XsdTypeMapper.BOOLEAN_XML_VALUE_FALSE;
            }
        } else if (type.equals(BigInteger.class)) {
            return val.toString();
        } else if (type.equals(BigDecimal.class)) {
            return val.toString();
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
            return val.toString();
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

}
