/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

/**
 * 
 * @author lazyman
 */
public class MidPointAuthenticationProvider implements AuthenticationProvider {

	private static final Trace TRACE = TraceManager.getTrace(MidPointAuthenticationProvider.class);
	@Autowired
	private UserDetailsService userManagerService;
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
			throw new BadCredentialsException("Invalid username/password");
		}

		PrincipalUser user = null;
		List<GrantedAuthority> grantedAuthorities = null;
		try {
			user = userManagerService.getUser((String) authentication.getPrincipal());
			authenticateUser(user, (String) authentication.getCredentials());
		} catch (BadCredentialsException ex) {
			throw ex;
		} catch (Exception ex) {
			TRACE.error("Can't get user with username '{}'. Unknown error occured, reason {}.", new Object[] {
					authentication.getPrincipal(), ex.getMessage() });
			TRACE.debug("Can't authenticate user '{}'.", new Object[] { authentication.getPrincipal() }, ex);
			throw new AuthenticationServiceException(
					"Currently we are unable to process your request. Kindly try again later.");
		}

		if (user != null) {
			grantedAuthorities = new ArrayList<GrantedAuthority>();
			grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));

			// List<Role> roles = new ArrayList<Role>(0);
			// //user.getAssociatedRoles();
			// for (Role role : roles) {
			// GrantedAuthority authority = new
			// SimpleGrantedAuthority(role.getRoleName());
			// grantedAuthorities.add(authority);
			// }
		} else {
			throw new BadCredentialsException("Invalid username/password");
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

	private void authenticateUser(PrincipalUser user, String password) throws BadCredentialsException {
		if (user == null) {
			throw new BadCredentialsException("Invalid username/password.");
		}

		if (!user.isEnabled()) {
			throw new BadCredentialsException("User is disabled.");
		}

		Credentials credentials = user.getCredentials();
		if (credentials.getFailedLogins() >= maxFailedLogins) {
			long lockedTill = credentials.getLastFailedLoginAttempt() + (60000L * loginTimeout);
			if (lockedTill > System.currentTimeMillis()) {
				long time = (lockedTill - System.currentTimeMillis()) / 60000;
				throw new BadCredentialsException("User is locked, please wait " + time + " minute(s)");
			}
		}

		String pwd = credentials.getPassword();
		if (pwd == null) {
			throw new BadCredentialsException("User doesn't have defined password.");
		}

		String encodedPwd = null;
		if ("hash".equals(credentials.getEncoding())) {
			encodedPwd = Credentials.hashWithSHA2(password);
		} else if ("base64".equals(credentials.getEncoding())) {
			encodedPwd = Base64.encodeBase64String(password.getBytes());
		}

		if (encodedPwd == null || encodedPwd.isEmpty()) {
			throw new BadCredentialsException("Couldn't authenticate user, reason: couldn't encode password.");
		}

		if (encodedPwd.equals(pwd)) {
			if (credentials.getFailedLogins() > 0) {
				credentials.clearFailedLogin();
				userManagerService.updateUser(user);
			}
			return;
		}

		throw new BadCredentialsException("Invalid username/password.");
	}
}
