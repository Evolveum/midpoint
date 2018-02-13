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
package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.impl.util.RestServiceUtil;

/**
 * @author Katka Valalikova
 * @author Radovan Semancik
 */
public class MidpointRestAuthenticationHandler implements ContainerRequestFilter, ContainerResponseFilter {

//	private static final Trace LOGGER = TraceManager.getTrace(MidpointRestAuthenticationHandler.class);

	@Autowired(required=true)
	private MidpointRestPasswordAuthenticator passwordAuthenticator;

	@Autowired(required=true)
	private MidpointRestSecurityQuestionsAuthenticator securityQuestionAuthenticator;


	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		// nothing to do
	}

	@Override
	public void filter(ContainerRequestContext requestCtx) throws IOException {
		Message m = JAXRSUtils.getCurrentMessage();

		AuthorizationPolicy policy = (AuthorizationPolicy) m.get(AuthorizationPolicy.class);
		if (policy != null) {
			passwordAuthenticator.handleRequest(policy, m, requestCtx);
			return;
		}

		String authorization = requestCtx.getHeaderString("Authorization");

		if (StringUtils.isBlank(authorization)){
			RestServiceUtil.createAbortMessage(requestCtx);
			return;
		}

		String[] parts = authorization.split(" ");
		String authenticationType = parts[0];

		if (parts.length == 1) {
			if (RestAuthenticationMethod.SECURITY_QUESTIONS.equals(authenticationType)) {
				RestServiceUtil.createSecurityQuestionAbortMessage(requestCtx, "{\"user\" : \"username\"}");
				return;
			}
		}

		if (parts.length != 2 || (!RestAuthenticationMethod.SECURITY_QUESTIONS.equals(authenticationType))) {
			RestServiceUtil.createAbortMessage(requestCtx);
			return;
		}
		String base64Credentials = (parts.length == 2) ? parts[1] : null;
		try {
			String decodedCredentials = new String(Base64Utility.decode(base64Credentials));
			if (RestAuthenticationMethod.SECURITY_QUESTIONS.equals(authenticationType)) {

				policy = new AuthorizationPolicy();
				policy.setAuthorizationType(RestAuthenticationMethod.SECURITY_QUESTIONS.getMethod());
				policy.setAuthorization(decodedCredentials);
			}
			securityQuestionAuthenticator.handleRequest(policy, m, requestCtx);
		} catch (Base64Exception e) {
			RestServiceUtil.createSecurityQuestionAbortMessage(requestCtx, "{\"user\" : \"username\"}");
			return;

		}

	}



//	protected void createAbortMessage(ContainerRequestContext requestCtx){
//		requestCtx.abortWith(Response.status(Status.UNAUTHORIZED)
//				.header("WWW-Authenticate", AuthenticationType.BASIC.getAuthenticationType() + " realm=\"midpoint\", " + AuthenticationType.SECURITY_QUESTIONS.getAuthenticationType()).build());
//	}
//

}
