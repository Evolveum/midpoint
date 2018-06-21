package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
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
		if (StringUtils.isBlank(path)){
			throw new IllegalStateException("Error while deserializing path. No path specified.");
		}
		//System.out.println("path: " + path);
//		if (path.startsWith("declare.*")){
			ItemPathHolder holder = new ItemPathHolder(path);
			return holder.toItemPath();
//		}
//		else {
//			String[] segments = path.split("/");
//			if (segments.length == 1){
//				String[] pathItems = segments[0].split(":");
//				if (pathItems.length == 1){
//					return new ItemPath(QNameUtil.nullNamespace(pathItems[0]));
//				}
//
//			} else
//		}
//		return new ItemPath(new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "name"));
	}

}
