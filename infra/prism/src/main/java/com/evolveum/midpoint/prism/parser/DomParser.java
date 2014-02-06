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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
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

public class DomParser implements Parser {
	
	private SchemaRegistry schemaRegistry;

	public DomParser(SchemaRegistry schemaRegistry) {
		super();
		this.schemaRegistry = schemaRegistry;
	}

	@Override
	public XNode parse(File file) throws SchemaException {
		Document document = DOMUtil.parseFile(file);
		return parse(document);
	}

	@Override
	public XNode parse(String dataString) throws SchemaException {
		Document document = DOMUtil.parseDocument(dataString);
		return parse(document);
	}

	public RootXNode parse(Document document) throws SchemaException {
		Element rootElement = DOMUtil.getFirstChildElement(document);
		RootXNode xroot = new RootXNode(DOMUtil.getQName(rootElement));
		extractCommonMetadata(rootElement, xroot);
		XNode xnode = parseElementContent(rootElement);
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

	/**
	 * Parses the element into a RootXNode. 
	 */
	public RootXNode parseElementAsRoot(Element element) throws SchemaException {
		RootXNode xroot = new RootXNode(DOMUtil.getQName(element));
		extractCommonMetadata(element, xroot);
		xroot.setSubnode(parseElementContent(element));
		return xroot;
	}
	
	/**
	 * Parses the element in a single-entry MapXNode. 
	 */
	public MapXNode parseElementAsMap(Element element) throws SchemaException {
		MapXNode xmap = new MapXNode();
		extractCommonMetadata(element, xmap);
		xmap.put(DOMUtil.getQName(element), parseElementContent(element));
		return xmap;
	}
	
	/**
	 * Parses the content of the element (the name of the provided element is ignored, only the content is parsed).
	 */
	public XNode parseElementContent(Element element) throws SchemaException {
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
		
		// Attributes
		for (Attr attr: DOMUtil.listApplicationAttributes(element)) {
			QName attrQName = DOMUtil.getQName(attr);
			XNode subnode = parseAttributeValue(attr);
			xmap.put(attrQName, subnode);
		}

		// Subelements
		QName lastElementQName = null;
		List<Element> lastElements = null;
		for (Element childElement: DOMUtil.listChildElements(element)) {
			QName childQName = DOMUtil.getQName(childElement);
			if (childQName == null) {
				throw new IllegalArgumentException("Null qname in element "+childElement+", subelement of "+element);
			}
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
				
		return xmap;
	}
	
	private void parseElementGroup(MapXNode xmap, QName elementQName, List<Element> elements) throws SchemaException {
		if (elements == null || elements.isEmpty()) {
			return;
		}
		if (elements.size() == 1) {
			XNode xsub = parseElementContent(elements.get(0));
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
			XNode xnode = parseElementContent(element);
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
		if (ItemPath.XSD_TYPE.equals(typeName)) {
			return (T) parsePath(element);
		} else if (XmlTypeConverter.canConvert(typeName)) {
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
		xnode.setAttribute(true);
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

	private ItemPath parsePath(Element element) {
		XPathHolder holder = new XPathHolder(element);
		return holder.toItemPath();
	}

	@Override
	public boolean canParse(File file) throws IOException {
		if (file == null) {
			return false;
		}
		return file.getName().endsWith(".xml");
	}

	@Override
	public boolean canParse(String dataString) {
		if (dataString == null) {
			return false;
		}
		if (dataString.startsWith("<?xml")) {
			return true;
		}
		Pattern p = Pattern.compile("\\A\\s*?<\\w+");
		Matcher m = p.matcher(dataString);
		if (m.find()) {
			return true;
		}
		return false;
	}

	@Override
	public String serializeToString(XNode xnode, QName rootElementName) throws SchemaException {
		DomSerializer serializer = new DomSerializer(this, schemaRegistry);
		RootXNode xroot;
		if (xnode instanceof RootXNode) {
			xroot = (RootXNode) xnode;
		} else {
			xroot = new RootXNode(rootElementName);
			xroot.setSubnode(xnode);
		}
		Element element = serializer.serialize(xroot);
		return DOMUtil.serializeDOMToString(element);
	}
	
	@Override
	public String serializeToString(RootXNode xnode) throws SchemaException {
		DomSerializer serializer = new DomSerializer(this, schemaRegistry);
		Element element = serializer.serialize(xnode);
		return DOMUtil.serializeDOMToString(element);
	}

	public Element serializeValueToDom(PrismReferenceValue rval, QName elementName, Document document) {
		// TODO Auto-generated method stub
		return null;
	}

	public Element serializeValueToDom(PrismContainerValue<?> pval, QName elementName, Document document) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Element serializeToElement(MapXNode xmap, QName elementName) throws SchemaException {
		DomSerializer serializer = new DomSerializer(this, schemaRegistry);
		return serializer.serializeToElement(xmap, elementName);
	}
	
	public Element serializeToElement(RootXNode xroot) throws SchemaException {
		DomSerializer serializer = new DomSerializer(this, schemaRegistry);
		return serializer.serialize(xroot);
	}
}
