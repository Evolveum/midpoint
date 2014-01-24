/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

public class DOMParser implements Parser {
	
	@Override
	public XNode parse(File file) throws SchemaException {
		Document document = DOMUtil.parseFile(file);
		return parse(document);
	}
	
	public RootXNode parse(Document document) throws SchemaException {
		Element rootElement = DOMUtil.getFirstChildElement(document);
		RootXNode xroot = new RootXNode(DOMUtil.getQName(rootElement));
		extractCommonMetadata(rootElement, xroot);
		XNode xnode = parseElement(rootElement);
		xroot.setSubnode(xnode);
		return xroot;
	}
	
	private void extractCommonMetadata(Element element, XNode xnode) throws SchemaException {
		QName xsiType = DOMUtil.resolveXsiType(element);
		if (xsiType != null) {
			xnode.setTypeQName(xsiType);
		}

		String maxOccursString = element.getAttributeNS(
				PrismConstants.A_MAX_OCCURS.getNamespaceURI(),
				PrismConstants.A_MAX_OCCURS.getLocalPart());
		if (!StringUtils.isBlank(maxOccursString)) {
			int maxOccurs = parseMultiplicity(maxOccursString, element);
			xnode.setMaxOccurs(maxOccurs);
		}
	}
	
	private int parseMultiplicity(String maxOccursString, Element element) throws SchemaException {
		if (PrismConstants.MULTIPLICITY_UNBONUNDED.equals(maxOccursString)) {
			return -1;
		}
		if (maxOccursString.startsWith("-")) {
			return -1;
		}
		if (StringUtils.isNumeric(maxOccursString)) {
			return Integer.valueOf(maxOccursString);
		} else {
			throw new SchemaException("Expecetd numeric value for " + PrismConstants.A_MAX_OCCURS.getLocalPart()
					+ " attribute on " + DOMUtil.getQName(element) + " but got " + maxOccursString);
		}
	}

	public XNode parseElement(Element element) throws SchemaException {
		if (DOMUtil.isNil(element)) {
			return null;
		}
		if (DOMUtil.hasChildElements(element) || DOMUtil.hasApplicationAttributes(element)) {
			return parseSubElemets(element);
		} else {
			return parsePrimitiveElement(element);
		}
	}

	private MapXNode parseSubElemets(Element element) throws SchemaException {
		MapXNode xmap = new MapXNode();
		extractCommonMetadata(element, xmap);
		
		// Subelements
		QName lastElementQName = null;
		List<Element> lastElements = null;
		for (Element childElement: DOMUtil.listChildElements(element)) {
			QName childQName = DOMUtil.getQName(childElement);
			if (childQName.equals(lastElementQName)) {
				lastElements.add(childElement);
			} else {
				parseElementGroup(xmap, lastElementQName, lastElements);
				lastElementQName = childQName;
				lastElements = new ArrayList<Element>();
				lastElements.add(childElement);
			}
		}
		parseElementGroup(xmap, lastElementQName, lastElements);
		
		// Attributes
		for (Attr attr: DOMUtil.listApplicationAttributes(element)) {
			QName attrQName = DOMUtil.getQName(attr);
			XNode subnode = parseAttributeValue(attr);
			xmap.put(attrQName, subnode);
		}
		
		return xmap;
	}
	
	private void parseElementGroup(MapXNode xmap, QName elementQName, List<Element> elements) throws SchemaException {
		if (elements == null || elements.isEmpty()) {
			return;
		}
		if (elements.size() == 1) {
			XNode xsub = parseElement(elements.get(0));
			xmap.put(elementQName, xsub);
		} else {
			ListXNode xlist = parseElementList(elements); 
			xmap.put(elementQName, xlist);
		}
	}

	/**
	 * Parses elements that all have the same element name. 
	 */
	private ListXNode parseElementList(List<Element> elements) throws SchemaException {
		ListXNode xlist = new ListXNode();
		for (Element element: elements) {
			XNode xnode = parseElement(element);
			xlist.add(xnode);
		}
		return xlist;
	}

	private <T> PrimitiveXNode<T> parsePrimitiveElement(final Element element) throws SchemaException {
		PrimitiveXNode<T> xnode = new PrimitiveXNode<T>();
		extractCommonMetadata(element, xnode);
		ValueParser<T> valueParser = new ValueParser<T>() {
			@Override
			public T parse(QName typeName) throws SchemaException {
				return parsePrimitiveElementValue(element, typeName);
			}
			@Override
			public String toString() {
				return "ValueParser(DOMe, "+PrettyPrinter.prettyPrint(DOMUtil.getQName(element))+": "+element.getTextContent()+")";
			}
		};
		xnode.setValueParser(valueParser);
		return xnode;
	}
		
	private <T> T parsePrimitiveElementValue(Element element, QName typeName) throws SchemaException {
		if (XmlTypeConverter.canConvert(typeName)) {
			return (T) XmlTypeConverter.toJavaValue(element, typeName);
		} else {
			throw new SchemaException("Cannot convert element '"+element+"' to "+typeName);
		}
	}
	
	private <T> PrimitiveXNode<T> parseAttributeValue(final Attr attr) {
		PrimitiveXNode<T> xnode = new PrimitiveXNode<T>();
		ValueParser<T> valueParser = new ValueParser<T>() {
			@Override
			public T parse(QName typeName) throws SchemaException {
				return parsePrimitiveAttrValue(attr, typeName);
			}
			@Override
			public String toString() {
				return "ValueParser(DOMa, "+PrettyPrinter.prettyPrint(DOMUtil.getQName(attr))+": "+attr.getTextContent()+")";
			}
		};
		xnode.setValueParser(valueParser);
		return xnode;
	}

	private <T> T parsePrimitiveAttrValue(Attr attr, QName typeName) throws SchemaException {
		if (DOMUtil.XSD_QNAME.equals(typeName)) {
			return (T) DOMUtil.getQNameValue(attr);
		}
		if (XmlTypeConverter.canConvert(typeName)) {
			String stringValue = attr.getTextContent();
			return XmlTypeConverter.toJavaValue(stringValue, typeName);
		} else {
			throw new SchemaException("Cannot convert attribute '"+attr+"' to "+typeName);
		}
	}

}
