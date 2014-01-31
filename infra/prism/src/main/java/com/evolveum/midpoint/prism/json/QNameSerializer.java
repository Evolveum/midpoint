package com.evolveum.midpoint.prism.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class QNameSerializer extends JsonSerializer<QName>{

	@Override
	public void serialize(QName value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
			JsonProcessingException {
		System.out.println("wualaaa QName serialization");
		jgen.writeStartObject();
		jgen.writeStringField("@namespace", value.getNamespaceURI());
		jgen.writeStringField("@localPart", value.getLocalPart());
		jgen.writeEndObject();
	}

}
