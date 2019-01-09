/**
 * Copyright (c) 2017-2018 Evolveum
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
package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.xml.ns._public.common.common_4.UserType;

/**
 * Manager of security context. Used for storing authentication into
 * the security context, set up security context for task ownership, etc. 
 * 
 * This is a part of low-level security functions. Those are security functions that
 * deal with the basic concepts of authentication, task ownership,
 * security context and so on.
 */
public interface SecurityContextManager {
		
	boolean isAuthenticated();

	/**
	 * Returns principal representing the currently logged-in user.
	 * Assumes that the user is logged-in. Otherwise an exception is thrown.
	 */
	MidPointPrincipal getPrincipal() throws SecurityViolationException;

    void setupPreAuthenticatedSecurityContext(Authentication authentication);
    
    void setupPreAuthenticatedSecurityContext(MidPointPrincipal principal);

	void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;
	
	<T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	/**
	 * Convenience method to deal with producers that can throw CommonException.
	 */
	default <T> T runAsChecked(CheckedProducer<T> producer, PrismObject<UserType> user) throws CommonException {
		return MiscUtil.runChecked((p) -> runAs(p, user), producer);
	}

	<T> T runPrivileged(Producer<T> producer);

	/**
	 * Convenience method to deal with producers that can throw CommonException.
	 */
	default <T> T runPrivilegedChecked(CheckedProducer<T> producer) throws CommonException {
		return MiscUtil.runChecked(this::runPrivileged, producer);
	}

	// runPrivileged method is in SecurityEnforcer. It needs to be there because it works with authorizations.

	MidPointPrincipalManager getUserProfileService();

	void setUserProfileService(MidPointPrincipalManager userProfileService);
	
	/**
	 * Store connection information for later use within current thread.
	 */
	void storeConnectionInformation(@Nullable HttpConnectionInformation value);

	/**
	 * Returns stored connection information.
	 * Should be used for non-HTTP threads that have no access to stored Request object (see {@link SecurityUtil#getCurrentConnectionInformation()}).
	 */
	@Nullable
	HttpConnectionInformation getStoredConnectionInformation();
}
