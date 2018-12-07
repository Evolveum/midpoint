package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class QNameSerializer extends JsonSerializer<QName> {

	@Override
	public void serialize(QName value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		jgen.writeString(QNameUtil.qNameToUri(value, false));
	}

	@Override
	public void serializeWithType(QName value, JsonGenerator jgen, SerializerProvider provider,
			TypeSerializer typeSer) throws IOException {
		serialize(value, jgen, provider);
	}

}
