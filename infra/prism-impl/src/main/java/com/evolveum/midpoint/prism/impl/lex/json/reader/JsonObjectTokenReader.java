/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import static com.evolveum.midpoint.prism.impl.lex.json.reader.RootObjectReader.DEFAULT_NAMESPACE_MARKER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.xnode.MapXNode;

import com.evolveum.midpoint.prism.xnode.MetadataAware;

import com.evolveum.midpoint.prism.xnode.XNode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.impl.lex.json.JsonInfraItems;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
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
    @NotNull private final MapXNodeImpl map = new MapXNodeImpl();

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
    private QNameUtil.QNameInfo elementName;

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

    /**
     * Namespace (@ns).
     * Should be set only once.
     */
    private String declaredNamespace;

    /**
     * Name of the currently processed field.
     * Overwritten many times.
     */
    private QNameUtil.QNameInfo currentFieldName;

    /**
     * Value of the currently processed field.
     * Overwritten many times.
     */
    private XNodeImpl currentFieldValue;

    JsonObjectTokenReader(@NotNull JsonReadingContext ctx) {
        this.ctx = ctx;
        this.parser = ctx.parser;
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
        }
    }

    private void processFields() throws IOException, SchemaException {
        while (!ctx.isAborted()) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                warnOrThrow("Unexpected end of data while parsing a map structure");
                ctx.setAborted();
                break;
            } else if (token == JsonToken.END_OBJECT) {
                break;
            } else if (token == JsonToken.FIELD_NAME) {
                processFieldName();
            } else {
                processFieldValue();
            }
        }
    }

    private void processFieldName() throws IOException, SchemaException {
        String newFieldName = parser.getCurrentName();
        if (currentFieldName != null) {
            warnOrThrow("Two field names in succession: " + currentFieldName + " and " + newFieldName);
        }
        currentFieldName = QNameUtil.uriToQNameInfo(newFieldName, true);
    }

    private void processFieldValue() throws IOException, SchemaException {
        assert currentFieldName != null;
        currentFieldValue = readValue();
        if (isNamespaceDeclaration()) {
            processNamespaceDeclaration();
        } else if (isContextDeclaration()) {
            processContextDeclaration();
        } else if (isTypeDeclaration()) {
            processTypeDeclaration();
        } else if (isElementDeclaration()) {
            processElementNameDeclaration();
        } else if (isWrappedValue()) {
            processWrappedValue();
        } else if (isMetadataValue()) {
            processMetadataValue();
        } else if (isIncompleteDeclaration()) {
            processIncompleteDeclaration();
        } else {
            processStandardFieldValue();
        }
        currentFieldName = null;
        currentFieldValue = null;
    }



    private XNodeImpl readValue() throws IOException, SchemaException {
        return new JsonOtherTokenReader(ctx).readValue();
    }

    private void processContextDeclaration() {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void processStandardFieldValue() {
        // Beware of potential unqualified value conflict (see MID-5326).
        // Therefore we use special "default-namespace" marker that is dealt with later.
        QName key;
        if (currentFieldName.explicitEmptyNamespace || QNameUtil.isQualified(currentFieldName.name)) {
            key = currentFieldName.name;
        } else {
            key = new QName(DEFAULT_NAMESPACE_MARKER, currentFieldName.name.getLocalPart());
            map.setHasDefaultNamespaceMarkers();
        }
        map.put(key, currentFieldValue);
    }

    private void processIncompleteDeclaration() throws SchemaException {
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

    private void processWrappedValue() throws SchemaException {
        if (wrappedValue != null) {
            warnOrThrow("Value ('" + JsonInfraItems.PROP_VALUE + "') defined more than once");
        }
        wrappedValue = currentFieldValue;
    }

    private void processMetadataValue() throws SchemaException {
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

    private void processElementNameDeclaration() throws SchemaException {
        if (elementName != null) {
            warnOrThrow("Element name defined more than once");
        }
        elementName = QNameUtil.uriToQNameInfo(getCurrentFieldStringValue(), true);
    }

    private void processTypeDeclaration() throws SchemaException {
        if (typeName != null) {
            warnOrThrow("Value type defined more than once");
        }
        typeName = QNameUtil.uriToQName(getCurrentFieldStringValue(), true);
    }

    private void processNamespaceDeclaration() throws SchemaException {
        if (declaredNamespace != null) {
            warnOrThrow("Default namespace defined more than once");
        }
        declaredNamespace = getCurrentFieldStringValue();
    }

    @NotNull
    private XNodeImpl postProcess() throws SchemaException {
        // Return either map or primitive value (in case of @type/@value) or incomplete xnode
        int haveRegular = !map.isEmpty() ? 1 : 0;
        int haveWrapped = wrappedValue != null ? 1 : 0;
        int haveIncomplete = Boolean.TRUE.equals(incomplete) ? 1 : 0;
        XNodeImpl rv;
        if (haveRegular + haveWrapped + haveIncomplete > 1) {
            warnOrThrow("More than one of '" + JsonInfraItems.PROP_VALUE + "', '" + JsonInfraItems.PROP_INCOMPLETE
                + "' and regular content present");
            rv = map;
        } else {
            if (haveIncomplete > 0) {
                rv = new IncompleteMarkerXNodeImpl();
            } else if (haveWrapped > 0) {
                rv = wrappedValue;
            } else {
                rv = map; // map can be empty here
            }
        }
        if (typeName != null) {
            if (wrappedValue != null && wrappedValue.getTypeQName() != null && !wrappedValue.getTypeQName().equals(typeName)) {
                warnOrThrow("Conflicting type names for '" + JsonInfraItems.PROP_VALUE
                    + "' (" + wrappedValue.getTypeQName() + ") and regular content (" + typeName + ") present");
            }
            rv.setTypeQName(typeName);
            rv.setExplicitTypeDeclaration(true);
        }
        if (elementName != null) {
            if (wrappedValue != null && wrappedValue.getElementName() != null) {
                boolean wrappedValueElementNoNamespace = ctx.noNamespaceElementNames.containsKey(wrappedValue);
                if (!wrappedValue.getElementName().equals(elementName.name)
                    || wrappedValueElementNoNamespace != elementName.explicitEmptyNamespace) {
                    warnOrThrow("Conflicting element names for '" + JsonInfraItems.PROP_VALUE
                        + "' (" + wrappedValue.getElementName() + "; no NS=" + wrappedValueElementNoNamespace
                        + ") and regular content (" + elementName.name + "; no NS="
                        + elementName.explicitEmptyNamespace + ") present");
                }
            }
            rv.setElementName(elementName.name);
            if (elementName.explicitEmptyNamespace) {
                ctx.noNamespaceElementNames.put(rv, null);
            }
        }

        if (declaredNamespace != null) {
            if (rv instanceof MapXNodeImpl) {
                ctx.defaultNamespaces.put((MapXNodeImpl) rv, declaredNamespace);
            }
            for (MapXNode metadataNode : metadata) {
                ctx.defaultNamespaces.put((MapXNodeImpl) metadataNode, declaredNamespace);
            }
        }

        if (!metadata.isEmpty()) {
            if (rv instanceof MetadataAware) {
                ((MetadataAware) rv).setMetadataNodes(metadata);
            } else {
                warnOrThrow("Couldn't apply metadata to non-metadata-aware node: " + rv.getClass());
            }
        }

        return rv;
    }

    private String getCurrentFieldStringValue() throws SchemaException {
        if (currentFieldValue instanceof PrimitiveXNodeImpl) {
            return ((PrimitiveXNodeImpl<?>) currentFieldValue).getStringValue();
        } else {
            warnOrThrow("Value of '" + currentFieldName + "' attribute must be a primitive one. It is " + currentFieldValue + " instead");
            return "";
        }
    }

    private XNodeProcessorEvaluationMode getEvaluationMode() {
        return ctx.prismParsingContext.getEvaluationMode();
    }

    // FIXME: Refactor this to dispatch map for infra values
    private boolean isTypeDeclaration() {
        return JsonInfraItems.PROP_TYPE_QNAME.equals(currentFieldName.name);
    }

    private boolean isIncompleteDeclaration() {
        return JsonInfraItems.PROP_INCOMPLETE_QNAME.equals(currentFieldName.name);
    }

    private boolean isElementDeclaration() {
        return JsonInfraItems.PROP_ELEMENT_QNAME.equals(currentFieldName.name);
    }

    private boolean isNamespaceDeclaration() {
        return JsonInfraItems.PROP_NAMESPACE_QNAME.equals(currentFieldName.name);
    }

    private boolean isWrappedValue() {
        return JsonInfraItems.PROP_VALUE_QNAME.equals(currentFieldName.name);
    }

    private boolean isMetadataValue() {
        return JsonInfraItems.PROP_METADATA_QNAME.equals(currentFieldName.name);
    }

    private boolean isContextDeclaration() {
        return JsonInfraItems.PROP_CONTEXT.equals(currentFieldName.name);
    }

    private void warnOrThrow(String message) throws SchemaException {
        ctx.prismParsingContext.warnOrThrow(LOGGER, message + ". At " + ctx.getPositionSuffix());
    }
}
