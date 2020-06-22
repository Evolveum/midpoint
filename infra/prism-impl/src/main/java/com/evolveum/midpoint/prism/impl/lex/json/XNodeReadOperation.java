/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;

import com.evolveum.midpoint.util.exception.SystemException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *
 */
class XNodeReadOperation {

    private static final Trace LOGGER = TraceManager.getTrace(XNodeReadOperation.class);

    private static final String DEFAULT_NAMESPACE_MARKER = "##DEFAULT-NAMESPACE##";

    @NotNull private final DocumentReadOperation parentOperation;

    /**
     * Source of JSON/YAML data.
     */
    @NotNull private final JsonParser parser;

    @NotNull final JsonReadingContext ctx;

    private final ParsingContext prismParsingContext;

    @NotNull private final LexicalProcessor.RootXNodeHandler objectHandler;

    @NotNull private final AbstractReader.YamlTagResolver yamlTagResolver;

    private boolean aborted;

    XNodeReadOperation(DocumentReadOperation parentOperation) {
        this.parentOperation = parentOperation;
        this.parser = parentOperation.parser;
        this.ctx = parentOperation.ctx;
        this.prismParsingContext = parentOperation.prismParsingContext;
        this.objectHandler = parentOperation.objectHandler;
        this.yamlTagResolver = parentOperation.yamlTagResolver;
    }

    void read() throws SchemaException, IOException {
        try {
            RootXNodeImpl root = parseRootValue();
            if (!objectHandler.handleData(root)) {
                setAborted();
            }
        } catch (SchemaException e) {
            throw e;
        } catch (JsonParseException e) {
            throw new SchemaException("Cannot parse JSON/YAML object: " + e.getMessage() + getPositionSuffixIfPresent(), e);
        } catch (IOException e) {
            throw new IOException("Cannot parse JSON/YAML object: " + e.getMessage() + getPositionSuffixIfPresent(), e);
        } catch (Throwable t) {
            throw new SystemException("Cannot parse JSON/YAML object: " + t.getMessage() + getPositionSuffixIfPresent(), t);
        }
    }

    private void setAborted() {
        aborted = true;
        parentOperation.setAborted();
    }

    private RootXNodeImpl parseRootValue() throws IOException, SchemaException {
        XNodeImpl xnode = parseValue();
        parser.nextToken();// todo
        return postProcessValueToRoot(xnode, null);
    }

    @NotNull
    private XNodeImpl parseValue() throws IOException, SchemaException {
        JsonToken currentToken = Objects.requireNonNull(parser.currentToken(), "currentToken");

        switch (currentToken) {
            case START_OBJECT:
                return parseJsonObject();
            case START_ARRAY:
                return parseToList();
            case VALUE_STRING:
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
            case VALUE_EMBEDDED_OBJECT:             // assuming it's a scalar value e.g. !!binary (TODO)
                return parseToPrimitive();
            case VALUE_NULL:
                return parseToEmptyPrimitive();
            default:
                throw new SchemaException("Unexpected current token: " + currentToken);
        }
    }

    /**
     * Normally returns a MapXNode. However, there are exceptions:
     * - JSON primitives/lists can be simulated by two-member object (@type + @value); in these cases we return respective XNode.
     * - Incomplete marker i.e. { "@incomplete" : "true" } should be interpreted as IncompleteMarkerXNode.
     */
    @NotNull
    private XNodeImpl parseJsonObject() throws SchemaException, IOException {
        assert parser.currentToken() != null;

        QNameUtil.QNameInfo elementNameInfo = null;

        QName typeName = null;
        Object tid = parser.getTypeId();
        if (tid != null) {
            typeName = yamlTagResolver.tagToTypeName(tid, ctx);
        }

        MapXNodeImpl map = new MapXNodeImpl();
        XNodeImpl wrappedValue = null;
        boolean defaultNamespaceDefined = false;
        QNameUtil.QNameInfo currentFieldNameInfo = null;
        Boolean incomplete = null;

        while (!aborted) {

            JsonToken token = parser.nextToken();
            if (token == null) {
                prismParsingContext.warnOrThrow(LOGGER, "Unexpected end of data while parsing a map structure at " + getPositionSuffix());
                setAborted();
                break;
            } else if (token == JsonToken.END_OBJECT) {
                break;
            } else if (token == JsonToken.FIELD_NAME) {
                String newFieldName = parser.getCurrentName();
                if (currentFieldNameInfo != null) {
                    prismParsingContext.warnOrThrow(LOGGER, "Two field names in succession: " + currentFieldNameInfo + " and " + newFieldName);
                }
                currentFieldNameInfo = QNameUtil.uriToQNameInfo(newFieldName, true);
            } else {
                assert currentFieldNameInfo != null;
                XNodeImpl valueXNode = parseValue();
                if (isSpecial(currentFieldNameInfo.name)) {
                    if (isNamespaceDeclaration(currentFieldNameInfo.name)) {
                        if (defaultNamespaceDefined) {
                            prismParsingContext.warnOrThrow(LOGGER, "Default namespace defined more than once at " + getPositionSuffix());
                        }
                        String namespaceUri = getStringValue(valueXNode, currentFieldNameInfo);
                        ctx.defaultNamespaces.put(map, namespaceUri);
                        defaultNamespaceDefined = true;
                    } else if (isTypeDeclaration(currentFieldNameInfo.name)) {
                        if (typeName != null) {
                            prismParsingContext.warnOrThrow(LOGGER, "Value type defined more than once at " + getPositionSuffix());
                        }
                        typeName = QNameUtil.uriToQName(getStringValue(valueXNode, currentFieldNameInfo), true);
                    } else if (isElementDeclaration(currentFieldNameInfo.name)) {
                        if (elementNameInfo != null) {
                            prismParsingContext.warnOrThrow(LOGGER, "Element name defined more than once at " + getPositionSuffix());
                        }
                        elementNameInfo = QNameUtil.uriToQNameInfo(getStringValue(valueXNode, currentFieldNameInfo), true);
                    } else if (isValue(currentFieldNameInfo.name)) {
                        if (wrappedValue != null) {
                            prismParsingContext.warnOrThrow(LOGGER, "Value ('" + Constants.PROP_VALUE + "') defined more than once at " + getPositionSuffix());
                        }
                        wrappedValue = valueXNode;
                    } else if (isIncompleteDeclaration(currentFieldNameInfo.name)) {
                        if (incomplete != null) {
                            prismParsingContext.warnOrThrow(LOGGER, "Duplicate @incomplete marker found with the value: " + valueXNode);
                        } else if (valueXNode instanceof PrimitiveXNodeImpl) {
                            //noinspection unchecked
                            Boolean value = ((PrimitiveXNodeImpl<Boolean>) valueXNode)
                                    .getParsedValue(DOMUtil.XSD_BOOLEAN, Boolean.class, getEvaluationMode());
                            incomplete = Boolean.TRUE.equals(value);
                        } else {
                            prismParsingContext.warnOrThrow(LOGGER, "@incomplete marker found with incompatible value: " + valueXNode);
                        }
                    }
                } else {
                    // Beware of potential unqualified value conflict (see MID-5326).
                    // Therefore we use special "default-namespace" marker that is dealt with later.
                    QName key;
                    if (currentFieldNameInfo.explicitEmptyNamespace || QNameUtil.isQualified(currentFieldNameInfo.name)) {
                        key = currentFieldNameInfo.name;
                    } else {
                        key = new QName(DEFAULT_NAMESPACE_MARKER, currentFieldNameInfo.name.getLocalPart());
                        map.setHasDefaultNamespaceMarkers();
                    }
                    map.put(key, valueXNode);
                }
                currentFieldNameInfo = null;
            }
        }
        // Return either map or primitive value (in case of @type/@value) or incomplete xnode
        int haveRegular = !map.isEmpty() ? 1 : 0;
        int haveWrapped = wrappedValue != null ? 1 : 0;
        int haveIncomplete = Boolean.TRUE.equals(incomplete) ? 1 : 0;
        XNodeImpl rv;
        if (haveRegular + haveWrapped + haveIncomplete > 1) {
            prismParsingContext.warnOrThrow(LOGGER, "More than one of '" + Constants.PROP_VALUE + "', '" + Constants.PROP_INCOMPLETE
                    + "' and regular content present at " + getPositionSuffix());
            rv = map;
        } else {
            if (haveIncomplete > 0) {
                rv = new IncompleteMarkerXNodeImpl();
            } else if (haveWrapped > 0) {
                rv = wrappedValue;
            } else {
                rv = map;   // map can be empty here
            }
        }
        if (typeName != null) {
            if (wrappedValue != null && wrappedValue.getTypeQName() != null && !wrappedValue.getTypeQName().equals(typeName)) {
                prismParsingContext.warnOrThrow(LOGGER, "Conflicting type names for '" + Constants.PROP_VALUE
                        + "' (" + wrappedValue.getTypeQName() + ") and regular content (" + typeName + ") present at "
                        + getPositionSuffix());
            }
            rv.setTypeQName(typeName);
            rv.setExplicitTypeDeclaration(true);
        }
        if (elementNameInfo != null) {
            if (wrappedValue != null && wrappedValue.getElementName() != null) {
                boolean wrappedValueElementNoNamespace = ctx.noNamespaceElementNames.containsKey(wrappedValue);
                if (!wrappedValue.getElementName().equals(elementNameInfo.name)
                        || wrappedValueElementNoNamespace != elementNameInfo.explicitEmptyNamespace) {
                    prismParsingContext.warnOrThrow(LOGGER, "Conflicting element names for '" + Constants.PROP_VALUE
                            + "' (" + wrappedValue.getElementName() + "; no NS=" + wrappedValueElementNoNamespace
                            + ") and regular content (" + elementNameInfo.name + "; no NS="
                            + elementNameInfo.explicitEmptyNamespace + ") present at "
                            + getPositionSuffix());
                }
            }
            rv.setElementName(elementNameInfo.name);
            if (elementNameInfo.explicitEmptyNamespace) {
                ctx.noNamespaceElementNames.put(rv, null);
            }
        }
        return rv;
    }

    private XNodeProcessorEvaluationMode getEvaluationMode() {
        return prismParsingContext != null ? prismParsingContext.getEvaluationMode() : XNodeProcessorEvaluationMode.STRICT;
    }

    private boolean isNotObjectsElement(@NotNull QName name) {
        return true;
    }

    private String getStringValue(XNodeImpl valueXNode, QNameUtil.QNameInfo currentFieldNameInfo) throws SchemaException {
        if (valueXNode instanceof PrimitiveXNodeImpl) {
            return ((PrimitiveXNodeImpl<?>) valueXNode).getStringValue();
        } else {
            prismParsingContext.warnOrThrow(LOGGER, "Value of '" + currentFieldNameInfo + "' attribute must be a primitive one. It is " + valueXNode + " instead. At " + getPositionSuffix());
            return "";
        }
    }

    private boolean isSpecial(QName fieldName) {
        return isTypeDeclaration(fieldName)
                || isIncompleteDeclaration(fieldName)
                || isElementDeclaration(fieldName)
                || isNamespaceDeclaration(fieldName)
                || isValue(fieldName);
    }

    private boolean isTypeDeclaration(QName fieldName) {
        return Constants.PROP_TYPE_QNAME.equals(fieldName);
    }

    private boolean isIncompleteDeclaration(QName fieldName) {
        return Constants.PROP_INCOMPLETE_QNAME.equals(fieldName);
    }

    private boolean isElementDeclaration(QName fieldName) {
        return Constants.PROP_ELEMENT_QNAME.equals(fieldName);
    }

    private boolean isNamespaceDeclaration(QName fieldName) {
        return Constants.PROP_NAMESPACE_QNAME.equals(fieldName);
    }

    private boolean isValue(QName fieldName) {
        return Constants.PROP_VALUE_QNAME.equals(fieldName);
    }

    private String getPositionSuffix() {
        return String.valueOf(parser.getCurrentLocation());
    }

    private ListXNodeImpl parseToList() throws SchemaException, IOException {
        Validate.notNull(parser.currentToken());

        ListXNodeImpl list = new ListXNodeImpl();
        Object tid = parser.getTypeId();
        if (tid != null) {
            list.setTypeQName(yamlTagResolver.tagToTypeName(tid, ctx));
        }
        for (;;) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                prismParsingContext.warnOrThrow(LOGGER, "Unexpected end of data while parsing a list structure at " + getPositionSuffix());
                return list;
            } else if (token == JsonToken.END_ARRAY) {
                return list;
            } else {
                list.add(parseValue());
            }
        }
    }

    private <T> PrimitiveXNodeImpl<T> parseToPrimitive() throws IOException, SchemaException {
        PrimitiveXNodeImpl<T> primitive = new PrimitiveXNodeImpl<>();

        Object tid = parser.getTypeId();
        if (tid != null) {
            QName typeName = yamlTagResolver.tagToTypeName(tid, ctx);
            primitive.setTypeQName(typeName);
            primitive.setExplicitTypeDeclaration(true);
        } else {
            // We don't try to determine XNode type from the implicit JSON/YAML type (integer, number, ...),
            // because XNode type prescribes interpretation in midPoint. E.g. YAML string type would be interpreted
            // as xsd:string, even if the schema would expect e.g. timestamp.
        }

        JsonNode jn = parser.readValueAs(JsonNode.class);
        ValueParser<T> vp = new JsonValueParser<>(parser, jn);
        primitive.setValueParser(vp);

        return primitive;
    }

    private <T> PrimitiveXNodeImpl<T> parseToEmptyPrimitive() {
        PrimitiveXNodeImpl<T> primitive = new PrimitiveXNodeImpl<>();
        primitive.setValueParser(new JsonNullValueParser<>());
        return primitive;
    }

    @SuppressWarnings("unused") // TODO remove if not needed
    private QName getCurrentTypeName(JsonReadingContext ctx) throws IOException, SchemaException {
        switch (parser.currentToken()) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return AbstractReader.determineNumberType(parser.getNumberType());
            case VALUE_FALSE:
            case VALUE_TRUE:
                return DOMUtil.XSD_BOOLEAN;
            case VALUE_STRING:
                return DOMUtil.XSD_STRING;
            case VALUE_NULL:
                return null;            // TODO?
            default:
                throw new SchemaException("Unexpected current token type: " + parser.currentToken() + "/" + parser.getText() + " at " + getPositionSuffix());
        }
    }

    @NotNull
    private String getPositionSuffixIfPresent() {
        return " At: " + getPositionSuffix();
    }

    @NotNull
    private RootXNodeImpl postProcessValueToRoot(XNodeImpl xnode, String defaultNamespace) throws SchemaException, IOException {
        if (!xnode.isSingleEntryMap()) {
            throw new SchemaException("Expected MapXNode with a single key; got " + xnode + " instead. At " + getPositionSuffix());
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
            qualifyElementNameIfNeeded(map, currentDefault, ctx);
        } else {
            qualifyElementNameIfNeeded(xnode, parentDefault, ctx);
            if (xnode instanceof ListXNodeImpl) {
                for (XNodeImpl item : (ListXNodeImpl) xnode) {
                    processDefaultNamespaces(item, parentDefault, ctx);
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
