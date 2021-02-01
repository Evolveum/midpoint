/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.dom;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathSerialization;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.SchemaXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.xnode.IncompleteMarkerXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.prism.impl.lex.dom.DomReader.VALUE_LOCAL_PART;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * @author semancik
 *
 */
class DomWriter {

    @NotNull private final Document document;
    @NotNull private final SchemaRegistry schemaRegistry;
    private final SerializationOptions serializationOptions;

    @NotNull private final QName valueElementName;
    @NotNull private final QName metadataElementName;

    DomWriter(@NotNull SchemaRegistry schemaRegistry, SerializationContext context) {
        this.document = DOMUtil.getDocument();
        this.schemaRegistry = schemaRegistry;
        this.serializationOptions = context != null ? context.getOptions() : null;
        this.valueElementName = new QName(schemaRegistry.getDefaultNamespace(), VALUE_LOCAL_PART);
        this.metadataElementName = new QName(schemaRegistry.getDefaultNamespace(), DomReader.METADATA_LOCAL_PART);
    }

    @NotNull
    Element writeRoot(@NotNull RootXNode root) throws SchemaException {
        return writeRootInternal((RootXNodeImpl) root, null);
    }

    Element writeRoots(@NotNull List<RootXNodeImpl> roots) throws SchemaException {
        QName aggregateElementName = schemaRegistry.getPrismContext().getObjectsElementName();
        if (aggregateElementName == null) {
            throw new IllegalStateException("Couldn't serialize list of objects because the aggregated element name is not set");
        }
        Element aggregateElement = createElement(aggregateElementName, null);
        for (RootXNodeImpl root : roots) {
            writeRootInternal(root, aggregateElement);
        }
        return aggregateElement;
    }

    /**
     * Seems to be used in strange circumstances (called from various hacks).
     * To be reconsidered eventually. Avoid using in new code.
     */
    @Deprecated
    Element writeMap(MapXNodeImpl xmap, QName elementName) throws SchemaException {
        Element element = createElement(elementName, null);
        writeMap(xmap, element);
        return element;
    }

    /**
     * TODO There is an overlap in functionality between this method and writeNode.
     *  We should reconcile that somehow eventually.
     *  But beware, there are also significant differences, e.g.
     *   - we set "global" namespace declarations and default namespace declaration here
     *   - when serializing primitives we insist on element representation (not attribute one)
     */
    @NotNull
    private Element writeRootInternal(@NotNull RootXNodeImpl root, Element parentElement) throws SchemaException {
        QName rootElementName = root.getRootElementName();
        Element rootElement = createAndAppendChild(rootElementName, parentElement);

        XNodeImpl subnode = root.getSubnode();
        if (subnode instanceof PrimitiveXNodeImpl) {
            writePrimitive((PrimitiveXNodeImpl) subnode, rootElement, rootElementName, false);
            return DOMUtil.getFirstChildElement(rootElement);
        } else {
            // At this point we can put frequently used namespaces (e.g. c, t, q, ri) into the document to eliminate their use
            // on many places inside the doc (MID-2198)
            DOMUtil.setNamespaceDeclarations(rootElement, getNamespacePrefixMapper().getNamespacesDeclaredByDefault());
            if (subnode instanceof MapXNodeImpl) {
                writeMap((MapXNodeImpl) subnode, rootElement);
            } else if (subnode.isHeterogeneousList()) {
                writeHeterogeneousList((ListXNodeImpl) subnode, rootElement);
            } else {
                throw new SchemaException(
                        "Sub-root xnode is not a map nor an explicit list, cannot serialize to XML (it is " + subnode + ")");
            }
            addDefaultNamespaceDeclaration(rootElement);
            return rootElement;
        }
    }

    /**
     * Adds xmlns='...common-3' if needed.
     * In fact, this is VERY ugly hack to avoid putting common-3 declarations all over the elements in bulk actions response.
     * e.getNamespaceURI returns something only if it was present during node creation.
     * So, in fact, this code is more than fragile. Seems to work with the current XNode->DOM serialization, though.
     */
    private void addDefaultNamespaceDeclaration(Element top) {
        List<Element> prefixLess = DOMUtil.getElementsWithoutNamespacePrefix(top);
        if (prefixLess.size() < 2) {
            return; // nothing to do here
        }
        Set<String> namespaces = prefixLess.stream()
                .map(e -> emptyIfNull(e.getNamespaceURI()))
                .collect(Collectors.toSet());
        if (namespaces.size() == 1) {
            String defaultNamespace = namespaces.iterator().next();
            if (StringUtils.isNotEmpty(defaultNamespace)) {
                DOMUtil.setNamespaceDeclaration(top, "", defaultNamespace);
            }
        }
    }

    private void writeMap(MapXNodeImpl xmap, Element parent) throws SchemaException {
        writeMetadata(xmap.getMetadataNodes(), parent);
        for (Entry<QName, XNodeImpl> entry: xmap.entrySet()) {
            QName elementQName = entry.getKey();
            XNodeImpl xsubnode = entry.getValue();
            writeNode(xsubnode, parent, elementQName);
        }
        setXsiTypeIfNeeded(xmap, parent);
    }

    private void writeNode(XNodeImpl node, Element parentElement, QName elementName) throws SchemaException {
        if (node == null) {
            return;
        }
        if (node instanceof IncompleteMarkerXNode) {
            Element child = createAndAppendChild(elementName, parentElement);
            DOMUtil.setAttributeValue(child, DOMUtil.IS_INCOMPLETE_ATTRIBUTE_NAME, "true");
        } else if (node instanceof RootXNodeImpl) {
            throw new IllegalStateException("Shouldn't be here!");
            // TODO remove eventually
//            Element child = createAndAppendChild(elementName, parentElement);
//            appendCommentIfPresent(child, node);
//            writeNode(((RootXNodeImpl) node).getSubnode(), child, ((RootXNodeImpl) node).getRootElementName());
        } else if (node instanceof MapXNodeImpl) {
            Element child = createAndAppendChild(elementName, parentElement);
            appendCommentIfPresent(child, node);
            writeMap((MapXNodeImpl)node, child);
        } else if (node instanceof PrimitiveXNodeImpl<?>) {
            PrimitiveXNodeImpl<?> xprim = (PrimitiveXNodeImpl<?>)node;
            writePrimitive(xprim, parentElement, elementName, xprim.isAttribute());
        } else if (node instanceof ListXNodeImpl) {
            ListXNodeImpl list = (ListXNodeImpl) node;
            if (list.isHeterogeneousList()) {
                Element child = createAndAppendChild(elementName, parentElement);
                writeHeterogeneousList(list, child);
            } else {
                for (XNodeImpl listItem : list) {
                    writeNode(listItem, parentElement, elementName);
                }
            }
        } else if (node instanceof SchemaXNodeImpl) {
            writeSchema((SchemaXNodeImpl)node, parentElement);
        } else {
            throw new IllegalArgumentException("Unknown subnode "+node);
        }
    }

    private void writeHeterogeneousList(ListXNodeImpl list, Element parent) throws SchemaException {
        for (XNodeImpl listItem : list) {
            if (listItem.getElementName() == null) {
                throw new SchemaException("In a list, there are both nodes with element names and nodes without them: " + list);
            }
            writeNode(listItem, parent, listItem.getElementName());
        }
        DOMUtil.setAttributeValue(parent, DOMUtil.IS_LIST_ATTRIBUTE_NAME, "true");
        setXsiTypeIfNeeded(list, parent);
    }

    private void writePrimitive(PrimitiveXNodeImpl<?> xprim, Element parentElement,
            QName elementOrAttributeName, boolean asAttribute) throws SchemaException {
        if (xprim.hasMetadata()) {
            Element valuePlusMetadataElement = createAndAppendChild(elementOrAttributeName, parentElement);
            writePrimitiveValue(xprim, valuePlusMetadataElement, valueElementName, false);
            writeMetadata(xprim.getMetadataNodes(), valuePlusMetadataElement);
        } else {
            writePrimitiveValue(xprim, parentElement, elementOrAttributeName, asAttribute);
        }
    }

    private void writeMetadata(List<MapXNode> metadataNodes, Element parentElement) throws SchemaException {
        for (MapXNode metadataNode : metadataNodes) {
            Element metadataElement = createAndAppendChild(metadataElementName, parentElement);
            writeMap((MapXNodeImpl) metadataNode, metadataElement);
        }
    }

    private void writePrimitiveValue(PrimitiveXNodeImpl<?> xprim, Element parentElement, QName elementOrAttributeName,
            boolean asAttribute) throws SchemaException {
        QName typeName = determineTypeName(xprim);

        if (typeName == null) { // this means that either xprim is unparsed or it is empty
            writePrimitiveWithoutType(xprim, parentElement, elementOrAttributeName, asAttribute);
        } else {
            writePrimitiveWithType(xprim, parentElement, elementOrAttributeName, asAttribute, typeName);
        }
    }

    private void writePrimitiveWithoutType(PrimitiveXNodeImpl<?> xprim, Element parentElement, QName elementOrAttributeName,
            boolean asAttribute) {
        if (!PrismContextImpl.isAllowSchemalessSerialization()) {
            throw new IllegalStateException("No type for primitive element "+elementOrAttributeName+", cannot serialize (schemaless serialization is disabled)");
        }

        // We cannot correctly serialize without a type. But this is needed sometimes. So just default to string.
        String stringValue = xprim.getStringValue();
        if (stringValue != null) {
            if (asAttribute) {
                DOMUtil.setAttributeValue(parentElement, elementOrAttributeName.getLocalPart(), stringValue);
                DOMUtil.setNamespaceDeclarations(parentElement, xprim.getRelevantNamespaceDeclarations());
            } else {
                Element element = createAndAppendChild(elementOrAttributeName, parentElement);
                appendCommentIfPresent(element, xprim);
                DOMUtil.setElementTextContent(element, stringValue);
                DOMUtil.setNamespaceDeclarations(element, xprim.getRelevantNamespaceDeclarations());
            }
        }
    }

    private void writePrimitiveWithType(PrimitiveXNodeImpl<?> xprim, Element parentElement, QName elementOrAttributeName,
            boolean asAttribute, QName typeName) throws SchemaException {
        Element element;
        // Note that item paths and QNames are special because of prefixes.
        if (ItemPathType.COMPLEX_TYPE.equals(typeName)) {
            if (asAttribute) {
                throw new UnsupportedOperationException("Serializing ItemPath as an attribute is not supported yet");
            } else {
                element = writeItemPath(xprim, parentElement, elementOrAttributeName);
            }
        } else {
            if (!asAttribute) {
                element = createAndAppendChild(elementOrAttributeName, parentElement);
                appendCommentIfPresent(element, xprim);
            } else {
                element = null;
            }

            if (DOMUtil.XSD_QNAME.equals(typeName)) {
                QName value = (QName) xprim.getParsedValueWithoutRecording(DOMUtil.XSD_QNAME);
                QName valueWithPrefix = setQNamePrefixExplicitIfNeeded(value);
                if (asAttribute) {
                    setQNameAttribute(parentElement, elementOrAttributeName.getLocalPart(), valueWithPrefix);
                } else {
                    DOMUtil.setQNameValue(element, valueWithPrefix);
                }
            } else {
                // not ItemType nor QName
                String value = xprim.getGuessedFormattedValue();
                String fixedValue = SerializationOptions.isEscapeInvalidCharacters(serializationOptions) ?
                        DOMUtil.escapeInvalidXmlCharsIfPresent(value) : value;
                if (asAttribute) {
                    DOMUtil.setAttributeValue(parentElement, elementOrAttributeName.getLocalPart(), fixedValue);
                } else {
                    DOMUtil.setElementTextContent(element, fixedValue);
                }
            }
        }
        if (!asAttribute) {
            setXsiTypeIfNeeded(xprim, element, typeName);
        }
    }

    private void setQNameAttribute(Element parentElement, String attributeName, QName value) {
        try {
            DOMUtil.setQNameAttribute(parentElement, attributeName, value);
        } catch (DOMException e) {
            throw new DOMException(e.code, e.getMessage() + "; setting attribute "+attributeName+" in element "+DOMUtil.getQName(parentElement)+" to QName value "+value);
        }
    }

    @Nullable
    private Element writeItemPath(PrimitiveXNodeImpl<?> xprim, Element parent, QName elementName) throws SchemaException {
        ItemPathType itemPathType = (ItemPathType) xprim.getValue();
        if (itemPathType == null) {
            // FIXME brutal hack (e.g. what about thread safety?)
            itemPathType = xprim.getParsedValue(ItemPathType.COMPLEX_TYPE, ItemPathType.class);
        }

        if (itemPathType != null) {
            Map<String, String> availableNamespaces = DOMUtil.getAllNonDefaultNamespaceDeclarations(parent);
            PrismNamespaceContext localNs = PrismNamespaceContext.from(availableNamespaces);

            ItemPathSerialization ctx = ItemPathSerialization.serialize(UniformItemPath.from(itemPathType.getItemPath()), localNs);

            Element element = createAndAppendChild(elementName, parent);
            DOMUtil.setNamespaceDeclarations(element,ctx.undeclaredPrefixes());
            element.setTextContent(ctx.getXPathWithoutDeclarations());
            return element;
        } else {
            return null;
        }
    }

    @Nullable
    private QName determineTypeName(PrimitiveXNodeImpl<?> xprim) {
        QName explicitTypeName = xprim.getTypeQName();
        if (explicitTypeName != null) {
            return qualifyIfNeeded(explicitTypeName);
        }

        // Ff typeQName is not explicitly specified, we try to determine it from parsed value.
        // We should probably set typeQName when parsing the value...
        if (xprim.isParsed()) {
            Object v = xprim.getValue();
            if (v != null) {
                return XsdTypeMapper.toXsdType(v.getClass());
            }
        }

        return null;
    }

    @Nullable
    private QName qualifyIfNeeded(QName explicitTypeName) {
        if (QNameUtil.isUnqualified(explicitTypeName)) {
            return XsdTypeMapper.determineQNameWithNs(explicitTypeName);
        } else {
            return explicitTypeName;
        }
    }

    private void appendCommentIfPresent(Element element, XNodeImpl xnode) {
        String text = xnode.getComment();
        if (StringUtils.isNotEmpty(text)) {
            DOMUtil.createComment(element, text);
        }
    }

    private void writeSchema(SchemaXNodeImpl xschema, Element parentElement) {
        Element schemaElement = xschema.getSchemaElement();
        if (schemaElement != null) {
            Element clonedSchemaElement = (Element) schemaElement.cloneNode(true);
            document.adoptNode(clonedSchemaElement);
            parentElement.appendChild(clonedSchemaElement);
        }
    }

    /**
     * Create XML element with the correct namespace prefix and namespace definition.
     * @param qname element QName
     * @return created DOM element
     */
    @NotNull
    private Element createElement(QName qname, Element parentElement) {
        try {
            String namespaceURI = qname.getNamespaceURI();
            if (!StringUtils.isBlank(namespaceURI)) {
                qname = setQNamePrefix(qname);
            }
            if (parentElement != null) {
                return DOMUtil.createElement(document, qname, parentElement, parentElement);
            } else {
                // This is needed otherwise the root element itself could not be created
                // Caller of this method is responsible for setting the topElement
                return DOMUtil.createElement(document, qname);
            }
        } catch (DOMException e) {
            throw new DOMException(e.code, e.getMessage() + "; creating element " + qname + " in element " + DOMUtil.getQName(parentElement));
        }
    }

    @NotNull
    private Element createAndAppendChild(QName childName, Element parent) {
        Element child = createElement(childName, parent);
        if (parent != null) {
            parent.appendChild(child);
        }
        return child;
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

    private DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
        return schemaRegistry.getNamespacePrefixMapper();
    }

    private void setXsiTypeIfNeeded(@NotNull XNodeImpl node, Element element) {
        if (node.getTypeQName() != null && node.isExplicitTypeDeclaration()) {
            DOMUtil.setXsiType(element, setQNamePrefixExplicitIfNeeded(node.getTypeQName()));
        }
    }

    private void setXsiTypeIfNeeded(@NotNull XNodeImpl node, Element element, QName typeName) {
        if (node.isExplicitTypeDeclaration()) {
            DOMUtil.setXsiType(element, setQNamePrefixExplicitIfNeeded(typeName));
        }
    }
}
