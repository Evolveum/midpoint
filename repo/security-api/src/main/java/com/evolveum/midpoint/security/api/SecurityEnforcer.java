/**
 * Copyright (c) 2014 Evolveum
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

import org.springframework.security.access.AccessDecisionManager;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author Radovan Semancik
 *
 */
public interface SecurityEnforcer extends AccessDecisionManager {
	
	UserProfileService getUserProfileService();

	void setUserProfileService(UserProfileService userProfileService);

	void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user);
	
	/**
	 * Returns principal representing the currently logged-in user.
	 * Assumes that the user is logged-in. Otherwise an exception is thrown.
	 */
	MidPointPrincipal getPrincipal() throws SecurityViolationException;

	/**
	 * Returns true if the currently logged-in user is authorized for specified action, returns false otherwise.
	 * Does not throw SecurityViolationException.
	 */
	<O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, 
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target) throws SchemaException;
	
	/**
	 * Evaluates authorization: simply returns if the currently logged it user is authorized for a
	 * specified action. If it is not authorized then a  SecurityViolationException is thrown and the
	 * error is recorded in the result.
	 */
	<O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, 
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, 
			OperationResult result) throws SecurityViolationException, SchemaException;	
	
}
