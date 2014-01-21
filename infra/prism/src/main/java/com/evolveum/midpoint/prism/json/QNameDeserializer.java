package com.evolveum.midpoint.prism.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class QNameDeserializer extends JsonDeserializer<QName>{

	@Override
	public QName deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		System.out.println("tralalaaaaa   QName deserializer");
//		Object ob = jp.getEmbeddedObject();
//		
//		JsonNode node = jp.getCodec().readTree(jp);
////		JsonNode node = (JsonNode) ob;
////		JsonNode node = jp.readValueAsTree();
//		String nameSpace = null;
//		String localPart = null;
//		if (node.isObject()){
//			Iterator<Entry<String,JsonNode>> obj = node.fields();
//			while (obj.hasNext()){
//				
//				Entry<String, JsonNode> o = obj.next();
//				if ("namespace".equals(o.getKey())){
//					nameSpace = o.getValue().asText();
//				} else if ("localPart".equals(o.getKey())){
//					localPart = o.getValue().asText();
//				}
////				o.get
//			}
//		}
//		return new QName(nameSpace, localPart);
//		jp.;
		return null;
	}

}
