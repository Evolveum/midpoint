/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.security.api.Authorization;

import com.evolveum.midpoint.security.api.MidPointPrincipalManager;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.context.SecurityContext;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Answers questions about authorizations, for example whether a user is authorized to do something.
 *
 * @author Radovan Semancik
 */
public interface SecurityEnforcer {

    /**
     * General access-decision method. Determines whether given `operationUrl` (in specified `phase`, with `params`)
     * is allowed for the given `principal`.
     */
    @NotNull AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AbstractAuthorizationParameters params,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * Returns `true` if the currently logged-in user is authorized for specified action (represented by `operationUrl`),
     * returns `false` otherwise.
     *
     * Does not throw {@link SecurityViolationException} if it is not.
     * (But may throw that exception e.g. if the authority cannot be determined because of some deeper security error.)
     *
     * @param phase check authorization for a specific phase. If null then all phases are checked.
     */
    default boolean isAuthorized(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AbstractAuthorizationParameters params,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        var decision = decideAccess(
                getMidPointPrincipal(), operationUrl, phase, params, options, task, result);
        return decision == AccessDecision.ALLOW;
    }

    default boolean isAuthorizedAll(@NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return isAuthorized(
                AuthorizationConstants.AUTZ_ALL_URL,
                null,
                AuthorizationParameters.EMPTY,
                Options.create(),
                task,
                result);
    }

    /**
     * Checks if the currently logged-in user is authorized for any of the specified actions.
     *
     * BEWARE: Only for preliminary/coarse-grained decisions! Use only when followed by more precise authorization checks.
     *
     * For example, it ignores any object or target qualification, DENY authorizations, and so on.
     */
    default boolean hasAnyAllowAuthorization(
            @NotNull List<String> actions, @Nullable AuthorizationPhaseType phase) {
        for (Authorization authorization : SecurityEnforcerUtil.getAuthorizations(getMidPointPrincipal())) {
            if (authorization.isAllow()
                    && authorization.matchesPhase(phase)
                    && authorization.matchesAnyAction(actions)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the currently logged-in user has a denied authorization for the specified action.
     *
     * BEWARE: Only for preliminary/coarse-grained decisions! Use only when followed by more precise authorization checks.
     *
     * For example, it ignores any object or target qualification, phase and so on.
     */
    default boolean isAuthorizationDenied(@NotNull String action) {
        for (Authorization authorization : SecurityEnforcerUtil.getAuthorizations(getMidPointPrincipal())) {
            if (authorization.isDeny()
                    && authorization.matchesAnyAction(List.of(action))) {
                return true;
            }
        }
        return false;
    }

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
    default <O extends ObjectType, T extends ObjectType> @NotNull AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull Collection<String> operationUrls,
            @NotNull AuthorizationParameters<O,T> params,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        AccessDecision finalDecision = AccessDecision.DEFAULT;
        for (String operationUrl : operationUrls) {
            AccessDecision decision = decideAccess(
                    principal, operationUrl, null, params, Options.create(), task, result);
            switch (decision) {
                case DENY -> { return AccessDecision.DENY; }
                case ALLOW -> finalDecision = AccessDecision.ALLOW;
                case DEFAULT -> { /* no change of the final decision */ }
            }
        }
        return finalDecision;
    }

    /**
     * Simplified version of {@link #decideAccess(MidPointPrincipal, Collection, AuthorizationParameters, Task, OperationResult)}.
     * It is practically applicable only for simple (non-parametric) cases such as access to GUI pages.
     */
    default @NotNull AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull Collection<String> operationUrls,
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
     * @see #isAuthorized(String, AuthorizationPhaseType, AbstractAuthorizationParameters, Options, Task, OperationResult)
     */
    default void authorize(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AbstractAuthorizationParameters params,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        if (!isAuthorized(operationUrl, phase, params, options, task, result)) {
            failAuthorization(operationUrl, phase, params, result);
        }
    }

    /**
     * Convenience variant of {@link #authorize(String, AuthorizationPhaseType, AbstractAuthorizationParameters, Options,
     * Task, OperationResult)} with the default options.
     */
    default void authorize(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AbstractAuthorizationParameters params,
            @NotNull Task task,
            @NotNull OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorize(operationUrl, phase, params, Options.create(), task, result);
    }

    /**
     * Convenience variant of {@link #authorize(String, AuthorizationPhaseType, AbstractAuthorizationParameters,
     * Task, OperationResult)} that is to be used when there is no object, target, nor other parameters.
     */
    default void authorize(
            @NotNull String operationUrl,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        authorize(operationUrl, null, AuthorizationParameters.EMPTY, task, result);
    }

    default void authorizeAll(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        authorize(AuthorizationConstants.AUTZ_ALL_URL, task, result);
    }

    /**
     * Produces authorization error with proper message and logs it using proper logger.
     */
    @Contract("_, _, _, _ -> fail")
    void failAuthorization(
            String operationUrl, AuthorizationPhaseType phase, AbstractAuthorizationParameters params, OperationResult result)
            throws SecurityViolationException;

    /**
     * Obtains currently logged-in principal, if it's of {@link MidPointPrincipal} type.
     *
     * @see SecurityContext#getAuthentication()
     */
    @Nullable MidPointPrincipal getMidPointPrincipal();

    /**
     * Compiles relevant security constraints ({@link ObjectSecurityConstraints}) for a current principal against given `object`.
     *
     * Returns a map-like object (indexed by operation and phase) covering all operations defined for the object.
     *
     * @see ObjectSecurityConstraints
     */
    <O extends ObjectType> @NotNull ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object,
            boolean fullInformationAvailable,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Compiles the security constraints related to given `actionUrls` and `phase` for a given principal against the `object`.
     *
     * So, unlike {@link #compileSecurityConstraints(PrismObject, boolean, Options, Task, OperationResult)}, it is focused
     * on a given operation, usually `#get`, `#search`, or `#read`.
     *
     * Note that the `value` is currently always {@link PrismObjectValue}. In the future we may lift this restriction,
     * and allow arbitrary {@link PrismValue} instances here. But this is simpler with respect to application of authorizations
     * to these values.
     */
    PrismEntityOpConstraints.ForValueContent compileOperationConstraints(
            @Nullable MidPointPrincipal principal,
            @NotNull PrismObjectValue<?> value,
            @Nullable AuthorizationPhaseType phase,
            @NotNull String[] actionUrls,
            @NotNull Options enforcerOptions,
            @NotNull CompileConstraintsOptions compileConstraintsOptions,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Returns a filter that covers all the objects for which the principal is authorized to apply `operationUrls`.
     *
     * The `filterType` parameter defines the class of the object for which should be the returned filter applicable.
     *
     * When the search is considered, if this method returns {@link NoneFilter} then no search should be done.
     * The principal is not authorized for that operation at all.
     *
     * It may return null in case that the original filter was also null.
     *
     * @param limitAuthorizationAction only consider authorizations that are not limited with respect to this action.
     * If `null` then all authorizations are considered.
     */
    <T> @Nullable ObjectFilter preProcessObjectFilter(
            @Nullable MidPointPrincipal principal,
            @NotNull String[] operationUrls,
            @Nullable AuthorizationPhaseType phase,
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter origFilter,
            @Nullable String limitAuthorizationAction,
            @NotNull List<OrderConstraintsType> paramOrderConstraints,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Similar to {@link #preProcessObjectFilter(MidPointPrincipal, String[], AuthorizationPhaseType, Class, ObjectFilter,
     * String, List, Options, Task, OperationResult)} but deals with the target-related authorization statements,
     * not object-related ones.
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
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /** TODO describe */
    <F extends FocusType> MidPointPrincipal createDonorPrincipal(
            MidPointPrincipal attorneyPrincipal,
            String attorneyAuthorizationAction,
            PrismObject<F> donor,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Determines the access to given item (e.g. `assignment`) of given object. Uses pre-computed security constraints.
     *
     * The `phase` is marked as not null, because this is how it is currently used.
     * This is to simplify the code. We can make it nullable in the future, if needed.
     *
     * TODO what is the role of `currentObject` w.r.t. `securityConstraints`?
     */
    <O extends ObjectType> AccessDecision determineItemDecision(
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            @NotNull String operationUrl,
            @NotNull AuthorizationPhaseType phase,
            @NotNull ItemPath itemPath);

    /**
     * Determines the access to given value (`containerValue`) carrying e.g. an assignment. It is assumed that the value
     * is part of the object, so it has its own "item path".
     *
     * Operation URL and phase are used to determine the access from `securityConstraints`.
     */
    <C extends Containerable> AccessDecision determineItemValueDecision(
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull PrismContainerValue<C> containerValue,
            @NotNull String operationUrl,
            @NotNull AuthorizationPhaseType phase,
            boolean consideringCreation,
            @NotNull String decisionContextDesc);

    record Options(
            @Nullable OwnerResolver customOwnerResolver,
            @Nullable LogCollector logCollector,
            @Nullable Consumer<Authorization> applicableAutzConsumer,
            boolean failOnNoAccess) {

        public static Options create() {
            return new Options(null, null, null, true);
        }

        /**
         * Custom owner resolver to be used for this operation.
         * If not specified, a default resolver (based on a {@link MidPointPrincipalManager}) is used.
         */
        public @NotNull Options withCustomOwnerResolver(OwnerResolver resolver) {
            return new Options(resolver, logCollector, applicableAutzConsumer, failOnNoAccess);
        }

        /** Sends all the tracing messages also to the specified collector (besides logging them as usual). */
        public @NotNull Options withLogCollector(LogCollector logCollector) {
            return new Options(customOwnerResolver, logCollector, applicableAutzConsumer, failOnNoAccess);
        }

        /** Sends all the applicable authorizations to the specified consumer. */
        public @NotNull Options withApplicableAutzConsumer(Consumer<Authorization> applicableAutzConsumer) {
            return new Options(customOwnerResolver, logCollector, applicableAutzConsumer, failOnNoAccess);
        }

        // ignored for now
        public @NotNull Options withNoFailOnNoAccess() {
            return new Options(customOwnerResolver, logCollector, applicableAutzConsumer, false);
        }
    }

    /** A sink for authorization/selector evaluation messages. Used e.g. for the authorization playground. */
    interface LogCollector {

        void log(String message);

        /** Returns `true` if the lower-level tracing (at selector level) is enabled. */
        boolean isSelectorTracingEnabled();
    }
}
