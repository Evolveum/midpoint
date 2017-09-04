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
package com.evolveum.midpoint.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author semancik
 *
 */
public class UglyHacks {

	// We hope that nobody will chose a crazy name like this. We don't want to use namespaces
	// here as that may be messed up as well.
	private static final String FORTIFIED_NAMESPACE_DECLARATIONS_ELEMENT_NAME = "FORtiFIed__xmlNS";
	private static final char FORTIFIED_NAMESPACE_DECLARATIONS_SEPARATOR = ' ';

	public static String forceXsiNsDeclaration(String originalXml) {
		int iOpeningBracket = -1;
		while (true) {
			iOpeningBracket = originalXml.indexOf('<', iOpeningBracket+1);
			if (iOpeningBracket < 0) {
				// No element ?
				return originalXml;
			}
			if (Character.isLetter(originalXml.charAt(iOpeningBracket+1))) {
				break;
			}
			// Processing instruction, skip it
		}
		int iClosingBracket = originalXml.indexOf('>', iOpeningBracket);
		if (iClosingBracket < 0) {
			// Element not closed?
			return originalXml;
		}
		String firstElement = originalXml.substring(iOpeningBracket, iClosingBracket);
		// Not perfect, but should be good enough. All this is a hack anyway
		if (firstElement.indexOf("xmlns:xsi") >= 0) {
			// Already has xsi declaration
			return originalXml;
		}
		int iEndOfElementName = iOpeningBracket;
		while (iEndOfElementName < iClosingBracket) {
			char ch = originalXml.charAt(iEndOfElementName);
			if (ch == ' ' || ch == '>') {
				break;
			}
			iEndOfElementName++;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(originalXml.substring(0, iEndOfElementName));
		sb.append(" xmlns:xsi='");
		sb.append(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
		sb.append("'");
		sb.append(originalXml.substring(iEndOfElementName));
		return sb.toString();
	}

	public static void fortifyNamespaceDeclarations(Node node) {
		DomElementVisitor visitor = new DomElementVisitor() {
			@Override
			public void visit(Element element) {
				fortifyNamespaceDeclarationsSingleElement(element);
			}
		};
		DomVisitorUtil.visitElements(node, visitor);
	}

	public static void fortifyNamespaceDeclarationsSingleElement(Element element) {
		List<String> xmlnss = new ArrayList<String>();
		NamedNodeMap attributes = element.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (DOMUtil.isNamespaceDefinition(attr)) {
				String prefix = DOMUtil.getNamespaceDeclarationPrefix(attr);
				String namespace = DOMUtil.getNamespaceDeclarationNamespace(attr);
				xmlnss.add(prefix + "=" + namespace);
			}
		}
		String fortifiedXmlnss = StringUtils.join(xmlnss, FORTIFIED_NAMESPACE_DECLARATIONS_SEPARATOR);
		element.setAttribute(FORTIFIED_NAMESPACE_DECLARATIONS_ELEMENT_NAME, fortifiedXmlnss);
	}

	public static void unfortifyNamespaceDeclarations(Node node) {
		DomElementVisitor visitor = new DomElementVisitor() {
			@Override
			public void visit(Element element) {
				unfortifyNamespaceDeclarationsSingleElement(element);
			}
		};
		DomVisitorUtil.visitElements(node, visitor);
	}

	public static void unfortifyNamespaceDeclarationsSingleElement(Element element) {
		String fortifiedXmlnss = element.getAttribute(FORTIFIED_NAMESPACE_DECLARATIONS_ELEMENT_NAME);
		if (element.hasAttribute(FORTIFIED_NAMESPACE_DECLARATIONS_ELEMENT_NAME)) {
			element.removeAttribute(FORTIFIED_NAMESPACE_DECLARATIONS_ELEMENT_NAME);
		}
		if (StringUtils.isBlank(fortifiedXmlnss)) {
			return;
		}
		String[] xmlnss = StringUtils.split(fortifiedXmlnss, FORTIFIED_NAMESPACE_DECLARATIONS_SEPARATOR);
		for (String xmlns: xmlnss) {
			String[] prefixAndUrl = StringUtils.split(xmlns, "=", 2);
			String prefix = prefixAndUrl[0];
			String url = prefixAndUrl[1];
			if (!DOMUtil.hasNamespaceDeclarationForPrefix(element, prefix)) {
				DOMUtil.setNamespaceDeclaration(element, prefix, url);
			}
		}
	}
}
