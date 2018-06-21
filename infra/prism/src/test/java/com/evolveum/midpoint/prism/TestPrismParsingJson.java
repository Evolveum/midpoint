package com.evolveum.midpoint.prism;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;

public class TestPrismParsingJson extends TestPrismParsing {

	@Override
	protected String getSubdirName() {
		return "json";
	}

	@Override
	protected String getFilenameSuffix() {
		return "json";
	}

	@Test
	public void f() {
	}

	@Override
	protected String getOutputFormat() {
		return PrismContext.LANG_JSON;
	}

}
