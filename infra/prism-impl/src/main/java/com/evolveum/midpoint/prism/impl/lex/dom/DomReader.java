/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.dom;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.impl.xnode.*;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.MetadataAware;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO
 */
class DomReader {

    public static final Trace LOGGER = TraceManager.getTrace(DomLexicalProcessor.class);

    private static final QName SCHEMA_ELEMENT_QNAME = DOMUtil.XSD_SCHEMA_ELEMENT;
    static final String VALUE_LOCAL_PART = "_value";
    static final String METADATA_LOCAL_PART = "_metadata";

    private final Element root;
    private final QName rootElementName;
    private final SchemaRegistry schemaRegistry;

    @NotNull private final QName valueElementName;
    @NotNull private final QName metadataElementName;

    DomReader(@NotNull Element root, SchemaRegistry schemaRegistry) {
        this.root = root;
        this.rootElementName = DOMUtil.getQName(root);
        this.schemaRegistry = schemaRegistry;
        this.valueElementName = new QName(schemaRegistry.getDefaultNamespace(), VALUE_LOCAL_PART);
        this.metadataElementName = new QName(schemaRegistry.getDefaultNamespace(), DomReader.METADATA_LOCAL_PART);
    }

    DomReader(Document document, SchemaRegistry schemaRegistry) {
        this(DOMUtil.getFirstChildElement(document), schemaRegistry);
    }

    @NotNull
    List<RootXNodeImpl> readObjects() throws SchemaException {
        if (rootIsNotObjectsMarker()) {
            return Collections.singletonList(read());
        } else {
            List<RootXNodeImpl> rv = new ArrayList<>();
            for (Element child : DOMUtil.listChildElements(root)) {
                rv.add(new DomReader(child, schemaRegistry).read());
            }
            return rv;
        }
    }

    @NotNull RootXNodeImpl read() throws SchemaException {
        RootXNodeImpl xroot = new RootXNodeImpl(rootElementName);
        XNodeImpl xnode = readElementContent(root, rootElementName, false);
        xroot.setSubnode(xnode);
        return xroot;
    }

    private boolean rootIsNotObjectsMarker() {
        QName objectsMarker = schemaRegistry.getPrismContext().getObjectsElementName();
        return objectsMarker != null && !QNameUtil.match(rootElementName, objectsMarker);
    }

    /**
     * Reads the content of the element.
     *
     * @param knownElementName Pre-fetched element name. Might be null (this is expected if storeElementName is true).
     */
    @NotNull
    private XNodeImpl readElementContent(@NotNull Element element, QName knownElementName, boolean storeElementName) throws SchemaException {
        XNodeImpl node;

        QName xsiType = DOMUtil.resolveXsiType(element);
        QName elementName = knownElementName != null ? knownElementName : DOMUtil.getQName(element);

        Element valueChild = DOMUtil.getMatchingChildElement(element, valueElementName);
        if (valueChild != null) {
            node = readElementContent(valueChild, elementName, false);
        } else if (DOMUtil.hasChildElements(element) || DOMUtil.hasApplicationAttributes(element)) {
            if (isList(element, elementName, xsiType)) {
                node = readElementContentToList(element);
            } else {
                node = readElementContentToMap(element);
            }
        } else if (DOMUtil.isMarkedAsIncomplete(element)) {
            // Note that it is of no use to check for "incomplete" on non-leaf elements. In XML the incomplete attribute
            // must be attached to an empty element.
            node = new IncompleteMarkerXNodeImpl();
        } else {
            node = parsePrimitiveElement(element);
        }
        readMetadata(element, node);
        readMaxOccurs(element, node);

        setTypeAndElementName(xsiType, elementName, node, storeElementName);
        return node;
    }

    private void readMetadata(@NotNull Element element, XNodeImpl node) throws SchemaException {
        Element metadataChild = DOMUtil.getMatchingChildElement(element, metadataElementName);
        if (metadataChild != null) {
            XNodeImpl metadata = readElementContent(metadataChild, null, false);
            if (metadata instanceof MapXNode) {
                if (node instanceof MetadataAware) {
                    ((MetadataAware) node).setMetadataNode((MapXNode) metadata);
                } else {
                    throw new SchemaException("Attempt to add metadata to non-metadata-aware XNode: " + node);
                }
            } else {
                throw new SchemaException("Metadata is not of Map type: " + metadata);
            }
        }
    }

    private void readMaxOccurs(Element element, XNodeImpl xnode) throws SchemaException {
        String maxOccursString = element.getAttributeNS(
                PrismConstants.A_MAX_OCCURS.getNamespaceURI(),
                PrismConstants.A_MAX_OCCURS.getLocalPart());
        if (!StringUtils.isBlank(maxOccursString)) {
            int maxOccurs = parseMultiplicity(maxOccursString, element);
            xnode.setMaxOccurs(maxOccurs);
        }
    }

    private int parseMultiplicity(String maxOccursString, Element element) throws SchemaException {
        if (PrismConstants.MULTIPLICITY_UNBOUNDED.equals(maxOccursString)) {
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

    private void setTypeAndElementName(QName xsiType, QName elementName, XNodeImpl xnode, boolean storeElementName) {
        if (xsiType != null) {
            xnode.setTypeQName(xsiType);
            xnode.setExplicitTypeDeclaration(true);
        }
        if (storeElementName) {
            xnode.setElementName(elementName);
        }
    }

    // all the sub-elements should be compatible (this is not enforced here, however)
    private ListXNodeImpl readElementContentToList(Element element) throws SchemaException {
        if (DOMUtil.hasApplicationAttributes(element)) {
            throw new SchemaException("List should have no application attributes: " + element);
        }
        return parseElementList(DOMUtil.listChildElements(element), null, true);
    }

    private MapXNodeImpl readElementContentToMap(Element element) throws SchemaException {
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
            if (QNameUtil.match(childName, metadataElementName)) {
                continue;
            }
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
            xsub = readElementContent(elements.get(0), elementName, false);
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
            xlist.add(readElementContent(element, elementName, storeElementNames));
        }
        return xlist;
    }

    // @pre element has no children nor application attributes
    private <T> PrimitiveXNodeImpl<T> parsePrimitiveElement(@NotNull Element element) {
        PrimitiveXNodeImpl<T> xnode = new PrimitiveXNodeImpl<>();
        xnode.setValueParser(new ElementValueParser<>(element));
        return xnode;
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
}
