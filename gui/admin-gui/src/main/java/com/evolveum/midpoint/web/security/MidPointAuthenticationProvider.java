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

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.XMLGregorianCalendar;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Date;
import java.util.*;

/**
 * @author lazyman
 * @author Radovan Semancik
 */
public class MidPointAuthenticationProvider implements AuthenticationProvider {

	private static final Trace LOGGER = TraceManager.getTrace(MidPointAuthenticationProvider.class);
	
	@Autowired(required = true)
	private transient UserProfileService userProfileService;
	
	@Autowired(required = true)
	private transient AuthenticationEvaluator authenticationEvaluator;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if (StringUtils.isBlank((String) authentication.getPrincipal())) {
			throw new BadCredentialsException("web.security.provider.invalid");
		}
		
		MidPointPrincipal principal = null;
		try {
			principal = userProfileService.getPrincipal((String) authentication.getPrincipal());
		} catch (ObjectNotFoundException ex) {
			LOGGER.debug("Authentication of user with username '{}' failed: not found: {}", ex.getMessage(), ex);
			throw new BadCredentialsException("web.security.provider.access.denied");
		} catch (Exception ex) {
			LOGGER.error("Can't get user with username '{}'. Unknown error occured, reason {}.",
					new Object[] { authentication.getPrincipal(), ex.getMessage(), ex });
			throw new AuthenticationServiceException("web.security.provider.unavailable");
		}
		
		Authentication token = null;
		try {
			token = authenticateUser(principal, authentication);
		} catch (BadCredentialsException ex) {
			LOGGER.debug("Authentication of user with username '{}' failed: bad credentials: {}", ex.getMessage(), ex);
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Can't authenticate user '{}': {}", new Object[] { authentication.getPrincipal() , ex.getMessage(), ex });
			throw new AuthenticationServiceException("web.security.provider.unavailable");
		}
		
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

	private Authentication authenticateUser(MidPointPrincipal principal, Authentication authentication) {
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		if (authentication instanceof UsernamePasswordAuthenticationToken) {
			return authenticationEvaluator.authenticateUserPassword(principal, connEnv, (String) authentication.getCredentials());
		} else if (authentication instanceof PreAuthenticatedAuthenticationToken) {
			PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal, null, 
					principal.getAuthorities());
			return token;
		} else {
			throw new AuthenticationServiceException("web.security.provider.unavailable");
		}
		
	}
	
	private ConnectionEnvironment createConnectionEnvironment() {
		ConnectionEnvironment connEnv = new ConnectionEnvironment();
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
