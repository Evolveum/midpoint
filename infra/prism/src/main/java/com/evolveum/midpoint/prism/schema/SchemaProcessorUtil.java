/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.prism.schema;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
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

	public static final String MULTIPLICITY_UNBOUNDED = "unbounded";

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
		List<Element> elements = new ArrayList<Element>();
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

	public static Boolean getAnnotationBoolean(XSAnnotation annotation, QName qname) throws SchemaException {
		Element element = getAnnotationElement(annotation, qname);
		if (element == null) {
			return null;
		}
		String textContent = element.getTextContent();
		if (textContent == null || textContent.isEmpty()) {
			return null;
		}
		return XmlTypeConverter.toJavaValue(element, Boolean.class);
	}

	public static Integer parseMultiplicity(String stringMultiplicity) {
		if (stringMultiplicity == null) {
			return null;
		}
		if (stringMultiplicity.equals(MULTIPLICITY_UNBOUNDED)) {
			return -1;
		}
		return Integer.parseInt(stringMultiplicity);
	}

}
