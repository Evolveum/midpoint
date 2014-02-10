package com.evolveum.midpoint.prism.parser;

import org.testng.annotations.Test;

public class TestYamlParser extends AbstractParserTest{
 
	
	@Override
	protected String getSubdirName() {
		return "yaml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "yaml";
	}

	@Override
	protected YamlParser createParser() {
		return new YamlParser();
	}

	@Test
	public void f() {
	}
}
