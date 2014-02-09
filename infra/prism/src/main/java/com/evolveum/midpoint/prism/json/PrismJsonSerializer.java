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
import org.w3c.dom.Element;

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
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.deser.DomElementJsonDeserializer;
import com.fasterxml.jackson.module.jaxb.ser.DomElementJsonSerializer;

public class PrismJsonSerializer extends AbstractParser2{
	
//	private static final String PROP_NAMESPACE = "@ns";
//	private static final String TYPE_DEFINITION = "@typeDef";
//	private static final String VALUE_FIELD = "@value";
//	
//	
	@Override
	protected JsonParser createParser(File file) throws SchemaException, IOException {
		JsonFactory factory = new JsonFactory();
		try {
			return factory.createParser(new FileInputStream(file));
		} catch (IOException e) {
			throw e;
		}
//		return parser;
	}

	@Override
	protected JsonParser createParser(String dataString) throws SchemaException {
		JsonFactory factory = new JsonFactory();
		try {
			return factory.createParser(dataString);
		} catch (IOException e) {
			throw new SchemaException("Cannot create JSON parser: " + e.getMessage(), e);
		}
		
	}
	public JsonGenerator createGenerator(StringWriter out) throws SchemaException{
		return createJsonGenerator(out);
	}
	private JsonGenerator createJsonGenerator(StringWriter out) throws SchemaException{
		try {
			JsonFactory factory = new JsonFactory();
			JsonGenerator generator = factory.createGenerator(out);
			generator.setPrettyPrinter(new DefaultPrettyPrinter());
			generator.setCodec(configureMapperForSerialization());
			
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
		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
		return mapper;
	}
	
	private Module createSerializerModule(){
		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
		module.addSerializer(QName.class, new QNameSerializer());
		module.addSerializer(PolyString.class, new PolyStringSerializer());
		module.addSerializer(ItemPath.class, new ItemPathSerializer());
//		module.addSerializer(Element.class, new DomElementJsonSerializer());
//		module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
		return module;
	}
	

}
