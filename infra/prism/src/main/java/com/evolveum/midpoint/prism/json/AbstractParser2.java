package com.evolveum.midpoint.prism.json;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

public abstract class AbstractParser2 implements Parser {
	
	private static final String PROP_NAMESPACE = "@ns";
	private static final String TYPE_DEFINITION = "@typeDef";
	private static final String VALUE_FIELD = "@value";
	
	
	@Override
	public XNode parse(File file) throws SchemaException, IOException {
		JsonParser parser = createParser(file);
		return parseObject(parser);
	}
	
	protected abstract JsonParser createParser(String dataString)  throws SchemaException;
	protected abstract JsonParser createParser(File file)  throws SchemaException, IOException ;
	
	@Override
	public XNode parse(String dataString) throws SchemaException {
//		JsonFactory factory = new JsonFactory();
//		JsonParser parser = null;
//		try {
//			parser = factory.createParser(dataString);
//		} catch (IOException e) {
//			throw new SchemaException("Cannot create JSON parser: " + e.getMessage(), e);
//		}
		JsonParser parser = createParser(dataString);
		return parseObject(parser);
	}

	@Override
	public boolean canParse(File file) throws IOException {
		if (file == null) {
			return false;
		}
		return file.getName().endsWith(".json");
	}

	@Override
	public boolean canParse(String dataString) {
		if (dataString == null) {
			return false;
		}
		return dataString.startsWith("{");
	}

	@Override
	public String serializeToString(XNode xnode, QName rootElementName) throws SchemaException {
		if (xnode instanceof RootXNode){
			xnode = ((RootXNode) xnode).getSubnode();
		}
		return serializeToJson(xnode, rootElementName);
	}

	@Override
	public String serializeToString(RootXNode xnode) throws SchemaException {
		QName rootElementName = xnode.getRootElementName();
		return serializeToJson(xnode.getSubnode(), rootElementName);
	}
	
	
	// ------------------- METHODS FOR SERIALIZATION ------------------------------
//	String globalNamespace = null;
	public String serializeToJson(XNode node, QName rootElement) throws SchemaException{
		try { 
//			globalNamespace = rootElement.getNamespaceURI();
			StringWriter out = new StringWriter();
			JsonGenerator generator = createGenerator(out);
			return writeObject(node, rootElement, generator, out);
		} catch (IOException ex){
			throw new SchemaException("Schema error during serializing to JSON.", ex);
		}

	}
	
	private String writeObject(XNode node, QName rootElement, JsonGenerator generator, StringWriter out) throws JsonGenerationException, IOException{
		generator.writeStartObject();
		generator.writeStringField(PROP_NAMESPACE, rootElement.getNamespaceURI());
		serializeToJson(node, rootElement, null, generator);
		generator.writeEndObject();
		generator.flush();
		generator.close();
		return out.toString();
	}
		
	
	String objectNs = null;
	private <T> void  serializeToJson(XNode node, QName nodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		
		if (node instanceof MapXNode){
			serializerFromMap((MapXNode) node, nodeName, globalNamespace, generator);
		} else if (node instanceof ListXNode){
			serializeFromList((ListXNode) node, nodeName, globalNamespace, generator);
		} else if (node instanceof PrimitiveXNode){
			serializeFromPrimitive((PrimitiveXNode) node, nodeName, generator);
		}
	}
	
	
	private void serializerFromMap(MapXNode map, QName nodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		if (nodeName == null){
			generator.writeStartObject();
		} else{
			System.out.println("writing node: " + nodeName.getLocalPart());
			generator.writeObjectFieldStart(nodeName.getLocalPart());
		}
		
		// this is used only by first iteration..we need to set namespace right after the root element
		if (StringUtils.isBlank(globalNamespace)){
			globalNamespace = nodeName.getNamespaceURI();
			generator.writeStringField(PROP_NAMESPACE, globalNamespace);
		}
		
		
		
		Iterator<Entry<QName, XNode>> subnodes = map.entrySet().iterator();
		while (subnodes.hasNext()){
			Entry<QName, XNode> subNode = subnodes.next();
			globalNamespace = serializeNsIfNeeded(subNode.getKey(), globalNamespace, generator);
			serializeToJson(subNode.getValue(), subNode. getKey(), globalNamespace, generator);
		}
		generator.writeEndObject();
	}
	
	private void serializeFromList(ListXNode list, QName nodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		ListIterator<XNode> sublist = list.listIterator();
		generator.writeArrayFieldStart(nodeName.getLocalPart());
		while (sublist.hasNext()){
			serializeToJson(sublist.next(), null, globalNamespace, generator);
		}
		generator.writeEndArray();
	}
	
	private void serializeFromPrimitive(PrimitiveXNode primitive, QName nodeName, JsonGenerator generator) throws JsonGenerationException, IOException{
		
//		if (primitive.isExplicitTypeDeclaration()) {
//			generator.writeStartObject();
//			generator.writeFieldName(TYPE_DEFINITION);
//			generator.writeObject(primitive.getTypeQName());
//
//			generator.writeObjectField(VALUE_FIELD, primitive.getValue());
//			generator.writeEndObject();
//		} else {
//			
			QName explicit = null;
			if (primitive.isExplicitTypeDeclaration()){
				explicit = primitive.getTypeQName();
			}

			if (explicit != null){
				System.out.println("can write: " +generator.canWriteTypeId());
				if (generator.canWriteTypeId()){
				System.out.println("writing type ID");
				System.out.println("explicit: " + explicit);
				if (explicit.equals(DOMUtil.XSD_STRING)){
					generator.writeTypeId("http://www.w3.org/2001/XMLSchema/string");
				} else if (explicit.equals(DOMUtil.XSD_INT)){
					System.out.println("writing int explicit: " + explicit);
					generator.writeTypeId("http://www.w3.org/2001/XMLSchema/int");
					generator.writeString(String.valueOf(primitive.getValue()));
					return;
				}
				}else{
					generator.writeStartObject();
					generator.writeStringField(TYPE_DEFINITION, QNameUtil.qNameToUri(primitive.getTypeQName()));
//					generator.writeObject();
		//
					generator.writeObjectField(VALUE_FIELD, primitive.getValue());
					generator.writeEndObject();
					return;
//					
				}
				
				
			}
			
			if (nodeName == null) {
				generator.writeObject(primitive.getValue());
			} else {
//				if (StringUtils.isNotBlank(nodeName.getNamespaceURI())
//						&& !nodeName.getNamespaceURI().equals(objectNs)) {
//					objectNs = nodeName.getNamespaceURI();
//				}
//				generator.
				generator.writeObjectField(nodeName.getLocalPart(), primitive.getValue());
			}
//		}
	}
	
	private String serializeNsIfNeeded(QName subNodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		if (subNodeName == null){
			return globalNamespace;
		}
		String subNodeNs = subNodeName.getNamespaceURI();
		if (StringUtils.isNotEmpty(subNodeNs)){
			if (!subNodeNs.equals(globalNamespace)){
				globalNamespace = subNodeNs;
//				generator.writeObjectId("other");
				generator.writeStringField(PROP_NAMESPACE, globalNamespace);
				
			}
		}
		return globalNamespace;
	}
	//------------------------END OF METHODS FOR SERIALIZATION -------------------------------
	
	//------------------------ METHODS FOR PARSING -------------------------------------------
	public XNode parseObject(JsonParser parser) throws SchemaException{
		
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule sm = new SimpleModule();
		sm.addDeserializer(QName.class, new QNameDeserializer());
		sm.addDeserializer(ItemPath.class, new ItemPathDeserializer());
		
		mapper.registerModule(sm);
		
		JsonNode obj = null;
		try {
			parser.setCodec(mapper);

			JsonToken t = parser.getCurrentToken();
			if (t == null){
				t = parser.nextToken();
			}
			
			if (t == null){
				t = parser.nextToken();
			}
			
			
			String rootNodeName = null;
			if (t == JsonToken.FIELD_NAME){
				rootNodeName = parser.getText();
			}
			
						
			RootXNode xmap = new RootXNode();
			
//			QName rootElement = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "user");
//			MapXNode subMap = new MapXNode();
//			xmap.setSubnode(subMap);
//			((RootXNode) xmap).setRootElementName(rootElement);
//				((RootXNode) xmap).setRootElementName(rootElement);
//			parser.
				while (parser.nextToken() != null){
					parseJsonObject(xmap, null, parser);
				}
				
//			}
//				cleanUpEmptyNodes(xmap);
			 return xmap;
		} catch (JsonParseException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		}
	}
	
		
	private <T> void parseJsonObject(XNode xmap, QName propertyName, final JsonParser parser) throws SchemaException {
		try{
			JsonToken token = parser.getCurrentToken();
//			System.out.println("token " + token);
//			JsonToken current = parser.getCurrentToken();
//			System.out.println("current " + current);
//			JsonToken value = parser.nextValue();
//			System.out.println("value " + value);
//			if (token == null){
//				token = parser.nextToken();
//			}
			String ns = null;
			if (propertyName != null){
				ns = propertyName.getNamespaceURI();
			}
			
			
			if (token == null){
				return;
			}
			
			if (token == JsonToken.FIELD_NAME){
			String fieldName = parser.getText();
				if (fieldName.startsWith(PROP_NAMESPACE)){
					ns = parser.nextTextValue();
					propertyName = new QName(ns, fieldName);
					
					token = parser.nextToken();
					if (token == JsonToken.FIELD_NAME){
						fieldName = parser.getText();
						propertyName = new QName(ns, fieldName);
					}
//					parseJsonObject(xmap, propertyName, parser);
				} else{
//				if ()
				propertyName = new QName(ns, fieldName);
				}
				token = parser.nextToken();
				System.out.println("filedName: " + propertyName );
				
			}
			
			
			
		switch (token){
//			case FIELD_NAME:
				
//				token = parser.nextToken();
//				parseJsonObject(xmap, propertyName, parser);
//				break;
			case START_OBJECT:
//				JsonFactory f=  new JsonFactory();
//				JsonValueParser jp = new JsonValueParser<>(parser);
				
//				final JsonParser parserCopy = parser;
//				if (!(xmap instanceof RootXNode)){
//				if (parser.nextToken() == JsonToken.FIELD_NAME ){
//					if (parser.getCurrentName().startsWith("@") && !parser.getCurrentName().equals("@ns")){
//						JsonNode obj = parser.readValueAs(JsonNode.class);
//						parseSpecial(xmap, propertyName, obj, parser);
//					}else{
//					parseJsonObject(xmap, new QName(propertyName.getNamespaceURI(), parser.getCurrentName()), parserCopy);
//					
//					}
//				}
//				break;
//				}
//				if (fieldName.startsWith("@")){
					
				
//				System.out.println("oid:  " + parser.getObjectId());
//				System.out.println("tid:  " + parser.getTypeId());
				parseToMap(propertyName, xmap, parser);
				break;
				
			case START_ARRAY:
//				final JsonParser parserCopy2 = parser;
//				if (!(xmap instanceof RootXNode)){
//				if (parser.nextToken() == JsonToken.FIELD_NAME ){
//					if (parser.getCurrentName().startsWith("@") && !parser.getCurrentName().equals("@ns")){
//						JsonNode obj = parser.readValueAs(JsonNode.class);
//						parseSpecial(xmap, propertyName, obj, parser);
//					} else{
//					parseJsonObject(xmap, new QName(propertyName.getNamespaceURI(), parser.getCurrentName()), parserCopy2);
//					}
//				}
//				break;
//				}
//				System.out.println("ARRAYoid:  " + parser.getObjectId());
//				System.out.println("ARRAYtid:  " + parser.getTypeId());
				parseToList(propertyName, xmap, parser);
				break;
			case VALUE_STRING:
			case VALUE_TRUE:
			case VALUE_FALSE:
			case VALUE_NUMBER_FLOAT:
			case VALUE_NUMBER_INT:
				parseToPrimitive(propertyName, xmap, parser);
				break;
			default:
				System.out.println("DEFAULT SWICH NODE");
				break;
				
		}
//		parseJsonObject(xmap, propertyName, parser);
		} catch (Exception e){
			//TODO: 
			throw new SchemaException("Error ", e);
		}
//		switch (obj.getNodeType()){
//			case OBJECT:
//				parseToMap(obj, propertyName, xmap, parser);
//				break;
//			case ARRAY:
//				parseToList(obj, propertyName, xmap, parser);
//				break;
//			default:
//				parseToPrimitive(obj, propertyName, xmap, parser);
//		}
		
	}
	
	private void parseToMap(QName propertyName, XNode parent, JsonParser parser) throws SchemaException, JsonParseException, IOException{
//		Iterator<Entry<String, JsonNode>> fields = node.fields();
//		String nsToUse = getNamespace(node, propertyName.getNamespaceURI());
//		String nsToUse = propertyName.getNamespaceURI();
	
//		if ("SPECIAL".equals(ns)){
//			return;
//		}
		QName parentPropertyName = propertyName;
		if (parser.getCurrentToken() == JsonToken.FIELD_NAME){
			if (parser.getCurrentName().equals(TYPE_DEFINITION)) {
				parseSpecial(parent, propertyName, parser);
				return;
			}
			parseJsonObject(parent, propertyName, parser);
		}
		if (parser.getCurrentToken() == null){
			return;
		}
		MapXNode subMap = new MapXNode();
		
//		QName parentPropertyName = propertyName;
//		System.out.println("CURRENT TOKEN: " + parser.getCurrentToken());
//		System.out.println("SUB MAP creation");
//		if (parent instanceof RootXNode){
//			System.out.println("SETTING SUBMAP FOR PARENT");
//			((RootXNode) parent).setRootElementName(parentPropertyName);
//			((RootXNode) parent).setSubnode(subMap);
//		} else {
//			System.out.println("ADDING submap");
//			addXNode(parentPropertyName, parent, subMap);
//		}
		
//		while (fields.hasNext()){
//			Entry<String, JsonNode> field = fields.next();
//			QName childrenName = new QName(nsToUse, field.getKey());
//			if (isSpecial(field.getValue())){
//				parseSpecial(subMap, childrenName, field.getValue(), parser);
//				continue;
//			} 
		
		String ns = processNamespace(parser);
//		if ("SPECIAL".equals(ns)){
//			return;
//		}
//		if (ns != null){
//			step(parser)
//		}
		while (moveNext(parser)){
//		moveNext(parser);
			System.out.println("MAP  -  hasNext : " + propertyName);
//			
			if (processSpecialIfNeeded(parser, parent, propertyName)){
				break;
			}
			
			if (ns != null){
				propertyName = new QName(ns, propertyName.getLocalPart());
			}
			
				parseJsonObject(subMap, propertyName, parser);
				step(parser);
			
		}
		
		//DO not add to the parent, if the map does not contain any values..
		if (subMap.isEmpty()){
			System.out.println("SUB MAP FOR PROPERTY: " + parentPropertyName + " NOT ADDED");
			return;
		}
		System.out.println("CURRENT TOKEN: " + parser.getCurrentToken());
		System.out.println("SUB MAP creation");
		if (parent instanceof RootXNode){
			System.out.println("SETTING SUBMAP FOR PARENT");
			((RootXNode) parent).setRootElementName(parentPropertyName);
			((RootXNode) parent).setSubnode(subMap);
		} else {
			System.out.println("ADDING submap");
			addXNode(parentPropertyName, parent, subMap);
		}
//		
		
		
//		}
	}
	
	private void removeEmpty(XNode parent, MapXNode subMap){
		
		if (parent instanceof ListXNode){
			((ListXNode) parent).remove(subMap);
			
		} else {
			throw new UnsupportedOperationException("Removing empty children not supported for " + parent);
		}
		
		
//		else if (parent instanceof MapXNode){
//			((MapXNode) parent).remove(propertyName);
//		}
		
	}
	private void step(JsonParser parser) throws SchemaException{
		try {
			parser.nextToken();
		} catch (JsonParseException e) {
			throw new SchemaException("Can't step next: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SchemaException("Can't step next: " + e.getMessage(), e);
		}
	}
	
	private boolean processSpecialIfNeeded(JsonParser parser, XNode xmap, QName propertyName) throws SchemaException{
		try {
			if (parser.getCurrentToken() == JsonToken.FIELD_NAME && parser.getCurrentName().equals(TYPE_DEFINITION)) {
				parseSpecial(xmap, propertyName, parser);
				return true;
			}
		} catch (JsonParseException e) {
			throw new SchemaException("Can't process namespace: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SchemaException("Can't process namespace: " + e.getMessage(), e);
		}
		return false;
	}
	
	private String processNamespace(JsonParser parser) throws SchemaException{
		JsonToken t;
		try {
			t = parser.nextToken();
		
		System.out.println("MOVE NEXT : " + t +" NAME: " + parser.getCurrentName());
		if (t == JsonToken.FIELD_NAME && parser.getCurrentName().startsWith(PROP_NAMESPACE)){
//			if (parser.getCurrentName().equals("@ns")){
//			if (parser.getCurrentName().equals(PROP_NAMESPACE)){
			System.out.println("ano ano ano");
			System.out.println(parser.getCurrentName());
			String ns = parser.nextTextValue();
			parser.nextToken();
			return ns;
			
			
				
			
		}
		return null;
		} catch (JsonParseException e) {
			throw new SchemaException("Can't process namespace: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SchemaException("Can't process namespace: " + e.getMessage(), e);		
		}
		
	}
	private boolean moveNext(JsonParser parser) throws SchemaException, JsonParseException, IOException{
//		try {
			JsonToken t = parser.getCurrentToken();
			System.out.println("MOVE NEXT : " + t +"  NNAME: " + parser.getCurrentName());
//			if (t == JsonToken.FIELD_NAME && parser.getCurrentName().startsWith("@ns")){
//				System.out.println("ano ano ano");
//				System.out.println(parser.getCurrentName());
//			}
			return (t != null && t != JsonToken.END_ARRAY && t != JsonToken.END_OBJECT);
//		} catch (JsonParseException e) {
//			throw new SchemaException("Error while moving next " + e.getMessage(), e);
//		} catch (IOException e) {
//			throw new SchemaException("Error while moving next " + e.getMessage(), e);		}
	}
	
	private void parseToList(QName propertyName, XNode parent, JsonParser parser) throws SchemaException, JsonParseException, IOException{
//		Iterator<JsonNode> elements = node.elements();
		
		if (parser.getCurrentToken() == null){
			return;
		}
		
		System.out.println("CURRENT LIST TOKEN: " + parser.getCurrentToken());
		ListXNode listNode = new ListXNode();
		System.out.println("LIST NODE CREATED, ADDING TO PARENT");
		addXNode(propertyName, parent, listNode);
//		while (elements.hasNext()){
//			JsonNode element = elements.next();
//			if (isSpecial(element)){
//				parseSpecial(listNode, propertyName, element, parser);
//				continue;
//			}'
		
		step(parser);
		while (moveNext(parser)){
			if (processSpecialIfNeeded(parser, listNode, propertyName)){
				continue;
			}
//		String ns = processNamespace(parser, listNode, propertyName);
//			if ("SPECIAL".equals(ns)){
//				parseSpecial(listNode, propertyName, parser);
//				return;
//			}
//		moveNext(parser);
			System.out.println("LIST HAS NEXT, proeprtyName " + propertyName);
			parseJsonObject(listNode, propertyName, parser);
			step(parser);
		}
//		}
	}
	
	private void parseToPrimitive(QName propertyName, XNode parent, JsonParser parser) throws JsonProcessingException, IOException{
		System.out.println("PROMITIVE CURRENT TOKEN : " + parser.getCurrentToken());
		if (propertyName != null && propertyName.getLocalPart().equals(PROP_NAMESPACE)){
			return;
		}
		PrimitiveXNode primitive = createPrimitiveXNode(parser);
		addXNode(propertyName, parent, primitive);
	}
	
	private <T> void parseSpecial(XNode xmap, QName propertyName, JsonParser parser) throws SchemaException{
		System.out.println("special");
		try{
		QName typeDefinition = null;
		if (parser.getCurrentToken() == JsonToken.FIELD_NAME && parser.getCurrentName().equals(TYPE_DEFINITION)){
			String uri = parser.nextTextValue();
			typeDefinition = QNameUtil.uriToQName(uri); 
		}
//		QName typeDefinition = extractTypeName(parser);
		
		if (parser.nextToken() == JsonToken.FIELD_NAME && parser.getCurrentName().equals(VALUE_FIELD)){
			parser.nextToken();
		}

		PrimitiveXNode primitive = createPrimitiveXNode(parser, typeDefinition);
		addXNode(propertyName, xmap, primitive);
		parser.nextToken();
		} catch (JsonParseException e){
			throw new SchemaException("Cannot parse special element: " + e.getMessage(), e);
		} catch (IOException e){
			throw new SchemaException("Cannot parse special element: " + e.getMessage(), e);
		}
	}
	
	//---------------------------END OF METHODS FOR PARSING ----------------------------------------
		
	//------------------------------ HELPER METHODS ------------------------------------------------	
	private PrimitiveXNode createPrimitiveXNode(final JsonParser parser) throws JsonProcessingException, IOException{
		return createPrimitiveXNode(parser, null);
	}
	
	private PrimitiveXNode createPrimitiveXNode(final JsonParser parser, QName typeDefinition) throws JsonProcessingException, IOException{
		PrimitiveXNode primitive = new PrimitiveXNode();
//		boolean f = parser.canReadObjectId();
//		System.out.println("can read obj id: " + f);
//		try{
//		System.out.println("obj id: " + parser.getObjectId());
//		System.out.println("type id: " + parser.getTypeId());
//		
//		} catch (Exception e){
//			throw new IllegalStateException(e);
//		}s
		Object tid = parser.getTypeId();
		System.out.println("tag: " + tid);		
		if (tid != null){
			if (tid.equals("http://www.w3.org/2001/XMLSchema/string")){
				System.out.println("Setting explicit definition");		
				typeDefinition = DOMUtil.XSD_STRING;
			} else if (tid.equals("http://www.w3.org/2001/XMLSchema/int")){
				System.out.println("Setting explicit definition");		
				typeDefinition = DOMUtil.XSD_INT;
			}
		}
		System.out.println("explicit definition " + typeDefinition);
		if (typeDefinition != null){
	
			primitive.setExplicitTypeDeclaration(true);
			primitive.setTypeQName(typeDefinition);
		}
		JsonNode jn = parser.readValueAs(JsonNode.class);
		ValueParser vp = new JsonValueParser(parser, jn);
		primitive.setValueParser(vp);
		
		return primitive;
	}
	
	private String getNamespace(JsonNode obj, String ns){
		JsonNode objNsNode = obj.get(PROP_NAMESPACE);
		
		if (objNsNode == null){
			return ns;
		}
		
		String objNs = objNsNode.asText();
		
		if (!objNs.equals(ns)){
			return objNs;
		}
		return ns;
	}
	
	private boolean isSpecial(JsonNode next){
		if (next.isObject()) {
			Iterator<String> nextFields = next.fieldNames();
			while (nextFields.hasNext()) {
				String str = nextFields.next();
				if (str.startsWith("@") && !str.equals(PROP_NAMESPACE)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private QName extractTypeName(JsonNode node, JsonParser parser) throws SchemaException{
		if (node.has(TYPE_DEFINITION)){
			System.out.println("has type def");
			JsonNode typeDef =  node.get(TYPE_DEFINITION);
			ObjectMapper m = (ObjectMapper) parser.getCodec();
			ObjectReader r = m.reader(QName.class);
			
			try {
				return r.readValue(typeDef);
			} catch (IOException e) {
				throw new SchemaException("Cannot extract type definition " + e.getMessage(), e);
			}
		}
		return null;
	}

	
	private void addXNode(QName fieldName, XNode parent, XNode children) {
		if (parent instanceof MapXNode) {
			((MapXNode) parent).put(fieldName, children);
		} else if (parent instanceof ListXNode) {
//			if ((children instanceof MapXNode ) && ((MapXNode)children).isEmpty()){
//				System.out.println("SUB MAP FOR PROPERTY: " + fieldName + " NOT ADDED");
//				return;
//			}
			((ListXNode) parent).add(children);
		}
	}
	
	public abstract JsonGenerator createGenerator(StringWriter out) throws SchemaException;
//	private JsonGenerator createJsonGenerator(StringWriter out) throws SchemaException{
//		try {
//			JsonFactory factory = new JsonFactory();
//			JsonGenerator generator = factory.createGenerator(out);
//			generator.setPrettyPrinter(new DefaultPrettyPrinter());
//			generator.setCodec(configureMapperForSerialization());
//			return generator;
//		} catch (IOException ex){
//			throw new SchemaException("Schema error during serializing to JSON.", ex);
//		}
//
//	}
//	
//	private ObjectMapper configureMapperForSerialization(){
//		ObjectMapper mapper = new ObjectMapper();
//		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
//		mapper.setSerializationInclusion(Include.NON_NULL);
//		mapper.registerModule(createSerializerModule());
//		return mapper;
//	}
//	
//	private Module createSerializerModule(){
//		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
//		module.addSerializer(QName.class, new QNameSerializer());
//		module.addSerializer(PolyString.class, new PolyStringSerializer());
//		module.addSerializer(ItemPath.class, new ItemPathSerializer());
//		module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
//		return module;
//	}
	


}
