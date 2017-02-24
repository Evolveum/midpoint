package com.evolveum.midpoint.testing.rest;

import java.io.File;

import javax.ws.rs.core.MediaType;

//@ContextConfiguration(locations = { "classpath:ctx-rest-test.xml" })
//@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRestServiceXml extends TestAbstractRestService {

	@Override
	protected String getAcceptHeader() {
		return MediaType.APPLICATION_XML;
	}

	@Override
	protected String getContentType() {
		return MediaType.APPLICATION_XML;
	}

	
	@Override
	protected File getRepoFile(String fileBaseName) {
		return new File(BASE_REPO_DIR + "/xml", fileBaseName + ".xml");
	}
	
	@Override
	protected File getRequestFile(String fileBaseName) {
		return new File(BASE_REQ_DIR + "/xml", fileBaseName + ".xml");
	}

}
