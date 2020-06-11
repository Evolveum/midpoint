/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.ParsingContextImpl;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.LexicalUtils;
import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLGenerator;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.fasterxml.jackson.core.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.w3c.dom.Element;

public abstract class AbstractJsonLexicalProcessor implements LexicalProcessor<String> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractJsonLexicalProcessor.class);

    private static final String PROP_NAMESPACE = "@ns";
    private static final QName PROP_NAMESPACE_QNAME = new QName(PROP_NAMESPACE);
    private static final String PROP_TYPE = "@type";
    private static final QName PROP_TYPE_QNAME = new QName(PROP_TYPE);
    private static final String PROP_INCOMPLETE = "@incomplete";
    private static final QName PROP_INCOMPLETE_QNAME = new QName(PROP_INCOMPLETE);
    private static final String PROP_ELEMENT = "@element";
    private static final QName PROP_ELEMENT_QNAME = new QName(PROP_ELEMENT);
    private static final String PROP_VALUE = "@value";
    private static final QName PROP_VALUE_QNAME = new QName(PROP_VALUE);

    private static final String DEFAULT_NAMESPACE_MARKER = "##DEFAULT-NAMESPACE##";

    @NotNull protected final SchemaRegistry schemaRegistry;

    AbstractJsonLexicalProcessor(@NotNull SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    //region Parsing implementation

    @NotNull
    @Override
    public RootXNodeImpl read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
        List<RootXNodeImpl> nodes = readInternal(source, parsingContext);
        if (nodes.isEmpty()) {
            throw new SchemaException("No data at input");          // shouldn't occur, because it is already treated in the called method
        } else if (nodes.size() > 1) {
            throw new SchemaException("More than one object found: " + nodes);
        } else {
            return nodes.get(0);
        }
    }

    /**
     * Honors multi-document files and multiple objects in a single document ('c:objects', list-as-root mechanisms).
     */
    @NotNull
    @Override
    public List<RootXNodeImpl> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
        return readInternal(source, parsingContext);
    }

    @NotNull
    private List<RootXNodeImpl> readInternal(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
        InputStream is = source.getInputStream();
        try {
            JsonParser parser = createJacksonParser(is);
            return parseFromStart(parser, parsingContext, null);
        } finally {
            if (source.closeStreamAfterParsing()) {
                closeQuietly(is);
            }
        }
    }

    private void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't close the input stream", e);
            }
        }
    }

    private static final class IterativeParsingContext {
        private final RootXNodeHandler handler;
        private boolean dataSent;                      // true if we really found the list of objects and sent it out
        private String defaultNamespace;               // default namespace, if present
        private boolean abortProcessing;               // used when handler returns 'do not continue'

        private IterativeParsingContext(RootXNodeHandler handler) {
            this.handler = handler;
        }
    }

    @Override
    public void readObjectsIteratively(@NotNull ParserSource source, @NotNull ParsingContext parsingContext,
            RootXNodeHandler handler) throws SchemaException, IOException {
        InputStream is = source.getInputStream();
        try {
            JsonParser parser = createJacksonParser(is);
            parseFromStart(parser, parsingContext, handler);
        } finally {
            if (source.closeStreamAfterParsing()) {
                closeQuietly(is);
            }
        }
    }

    protected abstract JsonParser createJacksonParser(InputStream stream) throws SchemaException, IOException;

    static class JsonParsingContext {
        @NotNull final JsonParser parser;
        @NotNull private final ParsingContextImpl prismParsingContext;

        // TODO consider getting rid of these IdentityHashMaps by support default namespace marking and resolution
        //  directly in XNode structures (like it was done for Map XNode keys recently).

        // Definitions of namespaces ('@ns') within maps; to be applied after parsing.
        @NotNull private final IdentityHashMap<MapXNodeImpl, String> defaultNamespaces = new IdentityHashMap<>();
        // Elements that should be skipped when filling-in default namespaces - those that are explicitly set with no-NS ('#name').
        // (Values for these entries are not important. Only key presence is relevant.)
        @NotNull private final IdentityHashMap<XNodeImpl, Object> noNamespaceElementNames = new IdentityHashMap<>();
        private JsonParsingContext(@NotNull JsonParser parser, @NotNull ParsingContextImpl prismParsingContext) {
            this.parser = parser;
            this.prismParsingContext = prismParsingContext;
        }

        private JsonParsingContext createChildContext() {
            return new JsonParsingContext(parser, prismParsingContext);
        }
    }

    @NotNull
    private List<RootXNodeImpl> parseFromStart(JsonParser unconfiguredParser, ParsingContext parsingContext,
            RootXNodeHandler handler) throws SchemaException {
        List<RootXNodeImpl> rv = new ArrayList<>();
        JsonParsingContext ctx = null;
        try {
            JsonParser parser = configureParser(unconfiguredParser);
            parser.nextToken();
            if (parser.currentToken() == null) {
                throw new SchemaException("Nothing to parse: the input is empty.");
            }
            do {
                ctx = new JsonParsingContext(parser, (ParsingContextImpl) parsingContext);
                IterativeParsingContext ipc = handler != null ? new IterativeParsingContext(handler) : null;
                XNodeImpl xnode = parseValue(ctx, ipc);
                if (ipc != null && ipc.dataSent) {
                    // all the objects were sent to the handler, nothing more to do
                } else {
                    List<RootXNodeImpl> roots = valueToRootList(xnode, null, ctx);
                    if (ipc == null) {
                        rv.addAll(roots);
                    } else {
                        for (RootXNodeImpl root : roots) {
                            if (!ipc.handler.handleData(root)) {
                                ipc.abortProcessing = true;
                                break;
                            }
                        }
                    }
                }
                if (ipc != null && ipc.abortProcessing) {
                    // set either here or in parseValue
                    break;
                }
            } while (ctx.parser.nextToken() != null);     // for multi-document YAML files
            return rv;
        } catch (IOException e) {
            throw new SchemaException("Cannot parse JSON/YAML object: " + e.getMessage() + getPositionSuffixIfPresent(ctx), e);
        }
    }

    @NotNull
    private List<RootXNodeImpl> valueToRootList(XNodeImpl node, String defaultNamespace, JsonParsingContext ctx) throws SchemaException, IOException {
        List<RootXNodeImpl> rv = new ArrayList<>();
        if (node instanceof ListXNodeImpl) {
            ListXNodeImpl list = (ListXNodeImpl) node;
            for (XNodeImpl listItem : list) {
                rv.addAll(valueToRootList(listItem, defaultNamespace, ctx));
            }
        } else {
            QName objectsMarker = schemaRegistry.getPrismContext().getObjectsElementName();
            RootXNodeImpl root = postProcessValueToRoot(node, defaultNamespace, ctx);
            if (root.getSubnode() instanceof ListXNodeImpl && objectsMarker != null && QNameUtil.match(objectsMarker, root.getRootElementName())) {
                return valueToRootList(root.getSubnode(), defaultNamespace, ctx);
            } else {
                rv.add(root);
            }
        }
        return rv;
    }

    @NotNull
    private RootXNodeImpl postProcessValueToRoot(XNodeImpl xnode, String defaultNamespace, JsonParsingContext ctx) throws SchemaException, IOException {
        if (!xnode.isSingleEntryMap()) {
            throw new SchemaException("Expected MapXNode with a single key; got " + xnode + " instead. At " + getPositionSuffix(ctx));
        }
        processDefaultNamespaces(xnode, defaultNamespace, ctx);
        processSchemaNodes(xnode);
        Entry<QName, XNodeImpl> entry = ((MapXNodeImpl) xnode).entrySet().iterator().next();
        RootXNodeImpl root = new RootXNodeImpl(entry.getKey(), entry.getValue());
        if (entry.getValue() != null) {
            root.setTypeQName(entry.getValue().getTypeQName());            // TODO - ok ????
        }
        return root;
    }

    @NotNull
    private String getPositionSuffixIfPresent(JsonParsingContext ctx) {
        return ctx != null ? " At: " + getPositionSuffix(ctx) : "";
    }

    // Default namespaces (@ns properties) are processed in the second pass, because they might be present within an object
    // at any place, even at the end.
    private void processDefaultNamespaces(XNodeImpl xnode, String parentDefault, JsonParsingContext ctx) {
        if (xnode instanceof MapXNodeImpl) {
            MapXNodeImpl map = (MapXNodeImpl) xnode;
            String currentDefault = ctx.defaultNamespaces.getOrDefault(map, parentDefault);
            map.replaceDefaultNamespaceMarkers(DEFAULT_NAMESPACE_MARKER, currentDefault);
            for (Entry<QName, XNodeImpl> entry : map.entrySet()) {
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

    private void qualifyElementNameIfNeeded(XNodeImpl node, String namespace, JsonParsingContext ctx) {
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
            for (Entry<QName, XNodeImpl> entry : map.entrySet()) {
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

    @NotNull
    private XNodeImpl parseValue(JsonParsingContext ctx, IterativeParsingContext ipc) throws IOException, SchemaException {
        Validate.notNull(ctx.parser.currentToken());

        switch (ctx.parser.currentToken()) {
            case START_OBJECT:
                return parseJsonObject(ctx, ipc);
            case START_ARRAY:
                return parseToList(ctx, ipc);
            case VALUE_STRING:
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
            case VALUE_EMBEDDED_OBJECT:             // assuming it's a scalar value e.g. !!binary (TODO)
                return parseToPrimitive(ctx);
            case VALUE_NULL:
                return parseToEmptyPrimitive();
            default:
                throw new SchemaException("Unexpected current token: " + ctx.parser.currentToken());
        }
    }

    /**
     * Normally returns a MapXNode. However, there are exceptions:
     * - JSON primitives/lists can be simulated by two-member object (@type + @value); in these cases we return respective XNode.
     * - Incomplete marker i.e. { "@incomplete" : "true" } should be interpreted as IncompleteMarkerXNode.
     */
    @NotNull
    private XNodeImpl parseJsonObject(JsonParsingContext ctx, IterativeParsingContext ipc) throws SchemaException, IOException {
        Validate.notNull(ctx.parser.currentToken());

        QName typeName = null;
        QNameUtil.QNameInfo elementNameInfo = null;

        Object tid = ctx.parser.getTypeId();
        if (tid != null) {
            typeName = tagToTypeName(tid, ctx);
        }

        final MapXNodeImpl map = new MapXNodeImpl();
        XNodeImpl wrappedValue = null;
        boolean defaultNamespaceDefined = false;
        QNameUtil.QNameInfo currentFieldNameInfo = null;
        Boolean incomplete = null;
        for (;;) {
            if (ipc != null && ipc.abortProcessing) {
                break;
            }

            JsonToken token = ctx.parser.nextToken();
            if (token == null) {
                ctx.prismParsingContext.warnOrThrow(LOGGER, "Unexpected end of data while parsing a map structure at " + getPositionSuffix(ctx));
                break;
            } else if (token == JsonToken.END_OBJECT) {
                break;
            } else if (token == JsonToken.FIELD_NAME) {
                String newFieldName = ctx.parser.getCurrentName();
                if (currentFieldNameInfo != null) {
                    ctx.prismParsingContext.warnOrThrow(LOGGER, "Two field names in succession: " + currentFieldNameInfo + " and " + newFieldName);
                }
                currentFieldNameInfo = QNameUtil.uriToQNameInfo(newFieldName, true);
            } else {
                assert currentFieldNameInfo != null;
                PrismContext prismContext = schemaRegistry.getPrismContext();
                // if we look for objects and found an entry different from c:objects
                boolean skipValue = false;
                if (ipc != null && !isSpecial(currentFieldNameInfo.name)
                        && (prismContext.getObjectsElementName() == null
                                || !QNameUtil.match(currentFieldNameInfo.name, prismContext.getObjectsElementName()))) {
                    if (ipc.dataSent) {
                        ctx.prismParsingContext.warnOrThrow(LOGGER, "Superfluous data after list of objects was found: " + currentFieldNameInfo.name);
                        skipValue = true;
                    } else {
                        ipc = null;
                    }
                }
                XNodeImpl valueXNode = parseValue(ctx, ipc);
                if (skipValue) {
                    continue;
                }
                if (isSpecial(currentFieldNameInfo.name)) {
                    if (isNamespaceDeclaration(currentFieldNameInfo.name)) {
                        if (defaultNamespaceDefined) {
                            ctx.prismParsingContext.warnOrThrow(LOGGER, "Default namespace defined more than once at " + getPositionSuffix(ctx));
                        }
                        String namespaceUri = getStringValue(valueXNode, currentFieldNameInfo, ctx);
                        ctx.defaultNamespaces.put(map, namespaceUri);
                        defaultNamespaceDefined = true;
                        if (ipc != null) {
                            if (ipc.dataSent) {
                                ctx.prismParsingContext.warnOrThrow(LOGGER, "When parsing list of objects, default namespace was present after the objects " + getPositionSuffix(ctx));
                            } else {
                                ipc.defaultNamespace = namespaceUri;    // there is a place for only one @ns declaration (at the level of "objects" map)
                            }
                        }
                    } else if (isTypeDeclaration(currentFieldNameInfo.name)) {
                        if (typeName != null) {
                            ctx.prismParsingContext.warnOrThrow(LOGGER, "Value type defined more than once at " + getPositionSuffix(ctx));
                        }
                        typeName = QNameUtil.uriToQName(getStringValue(valueXNode, currentFieldNameInfo, ctx), true);
                    } else if (isElementDeclaration(currentFieldNameInfo.name)) {
                        if (elementNameInfo != null) {
                            ctx.prismParsingContext.warnOrThrow(LOGGER, "Element name defined more than once at " + getPositionSuffix(ctx));
                        }
                        elementNameInfo = QNameUtil.uriToQNameInfo(getStringValue(valueXNode, currentFieldNameInfo, ctx), true);
                    } else if (isValue(currentFieldNameInfo.name)) {
                        if (wrappedValue != null) {
                            ctx.prismParsingContext.warnOrThrow(LOGGER, "Value ('" + PROP_VALUE + "') defined more than once at " + getPositionSuffix(ctx));
                        }
                        wrappedValue = valueXNode;
                    } else if (isIncompleteDeclaration(currentFieldNameInfo.name)) {
                        if (incomplete != null) {
                            ctx.prismParsingContext.warnOrThrow(LOGGER, "Duplicate @incomplete marker found with the value: " + valueXNode);
                        } else if (valueXNode instanceof PrimitiveXNodeImpl) {
                            //noinspection unchecked
                            Boolean value = ((PrimitiveXNodeImpl<Boolean>) valueXNode)
                                    .getParsedValue(DOMUtil.XSD_BOOLEAN, Boolean.class, ctx.prismParsingContext.getEvaluationMode());
                            incomplete = Boolean.TRUE.equals(value);
                        } else {
                            ctx.prismParsingContext.warnOrThrow(LOGGER, "@incomplete marker found with incompatible value: " + valueXNode);
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
            ctx.prismParsingContext.warnOrThrow(LOGGER, "More than one of '" + PROP_VALUE + "', '" + PROP_INCOMPLETE
                    + "' and regular content present at " + getPositionSuffix(ctx));
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
                ctx.prismParsingContext.warnOrThrow(LOGGER, "Conflicting type names for '" + PROP_VALUE
                        + "' (" + wrappedValue.getTypeQName() + ") and regular content (" + typeName + ") present at "
                        + getPositionSuffix(ctx));
            }
            rv.setTypeQName(typeName);
            rv.setExplicitTypeDeclaration(true);
        }
        if (elementNameInfo != null) {
            if (wrappedValue != null && wrappedValue.getElementName() != null) {
                boolean wrappedValueElementNoNamespace = ctx.noNamespaceElementNames.containsKey(wrappedValue);
                 if (!wrappedValue.getElementName().equals(elementNameInfo.name)
                         || wrappedValueElementNoNamespace != elementNameInfo.explicitEmptyNamespace) {
                     ctx.prismParsingContext.warnOrThrow(LOGGER, "Conflicting element names for '" + PROP_VALUE
                             + "' (" + wrappedValue.getElementName() + "; no NS=" + wrappedValueElementNoNamespace
                             + ") and regular content (" + elementNameInfo.name + "; no NS="
                             + elementNameInfo.explicitEmptyNamespace + ") present at "
                             + getPositionSuffix(ctx));
                 }
            }
            rv.setElementName(elementNameInfo.name);
            if (elementNameInfo.explicitEmptyNamespace) {
                ctx.noNamespaceElementNames.put(rv, null);
            }
        }
        return rv;
    }

    private String getStringValue(XNodeImpl valueXNode, QNameUtil.QNameInfo currentFieldNameInfo,
            JsonParsingContext ctx) throws SchemaException {
        String stringValue;
        if (!(valueXNode instanceof PrimitiveXNodeImpl)) {
            ctx.prismParsingContext.warnOrThrow(LOGGER, "Value of '" + currentFieldNameInfo + "' attribute must be a primitive one. It is " + valueXNode + " instead. At " + getPositionSuffix(ctx));
            stringValue = "";
        } else {
            stringValue = ((PrimitiveXNodeImpl<?>) valueXNode).getStringValue();
        }
        return stringValue;
    }

    private boolean isSpecial(QName fieldName) {
        return isTypeDeclaration(fieldName)
                || isIncompleteDeclaration(fieldName)
                || isElementDeclaration(fieldName)
                || isNamespaceDeclaration(fieldName)
                || isValue(fieldName);
    }

    private boolean isTypeDeclaration(QName fieldName) {
        return PROP_TYPE_QNAME.equals(fieldName);
    }

    private boolean isIncompleteDeclaration(QName fieldName) {
        return PROP_INCOMPLETE_QNAME.equals(fieldName);
    }

    private boolean isElementDeclaration(QName fieldName) {
        return PROP_ELEMENT_QNAME.equals(fieldName);
    }

    private boolean isNamespaceDeclaration(QName fieldName) {
        return PROP_NAMESPACE_QNAME.equals(fieldName);
    }

    private boolean isValue(QName fieldName) {
        return PROP_VALUE_QNAME.equals(fieldName);
    }

    private String getPositionSuffix(JsonParsingContext ctx) {
        return String.valueOf(ctx.parser.getCurrentLocation());
    }

    private ListXNodeImpl parseToList(JsonParsingContext ctx, IterativeParsingContext ipc) throws SchemaException, IOException {
        Validate.notNull(ctx.parser.currentToken());

        ListXNodeImpl list = new ListXNodeImpl();
        Object tid = ctx.parser.getTypeId();
        if (tid != null) {
            list.setTypeQName(tagToTypeName(tid, ctx));
        }
        if (ipc != null) {
            ipc.dataSent = true;
        }
        for (;;) {
            JsonToken token = ctx.parser.nextToken();
            if (token == null) {
                ctx.prismParsingContext.warnOrThrow(LOGGER, "Unexpected end of data while parsing a list structure at " + getPositionSuffix(ctx));
                return list;
            } else if (token == JsonToken.END_ARRAY) {
                return list;
            } else {
                if (ipc != null) {
                    JsonParsingContext childCtx = ctx.createChildContext();
                    XNodeImpl value = parseValue(childCtx, null);
                    List<RootXNodeImpl> childRoots = valueToRootList(value, ipc.defaultNamespace, childCtx);
                    for (RootXNodeImpl childRoot : childRoots) {
                        if (!ipc.handler.handleData(childRoot)) {
                            ipc.abortProcessing = true;
                            assert list.isEmpty();
                            return list;
                        }
                    }
                } else {
                    list.add(parseValue(ctx, null));
                }
            }
        }
    }

    private <T> PrimitiveXNodeImpl<T> parseToPrimitive(JsonParsingContext ctx) throws IOException, SchemaException {
        PrimitiveXNodeImpl<T> primitive = new PrimitiveXNodeImpl<>();

        Object tid = ctx.parser.getTypeId();
        if (tid != null) {
            QName typeName = tagToTypeName(tid, ctx);
            primitive.setTypeQName(typeName);
            primitive.setExplicitTypeDeclaration(true);
        } else {
            // We don't try to determine XNode type from the implicit JSON/YAML type (integer, number, ...),
            // because XNode type prescribes interpretation in midPoint. E.g. YAML string type would be interpreted
            // as xsd:string, even if the schema would expect e.g. timestamp.
        }

        JsonNode jn = ctx.parser.readValueAs(JsonNode.class);
        ValueParser<T> vp = new JsonValueParser<>(ctx.parser, jn);
        primitive.setValueParser(vp);

        return primitive;
    }

    private <T> PrimitiveXNodeImpl<T> parseToEmptyPrimitive() {
        PrimitiveXNodeImpl<T> primitive = new PrimitiveXNodeImpl<>();
        primitive.setValueParser(new JsonNullValueParser<>());
        return primitive;
    }

    @SuppressWarnings("unused") // TODO remove if not needed
    private QName getCurrentTypeName(JsonParsingContext ctx) throws IOException, SchemaException {
        switch (ctx.parser.currentToken()) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return determineNumberType(ctx.parser.getNumberType());
            case VALUE_FALSE:
            case VALUE_TRUE:
                return DOMUtil.XSD_BOOLEAN;
            case VALUE_STRING:
                return DOMUtil.XSD_STRING;
            case VALUE_NULL:
                return null;            // TODO?
            default:
                throw new SchemaException("Unexpected current token type: " + ctx.parser.currentToken() + "/" + ctx.parser.getText() + " at " + getPositionSuffix(ctx));
        }
    }

    QName determineNumberType(JsonParser.NumberType numberType) throws SchemaException {
        switch (numberType) {
            case BIG_DECIMAL:
                return DOMUtil.XSD_DECIMAL;
            case BIG_INTEGER:
                return DOMUtil.XSD_INTEGER;
            case LONG:
                return DOMUtil.XSD_LONG;
            case INT:
                return DOMUtil.XSD_INT;
            case FLOAT:
                return DOMUtil.XSD_FLOAT;
            case DOUBLE:
                return DOMUtil.XSD_DOUBLE;
            default:
                throw new SchemaException("Unsupported number type: " + numberType);
        }
    }

    protected abstract QName tagToTypeName(Object tid, JsonParsingContext ctx) throws IOException, SchemaException;

    private JsonParser configureParser(JsonParser parser) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule sm = new SimpleModule();
        sm.addDeserializer(QName.class, new QNameDeserializer());
        sm.addDeserializer(UniformItemPath.class, new ItemPathDeserializer());
        sm.addDeserializer(ItemPath.class, new ItemPathDeserializer());
        sm.addDeserializer(PolyString.class, new PolyStringDeserializer());
        sm.addDeserializer(ItemPathType.class, new ItemPathTypeDeserializer());

        mapper.registerModule(sm);
        parser.setCodec(mapper);
        return parser;
    }

    //endregion

    //region Serialization implementation

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
    @Override
    public String write(@NotNull XNode xnode, @NotNull QName rootElementName, SerializationContext serializationContext) throws SchemaException {
        return write(LexicalUtils.createRootXNode((XNodeImpl) xnode, rootElementName), serializationContext);
    }

    protected abstract JsonGenerator createJacksonGenerator(StringWriter out) throws SchemaException;

    @NotNull
    @Override
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
    @Override
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
            ctx.generator.writeFieldName(PROP_VALUE);
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
            ctx.generator.writeFieldName(PROP_INCOMPLETE);
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
            ctx.generator.writeObjectField(PROP_ELEMENT, createElementNameUri(elementName, ctx));
        }
        QName typeName = getExplicitType(xnode);
        if (typeName != null) {
            if (!supportsInlineTypes()) {
                ctx.generator.writeObjectField(PROP_TYPE, typeName);
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
        for (Entry<QName, XNodeImpl> entry : map.entrySet()) {
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
            ctx.generator.writeFieldName(PROP_NAMESPACE);
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
        Entry<String,Integer> max = null;
        for (Entry<String,Integer> count : counts.entrySet()) {
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

    private String createKeyUri(Entry<QName, XNodeImpl> entry, JsonSerializationContext ctx) {
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
                generator.writeStringField(PROP_NAMESPACE, globalNamespace);

            }
        }
        return globalNamespace;
    }
    //endregion

}
