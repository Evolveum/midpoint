/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.common.security.UserProfileService;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.XMLGregorianCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class MidPointAuthenticationProvider implements AuthenticationProvider {

	private static final Trace LOGGER = TraceManager.getTrace(MidPointAuthenticationProvider.class);
	@Autowired(required = true)
	private transient UserProfileService userManagerService;
	@Autowired(required = true)
	private transient Protector protector;
	private int loginTimeout;
	private int maxFailedLogins;

	public void setLoginTimeout(int loginTimeout) {
		if (loginTimeout < 0) {
			loginTimeout = 0;
		}
		this.loginTimeout = loginTimeout;
	}

	public void setMaxFailedLogins(int maxFailedLogins) {
		if (maxFailedLogins < 0) {
			maxFailedLogins = 0;
		}
		this.maxFailedLogins = maxFailedLogins;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if (StringUtils.isBlank((String) authentication.getPrincipal())
				|| StringUtils.isBlank((String) authentication.getCredentials())) {
			throw new BadCredentialsException("web.security.provider.invalid");
		}
		// throw new BadCredentialsException("web.security.provider.illegal");
		MidPointPrincipal user = null;
		List<GrantedAuthority> grantedAuthorities = null;
		try {
			user = userManagerService.getPrincipal((String) authentication.getPrincipal());
			authenticateUser(user, (String) authentication.getCredentials());
		} catch (BadCredentialsException ex) {
			if (user != null && user.getUser() != null && user.getUser().getCredentials() != null) {
				UserType userType = user.getUser();
				
				CredentialsType credentials = userType.getCredentials();
				PasswordType passwordType = credentials.getPassword();

				int failedLogins = passwordType.getFailedLogins() != null ? passwordType.getFailedLogins() : 0;
				passwordType.setFailedLogins(failedLogins++);
				XMLGregorianCalendar systemTime = MiscUtil.asXMLGregorianCalendar(new Date(System
						.currentTimeMillis()));
				LoginEventType event = new LoginEventType();
				event.setTimestamp(systemTime);
				event.setFrom(getRemoteHost());
				passwordType.setLastFailedLogin(event);
				userManagerService.updateUser(user);
			}

			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Can't get user with username '{}'. Unknown error occured, reason {}.",
					new Object[] { authentication.getPrincipal(), ex.getMessage() });
			LOGGER.debug("Can't authenticate user '{}'.", new Object[] { authentication.getPrincipal() }, ex);
			throw new AuthenticationServiceException("web.security.provider.unavailable");
		}

		if (user != null) {
			grantedAuthorities = new ArrayList<GrantedAuthority>();
			UserType userType = user.getUser();
			CredentialsType credentialsType = userType.getCredentials();

			if (credentialsType == null) {
				credentialsType = new CredentialsType();
				userType.setCredentials(credentialsType);
			}

			boolean isAdminGuiAccess = credentialsType.isAllowedIdmAdminGuiAccess() != null ? credentialsType
					.isAllowedIdmAdminGuiAccess() : false;
			if (isAdminGuiAccess) {
				grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
			} else {
				grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
			}

			/*
			 * List<Role> roles = new ArrayList<Role>(0);
			 * //user.getAssociatedRoles(); for (Role role : roles) {
			 * GrantedAuthority authority = new
			 * SimpleGrantedAuthority(role.getRoleName());
			 * grantedAuthorities.add(authority); }
			 */
		} else {
			throw new BadCredentialsException("web.security.provider.invalid");
		}
		return new UsernamePasswordAuthenticationToken(user, authentication.getCredentials(),
				grantedAuthorities);
	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		if (UsernamePasswordAuthenticationToken.class.equals(authentication)) {
			return true;
		}

		return false;
	}

	private void authenticateUser(MidPointPrincipal user, String password) throws BadCredentialsException {		
		if (user == null || user.getUser() == null || user.getUser().getCredentials() == null) {
			throw new BadCredentialsException("web.security.provider.invalid");
		}

		if (!user.isEnabled()) {
			throw new BadCredentialsException("web.security.provider.disabled");
		}
		
		UserType userType = user.getUser();
		CredentialsType credentials = userType.getCredentials();

		PasswordType passwordType = credentials.getPassword();
		int failedLogins = passwordType.getFailedLogins() != null ? passwordType.getFailedLogins() : 0;
		if (maxFailedLogins > 0 && failedLogins >= maxFailedLogins) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(MiscUtil.asDate(passwordType.getLastFailedLogin().getTimestamp()).getTime());
			calendar.add(Calendar.MINUTE, loginTimeout);
			long lockedTill = calendar.getTimeInMillis();

			if (lockedTill > System.currentTimeMillis()) {
				throw new BadCredentialsException("web.security.provider.locked");
			}
		}

		ProtectedStringType protectedString = passwordType.getValue();
		if (protectedString == null) {
			throw new BadCredentialsException("web.security.provider.password.bad");
		}

		if (StringUtils.isEmpty(password)) {
			throw new BadCredentialsException("web.security.provider.password.encoding");
		}
		
			Collection<Authorization> authorizations = user.getAuthorities();
    		if (authorizations == null || authorizations.isEmpty()){
    			throw new BadCredentialsException("web.security.provider.access.denied");
    		}
    		
			for (Authorization auth : authorizations){
    			if (auth.getAction() == null || auth.getAction().isEmpty()){
    				throw new BadCredentialsException("web.security.provider.access.denied");
    			}
			}
    	

		try {
			String decoded;
			if (protectedString.getEncryptedData() != null) {
				decoded = protector.decryptString(protectedString);
			} else {
				LOGGER.warn("Authenticating user based on clear value. Please check objects, "
						+ "this should not happen. Protected string should be encrypted.");
				decoded = protectedString.getClearValue();
			}
			if (password.equals(decoded)) {
				if (failedLogins > 0) {
					passwordType.setFailedLogins(0);
				}
				XMLGregorianCalendar systemTime = MiscUtil.asXMLGregorianCalendar(new Date(System
						.currentTimeMillis()));
				LoginEventType event = new LoginEventType();
				event.setTimestamp(systemTime);
				event.setFrom(getRemoteHost());

				passwordType.setPreviousSuccessfulLogin(passwordType.getLastSuccessfulLogin());
				passwordType.setLastSuccessfulLogin(event);

				userManagerService.updateUser(user);
				return;
			}
		} catch (EncryptionException ex) {
			throw new AuthenticationServiceException("web.security.provider.unavailable", ex);
		}

		throw new BadCredentialsException("web.security.provider.invalid");
	}

	public static String getRemoteHost() {
		WebRequest req = (WebRequest) RequestCycle.get().getRequest();
		HttpServletRequest httpReq = (HttpServletRequest) req.getContainerRequest();
		return httpReq.getRemoteHost();
	}
}
