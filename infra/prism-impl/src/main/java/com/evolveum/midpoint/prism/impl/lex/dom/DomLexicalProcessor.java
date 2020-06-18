/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.dom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.staxmate.dom.DOMConverter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.ParserElementSource;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.LexicalUtils;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class DomLexicalProcessor implements LexicalProcessor<String> {

    public static final Trace LOGGER = TraceManager.getTrace(DomLexicalProcessor.class);

    private static final QName SCHEMA_ELEMENT_QNAME = DOMUtil.XSD_SCHEMA_ELEMENT;

    @NotNull private final SchemaRegistry schemaRegistry;

    public DomLexicalProcessor(@NotNull SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @NotNull
    @Override
    public RootXNodeImpl read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
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
    public List<RootXNodeImpl> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
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

    private XMLInputFactory getXMLInputFactory() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        // TODO: cache? static? prism context?
        return xmlInputFactory;
    }

    // code taken from Validator class
    @Override
    public void readObjectsIteratively(@NotNull ParserSource source,
            @NotNull ParsingContext parsingContext, RootXNodeHandler handler)
            throws SchemaException, IOException {

        InputStream is = source.getInputStream();
        XMLStreamReader stream = null;
        try {
            stream = getXMLInputFactory().createXMLStreamReader(is);

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
            throw new SchemaException(
                    "Exception while parsing XML" + lineInfo + ": " + ex.getMessage(), ex);
        } finally {
            if (source.closeStreamAfterParsing()) {
                IOUtils.closeQuietly(is);
            }
        }
    }

    private boolean readSingleObjectIteratively(
            XMLStreamReader stream, Map<String, String> rootNamespaceDeclarations,
            DOMConverter domConverter, RootXNodeHandler handler)
            throws XMLStreamException, SchemaException {
        Document objectDoc = domConverter.buildDocument(stream);
        Element objectElement = DOMUtil.getFirstChildElement(objectDoc);
        DOMUtil.setNamespaceDeclarations(objectElement, rootNamespaceDeclarations);
        RootXNodeImpl rootNode = read(objectElement);
        return handler.handleData(rootNode);
    }

    private List<RootXNodeImpl> readObjects(Document document) throws SchemaException {
        Element root = DOMUtil.getFirstChildElement(document);
        QName objectsMarker = schemaRegistry.getPrismContext().getObjectsElementName();
        if (objectsMarker != null && !QNameUtil.match(DOMUtil.getQName(root), objectsMarker)) {
            return Collections.singletonList(read(root));
        } else {
            List<RootXNodeImpl> rv = new ArrayList<>();
            for (Element child : DOMUtil.listChildElements(root)) {
                rv.add(read(child));
            }
            return rv;
        }
    }

    @NotNull
    public RootXNodeImpl read(Document document) throws SchemaException {
        Element rootElement = DOMUtil.getFirstChildElement(document);
        return read(rootElement);
    }

    @NotNull
    public RootXNodeImpl read(Element rootElement) throws SchemaException {
        QName rootElementName = DOMUtil.getQName(rootElement);
        QName rootElementXsiType = DOMUtil.resolveXsiType(rootElement);

        RootXNodeImpl xroot = new RootXNodeImpl(rootElementName);
        extractCommonMetadata(rootElement, rootElementXsiType, xroot);
        XNodeImpl xnode = parseElementContent(rootElement, rootElementName, false);
        xroot.setSubnode(xnode);
        return xroot;
    }

    private void extractCommonMetadata(Element element, QName xsiType, XNodeImpl xnode)
            throws SchemaException {

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
            return Integer.parseInt(maxOccursString);
        } else {
            throw new SchemaException("Expected numeric value for " + PrismConstants.A_MAX_OCCURS.getLocalPart()
                    + " attribute on " + DOMUtil.getQName(element) + " but got " + maxOccursString);
        }
    }

    /**
     * Parses the content of the element.
     *
     * @param knownElementName Pre-fetched element name. Might be null (this is expected if storeElementName is true).
     */
    @NotNull
    private XNodeImpl parseElementContent(@NotNull Element element, QName knownElementName, boolean storeElementName) throws SchemaException {
        XNodeImpl node;

        QName xsiType = DOMUtil.resolveXsiType(element);
        QName elementName = knownElementName != null ? knownElementName : DOMUtil.getQName(element);

        if (DOMUtil.hasChildElements(element) || DOMUtil.hasApplicationAttributes(element)) {
            if (isList(element, elementName, xsiType)) {
                node = parseElementContentToList(element);
            } else {
                node = parseElementContentToMap(element);
            }
        } else if (DOMUtil.isMarkedAsIncomplete(element)) {
            // Note that it is of no use to check for "incomplete" on non-leaf elements. In XML the incomplete attribute
            // must be attached to an empty element.
            node = new IncompleteMarkerXNodeImpl();
        } else {
            node = parsePrimitiveElement(element);
        }
        if (storeElementName) {
            node.setElementName(elementName);
        }
        extractCommonMetadata(element, xsiType, node);
        return node;
    }

    // all the sub-elements should be compatible (this is not enforced here, however)
    private ListXNodeImpl parseElementContentToList(Element element) throws SchemaException {
        if (DOMUtil.hasApplicationAttributes(element)) {
            throw new SchemaException("List should have no application attributes: " + element);
        }
        return parseElementList(DOMUtil.listChildElements(element), null, true);
    }

    private MapXNodeImpl parseElementContentToMap(Element element) throws SchemaException {
        MapXNodeImpl xmap = new MapXNodeImpl();

        // Attributes
        for (Attr attr : DOMUtil.listApplicationAttributes(element)) {
            QName attrQName = DOMUtil.getQName(attr);
            XNodeImpl subnode = parseAttributeValue(attr);
            xmap.put(attrQName, subnode);
        }

        // Sub-elements
        QName lastElementName = null;
        List<Element> lastElements = null;
        for (Element childElement : DOMUtil.listChildElements(element)) {
            QName childName = DOMUtil.getQName(childElement);
            if (!match(childName, lastElementName)) {
                parseSubElementsGroupAsMapEntry(xmap, lastElementName, lastElements);
                lastElementName = childName;
                lastElements = new ArrayList<>();
            }
            lastElements.add(childElement);
        }
        parseSubElementsGroupAsMapEntry(xmap, lastElementName, lastElements);
        return xmap;
    }

    private boolean isList(@NotNull Element element, @NotNull QName elementName, @Nullable QName xsiType) {
        String isListAttribute = DOMUtil.getAttribute(element, DOMUtil.IS_LIST_ATTRIBUTE_NAME);
        if (StringUtils.isNotEmpty(isListAttribute)) {
            return Boolean.parseBoolean(isListAttribute);
        }
        // enable this after schema registry is optional (now it's mandatory)
//        if (schemaRegistry == null) {
//            return false;
//        }

        SchemaRegistry.IsList fromSchema = schemaRegistry.isList(xsiType, elementName);
        if (fromSchema != SchemaRegistry.IsList.MAYBE) {
            return fromSchema == SchemaRegistry.IsList.YES;
        }

        // checking the content
        if (DOMUtil.hasApplicationAttributes(element)) {
            return false;        // TODO - or should we fail in this case?
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
        ItemDefinition<?> def = schemaRegistry.findItemDefinitionByElementName(name);
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

    // All elements share the same elementName
    private void parseSubElementsGroupAsMapEntry(MapXNodeImpl xmap, QName elementName, List<Element> elements) throws SchemaException {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        XNodeImpl xsub;
        // We really want to have equals here, not match
        // we want to be very explicit about namespace here
        if (elementName.equals(SCHEMA_ELEMENT_QNAME)) {
            if (elements.size() == 1) {
                xsub = parseSchemaElement(elements.iterator().next());
            } else {
                throw new SchemaException("Too many schema elements");
            }
        } else if (elements.size() == 1) {
            xsub = parseElementContent(elements.get(0), elementName, false);
        } else {
            xsub = parseElementList(elements, elementName, false);
        }
        xmap.merge(elementName, xsub);
    }

    /**
     * Parses elements that should form the list.
     * <p>
     * Either they have the same element name, or they are stored as a sub-elements of "list" parent element.
     */
    @NotNull
    @Contract("!null, null, false -> fail")
    private ListXNodeImpl parseElementList(List<Element> elements, QName elementName, boolean storeElementNames) throws SchemaException {
        if (!storeElementNames && elementName == null) {
            throw new IllegalArgumentException("When !storeElementNames the element name must be specified");
        }
        ListXNodeImpl xlist = new ListXNodeImpl();
        for (Element element : elements) {
            xlist.add(parseElementContent(element, elementName, storeElementNames));
        }
        return xlist;
    }

    // @pre element has no children nor application attributes
    private <T> PrimitiveXNodeImpl<T> parsePrimitiveElement(@NotNull Element element) {
        PrimitiveXNodeImpl<T> xnode = new PrimitiveXNodeImpl<>();
        xnode.setValueParser(new ElementValueParser<>(element));
        return xnode;
    }

    static <T> T processIllegalArgumentException(String value, QName typeName, IllegalArgumentException e,
            XNodeProcessorEvaluationMode mode) {
        if (mode == XNodeProcessorEvaluationMode.COMPAT) {
            LOGGER.warn("Value of '{}' couldn't be parsed as '{}' -- interpreting as null because of COMPAT mode set", value,
                    typeName, e);
            return null;
        } else {
            throw e;
        }
    }

    private <T> PrimitiveXNodeImpl<T> parseAttributeValue(@NotNull Attr attr) {
        PrimitiveXNodeImpl<T> xnode = new PrimitiveXNodeImpl<>();
        xnode.setValueParser(new AttributeValueParser<>(attr));
        xnode.setAttribute(true);
        return xnode;
    }

    @NotNull
    private SchemaXNodeImpl parseSchemaElement(Element schemaElement) {
        SchemaXNodeImpl xschema = new SchemaXNodeImpl();
        xschema.setSchemaElement(schemaElement);
        return xschema;
    }

    @Override
    public boolean canRead(@NotNull File file) {
        return file.getName().endsWith(".xml");
    }

    private static final Pattern XML_DETECTION_PATTERN = Pattern.compile("\\A\\s*<\\w+");

    @Override
    public boolean canRead(@NotNull String dataString) {
        return dataString.charAt(0) == '<'
                || XML_DETECTION_PATTERN.matcher(dataString).find();
    }

    @NotNull
    @Override
    public String write(@NotNull XNode xnode, @NotNull QName rootElementName, SerializationContext serializationContext) throws SchemaException {
        DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry, serializationContext);
        RootXNodeImpl xroot = LexicalUtils.createRootXNode((XNodeImpl) xnode, rootElementName);
        Element element = serializer.serialize(xroot);
        return DOMUtil.serializeDOMToString(element);
    }

    @NotNull
    @Override
    public String write(@NotNull RootXNode xnode, SerializationContext serializationContext) throws SchemaException {
        DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry, serializationContext);
        Element element = serializer.serialize((RootXNodeImpl) xnode);
        return DOMUtil.serializeDOMToString(element);
    }

    @NotNull
    @Override
    public String write(@NotNull List<RootXNodeImpl> roots, @Nullable QName aggregateElementName,
            @Nullable SerializationContext context) throws SchemaException {
        Element aggregateElement = writeXRootListToElement(roots, aggregateElementName);
        return DOMUtil.serializeDOMToString(aggregateElement);
    }

    @NotNull
    public Element writeXRootListToElement(@NotNull List<RootXNodeImpl> roots, QName aggregateElementName) throws SchemaException {
        DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry, null);
        return serializer.serialize(roots, aggregateElementName);
    }

    public Element serializeXMapToElement(MapXNodeImpl xmap, QName elementName) throws SchemaException {
        DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry, null);
        return serializer.serializeToElement(xmap, elementName);
    }

    private Element serializeXPrimitiveToElement(PrimitiveXNodeImpl<?> xprim, QName elementName) throws SchemaException {
        DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry, null);
        return serializer.serializeXPrimitiveToElement(xprim, elementName);
    }

    @NotNull
    public Element writeXRootToElement(@NotNull RootXNodeImpl xroot) throws SchemaException {
        DomLexicalWriter serializer = new DomLexicalWriter(schemaRegistry, null);
        return serializer.serialize(xroot);
    }

    private Element serializeToElement(XNodeImpl xnode, QName elementName) throws SchemaException {
        Validate.notNull(xnode);
        Validate.notNull(elementName);
        if (xnode instanceof MapXNodeImpl) {
            return serializeXMapToElement((MapXNodeImpl) xnode, elementName);
        } else if (xnode instanceof PrimitiveXNodeImpl<?>) {
            return serializeXPrimitiveToElement((PrimitiveXNodeImpl<?>) xnode, elementName);
        } else if (xnode instanceof RootXNodeImpl) {
            return writeXRootToElement((RootXNodeImpl) xnode);
        } else if (xnode instanceof ListXNodeImpl) {
            ListXNodeImpl xlist = (ListXNodeImpl) xnode;
            if (xlist.size() == 0) {
                return null;
            } else if (xlist.size() > 1) {
                throw new IllegalArgumentException("Cannot serialize list xnode with more than one item: " + xlist);
            } else {
                return serializeToElement(xlist.get(0), elementName);
            }
        } else {
            throw new IllegalArgumentException("Cannot serialize " + xnode + " to element");
        }
    }

    public Element serializeSingleElementMapToElement(MapXNode map) throws SchemaException {
        MapXNodeImpl xmap = (MapXNodeImpl) map;
        if (xmap == null || xmap.isEmpty()) {
            return null;
        }
        Entry<QName, XNodeImpl> subEntry = xmap.getSingleSubEntry(xmap.toString());
        Element parent = serializeToElement(xmap, subEntry.getKey());
        return DOMUtil.getFirstChildElement(parent);
    }
}
