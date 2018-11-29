package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ItemPathDeserializer extends JsonDeserializer<UniformItemPath>{

	@Override
	public UniformItemPath deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		// TODO : implement..this is only for test
		if (jp.getCurrentToken() != JsonToken.VALUE_STRING) {
			throw new JsonParseException("Cannot parse path value. Expected that the value will be string but it is: " + jp.getCurrentTokenId(), jp.getCurrentLocation());
		}
		String path = jp.getText();
		if (StringUtils.isBlank(path)){
			throw new IllegalStateException("Error while deserializing path. No path specified.");
		}
		return ItemPath.parseFromString(path);
	}
}
