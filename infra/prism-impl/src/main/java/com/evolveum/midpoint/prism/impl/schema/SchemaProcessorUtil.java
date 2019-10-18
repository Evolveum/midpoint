/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.schema;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSType;

/**
 * Class to be used by schema processor but also by SchemaDefinitionFactory subclasses.
 *
 * @author Radovan Semancik
 *
 */
public class SchemaProcessorUtil {

    public static boolean hasAnnotation(XSType xsType, QName annotationElementName) {
        if (xsType.getName() == null) {
            return false;
        }
        Element annotationElement = getAnnotationElement(xsType.getAnnotation(), annotationElementName);
        if (annotationElement != null) {
            return true;
        }
        if (xsType.getBaseType() != null && !xsType.getBaseType().equals(xsType)) {
            return hasAnnotation(xsType.getBaseType(), annotationElementName);
        }
        return false;
    }

    public static Element getAnnotationElement(XSAnnotation annotation, QName qname) {
        if (annotation == null) {
            return null;
        }
        List<Element> elements = getAnnotationElements(annotation, qname);
        if (elements.isEmpty()) {
            return null;
        }
        return elements.get(0);
    }

    @NotNull
    public static List<Element> getAnnotationElements(XSAnnotation annotation, QName qname) {
        List<Element> elements = new ArrayList<>();
        if (annotation == null) {
            return elements;
        }

        Element xsdAnnotation = (Element) annotation.getAnnotation();
        NodeList list = xsdAnnotation.getElementsByTagNameNS(qname.getNamespaceURI(), qname.getLocalPart());
        if (list != null && list.getLength() > 0) {
            for (int i = 0; i < list.getLength(); i++) {
                elements.add((Element) list.item(i));
            }
        }

        return elements;
    }

    public static QName getAnnotationQName(XSAnnotation annotation, QName qname) {
        Element element = getAnnotationElement(annotation, qname);
        if (element == null) {
            return null;
        }
        return DOMUtil.getQNameValue(element);
    }

    /**
     * Parses "marker" boolean annotation. This means:
     * no element: false
     * empty element: true
     * non-empty element: parse element content as boolean
     */
    public static Boolean getAnnotationBooleanMarker(XSAnnotation annotation, QName qname) throws SchemaException {
        Element element = getAnnotationElement(annotation, qname);
        if (element == null) {
            return null;
        }
        String textContent = element.getTextContent();
        if (textContent == null || textContent.isEmpty()) {
            return true;
        }
        return XmlTypeConverter.toJavaValue(element, Boolean.class);
    }

    public static <T> T getAnnotationConverted(XSAnnotation annotation, QName qname, Class<T> type) throws SchemaException {
        Element element = getAnnotationElement(annotation, qname);
        if (element == null) {
            return null;
        }
        String textContent = element.getTextContent();
        if (textContent == null || textContent.isEmpty()) {
            return null;
        }
        return XmlTypeConverter.toJavaValue(element, type);
    }

    public static Boolean getAnnotationBoolean(XSAnnotation annotation, QName qname) throws SchemaException {
        return getAnnotationConverted(annotation, qname, Boolean.class);
    }

    public static Integer getAnnotationInteger(XSAnnotation annotation, QName qname) throws SchemaException {
        return getAnnotationConverted(annotation, qname, Integer.class);
    }

    public static String dumpAnnotation(XSAnnotation annotation) {
        if (annotation == null) {
            return null;
        }
        Element xsdAnnotation = (Element) annotation.getAnnotation();
        if (xsdAnnotation == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Element element : DOMUtil.listChildElements(xsdAnnotation)) {
            sb.append(element.getLocalName()).append(", ");
        }
        return sb.toString();
    }
}
