package com.evolveum.midpoint.prism.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class JsonValueParser<T> implements ValueParser<T> {

	private JsonParser parser;
	private JsonNode node;
		
	public JsonValueParser(JsonParser parser, JsonNode node) {
		this.parser = parser;
		this.node = node;
	}
	
	@Override
	public T parse(QName typeName) throws SchemaException {
		ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		Class clazz = XsdTypeMapper.toJavaType(typeName);
		
//		if (clazz == null){
//			clazz = schemaRegistry.determineCompileTimeClass(typeName);
//		}
		
		ObjectReader r = mapper.reader(clazz);
	    try {
			return r.readValue(node);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new SchemaException("Cannot parse value: " + e.getMessage(), e);
		}
	}
	
	@Override
	public String toString() {
		return "ValueParser(JSON value: "+node+")";
	}

}
