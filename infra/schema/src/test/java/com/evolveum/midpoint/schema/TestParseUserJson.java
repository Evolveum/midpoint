package com.evolveum.midpoint.schema;

import java.io.File;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;

public class TestParseUserJson extends TestParseUser {

	@Override
	protected String getSubdirName() {
		return "json/ns";
	}

	@Override
	protected String getLanguage() {
		return PrismContext.LANG_JSON;
	}

	@Override
	protected String getFilenameSuffix() {
		return "json";
	}

	@Override
	protected boolean hasNamespaces() {
		return true;
	}
}
