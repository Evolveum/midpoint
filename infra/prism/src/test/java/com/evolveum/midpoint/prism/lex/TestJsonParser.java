package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.lex.json.JsonLexicalProcessor;

public class TestJsonParser  extends AbstractLexicalProcessorTest {
	
	@Override
	protected String getSubdirName() {
		return "json";
	}

	@Override
	protected String getFilenameSuffix() {
		return "json";
	}

	@Override
	protected JsonLexicalProcessor createParser() {
		return new JsonLexicalProcessor();
	}

	@Override
	protected String getWhenItemSerialized() {
		return "\"when\" : \"2012-02-24T10:48:52.000Z\"";
	}
}
