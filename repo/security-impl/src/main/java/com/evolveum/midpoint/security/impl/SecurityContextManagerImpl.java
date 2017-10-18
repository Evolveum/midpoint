/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.security.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@Component("securityContextManager")
public class SecurityContextManagerImpl implements SecurityContextManager {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(SecurityContextManagerImpl.class);
	
	private UserProfileService userProfileService = null;
	private ThreadLocal<HttpConnectionInformation> connectionInformationThreadLocal = new ThreadLocal<>();

	@Override
	public UserProfileService getUserProfileService() {
		return userProfileService;
	}

	@Override
	public void setUserProfileService(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}
	
	@Override
	public MidPointPrincipal getPrincipal() throws SecurityViolationException {
		return SecurityUtil.getPrincipal();
	}

    @Override
	public boolean isAuthenticated() {
		return SecurityUtil.isAuthenticated();
	}

    
	@Override
    public void setupPreAuthenticatedSecurityContext(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }

	@Override
	public void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) throws SchemaException {
		MidPointPrincipal principal;
		if (userProfileService == null) {
			LOGGER.warn("No user profile service set up in SecurityEnforcer. "
					+ "This is OK in low-level tests but it is a serious problem in running system");
			principal = new MidPointPrincipal(user.asObjectable());
		} else {
			principal = userProfileService.getPrincipal(user);
		}
		Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
        setupPreAuthenticatedSecurityContext(authentication);
	}
	
	@Override
	public <T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException {

		LOGGER.debug("Running {} as {}", producer, user);
		Authentication origAuthentication = SecurityContextHolder.getContext().getAuthentication();
		setupPreAuthenticatedSecurityContext(user);
		try {
			return producer.run();
		} finally {
			SecurityContextHolder.getContext().setAuthentication(origAuthentication);
			LOGGER.debug("Finished running {} as {}", producer, user);
		}
	}
	
	
	@Override
	public <T> T runPrivileged(Producer<T> producer) {
		LOGGER.debug("Running {} as privileged", producer);
		Authentication origAuthentication = SecurityContextHolder.getContext().getAuthentication();
		LOGGER.trace("ORIG auth {}", origAuthentication);

		// Try to reuse the original identity as much as possible. All we need to is add AUTZ_ALL
		// to the list of authorities
		Authorization privilegedAuthorization = createPrivilegedAuthorization();
		Object newPrincipal = null;

		if (origAuthentication != null) {
			Object origPrincipal = origAuthentication.getPrincipal();
			if (origAuthentication instanceof AnonymousAuthenticationToken) {
				newPrincipal = origPrincipal;
			} else {
				LOGGER.trace("ORIG principal {} ({})", origPrincipal, origPrincipal != null ? origPrincipal.getClass() : null);
				if (origPrincipal != null) {
					if (origPrincipal instanceof MidPointPrincipal) {
						MidPointPrincipal newMidPointPrincipal = ((MidPointPrincipal)origPrincipal).clone();
						newMidPointPrincipal.getAuthorities().add(privilegedAuthorization);
						newPrincipal = newMidPointPrincipal;
					}
				}
			}

			Collection<GrantedAuthority> newAuthorities = new ArrayList<>();
			newAuthorities.addAll(origAuthentication.getAuthorities());
			newAuthorities.add(privilegedAuthorization);
			PreAuthenticatedAuthenticationToken newAuthorization = new PreAuthenticatedAuthenticationToken(newPrincipal, null, newAuthorities);

			LOGGER.trace("NEW auth {}", newAuthorization);
			SecurityContextHolder.getContext().setAuthentication(newAuthorization);
		} else {
			LOGGER.debug("No original authentication, do NOT setting any privileged security context");
		}


		try {
			return producer.run();
		} finally {
			SecurityContextHolder.getContext().setAuthentication(origAuthentication);
			LOGGER.debug("Finished running {} as privileged", producer);
			LOGGER.trace("Security context after privileged operation: {}", SecurityContextHolder.getContext());
		}

	}
	
	private Authorization createPrivilegedAuthorization() {
		AuthorizationType authorizationType = new AuthorizationType();
		authorizationType.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
		return new Authorization(authorizationType);
	}
	
	@Override
	public void storeConnectionInformation(HttpConnectionInformation value) {
		connectionInformationThreadLocal.set(value);
	}

	@Override
	public HttpConnectionInformation getStoredConnectionInformation() {
		return connectionInformationThreadLocal.get();
	}

	
}
