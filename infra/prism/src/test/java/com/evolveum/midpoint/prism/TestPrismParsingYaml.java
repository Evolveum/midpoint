package com.evolveum.midpoint.prism;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismTestUtil;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

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
