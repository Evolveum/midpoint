package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class ItemPathSerializer extends JsonSerializer<UniformItemPath> {

	@Override
	public void serialize(UniformItemPath value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		jgen.writeObject(value.serializeWithForcedDeclarations());

	}

	@Override
	public void serializeWithType(UniformItemPath value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
			throws IOException {
		serialize(value, jgen, provider);
	}

}
