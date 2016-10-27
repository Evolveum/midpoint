package com.evolveum.midpoint.prism;

import org.testng.annotations.Test;

public class TestPrismParsingYaml extends TestPrismParsing {

	@Override
	protected String getSubdirName() {
		return "yaml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "yaml";
	}

	@Override
	protected String getOutputFormat() {
		return PrismContext.LANG_YAML;
	}

}
