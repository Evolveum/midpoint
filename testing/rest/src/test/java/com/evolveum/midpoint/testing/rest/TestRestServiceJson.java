/*
 * Copyright (c) 2013-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.rest;

import java.io.File;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;

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
