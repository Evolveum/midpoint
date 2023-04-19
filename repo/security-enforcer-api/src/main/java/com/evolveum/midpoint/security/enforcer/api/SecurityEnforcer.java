/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import java.util.List;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Radovan Semancik
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
     * Simple access control decision similar to that used by spring security.
     * It is practically applicable for REST authorization with user from 'switch-to-principal' in parameters.
     * However, it supports authorization hierarchies. Therefore the ordering of elements in
     * required actions is important.
     */
    <O extends ObjectType, T extends ObjectType> AccessDecision decideAccess(MidPointPrincipal principal, List<String> requiredActions, AuthorizationParameters<O,T> params, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;


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
            Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Convenience variant of {@link #authorize(String, AuthorizationPhaseType, AuthorizationParameters, OwnerResolver, Task,
     * OperationResult)} that is to be used when there is no object, target, nor other parameters.
     */
    default void authorize(String operationUrl, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        authorize(operationUrl, null, AuthorizationParameters.EMPTY, null, task, result);
    }

    @Experimental
    default void authorizeAll(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        authorize(AuthorizationConstants.AUTZ_ALL_URL, null, AuthorizationParameters.EMPTY, null, task, result);
    }

    MidPointPrincipal getMidPointPrincipal();

    <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(
            PrismObject<O> object, OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Returns a filter that applies to all the objects/targets for which the principal is authorized.
     *
     * E.g. it can return a filter of all assignable roles for a principal. In that case #assign authorization is used,
     * and object is the user which should hold the assignment.
     *
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
    <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String[] operationUrls, AuthorizationPhaseType phase,
            Class<T> searchResultType, PrismObject<O> object, ObjectFilter origFilter,
            String limitAuthorizationAction, List<OrderConstraintsType> paramOrderConstraints, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * Question: does object make any sense here? E.g. when searching role members, the role OID should be determined from the query.
     *
     * @param includeSpecial include special authorizations such as "self"
     */
    <T extends ObjectType, O extends ObjectType> boolean canSearch(String[] operationUrls, AuthorizationPhaseType phase,
            Class<T> searchResultType, PrismObject<O> object, boolean includeSpecial, ObjectFilter filter, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    <T extends ObjectType, O extends ObjectType, F> F computeSecurityFilter(MidPointPrincipal principal,
                                                                            String[] operationUrls,
                                                                            AuthorizationPhaseType phase,
                                                                            Class<T> searchResultType,
                                                                            PrismObject<O> object,
                                                                            ObjectFilter origFilter,
                                                                            String limitAuthorizationAction,
                                                                            List<OrderConstraintsType> paramOrderConstraints,
                                                                            FilterGizmo<F> gizmo,
                                                                            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;
    /**
     * Returns decisions for individual items for "assign" authorization. This is usually applicable to assignment parameters.
     */
    <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(MidPointPrincipal midPointPrincipal,
            String operationUrl, PrismObject<O> object, PrismObject<R> target, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    <F extends FocusType> MidPointPrincipal createDonorPrincipal(MidPointPrincipal attorneyPrincipal, String attorneyAuthorizationAction, PrismObject<F> donor, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    <O extends ObjectType> AccessDecision determineSubitemDecision(ObjectSecurityConstraints securityConstraints,
            ObjectDelta<O> delta, PrismObject<O> currentObject, String operationUrl, AuthorizationPhaseType phase, ItemPath subitemRootPath);

    <C extends Containerable> AccessDecision determineSubitemDecision(
            ObjectSecurityConstraints securityConstraints, PrismContainerValue<C> containerValue, String operationUrl,
            AuthorizationPhaseType phase, ItemPath subitemRootPath, PlusMinusZero plusMinusZero, String decisionContextDesc);
}
