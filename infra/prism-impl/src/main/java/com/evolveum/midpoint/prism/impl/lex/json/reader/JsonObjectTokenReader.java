/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import static com.evolveum.midpoint.prism.impl.lex.json.JsonInfraItems.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.xnode.MapXNode;

import com.evolveum.midpoint.prism.xnode.MetadataAware;

import com.evolveum.midpoint.prism.xnode.XNode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.impl.lex.json.DefinitionContext;
import com.evolveum.midpoint.prism.impl.lex.json.JsonInfraItems;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Reads JSON/YAML objects. This is the most complex part of the reading process.
 */
class JsonObjectTokenReader {

    private static final Trace LOGGER = TraceManager.getTrace(RootObjectReader.class);

    @NotNull private final JsonParser parser;
    @NotNull private final JsonReadingContext ctx;

    /**
     * Map corresponding to the current object.
     * It might or might not be used as a return value - depending on circumstances.
     */
    @NotNull private MapXNodeImpl map;

    private final PrismNamespaceContext parentContext;

    /**
     * Name of the type of this XNode.
     * Derived from YAML tag or from @type declaration.
     * Should be set only once.
     */
    private QName typeName;

    /**
     * Name of the element for this XNode. (From @element declaration.)
     * Should be set only once.
     */
    private QName elementName;

    /**
     * Wrapped value (@value declaration).
     * Should be set only once.
     */
    private XNodeImpl wrappedValue;

    /**
     * Metadata (@metadata).
     */
    private final List<MapXNode> metadata = new ArrayList<>();

    /**
     * Value of the "incomplete" flag (@incomplete).
     * Should be set only once.
     */
    private Boolean incomplete;

    private boolean namespaceSensitiveStarted = false;

    private @NotNull DefinitionContext definition;

    private final DefinitionContext parentDefinition;

    private static final Map<QName, ItemProcessor> PROCESSORS = ImmutableMap.<QName, ItemProcessor>builder()
            // Namespace definition processing
            .put(PROP_NAMESPACE_QNAME, JsonObjectTokenReader::processNamespaceDeclaration)
            .put(PROP_CONTEXT_QNAME, JsonObjectTokenReader::processContextDeclaration)

            .put(PROP_INCOMPLETE_QNAME, JsonObjectTokenReader::processIncompleteDeclaration)

            .put(PROP_TYPE_QNAME, JsonObjectTokenReader::processTypeDeclaration)
            .put(PROP_VALUE_QNAME,namespaceSensitive(JsonObjectTokenReader::processWrappedValue))

            .put(PROP_METADATA_QNAME, namespaceSensitive(JsonObjectTokenReader::processMetadataValue))
            .put(PROP_ELEMENT_QNAME, namespaceSensitive(JsonObjectTokenReader::processElementNameDeclaration))
            .put(PROP_ITEM_QNAME, namespaceSensitive(JsonObjectTokenReader::processElementNameDeclaration))

            .build();

    private static final ItemProcessor STANDARD_PROCESSOR = namespaceSensitive(JsonObjectTokenReader::processStandardFieldValue);

    JsonObjectTokenReader(@NotNull JsonReadingContext ctx, PrismNamespaceContext parentContext, @NotNull DefinitionContext definition, @NotNull DefinitionContext parentDefinition) {
        this.ctx = ctx;
        this.parser = ctx.parser;
        this.parentContext = parentContext;
        this.definition = definition;
        this.parentDefinition = parentDefinition;
    }

    /**
     * Normally returns a MapXNode. However, there are exceptions:
     * - JSON primitives/lists can be simulated by two-member object (@type + @value); in these cases we return respective XNode.
     * - Incomplete marker i.e. { "@incomplete" : "true" } should be interpreted as IncompleteMarkerXNode.
     */
    @NotNull XNodeImpl read() throws IOException, SchemaException {
        processYamlTag();
        processFields();
        return postProcess();
    }

    private void processYamlTag() throws IOException, SchemaException {
        Object typeId = parser.getTypeId();
        if (typeId != null) {
            typeName = ctx.yamlTagResolver.tagToTypeName(typeId, ctx);
            definition = definition.withType(typeName);
        }
    }

    private void processFields() throws IOException, SchemaException {
        DefinitionContext currentField = null;
        while (!ctx.isAborted()) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                warnOrThrow("Unexpected end of data while parsing a map structure");
                ctx.setAborted();
                break;
            } else if (token == JsonToken.END_OBJECT) {
                break;
            } else if (token == JsonToken.FIELD_NAME) {
                currentField = processFieldName(currentField);
            } else {
                processFieldValue(currentField);
                currentField = null;
            }
        }
    }

    private @NotNull DefinitionContext processFieldName(DefinitionContext currentFieldName) throws IOException, SchemaException {
        String newFieldName = parser.getCurrentName();
        if (currentFieldName != null) {
            warnOrThrow("Two field names in succession: " + currentFieldName.getName() + " and " + newFieldName);
        }
        return definition.resolve(newFieldName, namespaceContext());
    }

    private @NotNull QName resolveQName(String name) throws SchemaException {
        return definition.unaware().resolve(name, namespaceContext()).getName();
    }


    private void processFieldValue(DefinitionContext name) throws IOException, SchemaException {
        assert name != null;
        XNodeImpl value = readValue(name);
        PROCESSORS.getOrDefault(name.getName(), STANDARD_PROCESSOR).apply(this, name.getName(), value);

    }

    private XNodeImpl readValue(DefinitionContext fieldDef) throws IOException, SchemaException {
        return new JsonOtherTokenReader(ctx,namespaceContext().inherited(), fieldDef, definition).readValue();
    }

    private PrismNamespaceContext namespaceContext() {
        if(map != null) {
            return map.namespaceContext();
        }
        return parentContext.inherited();
    }

    private void processContextDeclaration(QName name, XNodeImpl value) throws SchemaException {
        if(value instanceof MapXNode) {
            Builder<String, String> nsCtx = ImmutableMap.<String, String>builder();
            for(Entry<QName, ? extends XNode> entry : ((MapXNode) value).toMap().entrySet()) {
                String key = entry.getKey().getLocalPart();
                String ns = getCurrentFieldStringValue(entry.getKey(),entry.getValue());
                nsCtx.put(key, ns);
            }
            this.map = new MapXNodeImpl(parentContext.childContext(nsCtx.build()));
            return;
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    private void processStandardFieldValue(QName name, @NotNull XNodeImpl currentFieldValue) {
        // MID-5326:
        //   If namespace is defined, fieldName is always qualified,
        //   If namespace is undefined, then we can not effectivelly distinguish between
        map.put(name, currentFieldValue);
    }

    private void processIncompleteDeclaration(QName name, XNodeImpl currentFieldValue) throws SchemaException {
        if (incomplete != null) {
            warnOrThrow("Duplicate @incomplete marker found with the value: " + currentFieldValue);
        } else if (currentFieldValue instanceof PrimitiveXNodeImpl) {
            //noinspection unchecked
            Boolean realValue = ((PrimitiveXNodeImpl<Boolean>) currentFieldValue)
                .getParsedValue(DOMUtil.XSD_BOOLEAN, Boolean.class, getEvaluationMode());
            incomplete = Boolean.TRUE.equals(realValue);
        } else {
            warnOrThrow("@incomplete marker found with incompatible value: " + currentFieldValue);
        }
    }

    private void processWrappedValue(QName name, XNodeImpl currentFieldValue) throws SchemaException {
        if (wrappedValue != null) {
            warnOrThrow("Value ('" + JsonInfraItems.PROP_VALUE + "') defined more than once");
        }
        wrappedValue = currentFieldValue;
    }

    private void processMetadataValue(QName name, XNodeImpl currentFieldValue) throws SchemaException {
        if (currentFieldValue instanceof MapXNode) {
            metadata.add((MapXNode) currentFieldValue);
        } else if (currentFieldValue instanceof ListXNodeImpl) {
            for (XNode metadataValue : (ListXNodeImpl) currentFieldValue) {
                if (metadataValue instanceof MapXNode) {
                    metadata.add((MapXNode) metadataValue);
                } else {
                    warnOrThrow("Metadata is not a map XNode: " + metadataValue.debugDump());
                }
            }
        } else {
            warnOrThrow("Metadata is not a map or list XNode: " + currentFieldValue.debugDump());
        }
    }

    private void processElementNameDeclaration(QName name, XNodeImpl value) throws SchemaException {
        if (elementName != null) {
            warnOrThrow("Element name defined more than once");
        }
        String nsName = getCurrentFieldStringValue(name, value);
        @NotNull
        DefinitionContext maybeDefinition = parentDefinition.resolve(nsName, namespaceContext());
        elementName = resolveQName(nsName);
        replaceDefinition(maybeDefinition);
    }

    private void replaceDefinition(@NotNull DefinitionContext maybeDefinition) {
        definition = definition.moreSpecific(maybeDefinition);
    }

    /**
     *
     * @param name
     * @param value
     * @throws SchemaException
     */
    private void processTypeDeclaration(QName name, XNodeImpl value) throws SchemaException {
        if (typeName != null) {
            warnOrThrow("Value type defined more than once");
        }
        String stringValue = getCurrentFieldStringValue(name, value);
        // TODO: Compat: WE tread default prefixes as empty namespace, not default namespace
        typeName = DefinitionContext.resolveQName(stringValue, namespaceContext());
        definition = definition.withType(typeName);
    }

    private void processNamespaceDeclaration(QName name, XNodeImpl value) throws SchemaException {
        if(namespaceSensitiveStarted) {
            warnOrThrow("Namespace declared after other fields: " + ctx.getPositionSuffix());
        }
        if (map != null) {
            warnOrThrow("Namespace defined more than once");
        }
        var ns = getCurrentFieldStringValue(name, value);
        map = new MapXNodeImpl(parentContext.childContext(ImmutableMap.of("", ns)));
    }

    @NotNull
    private XNodeImpl postProcess() throws SchemaException {
        startNamespaceSensitive();
        // Return either map or primitive value (in case of @type/@value) or incomplete xnode
        int haveRegular = !map.isEmpty() ? 1 : 0;
        int haveWrapped = wrappedValue != null ? 1 : 0;
        int haveIncomplete = Boolean.TRUE.equals(incomplete) ? 1 : 0;

        XNodeImpl ret;
        if (haveRegular + haveWrapped + haveIncomplete > 1) {
            warnOrThrow("More than one of '" + PROP_VALUE + "', '" + PROP_INCOMPLETE
                + "' and regular content present");
            ret = map;
        } else {
            if (haveIncomplete > 0) {
                ret = new IncompleteMarkerXNodeImpl();
            } else if (haveWrapped > 0) {
                ret = wrappedValue;
            } else {
                ret = map; // map can be empty here
            }
        }
        addTypeNameTo(ret);
        addElementNameTo(ret);
        addMetadataTo(ret);
        return ret;
    }

    private void addMetadataTo(XNodeImpl rv) throws SchemaException {
        if (!metadata.isEmpty()) {
            if (rv instanceof MetadataAware) {
                ((MetadataAware) rv).setMetadataNodes(metadata);
            } else {
                warnOrThrow("Couldn't apply metadata to non-metadata-aware node: " + rv.getClass());
            }
        }
    }

    private void addElementNameTo(XNodeImpl rv) throws SchemaException {
        if (elementName != null) {
            if (wrappedValue != null && wrappedValue.getElementName() != null) {
                if (!wrappedValue.getElementName().equals(elementName)) {
                    warnOrThrow("Conflicting element names for '" + JsonInfraItems.PROP_VALUE
                        + "' (" + wrappedValue.getElementName()
                        + ") and regular content (" + elementName + "; ) present");
                }
            }
            rv.setElementName(elementName);
        }

    }

    private void addTypeNameTo(XNodeImpl rv) throws SchemaException {
        if (typeName != null) {
            if (wrappedValue != null && wrappedValue.getTypeQName() != null && !wrappedValue.getTypeQName().equals(typeName)) {
                warnOrThrow("Conflicting type names for '" + JsonInfraItems.PROP_VALUE
                    + "' (" + wrappedValue.getTypeQName() + ") and regular content (" + typeName + ") present");
            }
            rv.setTypeQName(typeName);
            rv.setExplicitTypeDeclaration(true);
        }
    }

    private String getCurrentFieldStringValue(QName name, XNode currentFieldValue) throws SchemaException {
        if (currentFieldValue instanceof PrimitiveXNodeImpl) {
            return ((PrimitiveXNodeImpl<?>) currentFieldValue).getStringValue();
        } else {
            warnOrThrow("Value of '" + name + "' attribute must be a primitive one. It is " + currentFieldValue + " instead");
            return "";
        }
    }

    private XNodeProcessorEvaluationMode getEvaluationMode() {
        return ctx.prismParsingContext.getEvaluationMode();
    }

    private void warnOrThrow(String format, Object... args) throws SchemaException {
        String message = Strings.lenientFormat(format, args);
        ctx.prismParsingContext.warnOrThrow(LOGGER, message + ". At " + ctx.getPositionSuffix());
    }

    private static ItemProcessor namespaceSensitive(ItemProcessor processor) {
        return (reader, name, value) -> {
            reader.startNamespaceSensitive();
            processor.apply(reader, name, value);
        };
    }


    private void startNamespaceSensitive() {
        namespaceSensitiveStarted = true;
        if(map == null) {
            map = new MapXNodeImpl(parentContext);
        }
    }


    @FunctionalInterface
    private interface ItemProcessor {

        void apply(JsonObjectTokenReader reader, QName itemName, XNodeImpl value) throws SchemaException;
    }

}
