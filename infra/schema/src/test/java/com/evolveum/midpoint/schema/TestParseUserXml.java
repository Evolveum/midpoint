package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;

public class TestParseUserXml extends TestParseUser {

	@Override
	protected String getSubdirName() {
		return "xml/ns";
	}

	@Override
	protected String getLanguage() {
		return PrismContext.LANG_XML;
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}

	@Override
	protected boolean hasNamespaces() {
		return true;
	}
}
