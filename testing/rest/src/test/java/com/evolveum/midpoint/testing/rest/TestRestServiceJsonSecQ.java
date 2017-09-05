/*
 * Copyright (c) 2010-2017 Evolveum
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

import org.apache.cxf.jaxrs.client.WebClient;

public class TestRestServiceJsonSecQ extends TestRestServiceJson{


	@Override
	protected void createAuthorizationHeader(WebClient client, String username, String password) {

		if (username == null) {
			return;
		}

		String authzHeader = "{"
				+ "\"user\" : \""+ username +"\","
				+ "\"answer\" : ["
					+ "{ "
						+ "\"qid\" : \"http://midpoint.evolveum.com/xml/ns/public/security/question-2#q001\","
						+ "\"qans\" : \"" + (password == null ? "" : password) + "\""
					+ "}"
				+ "]"
				+ "}";

			String authorizationHeader = "SecQ "
					+ org.apache.cxf.common.util.Base64Utility.encode((authzHeader).getBytes());
			client.header("Authorization", authorizationHeader);

	}
}
