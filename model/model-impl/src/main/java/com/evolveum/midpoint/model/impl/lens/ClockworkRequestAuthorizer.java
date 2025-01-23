/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.SchemaService;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Authorizes clockwork requests. Treats e.g. the following special authorizations:
 *
 * - {@link ModelAuthorizationAction#ASSIGN}
 * - {@link ModelAuthorizationAction#UNASSIGN}
 * - {@link ModelAuthorizationAction#CHANGE_CREDENTIALS}
 *
 * @author Radovan Semancik
 */
public class ClockworkRequestAuthorizer<F extends ObjectType, E extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkRequestAuthorizer.class);

    private static final String OP_AUTHORIZE_REQUEST = Clockwork.class.getName() + ".authorizeRequest";

    @NotNull private final LensContext<F> context;
    @NotNull private final LensElementContext<E> elementContext;
    private final boolean fullInformationAvailable;
    private final boolean isFocus;
    @NotNull private final Task task;

    /**
     * This is not quite fortunate approach as it effectively prevents creating child operation results in this class.
     * But, since the {@link #lensOwnerResolver} uses fixed operation result as well, we are limited to single result
     * object anyway.
     */
    @NotNull private final OperationResult result;

    /** For diagnostic purposes. */
    @NotNull private final String ctxHumanReadableName;

    @NotNull private final SecurityEnforcer securityEnforcer;
    @NotNull private final ModelObjectResolver objectResolver;
    @NotNull private final OwnerResolver lensOwnerResolver;
    @NotNull private final RelationRegistry relationRegistry;
    @NotNull private final PrismContext prismContext;

    private ClockworkRequestAuthorizer(
            @NotNull LensContext<F> context,
            @NotNull LensElementContext<E> elementContext,
            boolean fullInformationAvailable,
            @NotNull Task task,
            @NotNull OperationResult result) {
        this.context = context;
        this.elementContext = elementContext;
        this.fullInformationAvailable = fullInformationAvailable;
        this.isFocus = elementContext instanceof LensFocusContext<E>;
        this.task = task;
        this.result = result;
        this.ctxHumanReadableName = elementContext.getHumanReadableName();

        ModelBeans beans = ModelBeans.get();
        this.securityEnforcer = beans.securityEnforcer;
        this.objectResolver = beans.modelObjectResolver;
        this.lensOwnerResolver = new LensOwnerResolver<>(context, objectResolver, task, result);
        this.relationRegistry = SchemaService.get().relationRegistry();
        this.prismContext = PrismContext.get();
    }

    public static <F extends ObjectType> void authorizeContextRequest(
            LensContext<F> context, boolean fullInformationAvailable, Task task, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        OperationResult result = parentResult.createMinorSubresult(OP_AUTHORIZE_REQUEST);
        LOGGER.trace("Authorizing request for context");
        try {
            LensFocusContext<F> focusContext = context.getFocusContext();
            if (focusContext != null) {
                new ClockworkRequestAuthorizer<>(context, focusContext, fullInformationAvailable, task, result)
                        .authorize();
            }
            for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                new ClockworkRequestAuthorizer<>(context, projectionContext, fullInformationAvailable, task, result)
                        .authorize();
            }
            var authorizationState = fullInformationAvailable ?
                    LensContext.AuthorizationState.FULL : LensContext.AuthorizationState.PRELIMINARY;
            context.setRequestAuthorized(authorizationState);
            LOGGER.trace("Request authorized: {}", authorizationState);
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    private void authorize()
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {

        LOGGER.trace("Authorizing request for element context {}", ctxHumanReadableName);

        ObjectDelta<E> origPrimaryDelta = elementContext.getPrimaryDelta();
        if (origPrimaryDelta == null) {
            // If there is no delta then there is no request to authorize
            LOGGER.trace("Authorized request for element context {}, constraints=null", ctxHumanReadableName);
            return;
        }

        ObjectDelta<E> primaryDeltaClone = origPrimaryDelta.clone();
        // New object is needed when the object is being added.
        // But also in cases such as assignment of account and modification of
        // the same account in one operation.
        PrismObject<E> objectCurrentOrNew = elementContext.getObjectCurrentOrNew();
        if (objectCurrentOrNew == null) {
            if (!isFocus && !fullInformationAvailable) {
                LOGGER.trace("No projection object during preliminary autz evaluation -> skipping the check for now: {}",
                        elementContext);
                return;
            } else {
                throw new IllegalStateException("No object? In: " + elementContext);
            }
        }

        ObjectSecurityConstraints securityConstraints =
                securityEnforcer.compileSecurityConstraints(
                        objectCurrentOrNew,
                        fullInformationAvailable,
                        SecurityEnforcer.Options.create().withCustomOwnerResolver(lensOwnerResolver),
                        task, result);

        new DeltaAuthorization(primaryDeltaClone, objectCurrentOrNew, securityConstraints)
                .authorize();
    }

    /** This is the "real" authorization when we know there is something to authorize. */
    class DeltaAuthorization {

        /**
         * Clone of the primary delta. We gradually remove the already authorized items from it.
         * The rest is authorized in a standard way.
         */
        @NotNull private final ObjectDelta<E> primaryDeltaClone;

        /** The known value of the object according to which the constraints were computed. */
        @NotNull private final PrismObject<E> objectCurrentOrNew;

        /** Compiled constraints for {@link #objectCurrentOrNew}. */
        @NotNull private final ObjectSecurityConstraints securityConstraints;

        /** Operation URL derived from the primary delta. */
        @NotNull private final String operationUrl;

        /** Phase according to which we are checking (normally {@link AuthorizationPhaseType#REQUEST}). */
        @NotNull private final AuthorizationPhaseType authorizationPhase;

        DeltaAuthorization(
                @NotNull ObjectDelta<E> primaryDeltaClone,
                @NotNull PrismObject<E> objectCurrentOrNew,
                @NotNull ObjectSecurityConstraints securityConstraints) {
            this.primaryDeltaClone = primaryDeltaClone;
            this.objectCurrentOrNew = objectCurrentOrNew;
            this.securityConstraints = securityConstraints;
            this.operationUrl = ModelImplUtils.getOperationUrlFromDelta(primaryDeltaClone);
            this.authorizationPhase =
                    context.isExecutionPhaseOnly() ? AuthorizationPhaseType.EXECUTION : AuthorizationPhaseType.REQUEST;
        }

        void authorize()
                throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException {
            if (isFocus) {
                // Process assignments/inducements first. If the assignments/inducements are allowed then we
                // have to ignore the assignment item in subsequent security checks
                if (objectCurrentOrNew.canRepresent(AssignmentHolderType.class)) {
                    authorizeAssignmentsOrInducementsOperation(AssignmentOrInducement.ASSIGNMENT);
                }
                if (objectCurrentOrNew.canRepresent(AbstractRoleType.class)) {
                    authorizeAssignmentsOrInducementsOperation(AssignmentOrInducement.INDUCEMENT);
                }
            }

            // Process credential changes explicitly. There is a special authorization for that.
            authorizeCredentialsOperation();

            // Authorizing all other operations
            if (!primaryDeltaClone.isEmpty()) {
                // TODO: optimize, avoid evaluating the constraints twice
                securityEnforcer.authorize(
                        operationUrl,
                        authorizationPhase,
                        AuthorizationParameters.Builder.buildObjectDelta(
                                objectCurrentOrNew, primaryDeltaClone, fullInformationAvailable),
                        SecurityEnforcer.Options.create()
                                .withCustomOwnerResolver(lensOwnerResolver),
                        task, result);
            }

            LOGGER.trace("Authorized request for element context {} (full info: {}), constraints:\n{}",
                    ctxHumanReadableName, fullInformationAvailable, securityConstraints.debugDumpLazily(1));
        }

        private void authorizeAssignmentsOrInducementsOperation(@NotNull AssignmentOrInducement type)
                throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException {

            // TODO is this check correct? Currently, it would not match e.g. delta for assignment[1]/targetRef
            //  But this probably does not matter; we simply catch this using generic "authorize" call later.
            if (!primaryDeltaClone.hasItemOrSubitemDelta(type.itemName)) {
                return; // No changes in assignments/inducements
            }

            // Note that here we use "current or old", whereas the security constraints are computed from "current or new".
            // This may be e.g. because of the need to resolve ID-only PCV values. But I am not sure.
            PrismObject<E> currentOrOld = elementContext.getObjectCurrentOrOld();
            AccessDecision assignmentItemDecision = securityEnforcer.determineItemDecision(
                    securityConstraints, primaryDeltaClone, currentOrOld, operationUrl, authorizationPhase, type.itemName);

            LOGGER.trace("Security decision for {} item: {}", type, assignmentItemDecision);
            if (assignmentItemDecision == AccessDecision.ALLOW) {
                // Nothing to do, operation is allowed for all values
                LOGGER.debug("Allow assignment/unassignment to {} because access to {} is explicitly allowed",
                        objectCurrentOrNew, type);
            } else if (assignmentItemDecision == AccessDecision.DENY) {
                LOGGER.debug("Deny assignment/unassignment to {} because access to {} is explicitly denied",
                        objectCurrentOrNew, type);
                LOGGER.trace("Denied request for element context {}: access to {} is explicitly denied",
                        ctxHumanReadableName, type);
                throw new AuthorizationException("Access denied");
            } else {
                AuthorizationDecisionType allItemsDecision =
                        securityConstraints.findAllItemsDecision(operationUrl, authorizationPhase);
                if (allItemsDecision == AuthorizationDecisionType.ALLOW) {
                    // Nothing to do, operation is allowed for all values
                } else if (allItemsDecision == AuthorizationDecisionType.DENY) {
                    LOGGER.trace("Denied request for element context {}: access to {} items is explicitly denied",
                            elementContext.getHumanReadableName(), type);
                    throw new AuthorizationException("Access denied");
                } else {
                    // No blank decision for assignment modification yet
                    // process each assignment individually
                    authorizeAssignmentRequest(type, true, true);
                    if (!primaryDeltaClone.isAdd()) {
                        // We want to allow unassignment even if there are policies.
                        // Otherwise we would not be able to get rid of that assignment.
                        authorizeAssignmentRequest(type, false, false);
                    }
                }
            }

            // Assignments were authorized explicitly. Therefore we need to remove them from primary delta
            // to avoid another authorization.
            if (primaryDeltaClone.isAdd()) {
                PrismObject<E> objectToAdd = primaryDeltaClone.getObjectToAdd();
                objectToAdd.removeContainer(type.itemName);
            } else if (primaryDeltaClone.isModify()) {
                primaryDeltaClone.removeContainerModification(type.itemName);
            }
        }

        private void authorizeAssignmentRequest(
                @NotNull AssignmentOrInducement type,
                boolean consideringCreation,
                boolean prohibitPolicies)
                throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException {
            ContainerDelta<AssignmentType> focusItemDelta = primaryDeltaClone.findContainerDelta(type.itemName);
            if (focusItemDelta == null) {
                return;
            }
            Collection<PrismContainerValue<AssignmentType>> changedAssignmentValues =
                    resolveIdOnlyValues(type, focusItemDelta, consideringCreation);
            for (PrismContainerValue<AssignmentType> changedAssignmentValue : changedAssignmentValues) {
                new AssignmentValueAuthorization(changedAssignmentValue, type, consideringCreation, prohibitPolicies)
                        .authorize();
            }
        }

        private class AssignmentValueAuthorization {

            @NotNull private final PrismContainerValue<AssignmentType> changedAssignmentValue;
            @NotNull private final AssignmentType changedAssignment;
            @NotNull private final AssignmentOrInducement type;
            private final boolean consideringCreation;
            @NotNull private final ModelAuthorizationAction assignmentAction;
            @NotNull private final String operationDesc;
            private final boolean prohibitPolicies;

            AssignmentValueAuthorization(
                    @NotNull PrismContainerValue<AssignmentType> changedAssignmentValue,
                    @NotNull AssignmentOrInducement type,
                    boolean consideringCreation,
                    boolean prohibitPolicies) {
                this.changedAssignmentValue = changedAssignmentValue;
                this.changedAssignment = changedAssignmentValue.asContainerable();
                this.type = type;
                this.consideringCreation = consideringCreation;
                this.assignmentAction = consideringCreation ? ModelAuthorizationAction.ASSIGN : ModelAuthorizationAction.UNASSIGN;
                this.operationDesc = assignmentAction.name();
                this.prohibitPolicies = prohibitPolicies;
            }

            void authorize()
                    throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                    CommunicationException, ConfigurationException {
                ObjectReferenceType targetRef = changedAssignment.getTargetRef();
                var oid = Referencable.getOid(targetRef);
                if (oid == null) {
                    authorizeNoTargetOidAssignmentValue();
                    return;
                }

                PrismObject<ObjectType> target;
                try {
                    // We do cloning to avoid modification of the original value.
                    // (Just in case; as currently the resolver does not modify the value.)
                    PrismReferenceValue targetRefClone = targetRef.clone().asReferenceValue();
                    targetRefClone.setObject(null); // This is also just in case. We want to do the resolution in full.

                    // We do not worry about performance here too much. The value will be read only once because of the caching.
                    target = objectResolver.resolve(
                            targetRefClone,
                            "resolving " + type + " target",
                            task, result);
                } catch (ObjectNotFoundException e) {
                    LOGGER.warn("Object {} referenced as {} target in {} was not found",
                            targetRef.asReferenceValue().getOid(), type, objectCurrentOrNew);
                    target = null;
                }

                ObjectDelta<E> assignmentObjectDelta = objectCurrentOrNew.createModifyDelta();
                ContainerDelta<AssignmentType> assignmentDelta = assignmentObjectDelta.createContainerModification(type.itemName);
                // We do not care if this is add or delete. All that matters for authorization is that it is in a delta.
                assignmentDelta.addValuesToAdd(changedAssignment.asPrismContainerValue().clone());

                QName relation = Objects.requireNonNullElseGet(targetRef.getRelation(), prismContext::getDefaultRelation);

                AuthorizationParameters<E, ObjectType> autzParams = new AuthorizationParameters.Builder<E, ObjectType>()
                        .oldObject(objectCurrentOrNew)
                        .delta(assignmentObjectDelta)
                        .target(target)
                        .relation(relation)
                        .orderConstraints(determineOrderConstraints())
                        .fullInformationAvailable(fullInformationAvailable)
                        .build();

                if (prohibitPolicies) {
                    if (changedAssignment.getPolicyRule() != null
                            || !changedAssignment.getPolicyException().isEmpty()
                            || !changedAssignment.getPolicySituation().isEmpty()
                            || !changedAssignment.getTriggeredPolicyRule().isEmpty()) {
                        authorizePolicyAssignmentValue(autzParams);
                        return;
                    }
                }

                var options = SecurityEnforcer.Options.create().withCustomOwnerResolver(lensOwnerResolver);
                if (securityEnforcer.isAuthorized(
                        assignmentAction.getUrl(), authorizationPhase, autzParams, options, task, result)) {
                    LOGGER.debug("{} of target {} to {} allowed with {} authorization",
                            operationDesc, target, objectCurrentOrNew, assignmentAction.getUrl());
                    return;
                }
                if (relationRegistry.isDelegation(relation)) {
                    if (securityEnforcer.isAuthorized(
                            ModelAuthorizationAction.DELEGATE.getUrl(), authorizationPhase, autzParams, options, task, result)) {
                        LOGGER.debug("{} of target {} to {} allowed with {} authorization",
                                operationDesc, target, objectCurrentOrNew, ModelAuthorizationAction.DELEGATE.getUrl());
                        return;
                    }
                }
                LOGGER.debug("{} of target {} to {} denied", operationDesc, target, objectCurrentOrNew);
                String operationMessageKey = AssignmentOrInducement.ASSIGNMENT.equals(type) ?
                        "clockwork.request.authorizer.operation.withAssignment" : "clockwork.request.authorizer.operation.withInducement";
                securityEnforcer.failAuthorization(operationMessageKey, authorizationPhase, autzParams, result);
            }

            private void authorizeNoTargetOidAssignmentValue() throws SecurityViolationException {
                // This may still be allowed by #add and #modify authorizations.
                // We have already checked these, but there may be combinations of
                // assignments, one of the assignments allowed by #assign, other allowed by #modify (e.g. MID-4517).
                // Therefore check the items again. This is not very efficient to check it twice. But this is not a common case
                // so there should not be any big harm in suffering this inefficiency.
                AccessDecision assignmentValueDecision = securityEnforcer.determineItemValueDecision(
                        securityConstraints, changedAssignmentValue, operationUrl,
                        authorizationPhase, consideringCreation, operationDesc);
                if (assignmentValueDecision == AccessDecision.ALLOW) {
                    LOGGER.debug("{} of non-target {} to {} allowed with {} authorization",
                            operationDesc, type, objectCurrentOrNew, operationUrl);
                } else {
                    LOGGER.debug("{} of non-target {} not allowed", operationDesc, type);
                    LOGGER.trace("Denied request for object {}: {} of non-target {} not allowed",
                            objectCurrentOrNew, operationDesc, type);
                    securityEnforcer.failAuthorization(
                            operationDesc,
                            authorizationPhase,
                            AuthorizationParameters.Builder.buildObject(objectCurrentOrNew, fullInformationAvailable),
                            result);
                    throw new NotHereAssertionError();
                }
            }

            private void authorizePolicyAssignmentValue(@NotNull AuthorizationParameters<E, ObjectType> autzParams)
                    throws SecurityViolationException {
                // This may still be allowed by #add and #modify authorizations.
                // We have already checked these, but there may be combinations of
                // assignments, one of the assignments allowed by #assign, other allowed by #modify (e.g. MID-4517).
                // Therefore check the items again. This is not very efficient to check it twice. But this is not a common case
                // so there should not be any big harm in suffering this inefficiency.
                AccessDecision assignmentValueDecision = securityEnforcer.determineItemValueDecision(
                        securityConstraints, changedAssignmentValue, operationUrl,
                        authorizationPhase, consideringCreation, operationDesc);
                if (assignmentValueDecision == AccessDecision.ALLOW) {
                    LOGGER.debug("{} of policy assignment to {} allowed with {} authorization",
                            operationDesc, objectCurrentOrNew, operationUrl);
                } else {
                    securityEnforcer.failAuthorization(
                            "clockwork.request.authorizer.operation.authorizePolicyAssignmentValue",
                            authorizationPhase,
                            autzParams,
                            result);
                    throw new NotHereAssertionError();
                }
            }

            private List<OrderConstraintsType> determineOrderConstraints() {
                if (type == AssignmentOrInducement.ASSIGNMENT) {
                    return List.of(
                            new OrderConstraintsType()
                                    .order(0));
                } else {
                    AssignmentType changedAssignment = changedAssignmentValue.asContainerable();
                    List<OrderConstraintsType> assignmentOrderConstraints = changedAssignment.getOrderConstraint();
                    if (!assignmentOrderConstraints.isEmpty()) {
                        return assignmentOrderConstraints;
                    }
                    return List.of(
                            new OrderConstraintsType()
                                    .order(Objects.requireNonNullElse(changedAssignment.getOrder(), 1)));
                }
            }
        }

        private Collection<PrismContainerValue<AssignmentType>> resolveIdOnlyValues(
                AssignmentOrInducement type,
                ContainerDelta<AssignmentType> assignmentDelta,
                boolean consideringCreation) {
            Collection<PrismContainerValue<AssignmentType>> changedValues =
                    assignmentDelta.getValueChanges(consideringCreation ? PlusMinusZero.PLUS : PlusMinusZero.MINUS);
            if (consideringCreation) {
                return changedValues;
            }
            Collection<PrismContainerValue<AssignmentType>> resolved = new ArrayList<>(changedValues.size());
            LensFocusContext<F> focusContext = context.getFocusContext();
            PrismObject<F> existingObject = focusContext.getObjectCurrentOrOld();
            stateCheck(existingObject != null, "No focus while changing assignments? In %s", focusContext);
            PrismContainer<AssignmentType> existingAssignmentContainer = existingObject.findContainer(type.itemName);
            for (PrismContainerValue<AssignmentType> changedValue : changedValues) {
                if (changedValue.isIdOnly()) {
                    if (existingAssignmentContainer != null) {
                        PrismContainerValue<AssignmentType> existingValue =
                                existingAssignmentContainer.findValue(changedValue.getId());
                        if (existingValue != null) {
                            resolved.add(existingValue);
                        }
                    }
                } else {
                    resolved.add(changedValue);
                }
            }
            return resolved;
        }

        private void authorizeCredentialsOperation() throws AuthorizationException {
            if (primaryDeltaClone.isAdd()) {
                PrismObject<E> objectToAdd = primaryDeltaClone.getObjectToAdd();
                PrismContainer<CredentialsType> credentialsContainer = objectToAdd.findContainer(UserType.F_CREDENTIALS);
                if (credentialsContainer != null) {
                    List<ItemPath> pathsToRemove = new ArrayList<>();
                    for (Item<?, ?> item : credentialsContainer.getValue().getItems()) {
                        ItemPath subItemPath = item.getPath();
                        AuthorizationDecisionType subItemDecision = evaluateCredentialDecision(subItemPath.namedSegmentsOnly());
                        LOGGER.trace("AUTZ: credential add {} decision: {}", subItemPath, subItemDecision);
                        if (subItemDecision == AuthorizationDecisionType.ALLOW) {
                            // Remove it from primary delta, so it will not be evaluated later
                            pathsToRemove.add(subItemPath);
                        } else if (subItemDecision == AuthorizationDecisionType.DENY) {
                            LOGGER.trace("Denied request for element context {}: explicit credentials deny", ctxHumanReadableName);
                            throw new AuthorizationException("Access denied");
                        } else {
                            // Do nothing. The access will be evaluated later in a normal way
                        }
                    }
                    for (ItemPath pathToRemove : pathsToRemove) {
                        objectToAdd.removeContainer(pathToRemove);
                    }
                }
            } else if (primaryDeltaClone.isModify()) {
                Collection<? extends ItemDelta<?, ?>> credentialModifications =
                        primaryDeltaClone.findItemDeltasSubPath(UserType.F_CREDENTIALS);
                for (ItemDelta<?, ?> credentialModification : credentialModifications) {
                    ItemPath modPath = credentialModification.getPath();
                    AuthorizationDecisionType modDecision = evaluateCredentialDecision(modPath.namedSegmentsOnly());
                    LOGGER.trace("AUTZ: credential delta {} decision: {}", modPath, modDecision);
                    if (modDecision == AuthorizationDecisionType.ALLOW) {
                        // Remove it from primary delta, so it will not be evaluated later
                        primaryDeltaClone.removeModification(credentialModification);
                    } else if (modDecision == AuthorizationDecisionType.DENY) {
                        LOGGER.trace("Denied request for element context {}: explicit credentials deny", ctxHumanReadableName);
                        throw new AuthorizationException("Access denied");
                    } else {
                        // Do nothing. The access will be evaluated later in a normal way
                    }
                }
            } else {
                // ignoring delete
            }
        }

        private AuthorizationDecisionType evaluateCredentialDecision(@NotNull ItemPath nameOnlyItemPath) {
            return securityConstraints.findItemDecision(
                    nameOnlyItemPath,
                    ModelAuthorizationAction.CHANGE_CREDENTIALS.getUrl(),
                    authorizationPhase);
        }

        private enum AssignmentOrInducement {
            ASSIGNMENT(AssignmentHolderType.F_ASSIGNMENT),
            INDUCEMENT(AbstractRoleType.F_INDUCEMENT);

            @NotNull private final ItemName itemName;

            AssignmentOrInducement(@NotNull ItemName name) {
                this.itemName = name;
            }
        }
    }
}
