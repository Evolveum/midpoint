/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.impl.lex.LexicalUtils;
import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLGenerator;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes XNode into JSON/YAML.
 */
abstract class AbstractWriter {

    static final class JsonSerializationContext {
        @NotNull final JsonGenerator generator;
        @NotNull private final SerializationContext prismSerializationContext;
        private String currentNamespace;

        private JsonSerializationContext(@NotNull JsonGenerator generator, @Nullable SerializationContext prismSerializationContext) {
            this.generator = generator;
            this.prismSerializationContext = prismSerializationContext != null ?
                    prismSerializationContext :
                    new SerializationContext(null);
        }
    }
    @NotNull
    public String write(@NotNull XNode xnode, @NotNull QName rootElementName, SerializationContext serializationContext) throws SchemaException {
        return write(LexicalUtils.createRootXNode((XNodeImpl) xnode, rootElementName), serializationContext);
    }

    protected abstract JsonGenerator createJacksonGenerator(StringWriter out) throws SchemaException;

    @NotNull
    public String write(@NotNull RootXNode root, SerializationContext prismSerializationContext) throws SchemaException {
        return writeInternal((RootXNodeImpl) root, prismSerializationContext, false);
    }

    @NotNull
    private String writeInternal(@NotNull XNodeImpl root, SerializationContext prismSerializationContext, boolean useMultiDocument) throws SchemaException {
        StringWriter out = new StringWriter();
        try ( JsonGenerator generator = createJacksonGenerator(out) ) {
            JsonSerializationContext ctx = new JsonSerializationContext(generator, prismSerializationContext);
            if (root instanceof RootXNodeImpl) {
                root = ((RootXNodeImpl) root).toMapXNode();
            }
            if (root instanceof ListXNodeImpl && useMultiDocument && generator instanceof MidpointYAMLGenerator) {
                boolean first = true;
                for (XNodeImpl item : ((ListXNodeImpl) root)) {
                    if (!first) {
                        ((MidpointYAMLGenerator) generator).newDocument();
                    } else {
                        first = false;
                    }
                    serialize(item, ctx, false);
                }
            } else {
                serialize(root, ctx, false);                // TODO default namespace
            }
        } catch (IOException ex) {
            throw new SchemaException("Error during serializing to JSON/YAML: " + ex.getMessage(), ex);
        }
        return out.toString();
    }

    @NotNull
    public String write(@NotNull List<RootXNodeImpl> roots, QName aggregateElementName,
            @Nullable SerializationContext prismSerializationContext) throws SchemaException {
        ListXNodeImpl objectsList = new ListXNodeImpl();
        for (RootXNodeImpl root : roots) {
            objectsList.add(root.toMapXNode());
        }
        XNodeImpl aggregate;
        if (aggregateElementName != null) {
            aggregate = new RootXNodeImpl(aggregateElementName, objectsList);
            return writeInternal(aggregate, prismSerializationContext, false);
        } else {
            aggregate = objectsList;
            return writeInternal(aggregate, prismSerializationContext, true);
        }
    }

    private void serialize(XNodeImpl xnode, JsonSerializationContext ctx, boolean inValueWrapMode) throws IOException {
        if (xnode instanceof MapXNodeImpl) {
            serializeFromMap((MapXNodeImpl) xnode, ctx);
        } else if (xnode == null) {
            serializeFromNull(ctx);
        } else if (needsValueWrapping(xnode) && !inValueWrapMode) {
            ctx.generator.writeStartObject();
            resetInlineTypeIfPossible(ctx);
            writeAuxiliaryInformation(xnode, ctx);
            ctx.generator.writeFieldName(Constants.PROP_VALUE);
            serialize(xnode, ctx, true);
            ctx.generator.writeEndObject();
        } else if (xnode instanceof ListXNodeImpl) {
            serializeFromList((ListXNodeImpl) xnode, ctx);
        } else if (xnode instanceof PrimitiveXNodeImpl) {
            serializeFromPrimitive((PrimitiveXNodeImpl<?>) xnode, ctx);
        } else if (xnode instanceof SchemaXNodeImpl) {
            serializeFromSchema((SchemaXNodeImpl) xnode, ctx);
        } else if (xnode instanceof IncompleteMarkerXNodeImpl) {
            ctx.generator.writeStartObject();
            ctx.generator.writeFieldName(Constants.PROP_INCOMPLETE);
            ctx.generator.writeBoolean(true);
            ctx.generator.writeEndObject();
        } else {
            throw new UnsupportedOperationException("Cannot serialize from " + xnode);
        }
    }

    private void serializeFromNull(JsonSerializationContext ctx) throws IOException {
        ctx.generator.writeNull();
    }

    private void writeAuxiliaryInformation(XNodeImpl xnode, JsonSerializationContext ctx) throws IOException {
        QName elementName = xnode.getElementName();
        if (elementName != null) {
            ctx.generator.writeObjectField(Constants.PROP_ELEMENT, createElementNameUri(elementName, ctx));
        }
        QName typeName = getExplicitType(xnode);
        if (typeName != null) {
            if (!supportsInlineTypes()) {
                ctx.generator.writeObjectField(Constants.PROP_TYPE, typeName);
            }
        }
    }

    private boolean needsValueWrapping(XNodeImpl xnode) {
        return xnode.getElementName() != null
                || getExplicitType(xnode) != null && !supportsInlineTypes();
    }

    protected abstract boolean supportsInlineTypes();

    protected abstract void writeInlineType(QName typeName, JsonSerializationContext ctx) throws IOException;

    private void serializeFromMap(MapXNodeImpl map, JsonSerializationContext ctx) throws IOException {
        writeInlineTypeIfNeeded(map, ctx);
        ctx.generator.writeStartObject();
        resetInlineTypeIfPossible(ctx);
        String oldDefaultNamespace = ctx.currentNamespace;
        generateNsDeclarationIfNeeded(map, ctx);
        writeAuxiliaryInformation(map, ctx);
        for (Map.Entry<QName, XNodeImpl> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            ctx.generator.writeFieldName(createKeyUri(entry, ctx));
            serialize(entry.getValue(), ctx, false);
        }
        ctx.generator.writeEndObject();
        ctx.currentNamespace = oldDefaultNamespace;
    }

    protected void resetInlineTypeIfPossible(JsonSerializationContext ctx) {
    }

    private void generateNsDeclarationIfNeeded(MapXNodeImpl map, JsonSerializationContext ctx) throws IOException {
        SerializationOptions opts = ctx.prismSerializationContext.getOptions();
        if (!SerializationOptions.isUseNsProperty(opts) || map.isEmpty()) {
            return;
        }
        String namespace = determineNewCurrentNamespace(map, ctx);
        if (namespace != null && !StringUtils.equals(namespace, ctx.currentNamespace)) {
            ctx.currentNamespace = namespace;
            ctx.generator.writeFieldName(Constants.PROP_NAMESPACE);
            ctx.generator.writeString(namespace);
        }
    }

    private String determineNewCurrentNamespace(MapXNodeImpl map, JsonSerializationContext ctx) {
        Map<String,Integer> counts = new HashMap<>();
        for (QName childName : map.keySet()) {
            String childNs = childName.getNamespaceURI();
            if (StringUtils.isEmpty(childNs)) {
                continue;
            }
            if (childNs.equals(ctx.currentNamespace)) {
                return ctx.currentNamespace;                    // found existing => continue with it
            }
            increaseCounter(counts, childNs);
        }
        if (map.getElementName() != null && QNameUtil.hasNamespace(map.getElementName())) {
            increaseCounter(counts, map.getElementName().getNamespaceURI());
        }
        // otherwise, take the URI that occurs the most in the map
        Map.Entry<String,Integer> max = null;
        for (Map.Entry<String,Integer> count : counts.entrySet()) {
            if (max == null || count.getValue() > max.getValue()) {
                max = count;
            }
        }
        return max != null ? max.getKey() : null;
    }

    private void increaseCounter(Map<String, Integer> counts, String childNs) {
        Integer c = counts.get(childNs);
        counts.put(childNs, c != null ? c+1 : 1);
    }

    private String createKeyUri(Map.Entry<QName, XNodeImpl> entry, JsonSerializationContext ctx) {
        QName key = entry.getKey();
        if (namespaceMatch(ctx.currentNamespace, key.getNamespaceURI())) {
            return key.getLocalPart();
        } else if (StringUtils.isNotEmpty(ctx.currentNamespace) && !isAttribute(entry.getValue())) {
            return QNameUtil.qNameToUri(key, true);        // items with no namespace should be written as such (starting with '#')
        } else {
            return QNameUtil.qNameToUri(key, false);    // items with no namespace can be written in plain
        }
    }

    private String createElementNameUri(QName elementName, JsonSerializationContext ctx) {
        if (namespaceMatch(ctx.currentNamespace, elementName.getNamespaceURI())) {
            return elementName.getLocalPart();
        } else {
            return QNameUtil.qNameToUri(elementName, StringUtils.isNotEmpty(ctx.currentNamespace));
        }
    }

    private boolean isAttribute(XNodeImpl node) {
        return node instanceof PrimitiveXNodeImpl && ((PrimitiveXNodeImpl) node).isAttribute();
    }

    private boolean namespaceMatch(String currentNamespace, String itemNamespace) {
        if (StringUtils.isEmpty(currentNamespace)) {
            return StringUtils.isEmpty(itemNamespace);
        } else {
            return currentNamespace.equals(itemNamespace);
        }
    }

    private void serializeFromList(ListXNodeImpl list, JsonSerializationContext ctx) throws IOException {
        writeInlineTypeIfNeeded(list, ctx);
        ctx.generator.writeStartArray();
        resetInlineTypeIfPossible(ctx);
        for (XNodeImpl item : list) {
            serialize(item, ctx, false);
        }
        ctx.generator.writeEndArray();
    }

    private void writeInlineTypeIfNeeded(XNodeImpl node, JsonSerializationContext ctx) throws IOException {
        QName explicitType = getExplicitType(node);
        if (supportsInlineTypes() && explicitType != null) {
            writeInlineType(explicitType, ctx);
        }
    }

    private void serializeFromSchema(SchemaXNodeImpl node, JsonSerializationContext ctx) throws IOException {
        writeInlineTypeIfNeeded(node, ctx);
        Element schemaElement = node.getSchemaElement();
        DOMUtil.fixNamespaceDeclarations(schemaElement); // TODO reconsider if it's OK to modify schema DOM element in this way
        ctx.generator.writeObject(schemaElement);
    }


    private <T> void serializeFromPrimitive(PrimitiveXNodeImpl<T> primitive, JsonSerializationContext ctx) throws IOException {
        writeInlineTypeIfNeeded(primitive, ctx);
        if (primitive.isParsed()) {
            ctx.generator.writeObject(primitive.getValue());
        } else {
            ctx.generator.writeObject(primitive.getStringValue());
        }
    }

    private QName getExplicitType(XNodeImpl xnode) {
        return xnode.isExplicitTypeDeclaration() ? xnode.getTypeQName() : null;
    }

    @SuppressWarnings("unused") // TODO
    private String serializeNsIfNeeded(QName subNodeName, String globalNamespace, JsonGenerator generator) throws IOException {
        if (subNodeName == null){
            return globalNamespace;
        }
        String subNodeNs = subNodeName.getNamespaceURI();
        if (StringUtils.isNotBlank(subNodeNs)){
            if (!subNodeNs.equals(globalNamespace)){
                globalNamespace = subNodeNs;
                generator.writeStringField(Constants.PROP_NAMESPACE, globalNamespace);

            }
        }
        return globalNamespace;
    }
}
