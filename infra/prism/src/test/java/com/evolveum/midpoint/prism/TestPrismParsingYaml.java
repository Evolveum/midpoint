package com.evolveum.midpoint.prism;

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
