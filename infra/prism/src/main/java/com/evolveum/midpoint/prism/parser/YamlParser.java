package com.evolveum.midpoint.prism.parser;

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

import com.evolveum.midpoint.prism.json.AbstractParser;
import com.evolveum.midpoint.prism.json.ItemPathSerializer;
import com.evolveum.midpoint.prism.json.JaxbElementSerializer;
import com.evolveum.midpoint.prism.json.PolyStringSerializer;
import com.evolveum.midpoint.prism.json.QNameSerializer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
//import com.fasterxml.jackson.core.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

public class YamlParser extends AbstractParser{
	
//	private static final String PROP_NAMESPACE = "@ns";
//	private static final String TYPE_DEFINITION = "@typeDef";
//	private static final String VALUE_FIELD = "@value";
//
//	@Override
//	public XNode parse(File file) throws SchemaException, IOException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public XNode parse(String dataString) throws SchemaException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public boolean canParse(File file) throws IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean canParse(String dataString) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public String serializeToString(XNode xnode, QName rootElementName) throws SchemaException {
//		if (xnode instanceof RootXNode){
//			xnode = ((RootXNode) xnode).getSubnode();
//		}
//		return serializeToJson(xnode, rootElementName);
//	}
//
//	@Override
//	public String serializeToString(RootXNode xnode) throws SchemaException {
//		QName rootElementName = xnode.getRootElementName();
//		return serializeToJson(xnode.getSubnode(), rootElementName);
//	}
//	
//	
//	// ------------------- METHODS FOR SERIALIZATION ------------------------------
////	String globalNamespace = null;
//	public String serializeToJson(XNode node, QName rootElement) throws SchemaException{
//		try { 
////			globalNamespace = rootElement.getNamespaceURI();
//			StringWriter out = new StringWriter();
//			YAMLGenerator generator = createYAMLGenerator(out);
//			return writeObject(node, rootElement, generator, out);
//		} catch (IOException ex){
//			throw new SchemaException("Schema error during serializing to JSON.", ex);
//		}
//
//	}
//	
//	private String writeObject(XNode node, QName rootElement, YAMLGenerator generator, StringWriter out) throws JsonGenerationException, IOException{
//		generator.writeStartObject();
//		serializeToJson(node, rootElement, null, generator);
//		generator.writeEndObject();
//		generator.flush();
//		generator.close();
//		return out.toString();
//	}
//		
//	
//	String objectNs = null;
//	private <T> void  serializeToJson(XNode node, QName nodeName, String globalNamespace, YAMLGenerator generator) throws JsonGenerationException, IOException{
//		
//		if (node instanceof MapXNode){
//			serializerFromMap((MapXNode) node, nodeName, globalNamespace, generator);
//		} else if (node instanceof ListXNode){
//			serializeFromList((ListXNode) node, nodeName, globalNamespace, generator);
//		} else if (node instanceof PrimitiveXNode){
//			serializeFromPrimitive((PrimitiveXNode) node, nodeName, generator);
//		}
//	}
//	
//	
//	private void serializerFromMap(MapXNode map, QName nodeName, String globalNamespace, YAMLGenerator generator) throws JsonGenerationException, IOException{
//		if (nodeName == null){
//			generator.writeStartObject();
//		} else{
//			generator.writeObjectFieldStart(nodeName.getLocalPart());
//		}
//		
//		// this is used only by first iteration..we need to set namespace right after the root element
//		if (StringUtils.isBlank(globalNamespace)){
//			globalNamespace = nodeName.getNamespaceURI();
//			generator.writeStringField(PROP_NAMESPACE, globalNamespace);
//			
//		}
//		
//		
//		
//		
//		
//		Iterator<Entry<QName, XNode>> subnodes = map.entrySet().iterator();
//		while (subnodes.hasNext()){
//			Entry<QName, XNode> subNode = subnodes.next();
//			globalNamespace = serializeNsIfNeeded(subNode.getKey(), globalNamespace, generator);
//			serializeToJson(subNode.getValue(), subNode. getKey(), globalNamespace, generator);
//		}
//		generator.writeEndObject();
//	}
//	
//	private void serializeFromList(ListXNode list, QName nodeName, String globalNamespace, YAMLGenerator generator) throws JsonGenerationException, IOException{
//		ListIterator<XNode> sublist = list.listIterator();
//		generator.writeArrayFieldStart(nodeName.getLocalPart());
//		while (sublist.hasNext()){
//			serializeToJson(sublist.next(), null, globalNamespace, generator);
//		}
//		generator.writeEndArray();
//	}
//	
//	private void serializeFromPrimitive(PrimitiveXNode primitive, QName nodeName, YAMLGenerator generator) throws JsonGenerationException, IOException{
//		
//		if (primitive.isExplicitTypeDeclaration()) {
//			generator.writeStartObject();
//			generator.writeFieldName(TYPE_DEFINITION);
//			generator.writeObject(primitive.getTypeQName());
//
//			generator.writeObjectField(VALUE_FIELD, primitive.getValue());
//			generator.writeEndObject();
//		} else {
//
//			if (nodeName == null) {
//				generator.writeObject(primitive.getValue());
//			} else {
////				if (StringUtils.isNotBlank(nodeName.getNamespaceURI())
////						&& !nodeName.getNamespaceURI().equals(objectNs)) {
////					objectNs = nodeName.getNamespaceURI();
////				}
//				generator.writeObjectField(nodeName.getLocalPart(), primitive.getValue());
//			}
//		}
//	}
//	
//	private String serializeNsIfNeeded(QName subNodeName, String globalNamespace, YAMLGenerator generator) throws JsonGenerationException, IOException{
//		if (subNodeName == null){
//			return globalNamespace;
//		}
//		String subNodeNs = subNodeName.getNamespaceURI();
//		if (StringUtils.isNotEmpty(subNodeNs)){
//			if (!subNodeNs.equals(globalNamespace)){
//				globalNamespace = subNodeNs;
//				generator.writeStringField(PROP_NAMESPACE, globalNamespace);
//				
//			}
//		}
//		return globalNamespace;
//	}
	
	//------------------------END OF METHODS FOR SERIALIZATION -------------------------------
	
	public YAMLGenerator createGenerator(StringWriter out) throws SchemaException{
		try {
			YAMLFactory factory = new YAMLFactory();
			YAMLGenerator generator = factory.createGenerator(out);
			generator.setPrettyPrinter(new DefaultPrettyPrinter());
			generator.setCodec(configureMapperForSerialization());
//			generator.
			YAMLParser parser = factory.createParser(out.toString());
//			parser.
			return generator;
		} catch (IOException ex){
			throw new SchemaException("Schema error during serializing to JSON.", ex);
		}

		
	}
	
	private ObjectMapper configureMapperForSerialization(){
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.registerModule(createSerializerModule());
		return mapper;
	}
	
	private Module createSerializerModule(){
		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
		module.addSerializer(QName.class, new QNameSerializer());
		module.addSerializer(PolyString.class, new PolyStringSerializer());
		module.addSerializer(ItemPath.class, new ItemPathSerializer());
		module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
		return module;
	}
	
	@Override
	protected YAMLParser createParser(File file) throws SchemaException, IOException {
		YAMLFactory factory = new YAMLFactory();
		try {
			return factory.createParser(new FileInputStream(file));
		} catch (IOException e) {
			throw e;
		}
//		return parser;
	}

	@Override
	protected YAMLParser createParser(String dataString) throws SchemaException {
		YAMLFactory factory = new YAMLFactory();
		try {
			return factory.createParser(dataString);
		} catch (IOException e) {
			throw new SchemaException("Cannot create JSON parser: " + e.getMessage(), e);
		}
		
	}
	
}


