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

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.impl.lex.json.JsonValueParser;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * TODO
 */
class RootObjectReader {

    static final String DEFAULT_NAMESPACE_MARKER = "##DEFAULT-NAMESPACE##";

    @NotNull private final JsonReadingContext ctx;

    private final PrismNamespaceContext nsContext;

    RootObjectReader(@NotNull JsonReadingContext ctx, PrismNamespaceContext context) {
        this.ctx = ctx;
        this.nsContext = context;
    }

    void read() throws SchemaException, IOException {
        XNodeImpl xnode = new JsonOtherTokenReader(ctx, nsContext).readValue();
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
        processSchemaNodes(xnode);
        Map.Entry<QName, XNodeImpl> entry = ((MapXNodeImpl) xnode).entrySet().iterator().next();
        RootXNodeImpl root = new RootXNodeImpl(entry.getKey(), entry.getValue(), xnode.namespaceContext());
        if (entry.getValue() != null) {
            root.setTypeQName(entry.getValue().getTypeQName());            // TODO - ok ????
        }
        return root;
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
