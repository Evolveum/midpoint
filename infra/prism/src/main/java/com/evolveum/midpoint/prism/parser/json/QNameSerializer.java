package com.evolveum.midpoint.prism.parser.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class QNameSerializer extends JsonSerializer<QName>{

	@Override
	public void serialize(QName value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
			JsonProcessingException {
		//System.out.println("wualaaa QName serialization");
		jgen.writeString(QNameUtil.qNameToUri(value));
//		jgen.writeString(value.toString());
//		jgen.writeStartObject();
//		jgen.writeStringField("@namespace", value.getNamespaceURI());
//		jgen.writeStringField("@localPart", value.getLocalPart());
//		jgen.writeEndObject();
	}

	@Override
	public void serializeWithType(QName value, JsonGenerator jgen, SerializerProvider provider,
			TypeSerializer typeSer) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		//System.out.println("QName serialization with type");
		
//		jgen.writeStartObject();
//		typeSer.writeCustomTypePrefixForObject(value, jgen, "qname");
//		typeSer.writeCustomTypePrefixForArray(value, jgen, "qname");
//		typeSer.writeCustomTypePrefixForScalar(value, jgen, "qname");
		serialize(value, jgen, provider);
//		typeSer.writeCustomTypeSuffixForScalar(value, jgen, "tra");
//		typeSer.writeCustomTypeSuffixForArray(value, jgen, "qname");
//		typeSer.writeCustomTypeSuffixForObject(value, jgen, "bla");
//		jgen.writeTypeId("http://qname");
//		jgen.writeObject(value.toString());
//		jgen.writeEndObject();
		
	}
	
}
