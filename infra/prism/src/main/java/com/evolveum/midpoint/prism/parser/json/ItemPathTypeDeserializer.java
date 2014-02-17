package com.evolveum.midpoint.prism.parser.json;

import java.io.IOException;

import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ItemPathTypeDeserializer extends JsonDeserializer<ItemPathType>{

	@Override
	public ItemPathType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		System.out.println("ITEM PATH TYPE");
		System.out.println("current t: " +jp.getCurrentToken());
		System.out.println("cuuretn nmae " + jp.getCurrentName());
		System.out.println("item path type: " + jp.getText());
		
		ItemPathType itemPathType = new ItemPathType();
		itemPathType.getContent().add(jp.getText());
		
		return itemPathType;
	}

}
