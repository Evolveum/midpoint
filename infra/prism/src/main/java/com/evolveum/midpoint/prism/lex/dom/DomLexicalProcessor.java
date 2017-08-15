/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.prism.lex.dom;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.lex.LexicalUtils;
import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.staxmate.dom.DOMConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomLexicalProcessor implements LexicalProcessor<String> {

	public static final Trace LOGGER = TraceManager.getTrace(DomLexicalProcessor.class);

	private static final QName SCHEMA_ELEMENT_QNAME = DOMUtil.XSD_SCHEMA_ELEMENT;

	@NotNull private final SchemaRegistry schemaRegistry;

	public DomLexicalProcessor(@NotNull SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	@Deprecated
	public XNode read(File file, ParsingContext parsingContext) throws SchemaException, IOException {
		return read(new ParserFileSource(file), parsingContext);
	}

	@NotNull
	@Override
	public RootXNode read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
		if (source instanceof ParserElementSource) {
			return read(((ParserElementSource) source).getElement());
		}

		InputStream is = source.getInputStream();
		try {
			Document document = DOMUtil.parse(is);
			return read(document);
		} finally {
			if (source.closeStreamAfterParsing()) {
				IOUtils.closeQuietly(is);
			}
		}
	}

	@NotNull
	@Override
	public List<RootXNode> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
		InputStream is = source.getInputStream();
		try {
			Document document = DOMUtil.parse(is);
			return readObjects(document);
		} finally {
			if (source.closeStreamAfterParsing()) {
				IOUtils.closeQuietly(is);
			}
		}
	}

	// code taken from Validator class
	@Override
	public void readObjectsIteratively(@NotNull ParserSource source, @NotNull ParsingContext parsingContext,
			RootXNodeHandler handler) throws SchemaException, IOException {
		InputStream is = source.getInputStream();
		XMLStreamReader stream = null;
		try {
			stream = XMLInputFactory.newInstance().createXMLStreamReader(is);

			int eventType = stream.nextTag();
			if (eventType != XMLStreamConstants.START_ELEMENT) {
				throw new SystemException("StAX Malfunction?");
			}
			DOMConverter domConverter = new DOMConverter();
			Map<String, String> rootNamespaceDeclarations = new HashMap<>();

			QName objectsMarker = schemaRegistry.getPrismContext().getObjectsElementName();
			if (objectsMarker != null && !QNameUtil.match(stream.getName(), objectsMarker)) {
				readSingleObjectIteratively(stream, rootNamespaceDeclarations, domConverter, handler);
			}
			for (int i = 0; i < stream.getNamespaceCount(); i++) {
				rootNamespaceDeclarations.put(stream.getNamespacePrefix(i), stream.getNamespaceURI(i));
			}
			while (stream.hasNext()) {
				eventType = stream.next();
				if (eventType == XMLStreamConstants.START_ELEMENT) {
					if (!readSingleObjectIteratively(stream, rootNamespaceDeclarations, domConverter, handler)) {
						return;
					}
				}
			}
		} catch (XMLStreamException ex) {
			String lineInfo = stream != null
					? " on line " + stream.getLocation().getLineNumber()
					: "";
			throw new SchemaException("Exception while parsing XML" + lineInfo + ": " + ex.getMessage(), ex);
		} finally {
			if (source.closeStreamAfterParsing()) {
				IOUtils.closeQuietly(is);
			}
		}
	}

	private boolean readSingleObjectIteratively(XMLStreamReader stream, Map<String, String> rootNamespaceDeclarations,
			DOMConverter domConverter, RootXNodeHandler handler) throws XMLStreamException, SchemaException {
		Document objectDoc = domConverter.buildDocument(stream);
		Element objectElement = DOMUtil.getFirstChildElement(objectDoc);
		DOMUtil.setNamespaceDeclarations(objectElement, rootNamespaceDeclarations);
		RootXNode rootNode = read(objectElement);
		return handler.handleData(rootNode);
	}

	private List<RootXNode> readObjects(Document document) throws SchemaException{
		Element root = DOMUtil.getFirstChildElement(document);
		QName objectsMarker = schemaRegistry.getPrismContext().getObjectsElementName();
		if (objectsMarker != null && !QNameUtil.match(DOMUtil.getQName(root), objectsMarker)) {
			return Collections.singletonList(read(root));
		} else {
			List<RootXNode> rv = new ArrayList<>();
			for (Element child : DOMUtil.listChildElements(root)) {
				rv.add(read(child));
			}
			return rv;
		}
	}

	@NotNull
	public RootXNode read(Document document) throws SchemaException {
		Element rootElement = DOMUtil.getFirstChildElement(document);
		return read(rootElement);
	}

	@NotNull
	public RootXNode read(Element rootElement) throws SchemaException {
		RootXNode xroot = new RootXNode(DOMUtil.getQName(rootElement));
		extractCommonMetadata(rootElement, xroot);
		XNode xnode = parseElementContent(rootElement, false);
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
			throw new SchemaException("Expected numeric value for " + PrismConstants.A_MAX_OCCURS.getLocalPart()
					+ " attribute on " + DOMUtil.getQName(element) + " but got " + maxOccursString);
		}
	}

	/**
	 * Parses the content of the element (the name of the provided element is ignored (unless storeElementName=true),
	 * only the content is parsed).
	 */
	@Nullable
	private XNode parseElementContent(Element element, boolean storeElementName) throws SchemaException {
		if (DOMUtil.isNil(element)) {		// TODO: ok?
			return null;
		}
		XNode node;
		if (DOMUtil.hasChildElements(element) || DOMUtil.hasApplicationAttributes(element)) {
			if (isList(element)) {
				node = parseElementContentToList(element);
			} else {
				node = parseElementContentToMap(element);
			}
		} else {
			node = parsePrimitiveElement(element);
		}
		if (storeElementName) {
			node.setElementName(DOMUtil.getQName(element));
		}
		extractCommonMetadata(element, node);
		return node;
	}

	// all the sub-elements should be compatible (this is not enforced here, however)
	private ListXNode parseElementContentToList(Element element) throws SchemaException {
		if (DOMUtil.hasApplicationAttributes(element)) {
			throw new SchemaException("List should have no application attributes: " + element);
		}
		return parseElementList(DOMUtil.listChildElements(element), true);
	}

	private MapXNode parseElementContentToMap(Element element) throws SchemaException {
		MapXNode xmap = new MapXNode();

		// Attributes
		for (Attr attr: DOMUtil.listApplicationAttributes(element)) {
			QName attrQName = DOMUtil.getQName(attr);
			XNode subnode = parseAttributeValue(attr);
			xmap.put(attrQName, subnode);
		}

		// Sub-elements
		QName lastElementQName = null;
		List<Element> lastElements = null;
		for (Element childElement: DOMUtil.listChildElements(element)) {
			QName childQName = DOMUtil.getQName(childElement);
			if (match(childQName, lastElementQName)) {
				lastElements.add(childElement);
			} else {
				parseSubElementsGroupAsMapEntry(xmap, lastElementQName, lastElements);
				lastElementQName = childQName;
				lastElements = new ArrayList<>();
				lastElements.add(childElement);
			}
		}
		parseSubElementsGroupAsMapEntry(xmap, lastElementQName, lastElements);
		return xmap;
	}

	private boolean isList(Element element) throws SchemaException {
		String isListAttribute = DOMUtil.getAttribute(element, new QName(DOMUtil.IS_LIST_ATTRIBUTE_NAME));
		if (StringUtils.isNotEmpty(isListAttribute)) {
			return Boolean.valueOf(isListAttribute);
		}
		// enable this after schema registry is optional (now it's mandatory)
//		if (schemaRegistry == null) {
//			return false;
//		}

		// checking parent element fitness
		QName typeName = DOMUtil.resolveXsiType(element);
		if (typeName != null) {
			Collection<? extends ComplexTypeDefinition> definitions = schemaRegistry
					.findTypeDefinitionsByType(typeName, ComplexTypeDefinition.class);
			if (definitions.isEmpty()) {
				return false;	// to be safe (we support this heuristic only for known types)
			}
			if (QNameUtil.hasNamespace(typeName)) {
				assert definitions.size() <= 1;
				return definitions.iterator().next().isListMarker();
			} else {
				if (definitions.stream().allMatch(ComplexTypeDefinition::isListMarker)) {
					// great -- we are very probably OK -- so let's continue
				} else {
					return false;	// sorry, there's a possibility of failure
				}
			}
		} else {	// typeName == null
			Collection<? extends ComplexTypeDefinition> definitions =
					schemaRegistry.findTypeDefinitionsByElementName(DOMUtil.getQName(element), ComplexTypeDefinition.class);
			// TODO - or allMatch here? - allMatch would mean that if there's an extension (or resource item) with a name
			// of e.g. formItems, pipeline, sequence, ... - it would not be recognizable as list=true anymore. That's why
			// we will use anyMatch here.
			if (definitions.stream().anyMatch(ComplexTypeDefinition::isListMarker)) {
				// we are very hopefully OK -- so let's continue
			} else {
				return false;
			}
		}

		// checking the content
		if (DOMUtil.hasApplicationAttributes(element)) {
			return false;		// TODO - or should we fail in this case?
		}
		//System.out.println("Elements are compatible: " + DOMUtil.listChildElements(element) + ": " + rv);
		return elementsAreCompatible(DOMUtil.listChildElements(element));
	}

	private boolean elementsAreCompatible(List<Element> elements) {
		QName unified = null;
		for (Element element : elements) {
			QName root = getHierarchyRoot(DOMUtil.getQName(element));
			if (unified == null) {
				unified = root;
			} else if (!QNameUtil.match(unified, root)) {
				return false;
			} else if (QNameUtil.noNamespace(unified) && QNameUtil.hasNamespace(root)) {
				unified = root;
			}
		}
		return true;
	}

	private QName getHierarchyRoot(QName name) {
		ItemDefinition def = schemaRegistry.findItemDefinitionByElementName(name);
		if (def == null || !def.isHeterogeneousListItem()) {
			return name;
		} else {
			return def.getSubstitutionHead();
		}
	}

	private boolean match(QName name, QName existing) {
		if (existing == null) {
			return false;
		} else {
			return QNameUtil.match(name, existing);
		}
	}

	private void parseSubElementsGroupAsMapEntry(MapXNode xmap, QName elementQName, List<Element> elements) throws SchemaException {
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
			xsub = parseElementContent(elements.get(0), false);
		} else {
			xsub = parseElementList(elements, false);
		}
		xmap.merge(elementQName, xsub);
	}

	/**
	 * Parses elements that should form the list (either they have the same element name, or they are
	 * stored as a sub-elements of "list" parent element).
	 */
	private ListXNode parseElementList(List<Element> elements, boolean storeElementNames) throws SchemaException {
		ListXNode xlist = new ListXNode();
		for (Element element: elements) {
			XNode xnode = parseElementContent(element, storeElementNames);
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
        public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
            return parsePrimitiveElementValue(element, typeName, mode);
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
        public Map<String, String> getPotentiallyRelevantNamespaces() {
            return DOMUtil.getAllVisibleNamespaceDeclarations(element);
        }
        @Override
        public String toString() {
            return "ValueParser(DOMe, "+PrettyPrinter.prettyPrint(DOMUtil.getQName(element))+": "+element.getTextContent()+")";
        }
    }
    
    private static class PrimitiveAttributeParser<T> implements ValueParser<T>, Serializable {
   			
    	private Attr attr;
    	
    	public PrimitiveAttributeParser(Attr attr) {
			this.attr = attr;
		}
		@Override
		public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
			return parsePrimitiveAttrValue(attr, typeName, mode);
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

        @Override
        public Map<String, String> getPotentiallyRelevantNamespaces() {
            return DOMUtil.getAllVisibleNamespaceDeclarations(attr);
        }
    }

	private <T> PrimitiveXNode<T> parsePrimitiveElement(final Element element) throws SchemaException {
		PrimitiveXNode<T> xnode = new PrimitiveXNode<>();
		xnode.setValueParser(new PrimitiveValueParser<>(element));
		return xnode;
	}
		
	private static <T> T parsePrimitiveElementValue(Element element, QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
		try {
			if (ItemPathType.COMPLEX_TYPE.equals(typeName)) {
				return (T) parsePath(element);
			} else if (DOMUtil.XSD_QNAME.equals(typeName)) {
				return (T) DOMUtil.getQNameValue(element);
			} else if (XmlTypeConverter.canConvert(typeName)) {
				return (T) XmlTypeConverter.toJavaValue(element, typeName);
			} else if (DOMUtil.XSD_ANYTYPE.equals(typeName)) {
				return (T) element.getTextContent();                // if parsing primitive as xsd:anyType, we can safely parse it as string
			} else {
				throw new SchemaException("Cannot convert element '" + element + "' to " + typeName);
			}
		} catch (IllegalArgumentException e) {
			return processIllegalArgumentException(element.getTextContent(), typeName, e, mode);		// primitive way of ensuring compatibility mode
		}
	}

	private static <T> T processIllegalArgumentException(String value, QName typeName, IllegalArgumentException e, XNodeProcessorEvaluationMode mode) {
		if (mode != XNodeProcessorEvaluationMode.COMPAT) {
			throw e;
		}
		LOGGER.warn("Value of '{}' couldn't be parsed as '{}' -- interpreting as null because of COMPAT mode set", value, typeName, e);
		return null;
	}

	private <T> PrimitiveXNode<T> parseAttributeValue(final Attr attr) {
		PrimitiveXNode<T> xnode = new PrimitiveXNode<>();
		xnode.setValueParser(new PrimitiveAttributeParser<>(attr));
		xnode.setAttribute(true);
		return xnode;
	}

	private static <T> T parsePrimitiveAttrValue(Attr attr, QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
		if (DOMUtil.XSD_QNAME.equals(typeName)) {
			try {
				return (T) DOMUtil.getQNameValue(attr);
			} catch (IllegalArgumentException e) {
				return processIllegalArgumentException(attr.getTextContent(), typeName, e, mode);		// primitive way of ensuring compatibility mode
			}
		}
		if (XmlTypeConverter.canConvert(typeName)) {
			String stringValue = attr.getTextContent();
			try {
				return XmlTypeConverter.toJavaValue(stringValue, typeName);
			} catch (IllegalArgumentException e) {
				return processIllegalArgumentException(attr.getTextContent(), typeName, e, mode);		// primitive way of ensuring compatibility mode
			}
		} else {
			throw new SchemaException("Cannot convert attribute '"+attr+"' to "+typeName);
		}
	}

	@NotNull
	private static ItemPathType parsePath(Element element) {
		ItemPathHolder holder = new ItemPathHolder(element);
		return new ItemPathType(holder.toItemPath());
	}

	private SchemaXNode parseSchemaElement(Element schemaElement) {
		SchemaXNode xschema = new SchemaXNode();
		xschema.setSchemaElement(schemaElement);
		return xschema;
	}

	@Override
	public boolean canRead(@NotNull File file) throws IOException {
		return file.getName().endsWith(".xml");
	}

	@Override
	public boolean canRead(@NotNull String dataString) {
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

	@NotNull
	@Override
	public String write(@NotNull XNode xnode, @NotNull QName rootElementName, SerializationContext serializationContext) throws SchemaException {
		DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry);
		RootXNode xroot = LexicalUtils.createRootXNode(xnode, rootElementName);
		Element element = serializer.serialize(xroot);
		return DOMUtil.serializeDOMToString(element);
	}

	@NotNull
	@Override
	public String write(@NotNull RootXNode xnode, SerializationContext serializationContext) throws SchemaException {
		DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry);
		Element element = serializer.serialize(xnode);
		return DOMUtil.serializeDOMToString(element);
	}

	@NotNull
	@Override
	public String write(@NotNull List<RootXNode> roots, @NotNull QName aggregateElementName,
			@Nullable SerializationContext context) throws SchemaException {
		Element aggregateElement = writeXRootListToElement(roots, aggregateElementName);
		return DOMUtil.serializeDOMToString(aggregateElement);
	}

	@NotNull
	public Element writeXRootListToElement(@NotNull List<RootXNode> roots, QName aggregateElementName) throws SchemaException {
		DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry);
		return serializer.serialize(roots, aggregateElementName);
	}

	public Element serializeUnderElement(XNode xnode, QName rootElementName, Element parentElement) throws SchemaException {
		DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry);
		RootXNode xroot = LexicalUtils.createRootXNode(xnode, rootElementName);
		return serializer.serializeUnderElement(xroot, parentElement);
	}

    public Element serializeXMapToElement(MapXNode xmap, QName elementName) throws SchemaException {
		DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry);
		return serializer.serializeToElement(xmap, elementName);
	}

	private Element serializeXPrimitiveToElement(PrimitiveXNode<?> xprim, QName elementName) throws SchemaException {
		DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry);
		return serializer.serializeXPrimitiveToElement(xprim, elementName);
	}

	@NotNull
	public Element writeXRootToElement(@NotNull RootXNode xroot) throws SchemaException {
		DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry);
		return serializer.serialize(xroot);
	}

	private Element serializeToElement(XNode xnode, QName elementName) throws SchemaException {
        Validate.notNull(xnode);
        Validate.notNull(elementName);
		if (xnode instanceof MapXNode) {
			return serializeXMapToElement((MapXNode) xnode, elementName);
		} else if (xnode instanceof PrimitiveXNode<?>) {
			return serializeXPrimitiveToElement((PrimitiveXNode<?>) xnode, elementName);
		} else if (xnode instanceof RootXNode) {
			return writeXRootToElement((RootXNode)xnode);
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
