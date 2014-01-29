package com.evolveum.midpoint.prism.parser;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.json.PrismJsonSerializer;

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
	protected PrismJsonSerializer createParser() {
		return new PrismJsonSerializer();
	}

	@Test
	public void test(){
		
	}
}
