/**
 * Copyright (c) 2014-2017 Evolveum
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

import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.AccessDecisionManager;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.security.core.Authentication;

/**
 * @author Radovan Semancik
 *
 */
public interface SecurityEnforcer extends AccessDecisionManager {
	
	UserProfileService getUserProfileService();

	void setUserProfileService(UserProfileService userProfileService);

    void setupPreAuthenticatedSecurityContext(Authentication authentication);

	void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) throws SchemaException;
	
	boolean isAuthenticated();
	
	/**
	 * Returns principal representing the currently logged-in user.
	 * Assumes that the user is logged-in. Otherwise an exception is thrown.
	 */
	MidPointPrincipal getPrincipal() throws SecurityViolationException;

	/**
	 * Produces authorization error with proper message and logs it using proper logger.
	 */
	<O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl, AuthorizationPhaseType phase, PrismObject<O> object,
			ObjectDelta<O> delta, PrismObject<T> target, OperationResult result)
			throws SecurityViolationException;
	
	/**
	 * Returns true if the currently logged-in user is authorized for specified action, returns false otherwise.
	 * Does not throw SecurityViolationException.
	 * @param phase check authorization for a specific phase. If null then all phases are checked.
	 */
	<O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver) throws SchemaException;
	
	/**
	 * Evaluates authorization: simply returns if the currently logged it user is authorized for a
	 * specified action. If it is not authorized then a  SecurityViolationException is thrown and the
	 * error is recorded in the result.
	 * @param phase check authorization for a specific phase. If null then all phases are checked.
	 */
	<O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver, 
			OperationResult result) throws SecurityViolationException, SchemaException;	
	
	<O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OwnerResolver ownerResolver) throws SchemaException;
	
	/**
	 * TODO
	 * If it returns NoneFilter then no search should be done. The principal is not authorized for this operation at all.
	 * It may return null in case that the original filter was also null.
	 * 
	 * If object is null then the method will return a filter that is applicable to look for object.
	 * If object is present then the method will return a filter that is applicable to look for a target.
	 * 
	 * The objectType parameter defines the class of the object for which should be the returned filter applicable.
	 */
	<T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, AuthorizationPhaseType phase,
			Class<T> objectType, PrismObject<O> object, ObjectFilter origFilter) throws SchemaException;
	
	<T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException ;
	
	<T> T runPrivileged(Producer<T> producer);

	/**
	 * Returns decisions for individual items for "assign" authorization. This is usually applicable to assignment parameters.
	 */
	<O extends ObjectType, R extends AbstractRoleType> ItemSecurityDecisions getAllowedRequestAssignmentItems( MidPointPrincipal midPointPrincipal, PrismObject<O> object, PrismObject<R> target, OwnerResolver ownerResolver) throws SchemaException;

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
