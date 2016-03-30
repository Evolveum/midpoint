/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 * @author Radovan Semancik
 */
public class MidPointAuthenticationProvider implements AuthenticationProvider {

	private static final Trace LOGGER = TraceManager.getTrace(MidPointAuthenticationProvider.class);
	
	@Autowired(required = true)
	private transient AuthenticationEvaluator authenticationEvaluator;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		
		String enteredUsername = (String) authentication.getPrincipal();
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		Authentication token;
		if (authentication instanceof UsernamePasswordAuthenticationToken) {
			String enteredPassword = (String) authentication.getCredentials();
			token = authenticationEvaluator.authenticateUserPassword(connEnv, enteredUsername, enteredPassword);
		} else if (authentication instanceof PreAuthenticatedAuthenticationToken) {
			token = authenticationEvaluator.authenticateUserPreAuthenticated(connEnv, enteredUsername);
		} else {
			LOGGER.error("Unsupported authentication {}", authentication);
			throw new AuthenticationServiceException("web.security.provider.unavailable");
		}

		MidPointPrincipal principal = (MidPointPrincipal)token.getPrincipal();
		
		LOGGER.debug("User '{}' authenticated ({}), authorities: {}", new Object[]{authentication.getPrincipal(), 
				authentication.getClass().getSimpleName(), principal.getAuthorities()});	
		return token;
	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		if (UsernamePasswordAuthenticationToken.class.equals(authentication)) {
			return true;
		}
		if (PreAuthenticatedAuthenticationToken.class.equals(authentication)) {
			return true;
		}

		return false;
	}
	
	private ConnectionEnvironment createConnectionEnvironment() {
		ConnectionEnvironment connEnv = new ConnectionEnvironment();
		connEnv.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
		connEnv.setRemoteHost(getRemoteHost());
		return connEnv;
	}

	private static String getRemoteHost() {
        WebRequest req = (WebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = (HttpServletRequest) req.getContainerRequest();
        String remoteIp = httpReq.getRemoteHost();

        String localIp = httpReq.getLocalAddr();

        if (remoteIp.equals(localIp)){
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                remoteIp = inetAddress.getHostAddress();
            } catch (UnknownHostException ex) {
                LOGGER.error("Can't get local host: " + ex.getMessage());
            }
        }
        return remoteIp;
    }
}
