/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.dom;

import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.SchemaXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.prism.xnode.IncompleteMarkerXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * @author semancik
 *
 */
public class DomLexicalWriter {

    private Document doc;
    private boolean serializeCompositeObjects = false;
    private SchemaRegistry schemaRegistry;

    DomLexicalWriter(SchemaRegistry schemaRegistry) {
        super();
        this.schemaRegistry = schemaRegistry;
    }

    public boolean isSerializeCompositeObjects() {
        return serializeCompositeObjects;
    }

    public void setSerializeCompositeObjects(boolean serializeCompositeObjects) {
        this.serializeCompositeObjects = serializeCompositeObjects;
    }

    private DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
        if (schemaRegistry == null) {
            return null;
        }
        return schemaRegistry.getNamespacePrefixMapper();
    }

    private void initialize() {
        doc = DOMUtil.getDocument();
    }

    private void initializeWithExistingDocument(Document document) {
        doc = document;
        document.getDocumentElement();
    }

    @NotNull
    public Element serialize(@NotNull RootXNodeImpl rootxnode) throws SchemaException {
        initialize();
        return serializeInternal(rootxnode, null);
    }

    public Element serialize(@NotNull List<RootXNodeImpl> roots, @Nullable QName aggregateElementName) throws SchemaException {
        initialize();
        if (aggregateElementName == null) {
            aggregateElementName = schemaRegistry.getPrismContext().getObjectsElementName();
            if (aggregateElementName == null) {
                throw new IllegalStateException("Couldn't serialize list of objects because the aggregated element name is not set");
            }
        }
        Element aggregateElement = createElement(aggregateElementName, null);
        for (RootXNodeImpl root : roots) {
            serializeInternal(root, aggregateElement);
        }
        return aggregateElement;
    }

    public Element serializeUnderElement(RootXNodeImpl rootxnode, Element parentElement) throws SchemaException {
        initializeWithExistingDocument(parentElement.getOwnerDocument());
        return serializeInternal(rootxnode, parentElement);
    }

    @NotNull
    private Element serializeInternal(@NotNull RootXNodeImpl rootxnode, Element parentElement) throws SchemaException {
        QName rootElementName = rootxnode.getRootElementName();
        Element topElement = createElement(rootElementName, parentElement);
        if (parentElement != null) {
            parentElement.appendChild(topElement);
        }
        QName typeQName = rootxnode.getTypeQName();
        if (typeQName != null && rootxnode.isExplicitTypeDeclaration()) {
            DOMUtil.setXsiType(topElement, setQNamePrefixExplicitIfNeeded(typeQName));
        }
        XNodeImpl subnode = rootxnode.getSubnode();
        if (subnode instanceof PrimitiveXNodeImpl) {
            serializePrimitiveElementOrAttribute((PrimitiveXNodeImpl) subnode, topElement, rootElementName, false);
            return DOMUtil.getFirstChildElement(topElement);
        } else {
            if (subnode instanceof MapXNodeImpl) {
                // at this point we can put frequently used namespaces (e.g. c, t, q, ri) into the document to eliminate their use
                // on many places inside the doc (MID-2198)
                DOMUtil.setNamespaceDeclarations(topElement, getNamespacePrefixMapper().getNamespacesDeclaredByDefault());
                serializeMap((MapXNodeImpl) subnode, topElement);
            } else if (subnode.isHeterogeneousList()) {
                DOMUtil.setNamespaceDeclarations(topElement, getNamespacePrefixMapper().getNamespacesDeclaredByDefault());
                serializeExplicitList((ListXNodeImpl) subnode, topElement);
            } else {
                throw new SchemaException(
                        "Sub-root xnode is not a map nor an explicit list, cannot serialize to XML (it is " + subnode + ")");
            }
            addDefaultNamespaceDeclaration(topElement);
            return topElement;
        }
    }

    /**
     * Adds xmlns='...common-3' if needed.
     * In fact, this is VERY ugly hack to avoid putting common-3 declarations all over the elements in bulk actions response.
     * e.getNamespaceURI returns something only if it was present during node creation.
     * So, in fact, this code is more than fragile. Seems to work with the current XNode->DOM serialization, though.
     */
    private void addDefaultNamespaceDeclaration(Element top) {
        List<Element> prefixless = DOMUtil.getElementsWithoutNamespacePrefix(top);
        if (prefixless.size() < 2) {
            return;        // nothing to do here
        }
        Set<String> namespaces = prefixless.stream().map(e -> emptyIfNull(e.getNamespaceURI())).collect(Collectors.toSet());
        if (namespaces.size() != 1 || StringUtils.isEmpty(namespaces.iterator().next())) {
            return;
        }
        DOMUtil.setNamespaceDeclaration(top, "", namespaces.iterator().next());
    }

    Element serializeToElement(MapXNodeImpl xmap, QName elementName) throws SchemaException {
        initialize();
        Element element = createElement(elementName, null);
        serializeMap(xmap, element);
        return element;
    }

    private void serializeMap(MapXNodeImpl xmap, Element topElement) throws SchemaException {
        for (Entry<QName, XNodeImpl> entry: xmap.entrySet()) {
            QName elementQName = entry.getKey();
            XNodeImpl xsubnode = entry.getValue();
            serializeSubnode(xsubnode, topElement, elementQName);
        }
    }

    private void serializeSubnode(XNodeImpl xsubnode, Element parentElement, QName elementName) throws SchemaException {
        if (xsubnode == null) {
            return;
        }
        if (xsubnode instanceof IncompleteMarkerXNode) {
            Element element = createElement(elementName, parentElement);
            DOMUtil.setAttributeValue(element, DOMUtil.IS_INCOMPLETE_ATTRIBUTE_NAME, "true");
            parentElement.appendChild(element);
        } else if (xsubnode instanceof RootXNodeImpl) {
            Element element = createElement(elementName, parentElement);
            appendCommentIfPresent(element, xsubnode);
            parentElement.appendChild(element);
            serializeSubnode(((RootXNodeImpl) xsubnode).getSubnode(), element, ((RootXNodeImpl) xsubnode).getRootElementName());
        } else if (xsubnode instanceof MapXNodeImpl) {
            Element element = createElement(elementName, parentElement);
            appendCommentIfPresent(element, xsubnode);
            if (xsubnode.isExplicitTypeDeclaration() && xsubnode.getTypeQName() != null) {
                DOMUtil.setXsiType(element, setQNamePrefixExplicitIfNeeded(xsubnode.getTypeQName()));
            }
            parentElement.appendChild(element);
            serializeMap((MapXNodeImpl)xsubnode, element);
        } else if (xsubnode instanceof PrimitiveXNodeImpl<?>) {
            PrimitiveXNodeImpl<?> xprim = (PrimitiveXNodeImpl<?>)xsubnode;
            if (xprim.isAttribute()) {
                serializePrimitiveElementOrAttribute(xprim, parentElement, elementName, true);
            } else {
                serializePrimitiveElementOrAttribute(xprim, parentElement, elementName, false);
            }
        } else if (xsubnode instanceof ListXNodeImpl) {
            ListXNodeImpl xlist = (ListXNodeImpl)xsubnode;
            if (xlist.isHeterogeneousList()) {
                Element element = createElement(elementName, parentElement);
                serializeExplicitList(xlist, element);
                parentElement.appendChild(element);
            } else {
                for (XNodeImpl xsubsubnode : xlist) {
                    serializeSubnode(xsubsubnode, parentElement, elementName);
                }
            }
        } else if (xsubnode instanceof SchemaXNodeImpl) {
            serializeSchema((SchemaXNodeImpl)xsubnode, parentElement);
        } else {
            throw new IllegalArgumentException("Unknown subnode "+xsubnode);
        }
    }

    private void serializeExplicitList(ListXNodeImpl list, Element parent) throws SchemaException {
        for (XNodeImpl node : list) {
            if (node.getElementName() == null) {
                throw new SchemaException("In a list, there are both nodes with element names and nodes without them: " + list);
            }
            serializeSubnode(node, parent, node.getElementName());
        }
        DOMUtil.setAttributeValue(parent, DOMUtil.IS_LIST_ATTRIBUTE_NAME, "true");
    }

    Element serializeXPrimitiveToElement(PrimitiveXNodeImpl<?> xprim, QName elementName) throws SchemaException {
        initialize();
        Element parent = DOMUtil.createElement(doc, new QName("fake","fake"));
        serializePrimitiveElementOrAttribute(xprim, parent, elementName, false);
        return DOMUtil.getFirstChildElement(parent);
    }

    private void serializePrimitiveElementOrAttribute(PrimitiveXNodeImpl<?> xprim, Element parentElement, QName elementOrAttributeName, boolean asAttribute) throws SchemaException {
        QName typeQName = xprim.getTypeQName();

        // if typeQName is not explicitly specified, we try to determine it from parsed value
        // TODO we should probably set typeQName when parsing the value...
        if (typeQName == null && xprim.isParsed()) {
            Object v = xprim.getValue();
            if (v != null) {
                typeQName = XsdTypeMapper.toXsdType(v.getClass());
            }
        }

        if (typeQName == null) {    // this means that either xprim is unparsed or it is empty
            if (PrismContextImpl.isAllowSchemalessSerialization()) {
                // We cannot correctly serialize without a type. But this is needed
                // sometimes. So just default to string
                String stringValue = xprim.getStringValue();
                if (stringValue != null) {
                    if (asAttribute) {
                        DOMUtil.setAttributeValue(parentElement, elementOrAttributeName.getLocalPart(), stringValue);
                        DOMUtil.setNamespaceDeclarations(parentElement, xprim.getRelevantNamespaceDeclarations());
                    } else {
                        Element element;
                        try {
                            element = createElement(elementOrAttributeName, parentElement);
                            appendCommentIfPresent(element, xprim);
                        } catch (DOMException e) {
                            throw new DOMException(e.code, e.getMessage() + "; creating element "+elementOrAttributeName+" in element "+DOMUtil.getQName(parentElement));
                        }
                        parentElement.appendChild(element);
                        DOMUtil.setElementTextContent(element, stringValue);
                        DOMUtil.setNamespaceDeclarations(element, xprim.getRelevantNamespaceDeclarations());
                    }
                }
                return;
            } else {
                throw new IllegalStateException("No type for primitive element "+elementOrAttributeName+", cannot serialize (schemaless serialization is disabled)");
            }
        }

        // typeName != null after this point

        if (QNameUtil.isUnqualified(typeQName)) {
            typeQName = XsdTypeMapper.determineQNameWithNs(typeQName);
        }

        Element element = null;

        if (ItemPathType.COMPLEX_TYPE.equals(typeQName)) {
            //ItemPathType itemPathType = //ItemPathType.asItemPathType(xprim.getValue());            // TODO fix this hack
            ItemPathType itemPathType = (ItemPathType) xprim.getValue();
            if (itemPathType != null) {
                if (asAttribute) {
                    throw new UnsupportedOperationException("Serializing ItemPath as an attribute is not supported yet");
                }
                element = serializeItemPathTypeToElement(itemPathType, elementOrAttributeName, parentElement.getOwnerDocument());
                parentElement.appendChild(element);
            }

        } else {
            // not an ItemPathType

            if (!asAttribute) {
                try {
                    element = createElement(elementOrAttributeName, parentElement);
                } catch (DOMException e) {
                    throw new DOMException(e.code, e.getMessage() + "; creating element "+elementOrAttributeName+" in element "+DOMUtil.getQName(parentElement));
                }
                appendCommentIfPresent(element, xprim);
                parentElement.appendChild(element);
            }

            if (DOMUtil.XSD_QNAME.equals(typeQName)) {
                QName value = (QName) xprim.getParsedValueWithoutRecording(DOMUtil.XSD_QNAME);
                value = setQNamePrefixExplicitIfNeeded(value);
                if (asAttribute) {
                    try {
                        DOMUtil.setQNameAttribute(parentElement, elementOrAttributeName.getLocalPart(), value);
                    } catch (DOMException e) {
                        throw new DOMException(e.code, e.getMessage() + "; setting attribute "+elementOrAttributeName.getLocalPart()+" in element "+DOMUtil.getQName(parentElement)+" to QName value "+value);
                    }
                } else {
                    DOMUtil.setQNameValue(element, value);
                }
            } else {
                // not ItemType nor QName
                String value = xprim.getGuessedFormattedValue();

                if (asAttribute) {
                    DOMUtil.setAttributeValue(parentElement, elementOrAttributeName.getLocalPart(), value);
                } else {
                    DOMUtil.setElementTextContent(element, value);
                }
            }

        }
        if (!asAttribute && xprim.isExplicitTypeDeclaration()) {
            DOMUtil.setXsiType(element, setQNamePrefixExplicitIfNeeded(typeQName));
        }
    }

    private void appendCommentIfPresent(Element element, XNodeImpl xnode) {
        String text = xnode.getComment();
        if (StringUtils.isNotEmpty(text)) {
            DOMUtil.createComment(element, text);
        }
    }

    private void serializeSchema(SchemaXNodeImpl xschema, Element parentElement) {
        Element schemaElement = xschema.getSchemaElement();
        if (schemaElement == null){
            return;
        }
        Element clonedSchema = (Element) schemaElement.cloneNode(true);
        doc.adoptNode(clonedSchema);
        parentElement.appendChild(clonedSchema);
    }

    /**
     * Create XML element with the correct namespace prefix and namespace definition.
     * @param qname element QName
     * @return created DOM element
     */
    @NotNull
    private Element createElement(QName qname, Element parentElement) {
        String namespaceURI = qname.getNamespaceURI();
        if (!StringUtils.isBlank(namespaceURI)) {
            qname = setQNamePrefix(qname);
        }
        if (parentElement != null) {
            return DOMUtil.createElement(doc, qname, parentElement, parentElement);
        } else {
            // This is needed otherwise the root element itself could not be created
            // Caller of this method is responsible for setting the topElement
            return DOMUtil.createElement(doc, qname);
        }
    }

    private QName setQNamePrefix(QName qname) {
        DynamicNamespacePrefixMapper namespacePrefixMapper = getNamespacePrefixMapper();
        if (namespacePrefixMapper == null) {
            return qname;
        }
        return namespacePrefixMapper.setQNamePrefix(qname);
    }

    private QName setQNamePrefixExplicitIfNeeded(QName name) {
        if (name != null && StringUtils.isNotBlank(name.getNamespaceURI()) && StringUtils.isBlank(name.getPrefix())) {
            return setQNamePrefixExplicit(name);
        } else {
            return name;
        }
    }

    private QName setQNamePrefixExplicit(QName qname) {
        DynamicNamespacePrefixMapper namespacePrefixMapper = getNamespacePrefixMapper();
        if (namespacePrefixMapper == null) {
            return qname;
        }
        return namespacePrefixMapper.setQNamePrefixExplicit(qname);
    }

    public Element serializeItemPathTypeToElement(ItemPathType itemPathType, QName elementName, Document ownerDocument) {
        return ItemPathHolder.serializeToElement(itemPathType.getItemPath(), elementName, ownerDocument);
    }

}
