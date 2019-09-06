/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 * @author Radovan Semancik
 */
public class MidPointAuthenticationProvider implements AuthenticationProvider, MessageSourceAware {

	private static final Trace LOGGER = TraceManager.getTrace(MidPointAuthenticationProvider.class);

	private MessageSourceAccessor messages;

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messages = new MessageSourceAccessor(messageSource);
	}

	@Autowired
	private transient AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		try {
			String enteredUsername = (String) authentication.getPrincipal();
			LOGGER.trace("Authenticating username '{}'", enteredUsername);
	
			ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI);
	
			try {
				Authentication token;
				if (authentication instanceof UsernamePasswordAuthenticationToken) {
					String enteredPassword = (String) authentication.getCredentials();
					token = passwordAuthenticationEvaluator.authenticate(connEnv, new PasswordAuthenticationContext(enteredUsername, enteredPassword));
				} else if (authentication instanceof PreAuthenticatedAuthenticationToken) {
					token = passwordAuthenticationEvaluator.authenticateUserPreAuthenticated(connEnv, enteredUsername);
				} else {
					LOGGER.error("Unsupported authentication {}", authentication);
					throw new AuthenticationServiceException("web.security.provider.unavailable");
				}
		
				MidPointPrincipal principal = (MidPointPrincipal)token.getPrincipal();
		
				LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
						authentication.getClass().getSimpleName(), principal.getAuthorities());
				return token;
				
			} catch (AuthenticationException e) {
				LOGGER.info("Authentication failed for {}: {}", enteredUsername, e.getMessage());
				throw e;
			}
		} catch (RuntimeException | Error e) {
			// Make sure to explicitly log all runtime errors here. Spring security is doing very poor job and does not log this properly.
			LOGGER.error("Authentication (runtime) error: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		if (UsernamePasswordAuthenticationToken.class.equals(authentication)) {
			return true;
		}
		if (PreAuthenticatedAuthenticationToken.class.equals(authentication)) {
			return true;
		}

		return false;
	}
}
