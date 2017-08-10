/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.lex.json;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.lex.LexicalUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public abstract class AbstractJsonLexicalProcessor implements LexicalProcessor<String> {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractJsonLexicalProcessor.class);
	
	private static final String PROP_NAMESPACE = "@ns";
	private static final String PROP_TYPE = "@type";
	private static final String PROP_ELEMENT = "@element";
	private static final String PROP_VALUE = "@value";

	//region Parsing implementation

	@NotNull
	@Override
	public RootXNode read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
		InputStream is = source.getInputStream();
		try {
			JsonParser parser = createJacksonParser(is);
			return parseFromStart(parser, parsingContext);
		} finally {
			if (source.closeStreamAfterParsing()) {
				IOUtils.closeQuietly(is);
			}
		}
	}

	// TODO support also multi-document YAML files
	@NotNull
	@Override
	public List<RootXNode> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
		RootXNode root = read(source, parsingContext);
		if (root.getSubnode() instanceof ListXNode) {
			// TODO we could check if we really get 'objects' node here, just to be sure we can parse it as a list of objects
			ListXNode items = (ListXNode) root.getSubnode();
			List<RootXNode> rv = new ArrayList<>();
			for (XNode item : items) {
				if (!item.isSingleEntryMap()) {
					throw new SchemaException("When reading list of objects: Expected single-entry MapXNode, got " + item + " instead: " + item.debugDump());
				}
				rv.add(((MapXNode) item).getSingleEntryMapAsRoot());
			}
			return rv;
		} else {
			// seems to be a single node
			return Collections.singletonList(root);
		}
	}

    protected abstract JsonParser createJacksonParser(InputStream stream) throws SchemaException, IOException;

	class JsonParsingContext {
		@NotNull final JsonParser parser;
		@NotNull final ParsingContext prismParsingContext;
		// Definitions of namespaces ('@ns') within maps; to be applied after parsing.
		@NotNull final IdentityHashMap<MapXNode, String> defaultNamespaces = new IdentityHashMap<>();
		// Entries that should be skipped when filling-in default namespaces - those that are explicitly set with no-NS ('#name').
		// (Values for these entries are not important. Only key presence is relevant.)
		@NotNull final IdentityHashMap<Entry<QName,XNode>, Object> noNamespaceEntries = new IdentityHashMap<>();
		@NotNull final IdentityHashMap<XNode, Object> noNamespaceElementNames = new IdentityHashMap<>();
		JsonParsingContext(@NotNull JsonParser parser, @NotNull ParsingContext prismParsingContext) {
			this.parser = parser;
			this.prismParsingContext = prismParsingContext;
		}
	}

	@NotNull
	private RootXNode parseFromStart(JsonParser unconfiguredParser, ParsingContext parsingContext) throws SchemaException {
		JsonParsingContext ctx = null;
		try {
			JsonParser parser = configureParser(unconfiguredParser);
			parser.nextToken();
			if (parser.currentToken() == null) {
				throw new SchemaException("Nothing to parse: the input is empty.");
			}
			ctx = new JsonParsingContext(parser, parsingContext);
			XNode xnode = parseValue(ctx);
			if (!(xnode instanceof MapXNode) || ((MapXNode) xnode).size() != 1) {
				throw new SchemaException("Expected MapXNode with a single key; got " + xnode + " instead. At " + getPositionSuffix(ctx));
			}
			processDefaultNamespaces(xnode, null, ctx);
			processSchemaNodes(xnode);
			Entry<QName, XNode> entry = ((MapXNode) xnode).entrySet().iterator().next();
			RootXNode root = new RootXNode(entry.getKey(), entry.getValue());
			if (entry.getValue() != null) {
				root.setTypeQName(entry.getValue().getTypeQName());			// TODO - ok ????
			}
			return root;
		} catch (IOException e) {
			throw new SchemaException("Cannot parse JSON/YAML object: " + e.getMessage() +
					(ctx != null ? " At: " + getPositionSuffix(ctx) : ""),
					e);
		}
	}

	// Default namespaces (@ns properties) are processed in the second pass, because they might be present within an object
	// at any place, even at the end.
	private void processDefaultNamespaces(XNode xnode, String parentDefault, JsonParsingContext ctx) {
		if (xnode instanceof MapXNode) {
			MapXNode map = (MapXNode) xnode;
			final String currentDefault = ctx.defaultNamespaces.containsKey(map) ? ctx.defaultNamespaces.get(map) : parentDefault;
			for (Entry<QName, XNode> entry : map.entrySet()) {
				QName fieldName = entry.getKey();
				XNode subnode = entry.getValue();
				if (StringUtils.isNotEmpty(currentDefault)
						&& StringUtils.isEmpty(fieldName.getNamespaceURI())
						&& !ctx.noNamespaceEntries.containsKey(entry)) {
					map.qualifyKey(fieldName, currentDefault);
				}
				processDefaultNamespaces(subnode, currentDefault, ctx);
			}
			qualifyElementNameIfNeeded(map, currentDefault, ctx);
		} else {
			qualifyElementNameIfNeeded(xnode, parentDefault, ctx);
			if (xnode instanceof ListXNode) {
				for (XNode item : (ListXNode) xnode) {
					processDefaultNamespaces(item, parentDefault, ctx);
				}
			}
		}
	}

	private void qualifyElementNameIfNeeded(XNode node, String namespace, JsonParsingContext ctx) {
		if (node.getElementName() != null
				&& QNameUtil.noNamespace(node.getElementName())
				&& StringUtils.isNotEmpty(namespace)
				&& !ctx.noNamespaceElementNames.containsKey(node)) {
			node.setElementName(new QName(namespace, node.getElementName().getLocalPart()));
		}
	}

	// Schema nodes can be detected only after namespaces are resolved.
	// We simply convert primitive nodes to schema ones.
	private void processSchemaNodes(XNode xnode) throws SchemaException, IOException {
		if (xnode instanceof MapXNode) {
			MapXNode map = (MapXNode) xnode;
			XNode schemaNode = null;
			for (Entry<QName, XNode> entry : map.entrySet()) {
				QName fieldName = entry.getKey();
				XNode subnode = entry.getValue();
				if (DOMUtil.XSD_SCHEMA_ELEMENT.equals(fieldName)) {
					schemaNode = subnode;
				} else {
					processSchemaNodes(subnode);
				}
			}
			if (schemaNode != null) {
				if (schemaNode instanceof PrimitiveXNode) {
					PrimitiveXNode<?> primitiveXNode = (PrimitiveXNode<?>) schemaNode ;
					if (primitiveXNode.isParsed()) {
						throw new SchemaException("Cannot convert from PrimitiveXNode to SchemaXNode: node is already parsed: " + primitiveXNode);
					}
					SchemaXNode schemaXNode = new SchemaXNode();
					map.replace(DOMUtil.XSD_SCHEMA_ELEMENT, schemaXNode);
					schemaXNode.setSchemaElement(((JsonValueParser) primitiveXNode.getValueParser()).asDomElement());
				} else {
					throw new SchemaException("Cannot convert 'schema' field to SchemaXNode: not a PrimitiveNode but " + schemaNode);
				}
			}
		} else if (xnode instanceof ListXNode) {
			for (XNode item : (ListXNode) xnode) {
				processSchemaNodes(item);
			}
		}
	}

	@NotNull
	private XNode parseValue(JsonParsingContext ctx) throws IOException, SchemaException {
		Validate.notNull(ctx.parser.currentToken());

		switch (ctx.parser.currentToken()) {
			case START_OBJECT:
				return parseJsonObject(ctx);
			case START_ARRAY:
				return parseToList(ctx);
			case VALUE_STRING:
			case VALUE_TRUE:
			case VALUE_FALSE:
			case VALUE_NUMBER_FLOAT:
			case VALUE_NUMBER_INT:
				return parseToPrimitive(ctx);
			case VALUE_NULL:
				return parseToEmptyPrimitive();
			default:
				throw new SchemaException("Unexpected current token: " + ctx.parser.currentToken());
		}
	}

	/**
	 * Normally returns a MapXNode. However, JSON primitives/lists can be simulated by two-member object (@type + @value); in these cases we return respective XNode.
	 */
	@NotNull
	private XNode parseJsonObject(JsonParsingContext ctx) throws SchemaException, IOException {
		Validate.notNull(ctx.parser.currentToken());

		QName typeName = null;
		QNameUtil.QNameInfo elementNameInfo = null;

		Object tid = ctx.parser.getTypeId();
		if (tid != null) {
			typeName = tagToTypeName(tid, ctx);
		}

		final MapXNode map = new MapXNode();
		XNode wrappedValue = null;
		boolean defaultNamespaceDefined = false;
		QNameUtil.QNameInfo currentFieldNameInfo = null;
		for (;;) {
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
				XNode valueXNode = parseValue(ctx);
				assert currentFieldNameInfo != null;
				if (isSpecial(currentFieldNameInfo.name)) {
					if (isNamespaceDeclaration(currentFieldNameInfo.name)) {
						if (defaultNamespaceDefined) {
							ctx.prismParsingContext.warnOrThrow(LOGGER, "Default namespace defined more than once at " + getPositionSuffix(ctx));
						}
						ctx.defaultNamespaces.put(map, getStringValue(valueXNode, currentFieldNameInfo, ctx));
						defaultNamespaceDefined = true;
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
					}
				} else {
					Map.Entry<QName, XNode> entry = map.putReturningEntry(currentFieldNameInfo.name, valueXNode);
					if (currentFieldNameInfo.explicitEmptyNamespace) {
						ctx.noNamespaceEntries.put(entry, null);
					}
				}
				currentFieldNameInfo = null;
			}
		}
		// Return either map or primitive value (in case of @type/@value)
		XNode rv;
		if (wrappedValue != null) {
			if (!map.isEmpty()) {
				ctx.prismParsingContext.warnOrThrow(LOGGER, "Both '" + PROP_VALUE + "' and regular content present at " + getPositionSuffix(ctx));
				rv = map;
			} else {
				rv = wrappedValue;
			}
		} else {
			rv = map;
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

	private String getStringValue(XNode valueXNode, QNameUtil.QNameInfo currentFieldNameInfo,
			JsonParsingContext ctx) throws SchemaException {
		String stringValue;
		if (!(valueXNode instanceof PrimitiveXNode)) {
			ctx.prismParsingContext.warnOrThrow(LOGGER, "Value of '" + currentFieldNameInfo + "' attribute must be a primitive one. It is " + valueXNode + " instead. At " + getPositionSuffix(ctx));
			stringValue = "";
		} else {
			stringValue = ((PrimitiveXNode<?>) valueXNode).getStringValue();
		}
		return stringValue;
	}

	private boolean isSpecial(QName fieldName) {
		return isTypeDeclaration(fieldName)
				|| isElementDeclaration(fieldName)
				|| isNamespaceDeclaration(fieldName)
				|| isValue(fieldName);
	}

	private boolean isTypeDeclaration(QName fieldName) {
		return new QName(PROP_TYPE).equals(fieldName);
	}

	private boolean isElementDeclaration(QName fieldName) {
		return new QName(PROP_ELEMENT).equals(fieldName);
	}

	private boolean isNamespaceDeclaration(QName fieldName) {
		return new QName(PROP_NAMESPACE).equals(fieldName);
	}

	private boolean isValue(QName fieldName) {
		return new QName(PROP_VALUE).equals(fieldName);
	}

	private String getPositionSuffix(JsonParsingContext ctx) {
		return String.valueOf(ctx.parser.getCurrentLocation());
	}

	private ListXNode parseToList(JsonParsingContext ctx) throws SchemaException, IOException {
		Validate.notNull(ctx.parser.currentToken());

		ListXNode list = new ListXNode();
		Object tid = ctx.parser.getTypeId();
		if (tid != null) {
			list.setTypeQName(tagToTypeName(tid, ctx));
		}
		for (;;) {
			JsonToken token = ctx.parser.nextToken();
			if (token == null) {
				ctx.prismParsingContext.warnOrThrow(LOGGER, "Unexpected end of data while parsing a list structure at " + getPositionSuffix(ctx));
				return list;
			} else if (token == JsonToken.END_ARRAY) {
				return list;
			} else {
				list.add(parseValue(ctx));
			}
		}
	}

	private <T> PrimitiveXNode<T> parseToPrimitive(JsonParsingContext ctx) throws IOException, SchemaException {
		PrimitiveXNode<T> primitive = new PrimitiveXNode<T>();

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
		ValueParser<T> vp = new JsonValueParser<T>(ctx.parser, jn);
		primitive.setValueParser(vp);

		return primitive;
	}

	private <T> PrimitiveXNode<T> parseToEmptyPrimitive() throws IOException, SchemaException {
		PrimitiveXNode<T> primitive = new PrimitiveXNode<T>();
		primitive.setValueParser(new JsonNullValueParser<T>());
		return primitive;
	}

	// TODO remove if not needed
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
				return null;			// TODO?
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
		sm.addDeserializer(ItemPath.class, new ItemPathDeserializer());
		sm.addDeserializer(PolyString.class, new PolyStringDeserializer());
		sm.addDeserializer(ItemPathType.class, new ItemPathTypeDeserializer());

		mapper.registerModule(sm);
		parser.setCodec(mapper);
		return parser;
	}

	//endregion

	//region Serialization implementation

	class JsonSerializationContext {
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
		return write(LexicalUtils.createRootXNode(xnode, rootElementName), serializationContext);
	}

	protected abstract JsonGenerator createJacksonGenerator(StringWriter out) throws SchemaException;

	@NotNull
	@Override
	public String write(@NotNull RootXNode root, SerializationContext prismSerializationContext) throws SchemaException {
		StringWriter out = new StringWriter();
		try ( JsonGenerator generator = createJacksonGenerator(out) ) {
			JsonSerializationContext ctx = new JsonSerializationContext(generator, prismSerializationContext);
			serialize(root.toMapXNode(), ctx, false);				// TODO default namespace
		} catch (IOException ex) {
			throw new SchemaException("Error during serializing to JSON/YAML: " + ex.getMessage(), ex);
		}
		return out.toString();
	}

	// TODO support also multi-document YAML files
	@NotNull
	@Override
	public String write(@NotNull List<RootXNode> roots, @NotNull QName aggregateElementName,
			@Nullable SerializationContext prismSerializationContext) throws SchemaException {
		ListXNode objectsList = new ListXNode();
		for (RootXNode root : roots) {
			objectsList.add(root.toMapXNode());
		}
		RootXNode aggregate = new RootXNode(aggregateElementName, objectsList);
		return write(aggregate, prismSerializationContext);
	}

	private void serialize(XNode xnode, JsonSerializationContext ctx, boolean inValueWrapMode) throws IOException {
		if (xnode instanceof MapXNode) {
			serializeFromMap((MapXNode) xnode, ctx);
		} else if (xnode == null) {
			serializeFromNull(ctx);
		} else if (needsValueWrapping(xnode) && !inValueWrapMode) {
			ctx.generator.writeStartObject();
			resetInlineTypeIfPossible(ctx);
			writeAuxiliaryInformation(xnode, ctx);
			ctx.generator.writeFieldName(PROP_VALUE);
			serialize(xnode, ctx, true);
			ctx.generator.writeEndObject();
		} else if (xnode instanceof ListXNode) {
			serializeFromList((ListXNode) xnode, ctx);
		} else if (xnode instanceof PrimitiveXNode) {
			serializeFromPrimitive((PrimitiveXNode<?>) xnode, ctx);
		} else if (xnode instanceof SchemaXNode) {
			serializeFromSchema((SchemaXNode) xnode, ctx);
		} else {
			throw new UnsupportedOperationException("Cannot serialize from " + xnode);
		}
	}

	private void serializeFromNull(JsonSerializationContext ctx) throws IOException {
		ctx.generator.writeNull();
	}

	private void writeAuxiliaryInformation(XNode xnode, JsonSerializationContext ctx) throws IOException {
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

	private boolean needsValueWrapping(XNode xnode) {
		return xnode.getElementName() != null
				|| getExplicitType(xnode) != null && !supportsInlineTypes();
	}

	protected abstract boolean supportsInlineTypes();

	protected abstract void writeInlineType(QName typeName, JsonSerializationContext ctx) throws IOException;

	private void serializeFromMap(MapXNode map, JsonSerializationContext ctx) throws IOException {
		writeInlineTypeIfNeeded(map, ctx);
		ctx.generator.writeStartObject();
		resetInlineTypeIfPossible(ctx);
		String oldDefaultNamespace = ctx.currentNamespace;
		generateNsDeclarationIfNeeded(map, ctx);
		writeAuxiliaryInformation(map, ctx);
		for (Entry<QName,XNode> entry : map.entrySet()) {
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

	private void generateNsDeclarationIfNeeded(MapXNode map, JsonSerializationContext ctx) throws IOException {
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

	private String determineNewCurrentNamespace(MapXNode map, JsonSerializationContext ctx) {
		Map<String,Integer> counts = new HashMap<>();
		for (QName childName : map.keySet()) {
			String childNs = childName.getNamespaceURI();
			if (StringUtils.isEmpty(childNs)) {
				continue;
			}
			if (childNs.equals(ctx.currentNamespace)) {
				return ctx.currentNamespace;					// found existing => continue with it
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

	private String createKeyUri(Entry<QName,XNode> entry, JsonSerializationContext ctx) {
		QName key = entry.getKey();
		if (namespaceMatch(ctx.currentNamespace, key.getNamespaceURI())) {
			return key.getLocalPart();
		} else if (StringUtils.isNotEmpty(ctx.currentNamespace) && !isAttribute(entry.getValue())) {
			return QNameUtil.qNameToUri(key, true);		// items with no namespace should be written as such (starting with '#')
		} else {
			return QNameUtil.qNameToUri(key, false);	// items with no namespace can be written in plain
		}
	}

	private String createElementNameUri(QName elementName, JsonSerializationContext ctx) {
		if (namespaceMatch(ctx.currentNamespace, elementName.getNamespaceURI())) {
			return elementName.getLocalPart();
		} else {
			return QNameUtil.qNameToUri(elementName, StringUtils.isNotEmpty(ctx.currentNamespace));
		}
	}

	private boolean isAttribute(XNode node) {
		return node instanceof PrimitiveXNode && ((PrimitiveXNode) node).isAttribute();
	}

	private boolean namespaceMatch(String currentNamespace, String itemNamespace) {
		if (StringUtils.isEmpty(currentNamespace)) {
			return StringUtils.isEmpty(itemNamespace);
		} else {
			return currentNamespace.equals(itemNamespace);
		}
	}

	private void serializeFromList(ListXNode list, JsonSerializationContext ctx) throws IOException {
		writeInlineTypeIfNeeded(list, ctx);
		ctx.generator.writeStartArray();
		resetInlineTypeIfPossible(ctx);
		for (XNode item : list) {
			serialize(item, ctx, false);
		}
		ctx.generator.writeEndArray();
	}

	private void writeInlineTypeIfNeeded(XNode node, JsonSerializationContext ctx) throws IOException {
		QName explicitType = getExplicitType(node);
		if (supportsInlineTypes() && explicitType != null) {
			writeInlineType(explicitType, ctx);
		}
	}

	private void serializeFromSchema(SchemaXNode node, JsonSerializationContext ctx) throws IOException {
		writeInlineTypeIfNeeded(node, ctx);
		ctx.generator.writeObject(node.getSchemaElement());
	}


	private <T> void serializeFromPrimitive(PrimitiveXNode<T> primitive, JsonSerializationContext ctx) throws IOException {
		writeInlineTypeIfNeeded(primitive, ctx);
		if (primitive.isParsed()) {
			ctx.generator.writeObject(primitive.getValue());
		} else {
			ctx.generator.writeObject(primitive.getStringValue());
		}
	}

	protected QName getExplicitType(XNode xnode) {
		return xnode.isExplicitTypeDeclaration() ? xnode.getTypeQName() : null;
	}
	
	private String serializeNsIfNeeded(QName subNodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
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
