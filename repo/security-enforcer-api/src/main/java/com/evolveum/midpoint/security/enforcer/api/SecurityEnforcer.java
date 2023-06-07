/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import java.util.List;

import com.evolveum.midpoint.prism.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.context.SecurityContext;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Radovan Semancik
 */
public interface SecurityEnforcer {

    /**
     * Returns `true` if the currently logged-in user is authorized for specified action (represented by `operationUrl`),
     * returns `false` otherwise.
     *
     * Does not throw {@link SecurityViolationException}.
     *
     * @param phase check authorization for a specific phase. If null then all phases are checked.
     */
    <O extends ObjectType, T extends ObjectType> boolean isAuthorized(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<O, T> params,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Simple access control decision similar to that used by spring security.
     * It is usable for parametric cases; for example, REST login using proxy user ("switch-to-principal").
     *
     * Checks multiple actions (operation URLs).
     *
     * - If any of the operations fail the authorization (the decision is {@link AccessDecision#DENY}, the overall outcome is
     * also {@link AccessDecision#DENY}.
     * - If any of the operations results in {@link AccessDecision#ALLOW}, the result is {@link AccessDecision#ALLOW}.
     * - Otherwise (i.e., if all operations are {@link AccessDecision#DEFAULT}), the result is {@link AccessDecision#DEFAULT}.
     */
    <O extends ObjectType, T extends ObjectType> @NotNull AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull List<String> operationUrls,
            @NotNull AuthorizationParameters<O,T> params,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Simplified version of {@link #decideAccess(MidPointPrincipal, List, AuthorizationParameters, Task, OperationResult)}.
     * It is practically applicable only for simple (non-parametric) cases such as access to GUI pages.
     */
    default @NotNull AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull List<String> operationUrls,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return decideAccess(principal, operationUrls, AuthorizationParameters.EMPTY, task, result);
    }

    /**
     * Evaluates authorization: simply returns if the currently logged it user is authorized for a
     * specified action. If it is not authorized then a {@link SecurityViolationException} is thrown and the
     * error is recorded in the result.
     *
     * @param phase check authorization for a specific phase. If null then all phases are checked.
     *
     * @see #isAuthorized(String, AuthorizationPhaseType, AuthorizationParameters, OwnerResolver, Task, OperationResult)
     */
    default <O extends ObjectType, T extends ObjectType> void authorize(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<O, T> params,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Task task,
            @NotNull OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        if (!isAuthorized(operationUrl, phase, params, ownerResolver, task, result)) {
            failAuthorization(operationUrl, phase, params, result);
        }
    }

    /**
     * Convenience variant of {@link #authorize(String, AuthorizationPhaseType, AuthorizationParameters, OwnerResolver, Task,
     * OperationResult)} that is to be used when there is no object, target, nor other parameters.
     */
    default void authorize(
            @NotNull String operationUrl,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        authorize(operationUrl, null, AuthorizationParameters.EMPTY, null, task, result);
    }

    @Experimental
    default void authorizeAll(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        authorize(AuthorizationConstants.AUTZ_ALL_URL, task, result);
    }

    /**
     * Produces authorization error with proper message and logs it using proper logger.
     */
    @Contract("_, _, _, _ -> fail")
    <O extends ObjectType, T extends ObjectType> void failAuthorization(
            String operationUrl, AuthorizationPhaseType phase, AuthorizationParameters<O,T> params, OperationResult result)
            throws SecurityViolationException;

    /**
     * Obtains currently logged-in principal, if it's of {@link MidPointPrincipal} type.
     *
     * @see SecurityContext#getAuthentication()
     */
    @Nullable MidPointPrincipal getMidPointPrincipal();

    /**
     * Compiles relevant security constraints ({@link ObjectSecurityConstraints}) for a current principal against given `object`.
     */
    <O extends ObjectType> @NotNull ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * TODO
     *
     * Note that the `value` is currently always {@link PrismObjectValue}. In the future we may lift this restriction,
     * and allow arbitrary {@link PrismValue} instances here. But this is simpler with respect to application of authorizations
     * to these values.
     */
    @NotNull PrismEntityOpConstraints.ForValueContent compileOperationConstraints(
            @NotNull PrismObjectValue<?> value,
            @Nullable AuthorizationPhaseType phase,
            @Nullable OwnerResolver ownerResolver,
            @NotNull String[] actionUrls,
            @NotNull CompileConstraintsOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Returns a filter that covers all the objects for which the principal is authorized to apply `operationUrls`.
     *
     * The `searchResultType` parameter defines the class of the object for which should be the returned filter applicable.
     *
     * When the search is considered, if this method returns {@link NoneFilter} then no search should be done.
     * The principal is not authorized for that operation at all.
     *
     * It may return null in case that the original filter was also null.
     *
     * @param limitAuthorizationAction only consider authorizations that are not limited with respect to this action.
     * If null then all authorizations are considered.
     */
    <T> @Nullable ObjectFilter preProcessObjectFilter(
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            @Nullable ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /** Will be removed. */
    <T extends ObjectType> boolean canSearch(
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            boolean includeSpecial,
            ObjectFilter filter,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Similar to {@link #preProcessObjectFilter(String[], AuthorizationPhaseType, Class, ObjectFilter, String, List, Task,
     * OperationResult)} but deals with the target-related authorization statements, not object-related ones.
     *
     * The `object` is the object we are looking for targets for.
     *
     * Typical use: it can return a filter of all assignable roles for a principal. In that case `#assign` authorization is used,
     * and object is the user which should hold the assignment.
     */
    <T extends ObjectType, O extends ObjectType, F> F computeTargetSecurityFilter(
            MidPointPrincipal principal,
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            @NotNull PrismObject<O> object,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Returns decisions for individual items for "assign" authorization. This is usually applicable to assignment parameters.
     */
    <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(
            MidPointPrincipal midPointPrincipal,
            String operationUrl,
            PrismObject<O> object,
            PrismObject<R> target,
            OwnerResolver ownerResolver,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    <F extends FocusType> MidPointPrincipal createDonorPrincipal(
            MidPointPrincipal attorneyPrincipal,
            String attorneyAuthorizationAction,
            PrismObject<F> donor,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /** TODO describe */
    <O extends ObjectType> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints,
            @NotNull ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            String operationUrl,
            AuthorizationPhaseType phase,
            ItemPath itemPath);

    /** TODO describe */
    <C extends Containerable> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints,
            PrismContainerValue<C> containerValue,
            String operationUrl,
            AuthorizationPhaseType phase,
            @Nullable ItemPath itemPath,
            PlusMinusZero plusMinusZero,
            String decisionContextDesc);
}
