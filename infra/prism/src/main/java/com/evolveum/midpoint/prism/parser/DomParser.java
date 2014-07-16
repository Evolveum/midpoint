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
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class DomParser implements Parser {
	
	private static final QName SCHEMA_ELEMENT_QNAME = DOMUtil.XSD_SCHEMA_ELEMENT;
	
	private SchemaRegistry schemaRegistry;

	public DomParser(SchemaRegistry schemaRegistry) {
		super();
		this.schemaRegistry = schemaRegistry;
	}
	
	@Override
	public Collection<XNode> parseCollection(File file) throws SchemaException, IOException {
		Document document = DOMUtil.parseFile(file);
		return parseCollection(document);
	}

	@Override
	public Collection<XNode> parseCollection(InputStream stream) throws SchemaException, IOException {
		Document document = DOMUtil.parse(stream);
		return parseCollection(document);
	}
	
	private Collection<XNode> parseCollection(Document document) throws SchemaException{
		Element root = DOMUtil.getFirstChildElement(document);
		// TODO: maybe some check if this is a collection of other objects???
		List<Element> children = DOMUtil.listChildElements(root);
		Collection<XNode> nodes = new ArrayList<XNode>();
		for (Element child : children){
			RootXNode xroot = parse(child);
			nodes.add(xroot);
		}
		return nodes;
	}
	
	@Override
	public Collection<XNode> parseCollection(String dataString) throws SchemaException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public XNode parse(File file) throws SchemaException {
		Document document = DOMUtil.parseFile(file);
		return parse(document);
	}

    @Override
    public XNode parse(InputStream stream) throws SchemaException, IOException {
        Document document = DOMUtil.parse(stream);
        return parse(document);
    }


    @Override
	public XNode parse(String dataString) throws SchemaException {
		Document document = DOMUtil.parseDocument(dataString);
		return parse(document);
	}

	public RootXNode parse(Document document) throws SchemaException {
		Element rootElement = DOMUtil.getFirstChildElement(document);
		RootXNode xroot = parse(rootElement);
		return xroot;
	}
	
	public RootXNode parse(Element rootElement) throws SchemaException{
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
			xnode.setExplicitTypeDeclaration(true);
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
		XNode xsub;
		// We really want to have equals here, not match
		// we want to be very explicit about namespace here
		if (elementQName.equals(SCHEMA_ELEMENT_QNAME)) {
			if (elements.size() == 1) {
				xsub = parseSchemaElement(elements.iterator().next());
			} else {
				throw new SchemaException("Too many schema elements");
			}
		} else if (elements.size() == 1) {
			xsub = parseElementContent(elements.get(0));
		} else {
			xsub = parseElementList(elements); 
		}
		xmap.merge(elementQName, xsub);
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

    // changed from anonymous to be able to make it static (serializable independently of DomParser)
    private static class PrimitiveValueParser<T> implements ValueParser<T>, Serializable {

        private Element element;

        private PrimitiveValueParser(Element element) {
            this.element = element;
        }

        @Override
        public T parse(QName typeName) throws SchemaException {
            return parsePrimitiveElementValue(element, typeName);
        }
        @Override
        public boolean isEmpty() {
            return DOMUtil.isEmpty(element);
        }
        @Override
        public String getStringValue() {
            return element.getTextContent();
        }
        @Override
        public String toString() {
            return "ValueParser(DOMe, "+PrettyPrinter.prettyPrint(DOMUtil.getQName(element))+": "+element.getTextContent()+")";
        }
    };
    
    private static class PrimitiveAttributeParser<T> implements ValueParser<T>, Serializable {
   			
    	private Attr attr;
    	
    	public PrimitiveAttributeParser(Attr attr) {
			this.attr = attr;
		}
		@Override
		public T parse(QName typeName) throws SchemaException {
			return parsePrimitiveAttrValue(attr, typeName);
		}

		@Override
		public boolean isEmpty() {
			return DOMUtil.isEmpty(attr);
		}

		@Override
		public String getStringValue() {
			return attr.getValue();
		}

		@Override
		public String toString() {
			return "ValueParser(DOMa, " + PrettyPrinter.prettyPrint(DOMUtil.getQName(attr)) + ": "
					+ attr.getTextContent() + ")";
		}
	
    }

	private <T> PrimitiveXNode<T> parsePrimitiveElement(final Element element) throws SchemaException {
		PrimitiveXNode<T> xnode = new PrimitiveXNode<T>();
		extractCommonMetadata(element, xnode);
		xnode.setValueParser(new PrimitiveValueParser<T>(element));
		return xnode;
	}
		
	private static <T> T parsePrimitiveElementValue(Element element, QName typeName) throws SchemaException {
		if (ItemPath.XSD_TYPE.equals(typeName)) {
			return (T) parsePath(element);
		} else if (DOMUtil.XSD_QNAME.equals(typeName)) {
            return (T) DOMUtil.getQNameValue(element);
        } else if (XmlTypeConverter.canConvert(typeName)) {
			return (T) XmlTypeConverter.toJavaValue(element, typeName);
		} else {
			throw new SchemaException("Cannot convert element '"+element+"' to "+typeName);
		}
	}

	private <T> PrimitiveXNode<T> parseAttributeValue(final Attr attr) {
		PrimitiveXNode<T> xnode = new PrimitiveXNode<T>();
		xnode.setValueParser(new PrimitiveAttributeParser<T>(attr));
		xnode.setAttribute(true);
		return xnode;
	}

	private static <T> T parsePrimitiveAttrValue(Attr attr, QName typeName) throws SchemaException {
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

	private static ItemPath parsePath(Element element) {
		XPathHolder holder = new XPathHolder(element);
		return holder.toItemPath();
	}

	private SchemaXNode parseSchemaElement(Element schemaElement) {
		SchemaXNode xschema = new SchemaXNode();
		xschema.setSchemaElement(schemaElement);
		return xschema;
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

    public Element serializeXMapToElement(MapXNode xmap, QName elementName) throws SchemaException {
		DomSerializer serializer = new DomSerializer(this, schemaRegistry);
		return serializer.serializeToElement(xmap, elementName);
	}

	private Element serializeXPrimitiveToElement(PrimitiveXNode<?> xprim, QName elementName) throws SchemaException {
		DomSerializer serializer = new DomSerializer(this, schemaRegistry);
		return serializer.serializeXPrimitiveToElement(xprim, elementName);
	}
	
	public Element serializeXRootToElement(RootXNode xroot) throws SchemaException {
		DomSerializer serializer = new DomSerializer(this, schemaRegistry);
		return serializer.serialize(xroot);
	}

    // used only by JaxbDomHack.toAny(..) - hopefully it will disappear soon
    @Deprecated
    public Element serializeXRootToElement(RootXNode xroot, Document document) throws SchemaException {
        DomSerializer serializer = new DomSerializer(this, schemaRegistry);
        return serializer.serialize(xroot, document);
    }

    private Element serializeToElement(XNode xnode, QName elementName) throws SchemaException {
        Validate.notNull(xnode);
        Validate.notNull(elementName);
		if (xnode instanceof MapXNode) {
			return serializeXMapToElement((MapXNode) xnode, elementName);
		} else if (xnode instanceof PrimitiveXNode<?>) {
			return serializeXPrimitiveToElement((PrimitiveXNode<?>) xnode, elementName);
		} else if (xnode instanceof RootXNode) {
			return serializeXRootToElement((RootXNode)xnode);
		} else if (xnode instanceof ListXNode) {
			ListXNode xlist = (ListXNode) xnode;
			if (xlist.size() == 0) {
				return null;
			} else if (xlist.size() > 1) {
				throw new IllegalArgumentException("Cannot serialize list xnode with more than one item: "+xlist);
			} else {
				return serializeToElement(xlist.get(0), elementName);
			}
		} else {
			throw new IllegalArgumentException("Cannot serialized "+xnode+" to element");
		}
	}
	
	public Element serializeSingleElementMapToElement(MapXNode xmap) throws SchemaException {
		if (xmap == null || xmap.isEmpty()) {
			return null;
		}
		Entry<QName, XNode> subEntry = xmap.getSingleSubEntry(xmap.toString());
		Element parent = serializeToElement(xmap, subEntry.getKey());
		return DOMUtil.getFirstChildElement(parent);
	}

	
}
