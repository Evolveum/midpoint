package com.evolveum.midpoint.prism.parser.json;

import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.apache.commons.lang.StringUtils;

public class JsonValueParser<T> implements ValueParser<T> {

	private final JsonParser parser;
	private JsonNode node;
		
	public JsonValueParser(JsonParser parser, JsonNode node) {
		this.parser = parser;
		this.node = node;
	}
	
	public JsonValueParser(final JsonParser parser) {
		this.parser = parser;
	}
	
	public JsonParser getParser() {
		return parser;
	}
	@Override
	public T parse(QName typeName) throws SchemaException {
		ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		Class clazz = XsdTypeMapper.toJavaType(typeName);
			
		ObjectReader r = mapper.reader(clazz);
	    try {
//	    	if (parser.getCurrentToken() == null){
//	    		JsonToken t = parser.nextToken();
//	    		if (t == null){
//	    			t = parser.nextToken();
//	    		}
//	    	}
			return r.readValue(node);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new SchemaException("Cannot parse value: " + e.getMessage(), e);
		}
	}
	
	@Override
	public boolean isEmpty() {
		return node == null || parser == null ||
				StringUtils.isBlank(node.asText());			// to be consistent with PrimitiveXNode.isEmpty for parsed values
	}

	@Override
	public String getStringValue() {
		if (node == null){
			return null;
		}
		return node.asText();
	}

	@Override
	public String toString() {
		return "JsonValueParser(JSON value: "+node+")";
	}

}
