package com.evolveum.midpoint.prism.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.TrivialXPathParser;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ItemPathDeserializer extends JsonDeserializer<ItemPath>{

	@Override
	public ItemPath deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		// TODO Auto-generated method stub
//		System.out.println("there will be Item path deserializer");
		// TODO : implement..this is only for test
		if (jp.getCurrentToken() != JsonToken.VALUE_STRING){
			throw new JsonParseException("Cannot parse path value. Expected that the value will be string but it is: " + jp.getCurrentTokenId(), jp.getCurrentLocation());
		}
		String path = jp.getText();
		System.out.println("path: " + path);
		XPathHolder holder = new XPathHolder(path);
		return holder.toItemPath();
//		return new ItemPath(new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "name"));
	}

}
