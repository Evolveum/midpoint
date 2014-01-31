package com.evolveum.midpoint.prism.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

public class PrismJsonSerializer implements Parser{
	
	private static final String PROP_NAMESPACE = "@ns";
	private static final String TYPE_DEFINITION = "@typeDef";
	private static final String VALUE_FIELD = "@value";
	
	String globalNamespace = null;
	public String serializeToJson(XNode node, QName rootElement) throws SchemaException{
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
		module.addSerializer(QName.class, new QNameSerializer());
		module.addSerializer(PolyString.class, new PolyStringSerializer());
		
		JaxbElementSerializer jaxbSerializer = new JaxbElementSerializer();
		module.addSerializer(JAXBElement.class, jaxbSerializer);
		mapper.registerModule(module);
		
		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		
		ObjectWriter writer = mapper.writer();
		JsonFactory factory = new JsonFactory();
		StringWriter out = new StringWriter();
		try {
		JsonGenerator generator = factory.createGenerator(out);
		
		
		PrettyPrinter pp = new DefaultPrettyPrinter();

		generator.setPrettyPrinter(pp);
		generator.setCodec(mapper);
		globalNamespace = rootElement.getNamespaceURI();
		generator.writeStartObject();
		serializeToJson(node, rootElement,  generator);
		generator.writeEndObject();
		
		generator.flush();
		generator.close();
		} catch (IOException ex){
			throw new SchemaException("Schema error during serializing to JSON.", ex);
		}
		return out.toString();
	}
	
	String objectNs = null;
	private <T> void  serializeToJson(XNode node, QName nodeName, JsonGenerator generator) throws JsonGenerationException, IOException{
		
		if (node instanceof MapXNode){
			serializerFromMap((MapXNode) node, nodeName, generator);
		} else if (node instanceof ListXNode){
			serializeFromList((ListXNode) node, nodeName, generator);
		} else if (node instanceof PrimitiveXNode){
			serializeFromPrimitive((PrimitiveXNode) node, nodeName, generator);
		}
	}
	
	
	private void serializerFromMap(MapXNode map, QName nodeName, JsonGenerator generator) throws JsonGenerationException, IOException{
		if (nodeName == null){
			generator.writeStartObject();
		} else{
			generator.writeObjectFieldStart(nodeName.getLocalPart());
		}
		
		if (StringUtils.isBlank(objectNs)){
			objectNs = globalNamespace;
			generator.writeStringField(PROP_NAMESPACE, objectNs);
		}
		
		Iterator<Entry<QName, XNode>> subnodes = map.entrySet().iterator();
		while (subnodes.hasNext()){
			Entry<QName, XNode> subNode = subnodes.next();
			serializeToJson(subNode.getValue(), subNode. getKey(), generator);
		}
		
		if (nodeName != null && StringUtils.isNotEmpty(nodeName.getNamespaceURI()) && !nodeName.getNamespaceURI().equals(objectNs)){
			if (!objectNs.equals(globalNamespace)){
				generator.writeStringField(PROP_NAMESPACE, objectNs);
			}
		}
		generator.writeEndObject();
	}
	
	private void serializeFromList(ListXNode list, QName nodeName, JsonGenerator generator) throws JsonGenerationException, IOException{
		ListIterator<XNode> sublist = list.listIterator();
		generator.writeArrayFieldStart(nodeName.getLocalPart());
		while (sublist.hasNext()){
			serializeToJson(sublist.next(), null, generator);
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
				if (StringUtils.isNotBlank(nodeName.getNamespaceURI())
						&& !nodeName.getNamespaceURI().equals(objectNs)) {
					objectNs = nodeName.getNamespaceURI();
				}
				generator.writeObjectField(nodeName.getLocalPart(), primitive.getValue());
			}
		}
	}
	
	
	String globalNs = null;
	public XNode parseObject(JsonParser parser) throws SchemaException{
		
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule sm = new SimpleModule();
		sm.addDeserializer(QName.class, new QNameDeserializer());
		sm.addDeserializer(ItemPath.class, new ItemPathDeserializer());
		
		mapper.registerModule(sm);
		JsonNode obj = null;
		try {
			parser.setCodec(mapper);
			obj = parser.readValueAs(JsonNode.class);
			
			RootXNode xmap = new RootXNode();
			
			Iterator<Entry<String, JsonNode>> fields = obj.fields();
			
			while (fields.hasNext()){
				Entry<String, JsonNode> field = fields.next();
				String fieldName = field.getKey();
				
				if (fieldName != null){
					((RootXNode) xmap).setRootElementName(new QName(globalNs, fieldName));
				}
				
				
				parseJsonObject(xmap, globalNs, field.getKey(), field.getValue(), parser);
				
			}
			 return xmap;
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		}
		
//		return null;
		
	}
	
	private String getNamespace(JsonNode obj, String ns){
		JsonNode objNsNode = obj.get(PROP_NAMESPACE);
		String objNs = null;
		if (objNsNode != null){
			objNs = objNsNode.asText();
		}
		
		String nsToUse = globalNs;
		if (objNs != null && !objNs.equals(ns)){
			nsToUse = objNs;
		} else {
			nsToUse = ns;
		}
		return nsToUse;
	}
	
	private boolean isSpecial(JsonNode next){
		boolean isSpecial = false;
		if (next.isObject()){
			Iterator<String> nextFields = next.fieldNames();
			
			while (nextFields.hasNext()){
				String str = nextFields.next();
				if (str.startsWith("@") && !str.equals(PROP_NAMESPACE)){
					
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
	private <T> void setSpecial(XNode xmap, QName propertyName, JsonNode obj, final JsonParser parser) throws SchemaException{
		System.out.println("special");
		QName typeDefinition = extractTypeName(obj, parser);
		
		if (typeDefinition != null){
			obj = obj.get(VALUE_FIELD);
		}

		PrimitiveXNode primitive = createPrimitiveXNode(obj, parser, typeDefinition);
		addXNode(propertyName, xmap, primitive);
	}
	
	private <T> void parseJsonObject(XNode xmap, String ns, String fieldName, final JsonNode obj, final JsonParser parser) throws SchemaException {
		
		
		if (obj.isObject()){
			Iterator<Entry<String, JsonNode>> fields = obj.fields();
			String nsToUse = getNamespace(obj, ns);
			
			MapXNode subMap = new MapXNode();
			if (xmap instanceof RootXNode){
				JsonNode globalNsNode = obj.get(PROP_NAMESPACE);
				
				if (globalNsNode != null){
					globalNs = globalNsNode.asText();
				}
				((RootXNode) xmap).setRootElementName(new QName(globalNs, fieldName));
				((RootXNode) xmap).setSubnode(subMap);
			} else {
				addXNode(new QName(ns, fieldName), xmap, subMap);
			}
			
			while (fields.hasNext()){
				Entry<String, JsonNode> field = fields.next();
				if (isSpecial(field.getValue())){
					setSpecial(subMap, new QName(nsToUse, field.getKey()), field.getValue(), parser);
					continue;
				} 
				parseJsonObject(subMap, nsToUse, field.getKey(), field.getValue(), parser);
			}
							
		} else {
			if (obj.isArray()){
				Iterator<JsonNode> elements = obj.elements();
				ListXNode listNode = new ListXNode();
				addXNode(new QName(ns, fieldName), xmap, listNode);
				while (elements.hasNext()){
					JsonNode element = elements.next();
					if (isSpecial(element)){
						setSpecial(listNode, new QName(ns, fieldName), element, parser);
						continue;
					}
					parseJsonObject(listNode, ns, fieldName, element, parser);
				}
			} else {

				if (fieldName.equals(PROP_NAMESPACE)){
					return;
				}
				PrimitiveXNode primitive = createPrimitiveXNode(obj, parser);
				addXNode(new QName(ns, fieldName), xmap, primitive);
				
			}
		} 
		
	}
	
	private PrimitiveXNode createPrimitiveXNode(JsonNode node, JsonParser parser, QName typeDefinition){
		PrimitiveXNode primitive = new PrimitiveXNode();
		
		ValueParser vp = new JsonValueParser(parser, node);
		primitive.setValueParser(vp);
		if (typeDefinition != null){
			primitive.setExplicitTypeDeclaration(true);
			primitive.setTypeQName(typeDefinition);
		}
		return primitive;
	}
	
	private PrimitiveXNode createPrimitiveXNode(JsonNode node, JsonParser parser){
		return createPrimitiveXNode(node, parser, null);
	}
	
	private void addXNode(QName fieldName, XNode parent, XNode children){
		if (parent instanceof MapXNode){
			((MapXNode) parent).put(fieldName, children);
	} else if (parent instanceof ListXNode){
		((ListXNode) parent).add(children);
	}
	}

	private Object getRealValue(JsonNode obj) {
		switch (obj.getNodeType()) {
			case BOOLEAN:
				return obj.asBoolean();
			case NUMBER:
					return obj.asLong();
			default:
				return obj.asText();
		}
//		return null;
	}

	  
	
		@Override
		public XNode parse(File file) throws SchemaException, IOException {
			JsonFactory factory = new JsonFactory();
			JsonParser parser = null;
			try {
				parser = factory.createParser(new FileInputStream(file));
			} catch (IOException e) {
				throw e;
			}
			return parseObject(parser);
		}

		@Override
		public XNode parse(String dataString) throws SchemaException {
			JsonFactory factory = new JsonFactory();
			JsonParser parser = null;
			try {
				parser = factory.createParser(dataString);
			} catch (IOException e) {
				throw new SchemaException("Cannot create JSON parser: " + e.getMessage(), e);
			}
			return parseObject(parser);
		}

		@Override
		public boolean canParse(File file) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canParse(String dataString) {
			// TODO Auto-generated method stub
			return false;
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
}
