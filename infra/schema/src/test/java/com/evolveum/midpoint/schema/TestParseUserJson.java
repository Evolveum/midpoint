package com.evolveum.midpoint.schema;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;

public class TestParseUserJson extends TestParseUser{
	
  @Test
  public void f() {
  }


@Override
protected String getSubdirName() {
	return "json";
}

@Override
protected String getLanguage() {
	return PrismContext.LANG_JSON;
}

@Override
protected String getFilenameSuffix() {
	return "json";
}
}
