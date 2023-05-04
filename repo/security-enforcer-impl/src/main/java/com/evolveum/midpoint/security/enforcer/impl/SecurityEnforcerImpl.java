/*
 * Copyright (C) 2014-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.api.AuthorizationConstants.EXECUTION_ITEMS_ALLOWED_BY_DEFAULT;
import static com.evolveum.midpoint.security.api.AuthorizationConstants.OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.EXECUTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.REQUEST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.query.SelectorToFilterTranslator;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.security.enforcer.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO provide some description here
 *
 * Until it's done, please see
 *
 * - https://docs.evolveum.com/midpoint/reference/security/authorization/configuration/
 * - https://docs.evolveum.com/midpoint/reference/diag/troubleshooting/authorizations/
 *
 * @author Radovan Semancik
 */
@Component("securityEnforcer")
public class SecurityEnforcerImpl implements SecurityEnforcer {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    private static final boolean FILTER_TRACE_ENABLED = true;

    @Autowired private Beans beans;

    @Autowired
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;

    @Override
    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<O, T> params,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        var decision = decideAccessInternal(
                getMidPointPrincipal(), operationUrl, phase, params, ownerResolver, null, task, result);
        return decision == AccessDecision.ALLOW;
    }

    @Override
    public @NotNull <O extends ObjectType, T extends ObjectType> AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull List<String> operationUrls,
            @NotNull AuthorizationParameters<O, T> params,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        AccessDecision finalDecision = AccessDecision.DEFAULT;
        for (String operationUrl : operationUrls) {
            AccessDecision decision = decideAccessInternal(
                    principal, operationUrl, null, params, null, null, task, result);
            switch (decision) {
                case DENY:
                    return AccessDecision.DENY;
                case ALLOW:
                    finalDecision = AccessDecision.ALLOW;
                    break;
                case DEFAULT:
                    // no change of the final decision
            }
        }
        return finalDecision;
    }

    @Override
    public <O extends ObjectType, T extends ObjectType> void failAuthorization(
            String operationUrl, AuthorizationPhaseType phase, AuthorizationParameters<O, T> params, OperationResult result)
            throws SecurityViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        String username = getQuotedUsername(principal);
        PrismObject<T> target = params.getTarget();
        PrismObject<O> object = params.getAnyObject();
        String message;
        if (target == null && object == null) {
            message = "User '" + username + "' not authorized for operation " + operationUrl;
        } else if (target == null) {
            message = "User '" + username + "' not authorized for operation " + operationUrl + " on " + object;
        } else {
            message = "User '" + username + "' not authorized for operation " + operationUrl + " on " + object + " with target " + target;
        }
        LOGGER.error("{}", message);
        AuthorizationException e = new AuthorizationException(message);
        result.recordFatalError(e.getMessage(), e);
        throw e;
    }

    private String getQuotedUsername(MidPointPrincipal principal) {
        if (principal == null) {
            return "(none)";
        }
        return "'" + principal.getUsername() + "'";
    }

    private <O extends ObjectType, T extends ObjectType> @NotNull AccessDecision decideAccessInternal(
            @Nullable MidPointPrincipal midPointPrincipal,
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<O, T> params,
            @Nullable OwnerResolver ownerResolver,
            @Nullable Consumer<Authorization> applicableAutzConsumer,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (phase == null) {
            AccessDecision requestPhaseDecision = decideAccessForPhase(
                    midPointPrincipal, operationUrl, REQUEST,
                    params, ownerResolver, applicableAutzConsumer, task, result);
            if (requestPhaseDecision != AccessDecision.ALLOW) {
                return requestPhaseDecision;
            }
            return decideAccessForPhase(
                    midPointPrincipal, operationUrl, EXECUTION,
                    params, ownerResolver, applicableAutzConsumer, task, result);
        } else {
            return decideAccessForPhase(
                    midPointPrincipal, operationUrl, phase,
                    params, ownerResolver, applicableAutzConsumer, task, result);
        }
    }

    private <O extends ObjectType, T extends ObjectType> @NotNull AccessDecision decideAccessForPhase(
            @Nullable MidPointPrincipal midPointPrincipal,
            @NotNull String operationUrl,
            @NotNull AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<O, T> params,
            @Nullable OwnerResolver ownerResolver,
            @Nullable Consumer<Authorization> applicableAutzConsumer,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        argCheck(phase != null, "No phase");

        if (AuthorizationConstants.AUTZ_NO_ACCESS_URL.equals(operationUrl)) {
            return AccessDecision.DENY;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating authorization principal={}, op={}, phase={}, {}",
                    getUsername(midPointPrincipal), operationUrl, phase, params.shortDump());
        }

        // Step 1: Iterating through authentications, computing the overall decision and the items we are allowed to touch.
        // (They will be used in step 2.)

        AccessDecision overallDecision = AccessDecision.DEFAULT;
        AutzItemPaths allowedItems = new AutzItemPaths();
        for (Authorization authorization : getAuthorities(midPointPrincipal)) {
            AuthorizationApplicabilityChecker checker =
                    new AuthorizationApplicabilityChecker(authorization, midPointPrincipal, ownerResolver, beans, task, result);
            if (!checker.isApplicableToAction(operationUrl)
                    || !checker.isApplicableToPhase(phase, true)
                    || !checker.isApplicableToRelation(params.getRelation())
                    || !checker.isApplicableToOrderConstraints(params.getOrderConstraints())
                    || !checker.isApplicableToObject(params.getOdo())
                    || !checker.isApplicableToTarget(params.getTarget())) {
                continue;
            }

            if (applicableAutzConsumer != null) {
                applicableAutzConsumer.accept(authorization);
            }

            // The authorization is applicable to this situation. Now we can process the decision.
            if (authorization.isAllow()) {
                allowedItems.collectItems(authorization);
                LOGGER.trace("    ALLOW operation {} => but continuing evaluation of other authorizations", operationUrl);
                overallDecision = AccessDecision.ALLOW;
                // Do NOT break here. Other authorization statements may still deny the operation
            } else { // "deny" authorization
                if (!checker.isApplicableItem(params.getOldObject(), params.getDelta())) {
                    LOGGER.trace("    DENY authorization not matching items => continuing evaluation of other authorizations");
                    continue;
                }
                LOGGER.trace("    DENY authorization matching items => denying the whole operation: {}", operationUrl);
                overallDecision = AccessDecision.DENY;
                // Break right here. Deny cannot be overridden by allow. This decision cannot be changed.
                break;
            }
        }

        // Step 2: Checking the collected info on allowed items. We may still deny the operation.

        if (overallDecision == AccessDecision.ALLOW) {
            if (allowedItems.includesAllItems()) {
                LOGGER.trace("  Empty list of allowed items, operation allowed");
            } else {
                // The object and delta must not contain any item that is not explicitly allowed.
                LOGGER.trace("  Checking for allowed items: {}", allowedItems);

                ItemDecisionFunction itemDecisionFunction =
                        (itemPath, removingContainer) ->
                                decideUsingAllowedItems(itemPath, allowedItems, phase, removingContainer);
                AccessDecision itemsDecision;
                if (params.hasDelta()) {
                    // Behave as if this is execution phase for delete delta authorizations.
                    // We do not want to avoid deleting objects just because there are automatic/operational items that were
                    // generated by midPoint. Otherwise, we won't be really able to delete any object.
                    ItemDecisionFunction itemDecisionFunctionDelete =
                            (itemPath, removingContainer) ->
                                    decideUsingAllowedItems(itemPath, allowedItems, EXECUTION, removingContainer);
                    itemsDecision = decideOnDeltaByItems(
                            params.getDelta(), params.getOldObject(), itemDecisionFunction, itemDecisionFunctionDelete);
                } else if (params.hasObject()) {
                    itemsDecision = decideOnObjectValueByItems(params.getAnyObjectValue(), itemDecisionFunction);
                } else {
                    itemsDecision = null;
                }
                if (itemsDecision != AccessDecision.ALLOW) {
                    LOGGER.trace("    NOT ALLOWED operation because the item decision is {}", itemsDecision);
                    overallDecision = AccessDecision.DEFAULT;
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ result: principal={}, operation={}: {}",
                    getUsername(midPointPrincipal), prettyActionUrl(operationUrl), overallDecision);
        }
        return overallDecision;
    }

    private @NotNull AccessDecision decideUsingAllowedItems(
            @NotNull ItemPath itemPath,
            @NotNull AutzItemPaths allowedItems,
            @NotNull AuthorizationPhaseType phase,
            boolean removingContainer) {
        if (isAllowedByDefault(itemPath, phase, removingContainer)
                || allowedItems.includes(itemPath)) {
            return AccessDecision.ALLOW;
        } else {
            return AccessDecision.DEFAULT;
        }
    }

    private boolean isAllowedByDefault(ItemPath nameOnlyItemPath, AuthorizationPhaseType phase, boolean removingContainer) {
        return removingContainer && OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE.containsSubpathOrEquivalent(nameOnlyItemPath)
                || phase == EXECUTION && EXECUTION_ITEMS_ALLOWED_BY_DEFAULT.containsSubpathOrEquivalent(nameOnlyItemPath);
    }

    /**
     * Is the operation on given object allowed, regarding given "item decision function"?
     *
     * Contains special treatment regarding empty PCVs.
     */
    private AccessDecision decideOnObjectValueByItems(
            PrismContainerValue<?> value, ItemDecisionFunction itemDecisionFunction) {
        AccessDecision decision =
                decideOnContainerValueByItems(
                        value, itemDecisionFunction, false, "object");
        if (decision == null && value.hasNoItems()) {
            // There are no items in the object. Therefore there is no item that is allowed. Therefore decision is DEFAULT.
            // But also there is no item that is denied or not allowed.
            // This is a corner case. But this approach is often used by GUI to determine if
            // a specific class of object is allowed, e.g. if it is allowed to create (some) roles. This is used to
            // determine whether to display a particular menu item.
            // Therefore we should allow such cases.
            return AccessDecision.ALLOW;
        } else {
            return decision;
        }
    }

    /**
     * Can we apply the operation on container value, concerning given item decision function?
     *
     * Any {@link AccessDecision#DENY} item means denying the whole value.
     *
     * @see #decideOnDeltaByItems(ObjectDelta, PrismObject, ItemDecisionFunction, ItemDecisionFunction)
     */
    private AccessDecision decideOnContainerValueByItems(
            @NotNull PrismContainerValue<?> containerValue,
            ItemDecisionFunction itemDecisionFunction, boolean removingContainerValue, String contextDesc) {
        AccessDecision decision = null;
        // TODO: problem with empty containers such as orderConstraint in assignment. Skip all empty items ... for now.
        for (Item<?, ?> item : containerValue.getItems()) {
            ItemPath itemPath = item.getPath();
            AccessDecision itemDecision = itemDecisionFunction.decide(itemPath.namedSegmentsOnly(), removingContainerValue);
            logContainerValueItemDecision(itemDecision, contextDesc, itemPath);
            if (itemDecision == null) {
                // null decision means: skip this item
                continue;
            }
            if (itemDecision == AccessDecision.DEFAULT && item instanceof PrismContainer<?>) {
                // No decision for entire container. Sub-items will dictate the decision.
                //noinspection unchecked,rawtypes
                List<PrismContainerValue<?>> itemValues = (List) ((PrismContainer<?>) item).getValues();
                for (PrismContainerValue<?> itemValue : itemValues) {
                    AccessDecision itemValueDecision = decideOnContainerValueByItems(
                            itemValue, itemDecisionFunction, removingContainerValue, contextDesc);
                    // No need to compute decision per value, as the "combine" operation is associative.
                    decision = AccessDecision.combine(decision, itemValueDecision);
                    // We do not want to break the loop immediately here even if the decision would be DENY.
                    // We want all the denied items to get logged.
                }
            } else {
                if (itemDecision == AccessDecision.DENY) {
                    LOGGER.trace("  DENY operation because item {} in the object is not allowed", itemPath);
                    // We do not want to break the loop immediately here. We want all the denied items to get logged
                }
                decision = AccessDecision.combine(decision, itemDecision);
            }
        }
        logContainerValueDecision(decision, contextDesc, containerValue);
        return decision;
    }

    private void logContainerValueItemDecision(AccessDecision subDecision, String contextDesc, ItemPath path) {
        if (LOGGER.isTraceEnabled()) {
            if (subDecision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuthorizationLog()) {
                LOGGER.trace("    item {} for {}: decision={}", path, contextDesc, subDecision);
            }
        }
    }

    private void logContainerValueDecision(
            AccessDecision decision, String contextDesc, PrismContainerValue<?> cval) {
        if (LOGGER.isTraceEnabled()) {
            if (decision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuthorizationLog()) {
                LOGGER.trace("    container {} for {} (processed sub-items): decision={}", cval.getPath(), contextDesc, decision);
            }
        }
    }

    /**
     * Is the execution of the delta allowed, regarding given "item decision function(s)"?
     *
     * The currentObject parameter is the state of the object as we have seen it (the more recent the better).
     * This is used to check authorization for id-only delete deltas and replace deltas for containers.
     *
     * - `currentObject` should not be null except for `ADD` deltas
     *
     * @see #decideOnContainerValueByItems(PrismContainerValue, ItemDecisionFunction, boolean, String)
     */
    private <O extends ObjectType> @Nullable AccessDecision decideOnDeltaByItems(
            @NotNull ObjectDelta<O> delta, PrismObject<O> currentObject,
            ItemDecisionFunction itemDecisionFunction, ItemDecisionFunction itemDecisionFunctionDelete) {
        if (delta.isAdd()) {
            return decideOnObjectValueByItems(delta.getObjectToAdd().getValue(), itemDecisionFunction);
        } else if (delta.isDelete()) {
            return decideOnObjectValueByItems(currentObject.getValue(), itemDecisionFunctionDelete);
        } else {
            AccessDecision decision = null;
            for (ItemDelta<?, ?> modification : delta.getModifications()) {
                ItemPath itemPath = modification.getPath();
                AccessDecision modDecision = itemDecisionFunction.decide(itemPath.namedSegmentsOnly(), false);
                if (modDecision == null) {
                    // null decision means: skip this modification
                    continue;
                }
                if (modDecision == AccessDecision.DEFAULT && modification instanceof ContainerDelta<?>) {
                    // No decision for entire container. Sub-items will dictate the decision.
                    AccessDecision subDecision =
                            decideOnContainerDeltaByItems((ContainerDelta<?>) modification, currentObject, itemDecisionFunction);
                    decision = AccessDecision.combine(decision, subDecision);
                } else {
                    if (modDecision == AccessDecision.DENY) {
                        LOGGER.trace("  DENY operation because item {} in the delta is not allowed", itemPath);
                        // We do not want to break the loop immediately here. We want all the denied items to get logged
                    }
                    decision = AccessDecision.combine(decision, modDecision);
                }
            }
            return decision;
        }
    }

    private <C extends Containerable, O extends ObjectType> AccessDecision decideOnContainerDeltaByItems(
            ContainerDelta<C> delta, PrismObject<O> currentObject, ItemDecisionFunction itemDecisionFunction) {
        AccessDecision decision = null;
        ItemPath path = delta.getPath();

        // Everything is plain and simple for add. No need for any additional checks.
        for (PrismContainerValue<C> valueToAdd : emptyIfNull(delta.getValuesToAdd())) {
            AccessDecision valueDecision = decideOnContainerValueByItems(
                    valueToAdd, itemDecisionFunction, false, "delta add");
            decision = AccessDecision.combine(decision, valueDecision);
        }

        // For deleted container values watch out for id-only deltas. Those deltas do not have
        // any sub-items in them. So we need to use data from currentObject for autz evaluation.
        for (PrismContainerValue<C> valueToDelete : emptyIfNull(delta.getValuesToDelete())) {
            AccessDecision valueDecision = null;
            if (valueToDelete.isIdOnly()) {
                PrismContainerValue<C> fullValueToDelete =
                        determineContainerValueFromCurrentObject(path, valueToDelete.getId(), currentObject);
                if (fullValueToDelete != null) {
                    valueDecision = decideOnContainerValueByItems(
                            fullValueToDelete,
                            itemDecisionFunction,
                            true,
                            "delta delete (current value)");
                }
            } else {
                valueDecision = decideOnContainerValueByItems(
                        valueToDelete, itemDecisionFunction, true, "delta delete");
            }
            decision = AccessDecision.combine(decision, valueDecision);
        }

        // Values to replace should pass the ordinary check. But we also need to check old values
        // in currentObject, because those values are effectively deleted.
        Collection<PrismContainerValue<C>> valuesToReplace = delta.getValuesToReplace();
        if (valuesToReplace != null) {
            for (PrismContainerValue<C> valueToReplace : valuesToReplace) {
                AccessDecision valueDecision = decideOnContainerValueByItems(
                        valueToReplace, itemDecisionFunction, false, "delta replace");
                decision = AccessDecision.combine(decision, valueDecision);
            }
            Collection<PrismContainerValue<C>> oldValues = determineContainerValuesFromCurrentObject(path, currentObject);
            for (PrismContainerValue<C> oldValue : emptyIfNull(oldValues)) {
                AccessDecision oldValueDecision = decideOnContainerValueByItems(
                        oldValue, itemDecisionFunction, true, "delta replace (removed current value)");
                decision = AccessDecision.combine(decision, oldValueDecision);
            }
        }

        return decision;
    }

    private <C extends Containerable, O extends ObjectType> PrismContainerValue<C> determineContainerValueFromCurrentObject(
            ItemPath path, long id, PrismObject<O> currentObject) {
        Collection<PrismContainerValue<C>> oldValues = determineContainerValuesFromCurrentObject(path, currentObject);
        for (PrismContainerValue<C> oldValue : emptyIfNull(oldValues)) {
            if (id == oldValue.getId()) {
                return oldValue;
            }
        }
        return null;
    }

    private <C extends Containerable, O extends ObjectType> Collection<PrismContainerValue<C>> determineContainerValuesFromCurrentObject(
            ItemPath path, PrismObject<O> currentObject) {
        PrismContainer<C> container = currentObject.findContainer(path);
        return container != null ? container.getValues() : null;
    }

    @Override
    public @Nullable MidPointPrincipal getMidPointPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            LOGGER.warn("No authentication");
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            LOGGER.warn("Null principal");
            return null;
        }
        if (principal instanceof MidPointPrincipal) {
            return (MidPointPrincipal) principal;
        }
        if (AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
            return null;
        }
        LOGGER.warn("Unknown principal type {}", principal.getClass());
        return null;
    }

    private static @NotNull Collection<Authorization> getAuthorities(MidPointPrincipal principal) {
        if (principal == null) {
            // Anonymous access, possibly with elevated privileges
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Collection<Authorization> authorizations = new ArrayList<>();
            if (authentication != null) {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    if (authority instanceof Authorization) {
                        authorizations.add((Authorization) authority);
                    }
                }
            }
            return authorizations;
        } else {
            return principal.getAuthorities();
        }
    }

    @Override
    public @NotNull <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object, @Nullable OwnerResolver ownerResolver,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        argCheck(object != null, "Cannot compile security constraints of null object");
        MidPointPrincipal principal = getMidPointPrincipal();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating security constraints principal={}, object={}", getUsername(principal), object);
        }
        ObjectSecurityConstraintsImpl objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
        for (Authorization autz : getAuthorities(principal)) {
            AuthorizationApplicabilityChecker checker =
                    new AuthorizationApplicabilityChecker(autz, principal, ownerResolver, beans, task, result);
            if (checker.isApplicableToObject(object)) {
                objectSecurityConstraints.applyAuthorization(autz);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}",
                    getUsername(principal), object, objectSecurityConstraints.debugDump(1));
        }

        return objectSecurityConstraints;
    }

    @Override
    public @NotNull <O extends ObjectType> ObjectOperationConstraints compileOperationConstraints(
            @NotNull PrismObject<O> object, @Nullable OwnerResolver ownerResolver,
            @NotNull Collection<String> actionUrls, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        argCheck(object != null, "Cannot compile security constraints of null object");
        MidPointPrincipal principal = getMidPointPrincipal();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating operation security constraints principal={}, object={}", getUsername(principal), object);
        }
        ObjectOperationConstraintsImpl constraints = new ObjectOperationConstraintsImpl();
        for (Authorization autz : getAuthorities(principal)) {
            AuthorizationApplicabilityChecker checker =
                    new AuthorizationApplicabilityChecker(autz, principal, ownerResolver, beans, task, result);
            if (checker.isApplicableToActions(actionUrls)
                    && checker.isApplicableToObject(object)) {
                constraints.applyAuthorization(autz);
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}",
                    getUsername(principal), object, constraints.debugDump(1));
        }
        return constraints;
    }

    @Override
    public @Nullable <T extends ObjectType> ObjectFilter preProcessObjectFilter(
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            @Nullable ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        MidPointPrincipal principal = getMidPointPrincipal();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating search pre-process principal={}, searchResultType={}, orig filter {}",
                    getUsername(principal), searchResultType, origFilter);
        }
        FilterGizmo<ObjectFilter> gizmo = new FilterGizmoObjectFilterImpl();
        ObjectFilter securityFilter = computeSecurityFilterInternal(
                principal, operationUrls, phase, searchResultType, null, SearchType.OBJECT, true, origFilter,
                limitAuthorizationAction, paramOrderConstraints, gizmo, "filter pre-processing", task, result);
        ObjectFilter finalFilter = gizmo.and(origFilter, securityFilter);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluated search pre-process principal={}, objectType={}: {}",
                    getUsername(principal), getObjectTypeName(searchResultType), finalFilter);
        }
        if (finalFilter instanceof AllFilter) {
            return null; // compatibility
        } else {
            return finalFilter;
        }
    }

    @Override
    public <T extends ObjectType, O extends ObjectType, F> F computeTargetSecurityFilter(
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
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return computeSecurityFilterInternal(
                principal, operationUrls, phase, searchResultType, object, SearchType.TARGET, true, origFilter,
                limitAuthorizationAction, paramOrderConstraints, gizmo, "security filter computation", task, result);
    }

    private <T extends ObjectType, O extends ObjectType, F> F computeSecurityFilterInternal(
            MidPointPrincipal principal,
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            PrismObject<O> object,
            @NotNull SearchType searchType,
            boolean includeSpecial,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo,
            String desc,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "AUTZ: computing security filter principal={}, searchResultType={}, object={}, searchType={}: orig filter {}",
                    getUsername(principal), searchResultType, object, searchType, origFilter);
        }
        F securityFilter;
        if (phase != null) {
            securityFilter = computeSecurityFilterPhase(
                    principal, operationUrls, phase, true, searchResultType, object, searchType,
                    includeSpecial, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo,
                    desc, task, result);
        } else {
            F filterBoth = computeSecurityFilterPhase(
                    principal, operationUrls, null, false, searchResultType, object, searchType,
                    includeSpecial, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo,
                    desc, task, result);
            F filterRequest = computeSecurityFilterPhase(
                    principal, operationUrls, REQUEST, false, searchResultType, object, searchType,
                    includeSpecial, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo,
                    desc, task, result);
            F filterExecution = computeSecurityFilterPhase(
                    principal, operationUrls, EXECUTION, false, searchResultType, object, searchType,
                    includeSpecial, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo,
                    desc, task, result);
            securityFilter =
                    gizmo.or(
                            filterBoth,
                            gizmo.and(filterRequest, filterExecution));
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: computed security filter principal={}, searchResultType={}: {}",
                    getUsername(principal), getObjectTypeName(searchResultType), securityFilter);
        }
        return securityFilter;
    }

    @Override
    public <T extends ObjectType> boolean canSearch(
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            boolean includeSpecial,
            ObjectFilter origFilter,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        MidPointPrincipal principal = getMidPointPrincipal();
        FilterGizmo<ObjectFilter> gizmo = new FilterGizmoObjectFilterImpl();
        var securityFilter = computeSecurityFilterInternal(
                principal, operationUrls, phase, searchResultType, null, SearchType.OBJECT, includeSpecial, origFilter,
                null, null, gizmo, "canSearch decision", task, result);
        ObjectFilter finalFilter =
                ObjectQueryUtil.simplify(
                        ObjectQueryUtil.filterAnd(origFilter, securityFilter));
        return !(finalFilter instanceof NoneFilter);
    }

    /**
     * Returns additional security filter. This filter is supposed to be merged with the original filter.
     *
     * See also {@link SelectorToFilterTranslator}
     */
    private <T extends ObjectType, O extends ObjectType, F> F computeSecurityFilterPhase(
            MidPointPrincipal principal,
            String[] operationUrls,
            AuthorizationPhaseType phase,
            boolean includeNullPhase,
            Class<T> objectType,
            @Nullable PrismObject<O> object,
            @NotNull SearchType searchType,
            boolean includeSpecial,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo,
            String desc,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        QueryAutzItemPaths queryItemsSpec = new QueryAutzItemPaths();
        queryItemsSpec.addRequiredItems(origFilter); // MID-3916
        LOGGER.trace("  phase={}, initial query items spec {}", phase, queryItemsSpec.shortDumpLazily());

        SecurityFilterBuilder<F> totalSecurityFilterBuilder = new SecurityFilterBuilder<>(gizmo, queryItemsSpec);

        for (Authorization authorization : getAuthorities(principal)) {
            AuthorizationApplicabilityChecker checker = new AuthorizationApplicabilityChecker(
                    authorization, principal, null, beans, task, result);

            if (!checker.isApplicableToActions(operationUrls)
                    || !checker.isApplicableToPhase(phase, includeNullPhase)
                    || !checker.isApplicableToLimitations(limitAuthorizationAction, operationUrls)
                    || !checker.isApplicableToOrderConstraints(paramOrderConstraints)
                    || (object != null && !checker.isApplicableToObject(object))) {
                continue;
            }

            AuthorizationSecurityFilterBuilder<T> autzSecurityFilterBuilder;
            if (searchType == SearchType.OBJECT) {
                argCheck(object == null, "Searching for object but object is not null");
                autzSecurityFilterBuilder = new AuthorizationSecurityFilterBuilder<>(
                        principal, objectType, authorization, authorization.getObject(), "object",
                        includeSpecial, queryItemsSpec, origFilter, beans, task, result);
            } else if (searchType == SearchType.TARGET) {
                argCheck(object != null, "Searching for target but object is null");
                autzSecurityFilterBuilder = new AuthorizationSecurityFilterBuilder<>(
                        principal, objectType, authorization, authorization.getTarget(), "target",
                        includeSpecial, queryItemsSpec, origFilter, beans, task, result);
            } else {
                throw new AssertionError(searchType);
            }

            autzSecurityFilterBuilder.build();

            if (autzSecurityFilterBuilder.isApplicable()) {
                F autzSecurityFilter =
                        gizmo.adopt(
                                ObjectQueryUtil.simplify(autzSecurityFilterBuilder.getAutzObjSecurityFilter()),
                                authorization);
                // The authorization is applicable to this situation. Now we can process the decision.
                if (authorization.isAllow()) {
                    totalSecurityFilterBuilder.addAllow(autzSecurityFilter, authorization);
                } else { // "deny" type authorization
                    if (authorization.hasItemSpecification()) {
                        // This is a tricky situation. We have deny authorization, but it only denies access to
                        // some items. Therefore we need to find the objects and then filter out the items.
                        // Therefore do not add this authorization into the filter.
                    } else {
                        if (gizmo.isAll(autzSecurityFilter)) {
                            // This is "deny all". We cannot have anything stronger than that.
                            // There is no point in continuing the evaluation.
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("  phase={} done: principal={}, operation={}, {}: deny all",
                                        phase, getUsername(principal), prettyActionUrl(operationUrls), desc);
                            }
                            F secFilter = gizmo.createDenyAll();
                            traceFilter("secFilter", null, secFilter, gizmo);
                            return secFilter;
                        }
                        totalSecurityFilterBuilder.addDeny(autzSecurityFilter);
                    }
                }
            }
            totalSecurityFilterBuilder.trace(authorization);
        }

        totalSecurityFilterBuilder.trace(null);

        LOGGER.trace("  final items: {}", queryItemsSpec.shortDumpLazily());
        List<ItemPath> unsatisfiedItems = queryItemsSpec.evaluateUnsatisfiedItems();
        if (!unsatisfiedItems.isEmpty()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("  phase={} done: principal={}, operation={}, {}: deny because items {} are not allowed",
                        phase, getUsername(principal), prettyActionUrl(operationUrls), desc, unsatisfiedItems);
            }
            F secFilter = gizmo.createDenyAll();
            traceFilter("secFilter", null, secFilter, gizmo);
            return secFilter;
        }

        var securityFilterAllow = gizmo.simplify(totalSecurityFilterBuilder.getSecurityFilterAllow());
        if (securityFilterAllow == null) {
            // Nothing has been allowed. This means default deny.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("  phase={} done: principal={}, operation={}, {}: default deny",
                        phase, getUsername(principal), prettyActionUrl(operationUrls), desc);
            }
            F secFilter = gizmo.createDenyAll();
            traceFilter("secFilter", null, secFilter, gizmo);
            return secFilter;
        }

        var securityFilterDeny = totalSecurityFilterBuilder.getSecurityFilterDeny();
        if (securityFilterDeny == null) {
            // Nothing has been denied. We have "allow" filter only.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("  phase={} done: principal={}, operation={}, {}: allow\n  Filter:\n{}",
                        phase, getUsername(principal), prettyActionUrl(operationUrls), desc,
                        gizmo.debugDumpFilter(securityFilterAllow, 2));
            }
            traceFilter("securityFilterAllow", null, securityFilterAllow, gizmo);
            return securityFilterAllow;

        } else {
            // Both "allow" and "deny" filters
            F secFilter = gizmo.and(securityFilterAllow, gizmo.not(securityFilterDeny));
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("  phase={} done: principal={}, operation={}, {}: allow (with deny clauses)\n  Filter:\n{}",
                        phase, getUsername(principal), prettyActionUrl(operationUrls), desc,
                        secFilter == null ? "null" : gizmo.debugDumpFilter(secFilter, 2));
            }
            traceFilter("secFilter", null, secFilter, gizmo);
            return secFilter;
        }
    }

    static void traceFilter(String message, Object forObj, ObjectFilter filter) {
        if (FILTER_TRACE_ENABLED && LOGGER.isTraceEnabled()) {
            LOGGER.trace("FILTER {} for {}:\n{}",
                    message, forObj, filter == null ? DebugDumpable.INDENT_STRING + "null" : filter.debugDump(1));
        }
    }

    static <F> void traceFilter(String message, Object forObj, F filter, FilterGizmo<F> gizmo) {
        if (FILTER_TRACE_ENABLED && LOGGER.isTraceEnabled()) {
            LOGGER.trace("FILTER {} for {}:\n{}", message, forObj, gizmo.debugDumpFilter(filter, 1));
        }
    }

    private String getUsername(MidPointPrincipal principal) {
        return principal == null ? null : principal.getUsername();
    }

    static String prettyActionUrl(String fullUrl) {
        return DebugUtil.shortenUrl(AuthorizationConstants.NS_SECURITY_PREFIX, fullUrl);
    }

    static String prettyActionUrl(Collection<String> fullUrls) {
        return fullUrls.stream()
                .map(url -> prettyActionUrl(url))
                .collect(Collectors.joining(", "));
    }

    static String prettyActionUrl(String[] fullUrls) {
        if (fullUrls.length == 1) {
            return prettyActionUrl(fullUrls[0]);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fullUrls.length; i++) {
                sb.append(prettyActionUrl(fullUrls[i]));
                if (i < fullUrls.length - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }
    }

    private <O extends ObjectType> String getObjectTypeName(Class<O> type) {
        return type != null ? type.getSimpleName() : null;
    }

    @Override
    public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(
            MidPointPrincipal midPointPrincipal, String operationUrl, PrismObject<O> object,
            PrismObject<R> target, OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        ItemSecurityConstraintsImpl itemConstraints = new ItemSecurityConstraintsImpl();

        for (Authorization autz : getAuthorities(midPointPrincipal)) {
            AuthorizationApplicabilityChecker checker = new AuthorizationApplicabilityChecker(
                    autz, midPointPrincipal, ownerResolver, beans, task, result);
            if (checker.isApplicableToAction(operationUrl)
                    && checker.isApplicableToPhase(REQUEST, true)
                    && checker.isApplicableToObject(object)
                    && checker.isApplicableToTarget(target)) {
                itemConstraints.collectItems(autz);
            }
        }

        return itemConstraints;
    }

    @Override
    public <F extends FocusType> MidPointPrincipal createDonorPrincipal(MidPointPrincipal attorneyPrincipal,
            String attorneyAuthorizationAction, PrismObject<F> donor, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (attorneyPrincipal.getAttorney() != null) {
            throw new UnsupportedOperationException("Transitive attorney is not supported yet");
        }

        AuthorizationLimitationsCollector limitationsCollector = new AuthorizationLimitationsCollector();
        AuthorizationParameters<F, ObjectType> autzParams = AuthorizationParameters.Builder.buildObject(donor);
        AccessDecision decision = decideAccessInternal(
                attorneyPrincipal, attorneyAuthorizationAction, null, autzParams,
                null, limitationsCollector, task, result);
        if (!decision.equals(AccessDecision.ALLOW)) {
            failAuthorization(attorneyAuthorizationAction, null, autzParams, result);
        }

        MidPointPrincipal donorPrincipal =
                securityContextManager.getUserProfileService().getPrincipal(donor, limitationsCollector, result);
        donorPrincipal.setAttorney(attorneyPrincipal.getFocus());

        // chain principals so we can easily drop the power of attorney and return back to original identity
        donorPrincipal.setPreviousPrincipal(attorneyPrincipal);

        return donorPrincipal;
    }

    @Override
    public <O extends ObjectType> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints,
            @NotNull ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            String operationUrl,
            AuthorizationPhaseType phase,
            ItemPath itemPath) {
        ItemDecisionFunction itemDecisionFunction = (nameOnlyItemPath, removingContainer) ->
                decideUsingSecurityConstraints(
                        nameOnlyItemPath, removingContainer, securityConstraints, operationUrl, phase, itemPath);
        ItemDecisionFunction itemDecisionFunctionDelete = (nameOnlyItemPath, removingContainer) ->
                decideUsingSecurityConstraints(
                        nameOnlyItemPath, removingContainer, securityConstraints, operationUrl, EXECUTION, itemPath);
        return decideOnDeltaByItems(delta, currentObject, itemDecisionFunction, itemDecisionFunctionDelete);
    }

    @Override
    public <C extends Containerable> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints,
            PrismContainerValue<C> containerValue,
            String operationUrl,
            AuthorizationPhaseType phase,
            @Nullable ItemPath itemPath,
            PlusMinusZero plusMinusZero,
            String decisionContextDesc) {
        boolean removingContainer = plusMinusZero == PlusMinusZero.MINUS;
        return decideOnContainerValueByItems(
                containerValue,
                (nameOnlyItemPath, lRemovingContainer) -> decideUsingSecurityConstraints(
                        nameOnlyItemPath, lRemovingContainer, securityConstraints, operationUrl, phase, itemPath),
                removingContainer, decisionContextDesc);
    }

    private @Nullable AccessDecision decideUsingSecurityConstraints(
            @NotNull ItemPath nameOnlySubItemPath,
            boolean removingContainer,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull String operationUrl,
            AuthorizationPhaseType phase,
            @Nullable ItemPath itemRootPath) {
        if (isAllowedByDefault(nameOnlySubItemPath, phase, removingContainer)) {
            return null;
        }
        if (itemRootPath != null && !itemRootPath.isSubPathOrEquivalent(nameOnlySubItemPath)) {
            return null;
        }
        return AccessDecision.translate(
                securityConstraints.findItemDecision(nameOnlySubItemPath, operationUrl, phase));
    }

    // TODO name
    private enum SearchType {
        OBJECT, TARGET
    }
}
