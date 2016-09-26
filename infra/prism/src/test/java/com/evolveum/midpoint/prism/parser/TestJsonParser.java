package com.evolveum.midpoint.prism.parser;

import com.evolveum.midpoint.prism.parser.json.JsonParser;
import org.testng.annotations.Test;

public class TestJsonParser  extends AbstractParserTest {
	
	@Override
	protected String getSubdirName() {
		return "json";
	}

	@Override
	protected String getFilenameSuffix() {
		return "json";
	}

	@Override
	protected JsonParser createParser() {
		return new JsonParser();
	}

	@Test
	public void test(){
		
	}
}
