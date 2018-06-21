package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class QNameDeserializer extends JsonDeserializer<QName>{

	@Override
	public QName deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		//System.out.println("tralalaaaaa   QName deserializer");
//		Object ob = jp.getEmbeddedObject();

//		JsonNode node = jp.getCodec().readTree(jp);
//		JsonNode node = (JsonNode) ob;
//		TreeTraversingParser treeParser = (TreeTraversingParser) jp;
//		if (jp.getCurrentToken() == JsonToken.START_OBJECT){

//		}
//		jp.
		ObjectMapper m = new ObjectMapper();
		JsonNode node = m.readValue(jp, JsonNode.class);
//		JsonNode node = jp.readValueAsTree();
//		jp.
		switch (node.getNodeType()) {
			case OBJECT:
				return deserializeFromObject(node);
//				break;
			case STRING:
				return deserializeFromString(node);
//				break;
			default:
				throw new IllegalStateException();
//				break;
		}

	}

	private QName deserializeFromObject(JsonNode node){
		JsonNode qnameNode = node.get("@namespace");
		String namespace = null;
		if (qnameNode != null){
			namespace = qnameNode.asText();
		}

		qnameNode = node.get("@localPart");
		String localPart = null;
		if (qnameNode != null){
			localPart = qnameNode.asText();
		}
		return new QName(namespace, localPart);
	}

	private QName deserializeFromString(JsonNode node){
		String qnameUri = node.asText();
		return QNameUtil.uriToQName(qnameUri, true);
//		return new QName(node.asText());
	}

	@Override
	public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
			TypeDeserializer typeDeserializer) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		return super.deserializeWithType(jp, ctxt, typeDeserializer);
	}

}
