package com.evolveum.midpoint.prism;

import org.testng.annotations.Test;

public class TestCompareJson extends TestCompare {

	@Override
	protected String getSubdirName() {
		return "json";
	}

	@Override
	protected String getFilenameSuffix() {
		return "json";
	}
	
	@Test
	public void test(){
	}

}
