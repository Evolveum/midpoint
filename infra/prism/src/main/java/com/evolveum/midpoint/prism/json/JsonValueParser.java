package com.evolveum.midpoint.prism.json;

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
		
//		if (clazz == null){
//			clazz = schemaRegistry.determineCompileTimeClass(typeName);
//		}
		
		
		ObjectReader r = mapper.reader(clazz);
	    try {
//	    	TokenBuffer tb = parser.readValueAs(TokenBuffer.class);
	    	JsonFactory f = new JsonFactory();
//	    	node.as
//	    	System.out.println("OID: " + tb.asParser().getObjectId());
//	    	System.out.println("TID: " + tb.asParser().getTypeId());
	    	if (parser.getCurrentToken() == null){
	    		JsonToken t = parser.nextToken();
//	    		System.out.println("token: " + t);
	    		if (t == null){
	    			t = parser.nextToken();
//		    		System.out.println("token: " + t);
	    		}
	    	}
//	    	T val = (T) parser.readValueAs(clazz);
//	    	System.out.println("Parsed to :  " + val);
//	    	return val;
			return r.readValue(node);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new SchemaException("Cannot parse value: " + e.getMessage(), e);
		}
	}
	
	@Override
	public String toString() {
		return "JsonValueParser(JSON value: token: "+node+")";
	}

}
