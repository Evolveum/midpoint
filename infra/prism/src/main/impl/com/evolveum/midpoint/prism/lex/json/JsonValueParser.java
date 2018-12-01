package com.evolveum.midpoint.prism.lex.json;

import java.io.IOException;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JsonValueParser<T> implements ValueParser<T> {

	@NotNull private final JsonParser parser;
	private JsonNode node;

	public JsonValueParser(@NotNull JsonParser parser, JsonNode node) {
		this.parser = parser;
		this.node = node;
	}

	@NotNull
	public JsonParser getParser() {
		return parser;
	}

	@Override
	public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
		ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		Class clazz = XsdTypeMapper.toJavaType(typeName);

		ObjectReader r = mapper.readerFor(clazz);
	    try {
			return r.readValue(node);
			// TODO implement COMPAT mode
		} catch (IOException e) {
			throw new SchemaException("Cannot parse value: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isEmpty() {
		return node == null || StringUtils.isBlank(node.asText());			// to be consistent with PrimitiveXNode.isEmpty for parsed values
	}

	@Override
	public String getStringValue() {
		if (node == null) {
			return null;
		}
		return node.asText();
	}

	@Override
	public String toString() {
		return "JsonValueParser(JSON value: "+node+")";
	}

    @Override
    public Map<String, String> getPotentiallyRelevantNamespaces() {
        return null;                // TODO implement
    }

    Element asDomElement() throws IOException {
		ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		ObjectReader r = mapper.readerFor(Document.class);
		return ((Document) r.readValue(node)).getDocumentElement();
	}
}
