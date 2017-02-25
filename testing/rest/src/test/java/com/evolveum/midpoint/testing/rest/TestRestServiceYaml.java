package com.evolveum.midpoint.testing.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.impl.rest.MidpointAbstractProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import jdk.nashorn.internal.ir.annotations.Ignore;

public class TestRestServiceYaml extends TestAbstractRestService {

	@Override
	protected String getAcceptHeader() {
		return "application/yaml";
	}

	@Override
	protected String getContentType() {
		return "application/yaml";
	}
	
	
	@Override
	protected File getRepoFile(String fileBaseName) {
		return new File(BASE_REPO_DIR + "/yaml", fileBaseName + ".yml");
	}
	
	@Override
	protected File getRequestFile(String fileBaseName) {
		return new File(BASE_REQ_DIR + "/yaml", fileBaseName + ".yml");
	}

	@Override
	protected MidpointAbstractProvider getProvider() {
		return yamlProvider;
	}
	
}
