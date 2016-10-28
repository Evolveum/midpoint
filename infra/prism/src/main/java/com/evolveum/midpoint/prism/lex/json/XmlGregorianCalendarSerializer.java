package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ext.CoreXMLSerializers.XMLGregorianCalendarSerializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class XmlGregorianCalendarSerializer extends XMLGregorianCalendarSerializer{

	@Override
	public void serializeWithType(XMLGregorianCalendar value, JsonGenerator jgen,
			SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		serialize(value, jgen, provider);
	}
}
