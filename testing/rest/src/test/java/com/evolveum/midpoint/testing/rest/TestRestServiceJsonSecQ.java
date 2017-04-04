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
