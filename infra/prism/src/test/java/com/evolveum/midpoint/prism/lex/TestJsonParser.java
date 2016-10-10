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

}
