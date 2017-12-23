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
package com.evolveum.midpoint.security.enforcer.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.ItemSecurityDecisions;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
public interface SecurityEnforcer {
	
	/**
	 * Simple access control decision similar to that used by spring security.
	 * It is practically applicable only for simple (non-parametric) cases such as access to GUI pages.
	 * However, it supports authorization hierarchies. Therefore the ordering of elements in
	 * required actions is important.
	 */
	AccessDecision decideAccess(MidPointPrincipal principal, List<String> requiredActions, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	/**
	 * Produces authorization error with proper message and logs it using proper logger.
	 */
	<O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl, AuthorizationPhaseType phase, 
			AuthorizationParameters<O,T> params, OperationResult result)
			throws SecurityViolationException;

	/**
	 * Returns true if the currently logged-in user is authorized for specified action, returns false otherwise.
	 * Does not throw SecurityViolationException.
	 * @param phase check authorization for a specific phase. If null then all phases are checked.
	 */
	<O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	/**
	 * Evaluates authorization: simply returns if the currently logged it user is authorized for a
	 * specified action. If it is not authorized then a  SecurityViolationException is thrown and the
	 * error is recorded in the result.
	 * @param phase check authorization for a specific phase. If null then all phases are checked.
	 */
	<O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver,
			Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

	<O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	/**
	 * TODO
	 * If it returns NoneFilter then no search should be done. The principal is not authorized for this operation at all.
	 * It may return null in case that the original filter was also null.
	 *
	 * If object is null then the method will return a filter that is applicable to look for object.
	 * If object is present then the method will return a filter that is applicable to look for a target.
	 *
	 * The objectType parameter defines the class of the object for which should be the returned filter applicable.
	 * 
	 * @param limitAuthorizationAction only consider authorizations that are not limited with respect to this action.
	 *              If null then all authorizations are considered.
	 */
	<T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, AuthorizationPhaseType phase,
			Class<T> searchResultType, PrismObject<O> object, ObjectFilter origFilter, 
			String limitAuthorizationAction, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	/**
	 * @param includeSpecial include special authorizations such as "self"
	 */
	<T extends ObjectType, O extends ObjectType> boolean canSearch(String operationUrl, AuthorizationPhaseType phase,
			Class<T> searchResultType, PrismObject<O> object, boolean includeSpecial, ObjectFilter filter, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	/**
	 * Returns decisions for individual items for "assign" authorization. This is usually applicable to assignment parameters.
	 */
	<O extends ObjectType, R extends AbstractRoleType> ItemSecurityDecisions getAllowedRequestAssignmentItems(MidPointPrincipal midPointPrincipal,
			String operationUrl, PrismObject<O> object, PrismObject<R> target, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;
	
	MidPointPrincipal createDonorPrincipal(MidPointPrincipal attorneyPrincipal, String attorneyAuthorizationAction, PrismObject<UserType> donor, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	<O extends ObjectType> AccessDecision determineSubitemDecision(ObjectSecurityConstraints securityConstraints,
			ObjectDelta<O> delta, String operationUrl, AuthorizationPhaseType phase, ItemPath subitemRootPath);
}
