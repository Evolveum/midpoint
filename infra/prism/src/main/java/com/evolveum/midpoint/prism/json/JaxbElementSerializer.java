package com.evolveum.midpoint.prism.json;

import java.io.IOException;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.MimeType;
import javax.activation.MimeTypeParameterList;
import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.map.HashedMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.sun.activation.registries.MimeTypeEntry;

public class JaxbElementSerializer extends JsonSerializer<JAXBElement>{
	
	private ObjectMapper mapper = null;
	private Map<Class, String> classMapping = new HashedMap();
	
	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}
	
	public void addToMap(Class expectedClass, String simpleName){
		
		classMapping.put(expectedClass, simpleName);
	}
	
	@Override
	public void serialize(JAXBElement value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		System.out.println("JAXB ELEMENT SERIALIZATION");
		System.out.println("object: " + value.getValue());
//		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
		ObjectCodec o = jgen.getCodec();;
//		jgen.writeStartObject();
//		Object o = value.getValue();
//		String objectName = classMapping.get(o.getClass());
//		jgen.writeFieldName(o.getClass().getSimpleName().toLowerCase());
//		provider.getAnnotationIntrospector().allIntrospectors().add(new JaxbAnnotationIntrospector());
		jgen.writeObject(value.getValue());
//		DataHandler dh = new DataHandler(value.getValue(), "text/xml");
//		jgen.writeObject(dh);
//		jgen.writeEndObject();
	}

	@Override
	public void serializeWithType(JAXBElement value, JsonGenerator jgen, SerializerProvider provider,
			TypeSerializer typeSer) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		System.out.println("JAXB ELEMENT SERIALIZATION WITH TYPE");
		System.out.println("object: " + value.getValue());
//		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
//		ObjectCodec o = jgen.getCodec();;
//		jgen.writeStartObject();
//		Object o = value.getValue();
//		String objectName = classMapping.get(o.getClass());
//		jgen.writeFieldName(o.getClass().getSimpleName().toLowerCase());
//		provider.getAnnotationIntrospector().allIntrospectors().add(new JaxbAnnotationIntrospector());
		jgen.writeObject(value.getValue());
//		jgen.writeEndObject();
//		super.serializeWithType(value, jgen, provider, typeSer);
	}
}
