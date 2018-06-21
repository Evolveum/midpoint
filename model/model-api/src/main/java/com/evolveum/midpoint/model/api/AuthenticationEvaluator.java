/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public interface AuthenticationEvaluator<T extends AbstractAuthenticationContext> {

	UsernamePasswordAuthenticationToken authenticate(ConnectionEnvironment connEnv, T authnCtx)
			throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
			CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;

	UserType checkCredentials(ConnectionEnvironment connEnv, T authnCtx)
			throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
			CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;

	PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(ConnectionEnvironment connEnv, String enteredUsername);

}
