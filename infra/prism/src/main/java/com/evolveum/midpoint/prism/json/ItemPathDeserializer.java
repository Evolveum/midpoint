package com.evolveum.midpoint.prism.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ItemPathDeserializer extends JsonDeserializer<ItemPath>{

	@Override
	public ItemPath deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		// TODO Auto-generated method stub
		System.out.println("there will be Item path deserializer");
		// TODO : implement..this is only for test
		return new ItemPath(new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "name"));
	}

}
