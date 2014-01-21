package com.evolveum.midpoint.prism.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DOMUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

public class DOMDeserializer extends JsonDeserializer<Element>{

	@Override
	public Element deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		System.out.println("tralalaaaaa DOM Deserialization");
		ObjectReader c = (ObjectReader) jp.getCodec();
		JsonToken t = jp.getCurrentToken();
		
//		ObjectReader r = c.reader();
		JsonNode root = c.readValue(jp);
		
		Object tree = jp.readValueAsTree();
		
//		while (jp.nextToken() != JsonToken.END_OBJECT){
//			
//		}
//		String s = jp.nextTextValue();
////		jp.ne
//		if (t.equals(JsonToken.START_OBJECT)){
//		JsonNode root = jp.readValueAsTree();
		Document doc = DOMUtil.getDocument();
		Element rootE = doc.createElement(jp.getCurrentName());
		readElementValue(doc, root, rootE);
		Element ret = (Element) doc.getParentNode();
		return rootE;
//		}
//		return null;
	}

	private Element readElementValue(Document doc, JsonNode valueElement, Element root) {
		if (valueElement.isObject()){
		Iterator<Entry<String, JsonNode>> elements = valueElement.fields();
		while (elements.hasNext()){
			Entry<String, JsonNode> element = elements.next();
			Element e = doc.createElement(element.getKey());
			
			if (element.getValue().isObject()){
				readElementValue(doc, element.getValue(), e);
			} else if (element.getValue().isArray()){
				e.setTextContent("array");
//				element.getValue().
			} else{
				e.setTextContent(element.getValue().asText());
			}
			if (root != null){
				root.appendChild(e);
			}
			
		}
		} else {
			String text = valueElement.asText();
			if (StringUtils.isNotBlank(text)){
			root.setTextContent(valueElement.asText());
			} else 
				root = null;
		}
		
		
		
		return root;
		
	}
}
