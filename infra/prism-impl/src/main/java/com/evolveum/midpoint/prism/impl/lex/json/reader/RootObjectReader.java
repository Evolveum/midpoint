/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import java.io.IOException;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.impl.lex.json.JsonValueParser;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.MetadataAware;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * TODO
 */
class RootObjectReader {

    static final String DEFAULT_NAMESPACE_MARKER = "##DEFAULT-NAMESPACE##";

    @NotNull private final JsonReadingContext ctx;

    RootObjectReader(@NotNull JsonReadingContext ctx) {
        this.ctx = ctx;
    }

    void read() throws SchemaException, IOException {
        XNodeImpl xnode = new JsonOtherTokenReader(ctx).readValue();
        RootXNodeImpl root = postProcessValueToRoot(xnode, null);
        if (!ctx.objectHandler.handleData(root)) {
            ctx.setAborted();
        }
    }

    @NotNull
    private RootXNodeImpl postProcessValueToRoot(XNodeImpl xnode, String defaultNamespace) throws SchemaException, IOException {
        if (!xnode.isSingleEntryMap()) {
            throw new SchemaException("Expected MapXNode with a single key; got " + xnode + " instead. At " + ctx.getPositionSuffix());
        }
        processDefaultNamespaces(xnode, defaultNamespace, ctx);
        processSchemaNodes(xnode);
        Map.Entry<QName, XNodeImpl> entry = ((MapXNodeImpl) xnode).entrySet().iterator().next();
        RootXNodeImpl root = new RootXNodeImpl(entry.getKey(), entry.getValue());
        if (entry.getValue() != null) {
            root.setTypeQName(entry.getValue().getTypeQName());            // TODO - ok ????
        }
        return root;
    }


    // Default namespaces (@ns properties) are processed in the second pass, because they might be present within an object
    // at any place, even at the end.
    private void processDefaultNamespaces(XNodeImpl xnode, String parentDefault, JsonReadingContext ctx) {
        if (xnode instanceof MapXNodeImpl) {
            MapXNodeImpl map = (MapXNodeImpl) xnode;
            String currentDefault = ctx.defaultNamespaces.getOrDefault(map, parentDefault);
            map.replaceDefaultNamespaceMarkers(DEFAULT_NAMESPACE_MARKER, currentDefault);
            for (Map.Entry<QName, XNodeImpl> entry : map.entrySet()) {
                processDefaultNamespaces(entry.getValue(), currentDefault, ctx);
            }
            for (MapXNode metadataNode : map.getMetadataNodes()) {
                processDefaultNamespaces((XNodeImpl) metadataNode, currentDefault, ctx);
            }
            qualifyElementNameIfNeeded(map, currentDefault, ctx);
        } else {
            qualifyElementNameIfNeeded(xnode, parentDefault, ctx);
            if (xnode instanceof ListXNodeImpl) {
                for (XNodeImpl item : (ListXNodeImpl) xnode) {
                    processDefaultNamespaces(item, parentDefault, ctx);
                }
            }
            if (xnode instanceof MetadataAware) {
                for (MapXNode metadataNode : ((MetadataAware) xnode).getMetadataNodes()) {
                    processDefaultNamespaces((XNodeImpl) metadataNode, parentDefault, ctx);
                }
            }
        }
    }

    private void qualifyElementNameIfNeeded(XNodeImpl node, String namespace, JsonReadingContext ctx) {
        if (node.getElementName() != null
                && QNameUtil.noNamespace(node.getElementName())
                && StringUtils.isNotEmpty(namespace)
                && !ctx.noNamespaceElementNames.containsKey(node)) {
            node.setElementName(new QName(namespace, node.getElementName().getLocalPart()));
        }
    }

    // Schema nodes can be detected only after namespaces are resolved.
    // We simply convert primitive nodes to schema ones.
    private void processSchemaNodes(XNodeImpl xnode) throws SchemaException, IOException {
        if (xnode instanceof MapXNodeImpl) {
            MapXNodeImpl map = (MapXNodeImpl) xnode;
            XNodeImpl schemaNode = null;
            for (Map.Entry<QName, XNodeImpl> entry : map.entrySet()) {
                QName fieldName = entry.getKey();
                XNodeImpl subnode = entry.getValue();
                if (DOMUtil.XSD_SCHEMA_ELEMENT.equals(fieldName)) {
                    schemaNode = subnode;
                } else {
                    processSchemaNodes(subnode);
                }
            }
            if (schemaNode != null) {
                if (schemaNode instanceof PrimitiveXNodeImpl) {
                    PrimitiveXNodeImpl<?> primitiveXNode = (PrimitiveXNodeImpl<?>) schemaNode ;
                    if (primitiveXNode.isParsed()) {
                        throw new SchemaException("Cannot convert from PrimitiveXNode to SchemaXNode: node is already parsed: " + primitiveXNode);
                    }
                    SchemaXNodeImpl schemaXNode = new SchemaXNodeImpl();
                    map.replace(DOMUtil.XSD_SCHEMA_ELEMENT, schemaXNode);
                    schemaXNode.setSchemaElement(((JsonValueParser) primitiveXNode.getValueParser()).asDomElement());
                } else {
                    throw new SchemaException("Cannot convert 'schema' field to SchemaXNode: not a PrimitiveNode but " + schemaNode);
                }
            }
        } else if (xnode instanceof ListXNodeImpl) {
            for (XNodeImpl item : (ListXNodeImpl) xnode) {
                processSchemaNodes(item);
            }
        }
    }
}
