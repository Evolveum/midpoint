package com.evolveum.midpoint.prism.json;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.SerializationContext;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.parser.json.ItemPathDeserializer;
import com.evolveum.midpoint.prism.parser.json.JsonValueParser;
import com.evolveum.midpoint.prism.parser.json.QNameDeserializer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Deprecated
public abstract class AbstractParserDeprecated implements Parser {
	
	private static final String PROP_NAMESPACE = "@ns";
	private static final String TYPE_DEFINITION = "@typeDef";
	private static final String VALUE_FIELD = "@value";
	
	
	@Override
	public XNode parse(File file, ParsingContext parsingContext) throws SchemaException, IOException {
		JsonParser parser = createParser(file);
		return parseObject(parser);
	}
	
	protected abstract JsonParser createParser(String dataString)  throws SchemaException;
	protected abstract JsonParser createParser(File file)  throws SchemaException, IOException ;
	
	@Override
	public XNode parse(String dataString, ParsingContext parsingContext) throws SchemaException {
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
	public String serializeToString(XNode xnode, QName rootElementName, SerializationContext serializationContext) throws SchemaException {
		if (xnode instanceof RootXNode){
			xnode = ((RootXNode) xnode).getSubnode();
		}
		return serializeToJson(xnode, rootElementName);
	}

	@Override
	public String serializeToString(RootXNode xnode, SerializationContext serializationContext) throws SchemaException {
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
		serializeToJson(node, rootElement, rootElement.getNamespaceURI(), generator);
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
		
		if (primitive.isExplicitTypeDeclaration()) {
			generator.writeStartObject();
			generator.writeFieldName(TYPE_DEFINITION);
			generator.writeObject(primitive.getTypeQName());

			generator.writeObjectField(VALUE_FIELD, primitive.getValue());
			generator.writeEndObject();
		} else {

			if (nodeName == null) {
				generator.writeObject(primitive.getValue());
			} else {
//				if (StringUtils.isNotBlank(nodeName.getNamespaceURI())
//						&& !nodeName.getNamespaceURI().equals(objectNs)) {
//					objectNs = nodeName.getNamespaceURI();
//				}
				generator.writeObjectField(nodeName.getLocalPart(), primitive.getValue());
			}
		}
	}
	
	private String serializeNsIfNeeded(QName subNodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		if (subNodeName == null){
			return globalNamespace;
		}
		String subNodeNs = subNodeName.getNamespaceURI();
		if (StringUtils.isNotEmpty(subNodeNs)){
			if (!subNodeNs.equals(globalNamespace)){
				globalNamespace = subNodeNs;
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
//			String globalNs = (String) parser.getObjectId();
			parser.setCodec(mapper);
			
//			TokenBuffer tb = parser.readValueAs(TokenBuffer.class);
//			JsonParser p = tb.asParser();
//			System.out.println("======================== " + p.getObjectId());
//			System.out.println("======================== " + p.getTypeId());
//			tb.
//			tb.firstToken().asParser().getCurrentTokenId();
//			JsonToken.
//			YAMLFactory f = new YAMLFactory();
//			f.
			
			int i = parser.getCurrentTokenId();
			//System.out.println("id token : " + i);
//			Object o =parser.readValueAsTree();
			
			System.out.println("id: " + JsonTokenId.ID_START_OBJECT);
			JsonToken t = parser.getCurrentToken();
			//System.out.println("cuurent: " + t);
			
			JsonToken nt = parser.nextToken();
			//System.out.println("cuurent: " + nt);
			
//			JsonToken t = parser.getCurrentToken();
//			System.out.println("cuurent: " + t);
//			mapper.readTree(jp)
//			obj = ((YAMLParser)parser).readValueAs(JsonNode.class);
			
			
			
			RootXNode xmap = new RootXNode();
			
//			Iterator<Entry<String, JsonNode>> fields = obj.fields();
//			obj.
			
//			nt.
//			while (fields.hasNext()){
//				Entry<String, JsonNode> field = fields.next();
//				String fieldName = field.getKey();
//				
//				JsonNode globalNsNode = field.getValue().get(PROP_NAMESPACE);
//				if (globalNsNode == null){
//					throw new SchemaException("No global ns");
//				}
//				String globalNs = globalNsNode.asText();
//				
//				if (fieldName == null){
//					throw new SchemaException("cannot obtain type");
//				}
//		
				QName rootElement = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "user");
//				((RootXNode) xmap).setRootElementName(rootElement);
				
				parseJsonObject(xmap, rootElement, null, parser);
				
//			}
			 return xmap;
		} catch (JsonParseException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		}
	}
	
	private <T> void parseJsonObject(XNode xmap, QName propertyName, final JsonNode obj, final JsonParser parser) throws SchemaException {
		try{
			JsonToken token = parser.nextToken();
			//System.out.println("token " + token);
			JsonToken current = parser.getCurrentToken();
			//System.out.println("current " + current);
			JsonToken value = parser.nextValue();
			//System.out.println("value " + value);
			if (token == null){
				token = parser.nextToken();
			}
		switch (token){
			case START_OBJECT:
				parseToMap(obj, propertyName, xmap, parser);
				break;
			case START_ARRAY:
				parseToList(obj, propertyName, xmap, parser);
				break;
			default:
				parseToPrimitive(obj, propertyName, xmap, parser);
		}
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
	
	private void parseToMap(JsonNode node, QName propertyName, XNode parent, JsonParser parser) throws SchemaException{
		Iterator<Entry<String, JsonNode>> fields = node.fields();
		String nsToUse = getNamespace(node, propertyName.getNamespaceURI());
		
		MapXNode subMap = new MapXNode();
		if (parent instanceof RootXNode){
			((RootXNode) parent).setSubnode(subMap);
		} else {
			addXNode(propertyName, parent, subMap);
		}
		
		while (fields.hasNext()){
			Entry<String, JsonNode> field = fields.next();
			QName childrenName = new QName(nsToUse, field.getKey());
			if (isSpecial(field.getValue())){
				parseSpecial(subMap, childrenName, field.getValue(), parser);
				continue;
			} 
			parseJsonObject(subMap, childrenName, field.getValue(), parser);
		}
	}
	
	private void parseToList(JsonNode node, QName propertyName, XNode parent, JsonParser parser) throws SchemaException{
		Iterator<JsonNode> elements = node.elements();
		ListXNode listNode = new ListXNode();
		addXNode(propertyName, parent, listNode);
		while (elements.hasNext()){
			JsonNode element = elements.next();
			if (isSpecial(element)){
				parseSpecial(listNode, propertyName, element, parser);
				continue;
			}
			parseJsonObject(listNode, propertyName, element, parser);
		}
	}
	
	private void parseToPrimitive(JsonNode node, QName propertyName, XNode parent, JsonParser parser){
		if (propertyName.getLocalPart().equals(PROP_NAMESPACE)){
			return;
		}
		PrimitiveXNode primitive = createPrimitiveXNode(node, parser);
		addXNode(propertyName, parent, primitive);
	}
	
	private <T> void parseSpecial(XNode xmap, QName propertyName, JsonNode obj, final JsonParser parser) throws SchemaException{
		//System.out.println("special");
		QName typeDefinition = extractTypeName(obj, parser);
		
		if (typeDefinition != null){
			obj = obj.get(VALUE_FIELD);
		}

		PrimitiveXNode primitive = createPrimitiveXNode(obj, parser, typeDefinition);
		addXNode(propertyName, xmap, primitive);
	}
	
	//---------------------------END OF METHODS FOR PARSING ----------------------------------------
		
	//------------------------------ HELPER METHODS ------------------------------------------------	
	private PrimitiveXNode createPrimitiveXNode(JsonNode node, JsonParser parser){
		return createPrimitiveXNode(node, parser, null);
	}
	
	private PrimitiveXNode createPrimitiveXNode(JsonNode node, JsonParser parser, QName typeDefinition){
		PrimitiveXNode primitive = new PrimitiveXNode();
		boolean f = parser.canReadObjectId();
		//System.out.println("can read obj id: " + f);
		//try{
		//System.out.println("obj id: " + parser.getObjectId());
		//System.out.println("type id: " + parser.getTypeId());
		//
		//} catch (Exception e){
		//	throw new IllegalStateException(e);
		//}
		ValueParser vp = new JsonValueParser(parser, node);
		primitive.setValueParser(vp);
		if (typeDefinition != null){
			primitive.setExplicitTypeDeclaration(true);
			primitive.setTypeQName(typeDefinition);
		}
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
			//System.out.println("has type def");
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
