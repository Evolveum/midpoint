package com.evolveum.midpoint.prism.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.parser.json.ItemPathDeserializer;
import com.evolveum.midpoint.prism.parser.json.ItemPathTypeDeserializer;
import com.evolveum.midpoint.prism.parser.json.JsonValueParser;
import com.evolveum.midpoint.prism.parser.json.PolyStringDeserializer;
import com.evolveum.midpoint.prism.parser.json.QNameDeserializer;
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
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public abstract class AbstractParser implements Parser {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractParser.class);
	
	private static final String PROP_NAMESPACE = "@ns";
	protected static final String TYPE_DEFINITION = "@typeDef";
	protected static final String VALUE_FIELD = "@value";
	
	
	protected abstract JsonParser createParser(String dataString)  throws SchemaException;
    protected abstract JsonParser createParser(InputStream stream) throws SchemaException, IOException;
	protected abstract JsonParser createParser(File file)  throws SchemaException, IOException;
	public abstract JsonGenerator createGenerator(StringWriter out) throws SchemaException;
	
	@Override
	public Collection<XNode> parseCollection(File file) throws SchemaException, IOException {
		throw new UnsupportedOperationException("Parse objects not supported for json and yaml.");
	}
	
	@Override
	public Collection<XNode> parseCollection(String dataString) throws SchemaException {
		throw new UnsupportedOperationException("Parse objects not supported for json and yaml.");
	}
	
	@Override
	public Collection<XNode> parseCollection(InputStream stream) throws SchemaException, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public XNode parse(File file) throws SchemaException, IOException {
		JsonParser parser = createParser(file);
		return parseObject(parser);
	}

    @Override
    public XNode parse(InputStream stream) throws SchemaException, IOException {
        JsonParser parser = createParser(stream);
        return parseObject(parser);
    }

    @Override
	public XNode parse(String dataString) throws SchemaException {
		JsonParser parser = createParser(dataString);
		return parseObject(parser);
	}

	private boolean root = true;
	@Override
	public String serializeToString(XNode xnode, QName rootElementName) throws SchemaException {
		if (xnode instanceof RootXNode){
			xnode = ((RootXNode) xnode).getSubnode();
		}
		return serialize(xnode, null, rootElementName);
	}

	@Override
	public String serializeToString(RootXNode xnode) throws SchemaException {
		QName rootElementName = xnode.getRootElementName();
		QName explicitType = xnode.getTypeQName();
		return serialize(xnode.getSubnode(), explicitType, rootElementName);
	}
	
	protected abstract void writeExplicitType(QName explicitType, JsonGenerator generator) throws JsonGenerationException, IOException;
	
	// ------------------- METHODS FOR SERIALIZATION ------------------------------
	public String serialize(XNode node, QName explicitType, QName rootElement) throws SchemaException{
		JsonGenerator generator = null;
		StringWriter out = new StringWriter();
		try { 
			
			generator = createGenerator(out);

			generator.writeStartObject();
			generator.writeStringField(PROP_NAMESPACE, rootElement.getNamespaceURI());
			generator.writeObjectFieldStart(rootElement.getLocalPart());
			if (hasExplicitTypeDeclaration(rootElement)){
				writeExplicitType(explicitType, generator);
			}
			serialize(node, rootElement, rootElement.getNamespaceURI(), generator);
			generator.writeEndObject();
						
			
		} catch (IOException ex){
			throw new SchemaException("Schema error during serializing to JSON.", ex);
		} catch (RuntimeException e) {
			LoggingUtils.logException(LOGGER, "Unexpected exception while serializing", e);
			System.out.println("Unexpected exception while serializing: " + e);
			e.printStackTrace();
		} finally {
			if (generator != null) {
				try {
					generator.flush();
					generator.close();
				} catch (IOException e) {
					throw new SchemaException(e.getMessage(), e);
				} // beware, this code can throw any exception, masking the original one
				
			}

		}
		return out.toString();

	}
		
	private boolean hasExplicitTypeDeclaration(QName elementName){
		return elementName.getLocalPart().equals("object");
	}
	private <T> void  serialize(XNode node, QName nodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		
		if (node instanceof MapXNode){
			serializeFromMap((MapXNode) node, nodeName, globalNamespace, generator);
		} else if (node instanceof ListXNode){
			serializeFromList((ListXNode) node, nodeName, globalNamespace, generator);
		} else if (node instanceof PrimitiveXNode){
			serializeFromPrimitive((PrimitiveXNode<T>) node, nodeName, generator);
		} else if (node instanceof SchemaXNode){
			serializeFromSchema((SchemaXNode) node, nodeName, generator);
		} else if (node == null) {
			serializeFromNull(nodeName, generator);
		} else {
			throw new IllegalStateException("Unsupported node type: " + node.getClass().getSimpleName());
		}
	}
	
	private void writeObjectStart(QName nodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		if (nodeName == null) {
			generator.writeStartObject();
		} else {
			if (!globalNamespace.equals(nodeName.getNamespaceURI()) && StringUtils.isNotBlank(nodeName.getNamespaceURI())){
				generator.writeStringField(PROP_NAMESPACE, nodeName.getNamespaceURI());
				globalNamespace = nodeName.getNamespaceURI();
			}
			generator.writeObjectFieldStart(nodeName.getLocalPart());
		}
	}
	
	private void serializeFromMap(MapXNode map, QName nodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{

		if (root) {
			root = false;
		} else {
			writeObjectStart(nodeName, globalNamespace, generator);
		}
				
//		Iterator<Entry<QName, XNode>> subnodes = map.entrySet().iterator();
		Iterator<Entry<QName, XNode>> subnodes = getMapIterator(map);
		
		while (subnodes.hasNext()){
			Entry<QName, XNode> subNode = subnodes.next();
			XNode subNodeValue = subNode.getValue();
			if (subNodeValue instanceof PrimitiveXNode && ((PrimitiveXNode) subNodeValue).isAttribute()){
				continue;
			}
//			if (subNode.getKey().getLocalPart().equals("oid") || subNode.getKey().getLocalPart().equals("version")){
//				continue;
//			}
			globalNamespace = serializeNsIfNeeded(subNode.getKey(), globalNamespace, generator);
			break;
		}
		
		subnodes = getMapIterator(map);
		
		while (subnodes.hasNext()){
			Entry<QName, XNode> subNode = subnodes.next();
			globalNamespace = serializeNsIfNeeded(subNode.getKey(), globalNamespace, generator);
			serialize(subNode.getValue(), subNode. getKey(), globalNamespace, generator);
		}
		generator.writeEndObject();
		
	}
	
	private Iterator<Entry<QName, XNode>> getMapIterator(MapXNode map){
		if (map.entrySet() == null){
			throw new IllegalStateException("Strange thing happened. No entries in xmap");
		}
		return map.entrySet().iterator();
	}
	
	private void serializeFromList(ListXNode list, QName nodeName, String globalNamespace, JsonGenerator generator) throws JsonGenerationException, IOException{
		ListIterator<XNode> sublist = list.listIterator();
		generator.writeArrayFieldStart(nodeName.getLocalPart());
		while (sublist.hasNext()){
			serialize(sublist.next(), null, globalNamespace, generator);
		}
		generator.writeEndArray();
	}
	
	protected abstract <T> boolean serializeExplicitType(PrimitiveXNode<T> primitive, QName explicitType, JsonGenerator generator) throws JsonGenerationException, IOException ;
	
	protected <T> QName getExplicitType(PrimitiveXNode<T> primitive){
		QName explicit = null;
		if (primitive.isExplicitTypeDeclaration()) {
			explicit = primitive.getTypeQName();
		}
		return explicit;
	}
	
	@SuppressWarnings("unchecked")
	private <T> void serializeFromPrimitive(PrimitiveXNode<T> primitive, QName nodeName, JsonGenerator generator) throws JsonGenerationException, IOException{
		
		QName explicitType = getExplicitType(primitive);
		if (serializeExplicitType(primitive, explicitType, generator)){
			return;
		}
			
		T value = primitive.getValue();
		if ( value == null){
			value = (T) primitive.getStringValue();
		}
		if (nodeName == null) {
			generator.writeObject(value);
		} else {
			
			generator.writeObjectField(nodeName.getLocalPart(), value);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void serializeFromNull(QName nodeName, JsonGenerator generator) throws JsonGenerationException, IOException{
		if (nodeName == null) {
			generator.writeObject("");			// is this reasonable?
		} else {
			generator.writeObjectField(nodeName.getLocalPart(), "");
			//generator.writeFieldName(nodeName.getLocalPart());			// todo how to write empty objects?
		}
	}

	private void serializeFromSchema(SchemaXNode node, QName nodeName, JsonGenerator generator) throws JsonProcessingException, IOException {
		generator.writeObjectField(nodeName.getLocalPart(), node.getSchemaElement());
		
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
	//------------------------END OF METHODS FOR SERIALIZATION -------------------------------
	
	private void configureParser(JsonParser parser){
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule sm = new SimpleModule();
		sm.addDeserializer(QName.class, new QNameDeserializer());
		sm.addDeserializer(ItemPath.class, new ItemPathDeserializer());
		sm.addDeserializer(PolyString.class, new PolyStringDeserializer());
		sm.addDeserializer(ItemPathType.class, new ItemPathTypeDeserializer());

		mapper.registerModule(sm);
		parser.setCodec(mapper);
	}
	
	//------------------------ METHODS FOR PARSING -------------------------------------------
	public XNode parseObject(JsonParser parser) throws SchemaException{
		
		try {
			configureParser(parser);

			JsonToken t = parser.getCurrentToken();
			if (t == null){
				t = parser.nextToken();
			}
			
			if (t == null){
				t = parser.nextToken();
			}
						
			RootXNode xmap = new RootXNode();
//			xmap.
			
			while (parser.nextToken() != null){
				parse(xmap, null, parser);
			}
			 return xmap;
		} catch (JsonParseException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SchemaException("Cannot parse from JSON: " + e.getMessage(), e);
		}
	}
	
	private QName parseFieldName(XNode xnode, QName propertyName, JsonParser parser) throws JsonParseException, IOException{
		
		JsonToken token = parser.getCurrentToken();
		String ns = null;
		if (propertyName != null){
			ns = propertyName.getNamespaceURI();
		}
		if (token == JsonToken.FIELD_NAME) {
			String fieldName = parser.getText();
			if (fieldName.startsWith(PROP_NAMESPACE)) {
				ns = parser.nextTextValue();
				propertyName = new QName(ns, fieldName);

				token = parser.nextToken();
				if (token == JsonToken.FIELD_NAME) {
					fieldName = parser.getText();
					propertyName = new QName(ns, fieldName);
				}
			} else {
				propertyName = new QName(ns, fieldName);
			}
			token = parser.nextToken();

		}
		return propertyName;
		
	}
		
	private <T> void parse(XNode xmap, QName propertyName, JsonParser parser) throws SchemaException {
		try{
			JsonToken token = parser.getCurrentToken();
			
			if (token == null){
				return;
			}
			propertyName = parseFieldName(xmap, propertyName, parser);
			
		switch (parser.getCurrentToken()){
			case START_OBJECT:
				parseToMap(propertyName, xmap, parser);
				break;				
			case START_ARRAY:
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
//				System.out.println("DEFAULT SWICH NODE");
				break;
				
		}
		} catch (Exception e){
			//TODO: 
			throw new SchemaException("Error ", e);
		}
	}
	
	private void parseToMap(QName propertyName, XNode parent, JsonParser parser) throws SchemaException, JsonParseException, IOException{

//		System.out.println("parseToMap - propertyName " + propertyName);
		QName parentPropertyName = propertyName;
//		if (parser.getCurrentToken() == JsonToken.FIELD_NAME){
//			if (parser.getCurrentName().equals(TYPE_DEFINITION)) {
//				parseSpecial(parent, propertyName, parser);
//				return;
//			}
//			parse(parent, propertyName, parser);
//		}
		if (parser.getCurrentToken() == null){
			return;
		}
		MapXNode subMap = new MapXNode();
		
		String ns = processNamespace(parent, parser);

		boolean specialFound = false;
		boolean iterate = false;
		while (moveNext(parser, iterate)) {
			if (processSpecialIfNeeded(parser, parent, propertyName)) {
				specialFound = true;
				break;
			}
			if (ns != null) {
				propertyName = new QName(ns, propertyName.getLocalPart());
			}
			parse(subMap, propertyName, parser);
			// step(parser);
//			parser.nextToken();
			iterate = true;

		}

		//DO not add to the parent, if the map does not contain any values..
		// and a primitive value was found
		if (specialFound && subMap.isEmpty()) {
			return;
		}
//		System.out.println("CURRENT TOKEN: " + parser.getCurrentToken());
//		System.out.println("SUB MAP creation");
		if (parent instanceof RootXNode){
//			System.out.println("SETTING SUBMAP FOR PARENT");
			((RootXNode) parent).setRootElementName(parentPropertyName);
			((RootXNode) parent).setSubnode(subMap);
		} else {
//			System.out.println("ADDING submap");
			addXNode(parentPropertyName, parent, subMap);
		}
	}
	
	private boolean processSpecialIfNeeded(JsonParser parser, XNode xmap, QName propertyName) throws SchemaException{
		try {
			if (parser.getCurrentToken() == JsonToken.FIELD_NAME
					&& (parser.getCurrentName().equals(TYPE_DEFINITION) )) {
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
	
	private String processNamespace(XNode xnode, JsonParser parser) throws SchemaException{
		JsonToken t;
		try {
			t = parser.nextToken();
//			ObjectMapper m = new ObjectMapper();
//			m.
//			
//			ObjectMapper c = (ObjectMapper) parser.getCodec();
//			ObjectReader r = c.reader(JsonNode.class);
//			MappingIterator<JsonNode> nodes = r.readValues(parser); //Tree(jp)dValue(parser); //readValue(parser, JsonNode.class);
//		while (nodes.hasNext()){
//			JsonNode node = nodes.next();
//			if (node.get(PROP_NAMESPACE) != null){
//				String ns =  node.get(PROP_NAMESPACE).asText();
////				parser.nextToken();
////				parser.nextValue();
//				return ns;
//			} else if (node.get("@type") != null){
//				JsonValueParser<QName> vp = new JsonValueParser<QName>(parser, node.get("@type"));
//				QName type = vp.parse(DOMUtil.XSD_QNAME);
//				xnode.setExplicitTypeDeclaration(true);
//				xnode.setTypeQName(type);
//				return null;
//			}
//		}
//			JsonNode node = parser.readValueAs(JsonNode.class);
			
//		System.out.println("MOVE NEXT : " + t +" NAME: " + parser.getCurrentName());
			if (t == JsonToken.FIELD_NAME) {
				if (parser.getCurrentName().startsWith(PROP_NAMESPACE)) {

					// System.out.println("ano ano ano");
					// System.out.println(parser.getCurrentName());
					String ns = parser.nextTextValue();
					parser.nextToken();
					return ns;
				} else if (parser.getCurrentName().equals("@type")) {
					parser.nextToken();

					QName type = parser.readValueAs(QName.class);
					// propertyName = new QName(ns, fieldName);
					xnode.setExplicitTypeDeclaration(true);
					xnode.setTypeQName(type);
					parser.nextToken();
					
				}

			}
		return null;
		} catch (JsonParseException e) {
			throw new SchemaException("Can't process namespace: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SchemaException("Can't process namespace: " + e.getMessage(), e);		
		}
		
	}
	private boolean moveNext(JsonParser parser, boolean iterate) throws SchemaException, JsonParseException, IOException{
		
		if (iterate){
			parser.nextToken();
		}
		
		JsonToken t = parser.getCurrentToken();
		return (t != null && t != JsonToken.END_ARRAY && t != JsonToken.END_OBJECT);
	}
	
	private void parseToList(QName propertyName, XNode parent, JsonParser parser) throws SchemaException, JsonParseException, IOException{
		
		if (parser.getCurrentToken() == null){
			return;
		}
		
//		System.out.println("CURRENT LIST TOKEN: " + parser.getCurrentToken());
		ListXNode listNode = new ListXNode();
//		System.out.println("LIST NODE CREATED, ADDING TO PARENT");
		addXNode(propertyName, parent, listNode);
		
//		step(parser);
		while (moveNext(parser, true)){
			if (processSpecialIfNeeded(parser, listNode, propertyName)){
				continue;
			}
//			System.out.println("LIST HAS NEXT, proeprtyName " + propertyName);
			parse(listNode, propertyName, parser);
//			step(parser);
		}

	}
	
	private <T> void parseToPrimitive(QName propertyName, XNode parent, JsonParser parser) throws JsonProcessingException, IOException{
//		System.out.println("PROMITIVE CURRENT TOKEN : " + parser.getCurrentToken());
		if (propertyName != null){
			if (propertyName.getLocalPart().equals(PROP_NAMESPACE)){
				return;
			} else if (propertyName.equals(DOMUtil.XSD_SCHEMA_ELEMENT)){
				SchemaXNode schemaNode = new SchemaXNode();
				Node node = parser.readValueAs(Node.class);
				Element e = null;
				if (node instanceof Document){
					e = ((Document)node).getDocumentElement();
				}
				
				schemaNode.setSchemaElement(e);
				addXNode(DOMUtil.XSD_SCHEMA_ELEMENT, parent, schemaNode);
				return;
			}
		}
		PrimitiveXNode<T> primitive = createPrimitiveXNode(parser);
		addXNode(propertyName, parent, primitive);
	}
	
	private <T> void parseSpecial(XNode xmap, QName propertyName, JsonParser parser) throws SchemaException{
		// System.out.println("special");
		try {
			QName typeDefinition = null;
			if (parser.getCurrentToken() == JsonToken.FIELD_NAME
					&& parser.getCurrentName().equals(TYPE_DEFINITION)) {

				String uri = parser.nextTextValue();
				typeDefinition = QNameUtil.uriToQName(uri);

				if (parser.nextToken() == JsonToken.FIELD_NAME && parser.getCurrentName().equals(VALUE_FIELD)) {
					parser.nextToken();
				}

				PrimitiveXNode<T> primitive = createPrimitiveXNode(parser, typeDefinition);
				addXNode(propertyName, xmap, primitive);
				parser.nextToken();
			}
		} catch (JsonParseException e){
			throw new SchemaException("Cannot parse special element: " + e.getMessage(), e);
		} catch (IOException e){
			throw new SchemaException("Cannot parse special element: " + e.getMessage(), e);
		}
	}
	
	//---------------------------END OF METHODS FOR PARSING ----------------------------------------
		
	//------------------------------ HELPER METHODS ------------------------------------------------	
	private <T> PrimitiveXNode<T> createPrimitiveXNode(final JsonParser parser) throws JsonProcessingException, IOException{
		return createPrimitiveXNode(parser, null);
	}
	
	private <T> PrimitiveXNode<T> createPrimitiveXNode(final JsonParser parser, QName typeDefinition) throws JsonProcessingException, IOException{
		PrimitiveXNode<T> primitive = new PrimitiveXNode<T>();
		Object tid = parser.getTypeId();
//		System.out.println("tag: " + tid);		
		if (tid != null){
			if (tid.equals("http://www.w3.org/2001/XMLSchema/string")){
//				System.out.println("Setting explicit definition");		
				typeDefinition = DOMUtil.XSD_STRING;
			} else if (tid.equals("http://www.w3.org/2001/XMLSchema/int")){
//				System.out.println("Setting explicit definition");		
				typeDefinition = DOMUtil.XSD_INT;
			}
		}
//		System.out.println("explicit definition " + typeDefinition);
		if (typeDefinition != null){
	
			primitive.setExplicitTypeDeclaration(true);
			primitive.setTypeQName(typeDefinition);
		}
		JsonNode jn = parser.readValueAs(JsonNode.class);
		ValueParser<T> vp = new JsonValueParser<T>(parser, jn);
		primitive.setValueParser(vp);
		
		return primitive;
	}
	
	
	private void addXNode(QName fieldName, XNode parent, XNode children) {
		if (parent instanceof MapXNode) {
			((MapXNode) parent).put(fieldName, children);
		} else if (parent instanceof ListXNode) {
			((ListXNode) parent).add(children);
		}
	}
	
	

}
