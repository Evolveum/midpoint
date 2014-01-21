package com.evolveum.midpoint.prism.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.sun.tools.jxc.gen.config.Schema;

public class JaxbElementDeserializer extends JsonDeserializer<JAXBElement>{

	private Class expectedClass = null;
	private PrismJaxbProcessor jaxbProcessor;
	private JsonNode node;
	private PrismSchema prismSchema;
	
	@Override
	public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
			TypeDeserializer typeDeserializer) throws IOException, JsonProcessingException {
		System.out.println("JAXB ELEMETN DESERIALIZER WITH TYPE");
		System.out.println("json parser: " + jp.toString());
		System.out.println(jp.getCurrentName());
		JsonNode node = jp.readValueAsTree();
		Object o = des(node, jp, ctxt, typeDeserializer);
		return o;
	}
	
	@Override
	public JAXBElement deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		System.out.println("JAXB ELEMETN DESERIALIZER");
		System.out.println("json parser: " + jp.toString());
		System.out.println(jp.getCurrentName());
//		JsonNode node = jp.readValueAsTree();
//		Marshaller.
//		Xml
		ObjectReader codec = (ObjectReader) jp.getCodec();
		if (node != null){
			iterate(node, jp);
		}
//			if (node.isObject()){
//				Iterator<Entry<String, JsonNode>> fields = node.fields();
//				
//				while (fields.hasNext()){
//				
//				Entry<String, JsonNode> field = fields.next();
//				
//				QName pName = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-2a", field.getKey());
//				Class expectedJavaType = XsdTypeMapper.toJavaType(pName);
//		    	if (expectedJavaType == null) {
//		    		SchemaRegistry schemaRegistry = prismSchema.getPrismContext().getSchemaRegistry();
//		    		expectedJavaType = (Class) schemaRegistry.determineCompileTimeClass(pName);
//		    	}
//		    	if (field.getValue().isArray()){
//		    		
//		    	}
//		    	if (!field.getValue().isObject()){
////					ctxt.
////		    		ObjectReader r = ctxt.getFactory().c
//		    		ObjectMapper m = new ObjectMapper();
//		    		m.reader(expectedClass);
//		    		codec.readValue(node);
//				}
//		    	
//		    		if (prismSchema.getPrismContext().getPrismJaxbProcessor().canConvert(expectedJavaType)){
//		    			this.setExpectedClass(expectedJavaType);
//		    			this.node = field.getValue();
//		    			jp.readValueAs(JAXBElement.class);
//		    		}
//		    		jp.readValueAs(expectedJavaType);
//		    	}
//				}
//			} else {
//				
//			}
		
		
//		Object tree = codec.readTree(jp);
		
//		Object o = jp.readValueAs(expectedClass);
//		Unmarshaller unmarshaler = jaxbProcessor.getUnmarshaller();
//		unmarshaler.setProperty, value);
//		Object o  = des(node, jp, ctxt);
//		
//		System.out.println(node.);
//		jp.rea
		return null;
	}
	
	
	private void iterate(JsonNode node, JsonParser jp) throws JsonProcessingException, IOException{
		if (node.isObject()){
		
		Iterator<Entry<String, JsonNode>> fields = node.fields();
		
		while (fields.hasNext()){
		
		Entry<String, JsonNode> field = fields.next();
		
		QName pName = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-2a", field.getKey());
		ItemDefinition def = prismSchema.findItemDefinition(pName, ItemDefinition.class);
		Class expectedJavaType = null;
		if (def != null){
			
		expectedJavaType = XsdTypeMapper.toJavaType(def.getTypeName());
    	if (expectedJavaType == null) {
    		SchemaRegistry schemaRegistry = prismSchema.getPrismContext().getSchemaRegistry();
    		expectedJavaType = (Class) schemaRegistry.determineCompileTimeClass(def.getTypeName());
    	}
		}
    	if (field.getValue().isArray()){
    		Iterator<JsonNode> n = field.getValue().iterator();
    		while (n.hasNext()){
    		iterate(n.next(), jp);
    		}
    	} else{
    	
    	if (!field.getValue().isObject()){
			
    		if (expectedJavaType != null){
    		ObjectMapper m = new ObjectMapper();
    		
    		ObjectReader r = m.reader(expectedJavaType);
    		r.readValue(field.getValue());
    		} 
		}
    	}
    	if (expectedJavaType == null){
    		continue;
    	}
    		if (prismSchema.getPrismContext().getPrismJaxbProcessor().canConvert(expectedJavaType)){
    			this.setExpectedClass(expectedJavaType);
    			this.node = field.getValue();
    			jp.readValueAs(JAXBElement.class);
    		}
//    		jp.readValueAs(expectedJavaType);
    	}
		}
		
	}
	
	private Object des(JsonNode node, JsonParser jp, DeserializationContext ctxt){
		if (node.isObject()){
			Iterator<Entry<String, JsonNode>> nodes = node.fields();
			while (nodes.hasNext()){
				Entry<String, JsonNode> n = nodes.next();
				System.out.println("node name" + n.getKey() + " node value: " + n.getValue().toString());
				System.out.println();
				des(n.getValue(), jp, ctxt);
			}
		} else{
			try{
			System.out.println("pure value: " + node.toString());
			Object val =jp.readValueAs(expectedClass);
			return val;
			} catch (Exception ex){
				throw new IllegalStateException(ex);
			}
		}
		return null;
	}

	
	private Object des(JsonNode node, JsonParser jp, DeserializationContext ctxt, TypeDeserializer type){
		if (node.isObject()){
			Iterator<Entry<String, JsonNode>> nodes = node.fields();
			while (nodes.hasNext()){
				Entry<String, JsonNode> n = nodes.next();
				System.out.println("node name" + n.getKey() + " node value: " + n.getValue().toString());
				System.out.println();
				des(n.getValue(), jp, ctxt, type);
			}
		} else{
			try{
			System.out.println("pure value: " + node.toString());
			Object val =jp.readValueAs(type.getDefaultImpl());
			return val;
			} catch (Exception ex){
				throw new IllegalStateException(ex);
			}
		}
		return null;
	}
	
	public void setExpectedClass(Class expectedClass) {
		this.expectedClass = expectedClass;
	}
	
	public void setNode(JsonNode node) {
		this.node = node;
	}
	
	public void setPrismSchema(PrismSchema prismSchema) {
		this.prismSchema = prismSchema;
	}

}
