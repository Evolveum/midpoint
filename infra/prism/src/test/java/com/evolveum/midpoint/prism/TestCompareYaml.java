package com.evolveum.midpoint.prism;

import org.testng.annotations.Test;

public class TestCompareYaml extends TestCompare {
	@Test
	public void f() {
	}

	@Override
	protected String getSubdirName() {
		return "yaml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "yaml";
	}
}
