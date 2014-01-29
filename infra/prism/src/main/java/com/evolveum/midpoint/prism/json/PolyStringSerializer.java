package com.evolveum.midpoint.prism.json;

import java.io.IOException;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PolyStringSerializer extends JsonSerializer<PolyString>{

	@Override
	public void serialize(PolyString value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		System.out.println("wualaaaa polystring serialization");
//		jgen.writeStartObject();
		jgen.writeString(value.getOrig());
//		jgen.writeStringField("norm", value.getNorm());
//		jgen.writeEndObject();
		
	}

}
