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
