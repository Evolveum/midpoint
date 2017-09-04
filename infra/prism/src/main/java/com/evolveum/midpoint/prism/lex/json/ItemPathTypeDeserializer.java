package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ItemPathTypeDeserializer extends JsonDeserializer<ItemPathType>{

	@Override
	public ItemPathType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		//System.out.println("ITEM PATH TYPE");
		//System.out.println("current t: " +jp.getCurrentToken());
		//System.out.println("cuuretn nmae " + jp.getCurrentName());
		//System.out.println("item path type: " + jp.getText());

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
			ItemPath itemPath = holder.toItemPath();
			ItemPathType itemPathType = new ItemPathType(itemPath);
			return itemPathType;
//		ItemPathType itemPathType = new ItemPathType();
//		itemPathType.getContent().add(jp.getText());
//
//		return itemPathType;
	}

}
