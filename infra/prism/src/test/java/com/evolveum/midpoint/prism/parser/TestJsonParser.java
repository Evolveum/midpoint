package com.evolveum.midpoint.prism.parser;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.json.PrismJsonSerializer;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

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
		return new PrismJsonSerializer(PrismTestUtil.getSchemaRegistry());
	}

	@Test
	public void test(){
		
	}
}
