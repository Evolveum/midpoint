package com.evolveum.midpoint.testing.rest;

import java.io.File;

import javax.ws.rs.core.MediaType;

import com.evolveum.midpoint.model.impl.rest.MidpointAbstractProvider;

//@ContextConfiguration(locations = { "classpath:ctx-rest-test.xml" })
//@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRestServiceJson extends TestAbstractRestService {

	@Override
	protected String getAcceptHeader() {
		return MediaType.APPLICATION_JSON;
	}

	@Override
	protected String getContentType() {
		return MediaType.APPLICATION_JSON;
	}

	
	@Override
	protected File getRepoFile(String fileBaseName) {
		return new File(BASE_REPO_DIR + "/json", fileBaseName + ".json");
	}
	
	@Override
	protected File getRequestFile(String fileBaseName) {
		return new File(BASE_REQ_DIR + "/json", fileBaseName + ".json");
	}

	@Override
	protected MidpointAbstractProvider getProvider() {
		return jsonProvider;
	}
	

}
