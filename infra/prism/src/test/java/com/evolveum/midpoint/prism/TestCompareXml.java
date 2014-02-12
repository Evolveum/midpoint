package com.evolveum.midpoint.prism;

import org.testng.annotations.Test;

public class TestCompareXml extends TestCompare{
	@Test
	public void f() {
	}

	@Override
	protected String getSubdirName() {
		return "xml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}
}
