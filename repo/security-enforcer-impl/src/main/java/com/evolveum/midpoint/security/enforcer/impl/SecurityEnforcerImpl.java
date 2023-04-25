/*
 * Copyright (C) 2014-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import static java.util.Collections.emptySet;

import static com.evolveum.midpoint.prism.PrismObjectValue.asObjectable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.SchemaDeputyUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.security.enforcer.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author Radovan Semancik
 */
@Component("securityEnforcer")
public class SecurityEnforcerImpl implements SecurityEnforcer {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    private static final boolean FILTER_TRACE_ENABLED = true;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;

    @Autowired
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;

    @Override
    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(
            String operationUrl, AuthorizationPhaseType phase, AuthorizationParameters<O, T> params,
            OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        AccessDecision decision = isAuthorizedInternal(getMidPointPrincipal(), operationUrl, phase, params, ownerResolver, null, task, result);
        return decision.equals(AccessDecision.ALLOW);
    }

    private <O extends ObjectType, T extends ObjectType> AccessDecision isAuthorizedInternal(
            MidPointPrincipal midPointPrincipal, String operationUrl, AuthorizationPhaseType phase,
            AuthorizationParameters<O, T> params, OwnerResolver ownerResolver,
            Consumer<Authorization> applicableAutzConsumer, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (phase == null) {
            AccessDecision requestPhaseDecision = isAuthorizedPhase(midPointPrincipal,
                    operationUrl, AuthorizationPhaseType.REQUEST, params, ownerResolver, applicableAutzConsumer, task, result);
            if (!requestPhaseDecision.equals(AccessDecision.ALLOW)) {
                return requestPhaseDecision;
            }
            return isAuthorizedPhase(midPointPrincipal, operationUrl, AuthorizationPhaseType.EXECUTION,
                    params, ownerResolver, applicableAutzConsumer, task, result);
        } else {
            return isAuthorizedPhase(midPointPrincipal, operationUrl, phase, params, ownerResolver, applicableAutzConsumer, task, result);
        }
    }

    private <O extends ObjectType, T extends ObjectType> AccessDecision isAuthorizedPhase(
            MidPointPrincipal midPointPrincipal, String operationUrl, AuthorizationPhaseType phase,
            AuthorizationParameters<O, T> params, OwnerResolver ownerResolver,
            Consumer<Authorization> applicableAutzConsumer, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        if (AuthorizationConstants.AUTZ_NO_ACCESS_URL.equals(operationUrl)) {
            return AccessDecision.DENY;
        }

        if (phase == null) {
            throw new IllegalArgumentException("No phase");
        }
        AccessDecision decision = AccessDecision.DEFAULT;
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating authorization principal={}, op={}, phase={}, {}",
                    getUsername(midPointPrincipal), operationUrl, phase, params.shortDump());
        }
        final AutzItemPaths allowedItems = new AutzItemPaths();
        Collection<Authorization> authorities = getAuthorities(midPointPrincipal);
        if (authorities != null) {
            for (Authorization authority : authorities) {
                String autzHumanReadableDesc = authority.getHumanReadableDesc();
                LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

                // First check if the authorization is applicable.

                // action
                if (!authority.getAction().contains(operationUrl) && !authority.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
                    LOGGER.trace("    {} not applicable for operation {}", autzHumanReadableDesc, operationUrl);
                    continue;
                }

                // phase
                if (authority.getPhase() == null) {
                    LOGGER.trace("    {} is applicable for all phases (continuing evaluation)", autzHumanReadableDesc);
                } else {
                    if (authority.getPhase() != phase) {
                        LOGGER.trace("    {} is not applicable for phases {} (breaking evaluation)", autzHumanReadableDesc, phase);
                        continue;
                    } else {
                        LOGGER.trace("    {} is applicable for phases {} (continuing evaluation)", autzHumanReadableDesc, phase);
                    }
                }

                // relation
                if (!isApplicableRelation(authority, params.getRelation())) {
                    LOGGER.trace("    {} not applicable for relation {}", autzHumanReadableDesc, params.getRelation());
                    continue;
                }

                // orderConstraints
                if (!isApplicableOrderConstraints(authority, params.getOrderConstraints())) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("    {} not applicable for orderConstraints {}", autzHumanReadableDesc,
                                SchemaDebugUtil.shortDumpOrderConstraintsList(params.getOrderConstraints()));
                    }
                    continue;
                }

                // object
                if (isApplicableObject(authority, params.getOdo(), midPointPrincipal, ownerResolver, autzHumanReadableDesc, task, result)) {
                    LOGGER.trace("    {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, params.getAnyObject());
                } else {
                    LOGGER.trace("    {} not applicable for object {}, none of the object specifications match (breaking evaluation)",
                            autzHumanReadableDesc, params.getAnyObject());
                    continue;
                }

                // target
                if (isApplicable(authority.getTarget(), params.getTarget(), midPointPrincipal, ownerResolver, "target", autzHumanReadableDesc, task, result)) {
                    LOGGER.trace("    {} applicable for target {} (continuing evaluation)", autzHumanReadableDesc, params.getAnyObject());
                } else {
                    LOGGER.trace("    {} not applicable for target {}, none of the target specifications match (breaking evaluation)",
                            autzHumanReadableDesc, params.getAnyObject());
                    continue;
                }

                if (applicableAutzConsumer != null) {
                    applicableAutzConsumer.accept(authority);
                }

                // authority is applicable to this situation. now we can process the decision.
                AuthorizationDecisionType autzDecision = authority.getDecision();
                if (autzDecision == AuthorizationDecisionType.ALLOW) {
                    allowedItems.collectItems(authority);
                    LOGGER.trace("    {}: ALLOW operation {} (but continue evaluation)", autzHumanReadableDesc, operationUrl);
                    decision = AccessDecision.ALLOW;
                    // Do NOT break here. Other authorization statements may still deny the operation
                } else {
                    // item
                    if (isApplicableItem(authority, params.getOldObject(), params.getDelta())) {
                        LOGGER.trace("    {}: Deny authorization applicable for items (continuing evaluation)", autzHumanReadableDesc);
                    } else {
                        LOGGER.trace("    {} not applicable for items (breaking evaluation)", autzHumanReadableDesc);
                        continue;
                    }
                    LOGGER.trace("    {}: DENY operation {}", autzHumanReadableDesc, operationUrl);
                    decision = AccessDecision.DENY;
                    // Break right here. Deny cannot be overridden by allow. This decision cannot be changed.
                    break;
                }
            }
        }

        if (decision == AccessDecision.ALLOW) {
            // Still check allowedItems. We may still deny the operation.
            if (allowedItems.includesAllItems()) {
                // This means all items are allowed. No need to check anything
                LOGGER.trace("  Empty list of allowed items, operation allowed");
            } else {
                // all items in the object and delta must be allowed
                LOGGER.trace("  Checking for allowed items: {}", allowedItems);

                ItemDecisionFunction itemDecisionFunction = (itemPath, removingContainer) -> decideAllowedItems(itemPath, allowedItems, phase, removingContainer);
                AccessDecision itemsDecision = null;
                if (params.hasDelta()) {
                    // Behave as if this is execution phase for delete delta authorizations. We do not want to avoid deleting objects just because there
                    // are automatic/operational items that were generated by midPoint. Otherwise we won't be really able to delete any object.
                    ItemDecisionFunction itemDecisionFunctionDelete = (itemPath, removingContainer) -> decideAllowedItems(itemPath, allowedItems, AuthorizationPhaseType.EXECUTION, removingContainer);
                    itemsDecision = determineDeltaDecision(params.getDelta(), params.getOldObject(), itemDecisionFunction, itemDecisionFunctionDelete);
                } else if (params.hasObject()) {
                    itemsDecision = determineObjectDecision(params.getAnyObject(), itemDecisionFunction);
                }
                if (itemsDecision != AccessDecision.ALLOW) {
                    LOGGER.trace("    NOT ALLOWED operation because the item decision is {}", itemsDecision);
                    decision = AccessDecision.DEFAULT;
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ result: principal={}, operation={}: {}",
                    getUsername(midPointPrincipal), prettyActionUrl(operationUrl), decision);
        }
        return decision;
    }

    private AccessDecision decideAllowedItems(final ItemPath itemPath,
            final AutzItemPaths allowedItems, final AuthorizationPhaseType phase, final boolean removingContainer) {
        if (isAllowedItem(itemPath, allowedItems, phase, removingContainer)) {
            return AccessDecision.ALLOW;
        } else {
            return AccessDecision.DEFAULT;
        }
    }

    private <O extends ObjectType> AccessDecision determineObjectDecision(
            PrismContainer<O> object, ItemDecisionFunction itemDecisionFunction) {
        AccessDecision containerDecision = determineContainerDecision(object.getValue(), itemDecisionFunction, false, "object");
        if (containerDecision == null && object.isEmpty()) {
            // There are no items in the object. Therefore there is no item that is allowed. Therefore decision is DEFAULT.
            // But also there is no item that is denied or not allowed.
            // This is a corner case. But this approach is often used by GUI to determine if
            // a specific class of object is allowed, e.g. if it is allowed to create (some) roles. This is used to
            // determine whether to display a particular menu item.
            // Therefore we should allow such cases.
            return AccessDecision.ALLOW;
        }
        return containerDecision;
    }

    private <C extends Containerable, O extends ObjectType> AccessDecision determineContainerDeltaDecision(
            ContainerDelta<C> cdelta, PrismObject<O> currentObject, ItemDecisionFunction itemDecisionFunction) {
        AccessDecision decision = null;
        ItemPath path = cdelta.getPath();

        // Everything is plain and simple for add. No need for any additional checks.
        Collection<PrismContainerValue<C>> valuesToAdd = cdelta.getValuesToAdd();
        if (valuesToAdd != null) {
            for (PrismContainerValue<C> cval : valuesToAdd) {
                AccessDecision subdecision = determineContainerDecision(cval, itemDecisionFunction, false, "delta add");
                decision = AccessDecision.combine(decision, subdecision);
            }
        }

        // For deleted container values watch out for id-only deltas. Those deltas do not have
        // any subitems in them. So we need to use data from currentObject for autz evaluation.
        Collection<PrismContainerValue<C>> valuesToDelete = cdelta.getValuesToDelete();
        if (valuesToDelete != null) {
            for (PrismContainerValue<C> cval : valuesToDelete) {
                AccessDecision subdecision = null;
                if (cval.isIdOnly()) {
                    PrismContainerValue<C> currentObjectCval = determineContainerValueFromCurrentObject(path, cval.getId(), currentObject);
                    if (currentObjectCval != null) {
                        subdecision = determineContainerDecision(currentObjectCval, itemDecisionFunction, true, "delta delete (current value)");
                    }
                } else {
                    subdecision = determineContainerDecision(cval, itemDecisionFunction, true, "delta delete");
                }
                if (subdecision != null) {
                    decision = AccessDecision.combine(decision, subdecision);
                }
            }
        }

        // Values to replace should pass the ordinary check. But we also need to check old values
        // in currentObject, because those values are efficiently deleted.
        Collection<PrismContainerValue<C>> valuesToReplace = cdelta.getValuesToReplace();
        if (valuesToReplace != null) {
            for (PrismContainerValue<C> cval : valuesToReplace) {
                AccessDecision subdecision = determineContainerDecision(cval, itemDecisionFunction, false, "delta replace");
                decision = AccessDecision.combine(decision, subdecision);
            }
            Collection<PrismContainerValue<C>> oldCvals = determineContainerValuesFromCurrentObject(path, currentObject);
            if (oldCvals != null) {
                for (PrismContainerValue<C> cval : oldCvals) {
                    AccessDecision subdecision = determineContainerDecision(cval, itemDecisionFunction, true, "delta replace (removed current value)");
                    decision = AccessDecision.combine(decision, subdecision);
                }
            }
        }

        return decision;
    }

    private <C extends Containerable> void logSubitemContainerDecision(AccessDecision subdecision, String location, PrismContainerValue<C> cval) {
        if (LOGGER.isTraceEnabled()) {
            if (subdecision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuthorizationLog()) {
                LOGGER.trace("    container {} for {} (processed subitems): decision={}", cval.getPath(), location, subdecision);
            }
        }
    }

    private void logSubitemDecision(AccessDecision subdecision, String location, ItemPath path) {
        if (LOGGER.isTraceEnabled()) {
            if (subdecision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuthorizationLog()) {
                LOGGER.trace("    item {} for {}: decision={}", path, location, subdecision);
            }
        }
    }

    private <C extends Containerable, O extends ObjectType> PrismContainerValue<C> determineContainerValueFromCurrentObject(
            ItemPath path,
            long id, PrismObject<O> currentObject) {
        Collection<PrismContainerValue<C>> oldCvals = determineContainerValuesFromCurrentObject(path, currentObject);
        if (oldCvals == null) {
            return null;
        }
        for (PrismContainerValue<C> oldCval : oldCvals) {
            if (id == oldCval.getId()) {
                return oldCval;
            }
        }
        return null;
    }

    private <C extends Containerable, O extends ObjectType> Collection<PrismContainerValue<C>> determineContainerValuesFromCurrentObject(
            ItemPath path,
            PrismObject<O> currentObject) {
        PrismContainer<C> container = currentObject.findContainer(path);
        if (container == null) {
            return null;
        }
        return container.getValues();
    }

    private AccessDecision determineContainerDecision(PrismContainerValue<?> cval,
            ItemDecisionFunction itemDecisionFunction, boolean removingContainer, String decisionContextDesc) {
        Collection<Item<?, ?>> items = cval.getItems();
        // Note: cval.isEmpty() will also check for id. We do not care about that.
        if (items.isEmpty()) {
            // TODO: problem with empty containers such as
            //  orderConstraint in assignment. Skip all
            //  empty items ... for now.
            logSubitemContainerDecision(null, decisionContextDesc, cval);
            return null;
        }
        AccessDecision decision = null;
        for (Item<?, ?> item : items) {
            ItemPath itemPath = item.getPath();
            AccessDecision itemDecision = itemDecisionFunction.decide(itemPath.namedSegmentsOnly(), removingContainer);
            logSubitemDecision(itemDecision, decisionContextDesc, itemPath);
            if (itemDecision == null) {
                // null decision means: skip this
                continue;
            }
            if (itemDecision == AccessDecision.DEFAULT && item instanceof PrismContainer<?>) {
                // No decision for entire container. Subitems will dictate the decision.
                @SuppressWarnings("unchecked") List<PrismContainerValue<?>> subValues = (List) ((PrismContainer<?>) item).getValues();
                AccessDecision containerDecision = null;
                for (PrismContainerValue<?> subValue : subValues) {
                    AccessDecision subdecision = determineContainerDecision(subValue, itemDecisionFunction, removingContainer, decisionContextDesc);
                    containerDecision = AccessDecision.combine(containerDecision, subdecision);
                    // We do not want to break the loop immediately here. We want all the denied items to get logged
                }
                if (containerDecision == null) {
                    // All items that were inside all the container values are ignored (e.g. metadata).
                    // This is efficiently the same situation as an empty container.
                    // So just ignore it.
                    //noinspection UnnecessaryContinue : explicit continue here makes the code more readable
                    continue;
                } else {
                    decision = AccessDecision.combine(decision, containerDecision);
                }
            } else {
                if (itemDecision == AccessDecision.DENY) {
                    LOGGER.trace("  DENY operation because item {} in the object is not allowed", itemPath);
                    // We do not want to break the loop immediately here. We want all the denied items to get logged
                }
                decision = AccessDecision.combine(decision, itemDecision);
            }
        }
        logSubitemContainerDecision(decision, decisionContextDesc, cval);
        return decision;
    }

    /**
     * The currentObject parameter is the state of the object as we have seen it (the more recent the better).
     * This is used to check authorization for id-only delete deltas and replace deltas for containers.
     */
    private <O extends ObjectType> AccessDecision determineDeltaDecision(ObjectDelta<O> delta, PrismObject<O> currentObject,
            ItemDecisionFunction itemDecisionFunction, ItemDecisionFunction itemDecisionFunctionDelete) {
        if (delta.isAdd()) {
            return determineObjectDecision(delta.getObjectToAdd(), itemDecisionFunction);
        } else if (delta.isDelete()) {
            return determineObjectDecision(currentObject, itemDecisionFunctionDelete);
        } else {
            AccessDecision decision = null;
            for (ItemDelta<?, ?> itemDelta : delta.getModifications()) {
                ItemPath itemPath = itemDelta.getPath();
                AccessDecision itemDecision = itemDecisionFunction.decide(itemPath.namedSegmentsOnly(), false);
                if (itemDecision == null) {
                    // null decision means: skip this
                    continue;
                }
                if (itemDecision == AccessDecision.DEFAULT && itemDelta instanceof ContainerDelta<?>) {
                    // No decision for entire container. Subitems will dictate the decision.
                    AccessDecision subdecision = determineContainerDeltaDecision((ContainerDelta<?>) itemDelta, currentObject, itemDecisionFunction);
                    decision = AccessDecision.combine(decision, subdecision);
                } else {
                    if (itemDecision == AccessDecision.DENY) {
                        LOGGER.trace("  DENY operation because item {} in the delta is not allowed", itemPath);
                        // We do not want to break the loop immediately here. We want all the denied items to get logged
                    }
                    decision = AccessDecision.combine(decision, itemDecision);
                }
            }
            return decision;
        }
    }

    private boolean isAllowedItem(ItemPath nameOnlyItemPath,
            AutzItemPaths allowedItems, AuthorizationPhaseType phase, boolean removingContainer) {
        if (removingContainer && isInList(nameOnlyItemPath, AuthorizationConstants.OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE)) {
            return true;
        }
        if (AuthorizationPhaseType.EXECUTION.equals(phase) && isInList(nameOnlyItemPath, AuthorizationConstants.EXECUTION_ITEMS_ALLOWED_BY_DEFAULT)) {
            return true;
        }
        return allowedItems.includes(nameOnlyItemPath);
    }

    private boolean isInList(ItemPath itemPath, Collection<ItemPath> allowedItems) {
        return ItemPathCollectionsUtil.containsSubpathOrEquivalent(allowedItems, itemPath);
    }

    @Override
    public <O extends ObjectType, T extends ObjectType> void authorize(
            String operationUrl, AuthorizationPhaseType phase, AuthorizationParameters<O, T> params,
            OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        boolean allow = isAuthorized(operationUrl, phase, params, ownerResolver, task, result);
        if (!allow) {
            failAuthorization(operationUrl, phase, params, result);
        }
    }

    @Override
    public <O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl, AuthorizationPhaseType phase,
            AuthorizationParameters<O, T> params, OperationResult result) throws SecurityViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        String username = getQuotedUsername(principal);
        String message;
        if (params.getTarget() == null && params.getAnyObject() == null) {
            message = "User '" + username + "' not authorized for operation " + operationUrl;
        } else if (params.getTarget() == null) {
            message = "User '" + username + "' not authorized for operation " + operationUrl + " on " + params.getAnyObject();
        } else {
            message = "User '" + username + "' not authorized for operation " + operationUrl + " on " + params.getAnyObject() + " with target " + params.getTarget();
        }
        LOGGER.error("{}", message);
        AuthorizationException e = new AuthorizationException(message);
        result.recordFatalError(e.getMessage(), e);
        throw e;
    }

    private <O extends ObjectType> boolean isApplicableObject(Authorization autz, ObjectDeltaObject<O> odo,
            MidPointPrincipal midPointPrincipal, OwnerResolver ownerResolver, String autzHumanReadableDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        List<OwnedObjectSelectorType> objectSpecTypes = autz.getObject();
        if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
            if (odo == null) {
                LOGGER.trace("  object not applicable for null {}", autzHumanReadableDesc);
                return false;
            }
            if (odo.getObjectDelta() != null && odo.getObjectDelta().isModify()) {
                if (autz.keepZoneOfControl()) {
                    PrismObject<O> oldObject = odo.getOldObject();
                    if (oldObject == null) {
                        throw new IllegalStateException("No old object in odo " + odo);
                    }
                    if (!isApplicable(objectSpecTypes, oldObject, midPointPrincipal, ownerResolver, "object(old)", autzHumanReadableDesc, task, result)) {
                        return false;
                    }
                    PrismObject<O> newObject = odo.getNewObject();
                    if (newObject == null) {
                        throw new IllegalStateException("No new object in odo " + odo);
                    }
                    return isApplicable(objectSpecTypes, newObject, midPointPrincipal, ownerResolver, "object(new)", autzHumanReadableDesc, task, result);
                } else {
                    PrismObject<O> object = odo.getOldObject();
                    if (object == null) {
                        throw new IllegalStateException("No old object in odo " + odo);
                    }
                    return isApplicable(objectSpecTypes, object, midPointPrincipal, ownerResolver, "object(old)", autzHumanReadableDesc, task, result);
                }
            } else {
                // Old and new object should be the same. Or there is just one of them. Any one of them will do.
                PrismObject<O> object = odo.getAnyObject();
                if (object == null) {
                    throw new IllegalStateException("No object in odo " + odo);
                }
                return isApplicable(objectSpecTypes, object, midPointPrincipal, ownerResolver, "object", autzHumanReadableDesc, task, result);
            }
        } else {
            LOGGER.trace("    {}: No object specification in authorization (authorization is applicable)", autzHumanReadableDesc);
            return true;
        }
    }

    private <O extends ObjectType> boolean isApplicable(
            List<OwnedObjectSelectorType> objectSpecTypes, PrismObject<O> object,
            MidPointPrincipal midPointPrincipal, OwnerResolver ownerResolver, String desc, String autzHumanReadableDesc,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
            if (object == null) {
                LOGGER.trace("  {} not applicable for null {}", autzHumanReadableDesc, desc);
                return false;
            }
            for (OwnedObjectSelectorType autzObject : objectSpecTypes) {
                if (isApplicable(autzObject, object, midPointPrincipal, emptySet(), ownerResolver, desc, autzHumanReadableDesc, task, result)) {
                    return true;
                }
            }
            return false;
        } else {
            LOGGER.trace("    {}: No {} specification in authorization (authorization is applicable)", autzHumanReadableDesc, desc);
            return true;
        }
    }

    /**
     * @param otherSelfOids Which OIDs should match "self" in addition to the current principal OID. Usually these could be
     * some or all of delegators' OIDs, i.e. people that delegated privileges to the current principal.
     * The reason is that if we want to match assignee or requestor (probably targetObject and owner as well)
     * we want to give appropriate privileges also to assignee/requestor delegates.
     */
    private <O extends ObjectType> boolean isApplicable(SubjectedObjectSelectorType objectSelector, PrismObject<O> object,
            MidPointPrincipal principal, Collection<String> otherSelfOids, OwnerResolver ownerResolver, String desc,
            String autzHumanReadableDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectFilterExpressionEvaluator filterExpressionEvaluator =
                createFilterEvaluator(principal, desc, autzHumanReadableDesc, task, result);
        if (!repositoryService.selectorMatches(objectSelector, object, filterExpressionEvaluator, LOGGER,
                "    " + autzHumanReadableDesc + " not applicable for " + desc + " because of")) {
            // No need to log inapplicability here. It should be logged inside repositoryService.selectorMatches()
            return false;
        }

        OrgRelationObjectSpecificationType specOrgRelation = objectSelector.getOrgRelation();
        RoleRelationObjectSpecificationType specRoleRelation = objectSelector.getRoleRelation();

        // Special
        List<SpecialObjectSpecificationType> specSpecial = objectSelector.getSpecial();
        if (specSpecial != null && !specSpecial.isEmpty()) {
            if (objectSelector.getFilter() != null || objectSelector.getOrgRef() != null || specOrgRelation != null || specRoleRelation != null) {
                throw new SchemaException("Both filter/org/role/archetype and special " + desc + " specification specified in " + autzHumanReadableDesc);
            }
            for (SpecialObjectSpecificationType special : specSpecial) {
                if (special == SpecialObjectSpecificationType.SELF) {
                    String principalOid = principal != null ? principal.getOid() : null;
                    if (principalOid == null) {
                        // This is a rare case. It should not normally happen. But it may happen in tests
                        // or during initial import. Therefore we are not going to die here. Just ignore it.
                    } else {
                        if (principalOid.equals(object.getOid())) {
                            LOGGER.trace("    {}: 'self' authorization applicable for {} - match on principal OID ({})",
                                    autzHumanReadableDesc, desc, principalOid);
                            return true;
                        } else if (otherSelfOids != null && otherSelfOids.contains(object.getOid())) {
                            LOGGER.trace("    {}: 'self' authorization applicable for {} - match on other 'self OID' ({})",
                                    autzHumanReadableDesc, desc, object.getOid());
                            return true;
                        } else {
                            LOGGER.trace("    {}: 'self' authorization not applicable for {}, principal OID: {} (other accepted self OIDs: {}), {} OID {}",
                                    autzHumanReadableDesc, desc, principalOid, otherSelfOids, desc, object.getOid());
                        }
                    }
                } else {
                    throw new SchemaException("Unsupported special " + desc + " specification specified in " + autzHumanReadableDesc + ": " + special);
                }
            }
            LOGGER.trace("    {}: special authorization not applicable for {}", autzHumanReadableDesc, desc);
            return false;
        } else {
            LOGGER.trace("    {}: specials empty: {}", autzHumanReadableDesc, specSpecial);
        }

        // orgRelation
        if (specOrgRelation != null) {
            boolean match = false;
            for (ObjectReferenceType subjectParentOrgRef : principal.getFocus().getParentOrgRef()) {
                if (matchesOrgRelation(object, subjectParentOrgRef, specOrgRelation, autzHumanReadableDesc, desc)) {
                    LOGGER.trace("    org {} applicable for {}, object OID {} because subject org {} matches",
                            autzHumanReadableDesc, desc, object.getOid(), subjectParentOrgRef.getOid());
                    match = true;
                    break;
                }
            }
            if (!match) {
                LOGGER.trace("    org {} not applicable for {}, object OID {} because none of the subject orgs matches",
                        autzHumanReadableDesc, desc, object.getOid());
                return false;
            }
        }

        // roleRelation
        if (specRoleRelation != null) {
            boolean match = false;
            for (ObjectReferenceType subjectRoleMembershipRef : principal.getFocus().getRoleMembershipRef()) {
                if (matchesRoleRelation(object, subjectRoleMembershipRef, specRoleRelation)) {
                    LOGGER.trace("    {} applicable for {}, object OID {} because subject role relation {} matches",
                            autzHumanReadableDesc, desc, object.getOid(), subjectRoleMembershipRef.getOid());
                    match = true;
                    break;
                }
            }
            if (!match) {
                LOGGER.trace("    {} not applicable for {}, object OID {} because none of the subject roles matches",
                        autzHumanReadableDesc, desc, object.getOid());
                return false;
            }
        }

        if (objectSelector instanceof OwnedObjectSelectorType) {
            OwnedObjectSelectorType ownedObjectSelector = (OwnedObjectSelectorType) objectSelector;

            // Owner
            SubjectedObjectSelectorType ownerSpec = ownedObjectSelector.getOwner();
            if (ownerSpec != null) {
                if (ownerResolver == null) {
                    ownerResolver = securityContextManager.getUserProfileService();
                    if (ownerResolver == null) {
                        LOGGER.trace("    {}: owner object spec not applicable for {}, object OID {} because there is no owner resolver",
                                autzHumanReadableDesc, desc, object.getOid());
                        return false;
                    }
                }
                PrismObject<? extends FocusType> owner = ownerResolver.resolveOwner(object);
                if (owner == null) {
                    LOGGER.trace("    {}: owner object spec not applicable for {}, object OID {} because it has no owner",
                            autzHumanReadableDesc, desc, object.getOid());
                    return false;
                }
                boolean ownerApplicable = isApplicable(ownerSpec, owner, principal, emptySet(), ownerResolver,
                        "owner of " + desc, autzHumanReadableDesc, task, result);
                if (!ownerApplicable) {
                    LOGGER.trace("    {}: owner object spec not applicable for {}, object OID {} because owner does not match (owner={})",
                            autzHumanReadableDesc, desc, object.getOid(), owner);
                    return false;
                }
            }

            // Delegator
            SubjectedObjectSelectorType delegatorSpec = ownedObjectSelector.getDelegator();
            if (delegatorSpec != null) {
                if (!isSelf(delegatorSpec)) {
                    throw new SchemaException("Unsupported non-self delegator clause");
                }
                if (!object.canRepresent(UserType.class)) {
                    LOGGER.trace("    {}: delegator object spec not applicable for {}, because the object is not user",
                            autzHumanReadableDesc, desc);
                    return false;
                }
                boolean found = false;
                for (ObjectReferenceType objectDelegatedRef : ((UserType) object.asObjectable()).getDelegatedRef()) {
                    if (principal.getOid().equals(objectDelegatedRef.getOid())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (BooleanUtils.isTrue(delegatorSpec.isAllowInactive())) {
                        for (AssignmentType objectAssignment : ((UserType) object.asObjectable()).getAssignment()) {
                            ObjectReferenceType objectAssignmentTargetRef = objectAssignment.getTargetRef();
                            if (objectAssignmentTargetRef == null) {
                                continue;
                            }
                            if (principal.getOid().equals(objectAssignmentTargetRef.getOid())) {
                                if (relationRegistry.isDelegation(objectAssignmentTargetRef.getRelation())) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!found) {
                        LOGGER.trace("    {}: delegator object spec not applicable for {}, object OID {} because delegator does not match",
                                autzHumanReadableDesc, desc, object.getOid());
                        return false;
                    }
                }
            }

            // Requestor
            SubjectedObjectSelectorType requestorSpec = ownedObjectSelector.getRequester();
            if (requestorSpec != null) {
                PrismObject<? extends ObjectType> requestor = getRequestor(object, result);
                if (requestor == null) {
                    LOGGER.trace("    {}: requester object spec not applicable for {}, object OID {} because it has no requestor",
                            autzHumanReadableDesc, desc, object.getOid());
                    return false;
                }
                boolean requestorApplicable = isApplicable(requestorSpec, requestor, principal, getDelegatorsForRequestor(principal),
                        ownerResolver, "requestor of " + desc, autzHumanReadableDesc, task, result);
                if (!requestorApplicable) {
                    LOGGER.trace("    {}: requester object spec not applicable for {}, object OID {} because requestor does not match (requestor={})",
                            autzHumanReadableDesc, desc, object.getOid(), requestor);
                    return false;
                }
            }

            // Requestor
            SubjectedObjectSelectorType relatedObjectSpec = ownedObjectSelector.getRelatedObject();
            if (relatedObjectSpec != null) {
                PrismObject<? extends ObjectType> relatedObject = getRelatedObject(object, result);
                if (relatedObject == null) {
                    LOGGER.trace("    {}: related object spec not applicable for {}, object OID {} because it has no related object",
                            autzHumanReadableDesc, desc, object.getOid());
                    return false;
                }
                boolean relatedObjectApplicable = isApplicable(relatedObjectSpec, relatedObject, principal, getDelegatorsForRelatedObjects(principal),
                        ownerResolver, "related object of " + desc, autzHumanReadableDesc, task, result);
                if (!relatedObjectApplicable) {
                    LOGGER.trace("    {}: related object spec not applicable for {}, object OID {} because related object does not match (related object={})",
                            autzHumanReadableDesc, desc, object.getOid(), relatedObject);
                    return false;
                }
            }

            // Assignee
            SubjectedObjectSelectorType assigneeSpec = ownedObjectSelector.getAssignee();
            if (assigneeSpec != null) {
                List<PrismObject<? extends ObjectType>> assignees = getAssignees(object, result);
                if (assignees.isEmpty()) {
                    LOGGER.trace("    {}: assignee spec not applicable for {}, object OID {} because it has no assignees",
                            autzHumanReadableDesc, desc, object.getOid());
                    return false;
                }
                Collection<String> relevantDelegators = getDelegatorsForAssignee(principal);
                boolean assigneeApplicable = false;
                for (PrismObject<? extends ObjectType> assignee : assignees) {
                    if (isApplicable(assigneeSpec, assignee, principal, relevantDelegators,
                            ownerResolver, "assignee of " + desc, autzHumanReadableDesc, task, result)) {
                        assigneeApplicable = true;
                        break;
                    }
                }
                if (!assigneeApplicable) {
                    LOGGER.trace("    {}: assignee spec not applicable for {}, object OID {} because none of the assignees match (assignees={})",
                            autzHumanReadableDesc, desc, object.getOid(), assignees);
                    return false;
                }
            }

            // Tenant
            TenantSelectorType tenantSpec = ownedObjectSelector.getTenant();
            if (tenantSpec != null) {
                if (BooleanUtils.isTrue(tenantSpec.isSameAsSubject())) {
                    ObjectReferenceType subjectTenantRef = principal.getFocus().getTenantRef();
                    if (subjectTenantRef == null || subjectTenantRef.getOid() == null) {
                        LOGGER.trace("    {}: tenant object spec not applicable for {}, object OID {} because subject does not have tenantRef",
                                autzHumanReadableDesc, desc, object.getOid());
                        return false;
                    }
                    ObjectReferenceType objectTenantRef = object.asObjectable().getTenantRef();
                    if (objectTenantRef == null || objectTenantRef.getOid() == null) {
                        LOGGER.trace("    {}: tenant object spec not applicable for {}, object OID {} because object does not have tenantRef",
                                autzHumanReadableDesc, desc, object.getOid());
                        return false;
                    }
                    if (!subjectTenantRef.getOid().equals(objectTenantRef.getOid())) {
                        LOGGER.trace("    {}: tenant object spec not applicable for {}, object OID {} because of tenant mismatch",
                                autzHumanReadableDesc, desc, object.getOid());
                        return false;
                    }
                    if (!BooleanUtils.isTrue(tenantSpec.isIncludeTenantOrg())) {
                        O objectType = object.asObjectable();
                        if (objectType instanceof OrgType) {
                            if (BooleanUtils.isTrue(((OrgType) objectType).isTenant())) {
                                LOGGER.trace("    {}: tenant object spec not applicable for {}, object OID {} because it is a tenant org and it is not included",
                                        autzHumanReadableDesc, desc, object.getOid());
                                return false;
                            }
                        }
                    }
                } else {
                    LOGGER.trace("    {}: tenant object spec not applicable for {}, object OID {} because there is a strange tenant specification in authorization",
                            autzHumanReadableDesc, desc, object.getOid());
                    return false;
                }
            }
        }

        LOGGER.trace("    {} applicable for {} (filter)", autzHumanReadableDesc, desc);
        return true;
    }

    private Collection<String> getDelegatorsForRequestor(MidPointPrincipal principal) {
        return getDelegators(principal, OtherPrivilegesLimitationType.F_CASE_MANAGEMENT_WORK_ITEMS);
    }

    @SuppressWarnings("unused")
    private Collection<String> getDelegatorsForRelatedObjects(MidPointPrincipal principal) {
        // Beware: This is called for both tasks and cases.
        // We do not allow delegators here. Each user should see only cases and tasks related to him (personally).
        return emptySet();
    }

    private Collection<String> getDelegatorsForAssignee(MidPointPrincipal principal) {
        return getDelegators(principal, OtherPrivilegesLimitationType.F_CASE_MANAGEMENT_WORK_ITEMS);
    }

    private Collection<String> getDelegators(MidPointPrincipal principal, ItemName... limitationItemNames) {
        Collection<String> rv = new HashSet<>();
        for (DelegatorWithOtherPrivilegesLimitations delegator : principal.getDelegatorWithOtherPrivilegesLimitationsCollection()) {
            for (ItemName limitationItemName : limitationItemNames) {
                if (SchemaDeputyUtil.limitationsAllow(delegator.getLimitations(), limitationItemName)) {
                    rv.add(delegator.getDelegator().getOid());
                    break;
                }
            }
        }
        return rv;
    }

    private <O extends ObjectType> PrismObject<? extends ObjectType> getRequestor(PrismObject<O> object,
            OperationResult result) {
        O objectBean = asObjectable(object);
        if (objectBean instanceof CaseType) {
            return resolveReference(((CaseType) objectBean).getRequestorRef(), object, "requestor", result);
        } else {
            return null;
        }
    }

    private <O extends ObjectType> PrismObject<? extends ObjectType> getRelatedObject(PrismObject<O> object,
            OperationResult result) {
        O objectBean = asObjectable(object);
        if (objectBean instanceof CaseType) {
            return resolveReference(((CaseType) objectBean).getObjectRef(), object, "related object", result);
        } else if (objectBean instanceof TaskType) {
            return resolveReference(((TaskType) objectBean).getObjectRef(), object, "related object", result);
        } else {
            return null;
        }
    }

    @NotNull
    private <O extends ObjectType> List<PrismObject<? extends ObjectType>> getAssignees(PrismObject<O> object,
            OperationResult result) {
        List<PrismObject<? extends ObjectType>> rv = new ArrayList<>();
        O objectBean = asObjectable(object);
        if (objectBean instanceof CaseType) {
            List<ObjectReferenceType> assignees = CaseTypeUtil.getAllCurrentAssignees(((CaseType) objectBean));
            for (ObjectReferenceType assignee : assignees) {
                CollectionUtils.addIgnoreNull(rv, resolveReference(assignee, object, "assignee", result));
            }
        }
        return rv;
    }

    private <O extends ObjectType> PrismObject<? extends ObjectType> resolveReference(ObjectReferenceType ref,
            PrismObject<O> object, String referenceName, OperationResult result) {
        if (ref != null && ref.getOid() != null) {
            Class<? extends ObjectType> type = ref.getType() != null ?
                    prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()) : UserType.class;
            try {
                return repositoryService.getObject(type, ref.getOid(), null, result);
            } catch (ObjectNotFoundException | SchemaException e) {
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't resolve {} of {}", e, referenceName, object);
                return null;
            }
        } else {
            return null;
        }
    }

    private ObjectFilterExpressionEvaluator createFilterEvaluator(MidPointPrincipal principal,
            String objectTargetDesc, String autzHumanReadableDesc, Task task, OperationResult result) {
        return filter -> {
            if (filter == null) {
                return null;
            }
            VariablesMap variables = new VariablesMap();
            PrismObject<? extends FocusType> subject = principal != null ? principal.getFocus().asPrismObject() : null;
            PrismObjectDefinition<? extends FocusType> def;
            if (subject != null) {
                def = subject.getDefinition();
            } else {
                def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(subject.asObjectable().getClass());
            }
            variables.addVariableDefinition(ExpressionConstants.VAR_SUBJECT, subject, def);
            return ExpressionUtil.evaluateFilterExpressions(filter, variables, MiscSchemaUtil.getExpressionProfile(), expressionFactory, prismContext,
                    "expression in " + objectTargetDesc + " in authorization " + autzHumanReadableDesc, task, result);
        };
    }

    private <O extends ObjectType> ObjectFilter parseAndEvaluateFilter(
            MidPointPrincipal principal, PrismObjectDefinition<O> objectDefinition, SearchFilterType specFilterType,
            String objectTargetDesc, String autzHumanReadableDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectFilter specFilter = prismContext.getQueryConverter().createObjectFilter(objectDefinition, specFilterType);
        if (specFilter == null) {
            return null;
        }
        ObjectFilterExpressionEvaluator filterEvaluator = createFilterEvaluator(principal, objectTargetDesc, autzHumanReadableDesc, task, result);
        return filterEvaluator.evaluate(specFilter);
    }

    private boolean isSelf(SubjectedObjectSelectorType spec) throws SchemaException {
        List<SpecialObjectSpecificationType> specSpecial = spec.getSpecial();
        if (specSpecial != null && !specSpecial.isEmpty()) {
            if (spec.getFilter() != null || spec.getOrgRef() != null || spec.getOrgRelation() != null || spec.getRoleRelation() != null) {
                return false;
            }
            for (SpecialObjectSpecificationType special : specSpecial) {
                if (special == SpecialObjectSpecificationType.SELF) {
                    return true;
                } else {
                    throw new SchemaException("Unsupported special object specification specified in authorization: " + special);
                }
            }
        }
        return false;
    }

    private <O extends ObjectType> boolean matchesOrgRelation(PrismObject<O> object, ObjectReferenceType subjectParentOrgRef,
            OrgRelationObjectSpecificationType specOrgRelation, String autzHumanReadableDesc, String desc) throws SchemaException {
        if (!prismContext.relationMatches(specOrgRelation.getSubjectRelation(), subjectParentOrgRef.getRelation())) {
            return false;
        }
        if (BooleanUtils.isTrue(specOrgRelation.isIncludeReferenceOrg()) && subjectParentOrgRef.getOid().equals(object.getOid())) {
            return true;
        }
        if (specOrgRelation.getScope() == null) {
            return repositoryService.isDescendant(object, subjectParentOrgRef.getOid());
        }
        switch (specOrgRelation.getScope()) {
            case ALL_DESCENDANTS:
                return repositoryService.isDescendant(object, subjectParentOrgRef.getOid());
            case DIRECT_DESCENDANTS:
                return hasParentOrgRef(object, subjectParentOrgRef.getOid());
            case ALL_ANCESTORS:
                return repositoryService.isAncestor(object, subjectParentOrgRef.getOid());
            default:
                throw new UnsupportedOperationException("Unknown orgRelation scope " + specOrgRelation.getScope());
        }
    }

    private <O extends ObjectType> boolean hasParentOrgRef(PrismObject<O> object, String oid) {
        List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
        for (ObjectReferenceType objParentOrgRef : objParentOrgRefs) {
            if (oid.equals(objParentOrgRef.getOid())) {
                return true;
            }
        }
        return false;
    }

    private <O extends ObjectType> boolean matchesRoleRelation(PrismObject<O> object,
            ObjectReferenceType subjectRoleMembershipRef,
            RoleRelationObjectSpecificationType specRoleRelation) {
        if (!prismContext.relationMatches(specRoleRelation.getSubjectRelation(), subjectRoleMembershipRef.getRelation())) {
            return false;
        }
        if (BooleanUtils.isTrue(specRoleRelation.isIncludeReferenceRole()) && subjectRoleMembershipRef.getOid().equals(object.getOid())) {
            return true;
        }
        if (!BooleanUtils.isFalse(specRoleRelation.isIncludeMembers())) {
            if (!object.canRepresent(FocusType.class)) {
                return false;
            }
            for (ObjectReferenceType objectRoleMembershipRef : ((FocusType) object.asObjectable()).getRoleMembershipRef()) {
                if (!subjectRoleMembershipRef.getOid().equals(objectRoleMembershipRef.getOid())) {
                    continue;
                }
                if (!prismContext.relationMatches(specRoleRelation.getObjectRelation(), objectRoleMembershipRef.getRelation())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private <O extends ObjectType> boolean isApplicableItem(Authorization autz,
            PrismObject<O> object, ObjectDelta<O> delta) throws SchemaException {
        List<ItemPathType> itemPaths = autz.getItem();
        if (itemPaths.isEmpty()) {
            List<ItemPathType> exceptItems = autz.getExceptItem();
            if (exceptItems.isEmpty()) {
                // No item constraints. Applicable for all items.
                LOGGER.trace("  items empty");
                return true;
            } else {
                return isApplicableItem(autz, object, delta, exceptItems, false);
            }
        } else {
            return isApplicableItem(autz, object, delta, itemPaths, true);
        }

    }

    private <O extends ObjectType> boolean isApplicableItem(Authorization autz,
            PrismObject<O> object, ObjectDelta<O> delta, List<ItemPathType> itemPaths, boolean positive)
            throws SchemaException {
        for (ItemPathType itemPathType : itemPaths) {
            ItemPath itemPath = itemPathType.getItemPath();
            if (delta == null) {
                if (object != null) {
                    if (object.containsItem(itemPath, false)) {
                        if (positive) {
                            LOGGER.trace("  applicable object item " + itemPath);
                            return true;
                        } else {
                            LOGGER.trace("  excluded object item " + itemPath);
                            return false;
                        }
                    }
                }
            } else {
                ItemDelta<?, ?> itemDelta = delta.findItemDelta(itemPath);
                if (itemDelta != null && !itemDelta.isEmpty()) {
                    if (positive) {
                        LOGGER.trace("  applicable delta item " + itemPath);
                        return true;
                    } else {
                        LOGGER.trace("  excluded delta item " + itemPath);
                        return false;
                    }
                }
            }
        }
        if (positive) {
            LOGGER.trace("  no applicable item");
            return false;
        } else {
            LOGGER.trace("  no excluded item");
            return true;
        }
    }

    /**
     * Simple access control decision similar to that used by spring security.
     * It is practically applicable only for simple (non-parametric) cases such as access to GUI pages.
     * However, it supports authorization hierarchies. Therefore the ordering of elements in
     * required actions is important.
     */
    @Override
    public AccessDecision decideAccess(MidPointPrincipal principal, List<String> requiredActions, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return decideAccess(principal, requiredActions, AuthorizationParameters.EMPTY, task, result);
    }

    /**
     * Simple access control decision similar to that used by spring security.
     * It is practically applicable for REST authorization with user from 'switch-to-principal' in parameters.
     * However, it supports authorization hierarchies. Therefore the ordering of elements in
     * required actions is important.
     */
    @Override
    public <O extends ObjectType, T extends ObjectType> AccessDecision decideAccess(MidPointPrincipal principal,
            List<String> requiredActions, AuthorizationParameters<O, T> params, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        AccessDecision finalDecision = AccessDecision.DEFAULT;
        for (String requiredAction : requiredActions) {
            AccessDecision decision = isAuthorizedInternal(principal, requiredAction, null, params, null, null, task, result);
            if (AccessDecision.DENY.equals(decision)) {
                return AccessDecision.DENY;
            }
            if (AccessDecision.ALLOW.equals(decision)) {
                finalDecision = AccessDecision.ALLOW;
            }
        }
        return finalDecision;
    }

    private String getQuotedUsername(MidPointPrincipal principal) {
        if (principal == null) {
            return "(none)";
        }
        return "'" + principal.getUsername() + "'";
    }

    @Override
    public MidPointPrincipal getMidPointPrincipal() {
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
        if (!(principal instanceof MidPointPrincipal)) {
            if (authentication.getPrincipal() instanceof String && AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
                return null;
            }
            LOGGER.warn("Unknown principal type {}", principal.getClass());
            return null;
        }
        return (MidPointPrincipal) principal;
    }

    private @NotNull Collection<Authorization> getAuthorities(MidPointPrincipal principal) {
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
    public <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object, @Nullable OwnerResolver ownerResolver,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        MidPointPrincipal principal = getMidPointPrincipal();
        if (object == null) {
            throw new IllegalArgumentException("Cannot compile security constraints of null object");
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating security constraints principal={}, object={}", getUsername(principal), object);
        }
        ObjectSecurityConstraintsImpl objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
        for (Authorization autz : getAuthorities(principal)) {
            String autzHumanReadableDesc = autz.getHumanReadableDesc();
            LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

            // skip action applicability evaluation. We are interested in all actions

            // object
            if (isApplicable(autz.getObject(), object, principal, ownerResolver, "object", autzHumanReadableDesc, task, result)) {
                LOGGER.trace("    {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
            } else {
                LOGGER.trace("    {} not applicable for object {}, none of the object specifications match (breaking evaluation)",
                        autzHumanReadableDesc, object);
                continue;
            }

            // skip target applicability evaluation. We do not have a target here

            objectSecurityConstraints.applyAuthorization(autz);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}",
                    getUsername(principal), object, objectSecurityConstraints.debugDump(1));
        }

        return objectSecurityConstraints;
    }

    @Override
    public <O extends ObjectType> ObjectOperationConstraints compileOperationConstraints(
            @NotNull PrismObject<O> object, @Nullable OwnerResolver ownerResolver,
            @NotNull Collection<String> actionUrls, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        argCheck(object != null, "Cannot compile security constraints of null object");
        MidPointPrincipal principal = getMidPointPrincipal();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating read security constraints principal={}, object={}", getUsername(principal), object);
        }
        ObjectOperationConstraintsImpl constraints = new ObjectOperationConstraintsImpl();
        for (Authorization autz : getAuthorities(principal)) {
            String autzHumanReadableDesc = autz.getHumanReadableDesc();
            LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

            // operation
            boolean operationRelevant = false;
            for (String autzAction : autz.getAction()) {
                if (actionUrls.contains(autzAction)) {
                    operationRelevant = true;
                    break;
                }
            }
            if (!operationRelevant) {
                LOGGER.trace("    {} not applicable for operations {}", autzHumanReadableDesc, actionUrls);
                continue;
            }

            // object
            if (isApplicable(autz.getObject(), object, principal, ownerResolver, "object", autzHumanReadableDesc, task, result)) {
                LOGGER.trace("    {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
            } else {
                LOGGER.trace("    {} not applicable for object {}, none of the object specifications match (breaking evaluation)",
                        autzHumanReadableDesc, object);
                continue;
            }

            // skip target applicability evaluation. We do not have a target here

            constraints.applyAuthorization(autz);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}",
                    getUsername(principal), object, constraints.debugDump(1));
        }

        return constraints;
    }

    @Override
    public <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            PrismObject<O> object,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        MidPointPrincipal principal = getMidPointPrincipal();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating search pre-process principal={}, searchResultType={}, object={}: orig filter {}",
                    getUsername(principal), searchResultType, object, origFilter);
        }
        if (origFilter == null) {
            origFilter = FilterCreationUtil.createAll(prismContext);
        }
        FilterGizmo<ObjectFilter> gizmo = new FilterGizmoObjectFilterImpl(prismContext);
        ObjectFilter securityFilter = computeSecurityFilter(principal, operationUrls, phase,
                searchResultType, object, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo, task, result);
        ObjectFilter finalFilter = gizmo.and(origFilter, securityFilter);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluated search pre-process principal={}, objectType={}: {}",
                    getUsername(principal), getObjectType(searchResultType), finalFilter);
        }
        if (finalFilter instanceof AllFilter) {
            // compatibility
            return null;
        }
        return finalFilter;
    }

    @Override
    public <T extends ObjectType, O extends ObjectType, F> F computeSecurityFilter(MidPointPrincipal principal,
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            PrismObject<O> object,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: computing security filter principal={}, searchResultType={}, object={}: orig filter {}",
                    getUsername(principal), searchResultType, object, origFilter);
        }
        F securityFilter;
        if (phase != null) {
            securityFilter = computeSecurityFilterPhase(principal, operationUrls, phase,
                    true, searchResultType, object, true, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo, "search pre-process", task, result);
        } else {
            F filterBoth = computeSecurityFilterPhase(principal, operationUrls, null,
                    false, searchResultType, object, true, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo, "search pre-process", task, result);
            F filterRequest = computeSecurityFilterPhase(principal, operationUrls, AuthorizationPhaseType.REQUEST,
                    false, searchResultType, object, true, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo, "search pre-process", task, result);
            F filterExecution = computeSecurityFilterPhase(principal, operationUrls, AuthorizationPhaseType.EXECUTION,
                    false, searchResultType, object, true, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo, "search pre-process", task, result);
            securityFilter = gizmo.or(filterBoth, gizmo.and(filterRequest, filterExecution));
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: computed security filter principal={}, objectType={}: {}",
                    getUsername(principal), getObjectType(searchResultType), securityFilter);
        }
        return securityFilter;
    }

    /**
     * Question: does object make any sense here? E.g. when searching role members, the role OID should be determined from the query.
     */
    @Override
    public <T extends ObjectType, O extends ObjectType> boolean canSearch(String[] operationUrls,
            AuthorizationPhaseType phase, Class<T> searchResultType, PrismObject<O> object,
            boolean includeSpecial, ObjectFilter origFilter, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        MidPointPrincipal principal = getMidPointPrincipal();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluating search permission principal={}, searchResultType={}, object={}: filter {}",
                    getUsername(principal), searchResultType, object, origFilter);
        }
        if (origFilter == null) {
            return true;
        }
        FilterGizmo<ObjectFilter> gizmo = new FilterGizmoObjectFilterImpl(prismContext);
        ObjectFilter securityFilter;
        if (phase != null) {
            securityFilter = computeSecurityFilterPhase(principal, operationUrls, phase,
                    true, searchResultType, object, includeSpecial, origFilter, null, null, gizmo, "search permission", task, result);
        } else {
            ObjectFilter filterBoth = computeSecurityFilterPhase(principal, operationUrls, null,
                    false, searchResultType, object, includeSpecial, origFilter, null, null, gizmo, "search permission", task, result);
            ObjectFilter filterRequest = computeSecurityFilterPhase(principal, operationUrls, AuthorizationPhaseType.REQUEST,
                    false, searchResultType, object, includeSpecial, origFilter, null, null, gizmo, "search permission", task, result);
            ObjectFilter filterExecution = computeSecurityFilterPhase(principal, operationUrls, AuthorizationPhaseType.EXECUTION,
                    false, searchResultType, object, includeSpecial, origFilter, null, null, gizmo, "search permission", task, result);
            securityFilter = ObjectQueryUtil.filterOr(filterBoth, ObjectQueryUtil.filterAnd(filterRequest, filterExecution,
                    prismContext), prismContext);
        }
        ObjectFilter finalFilter = ObjectQueryUtil.filterAnd(origFilter, securityFilter, prismContext);
        finalFilter = ObjectQueryUtil.simplify(finalFilter, prismContext);
        boolean decision = !(finalFilter instanceof NoneFilter);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AUTZ: evaluated search permission principal={}, objectType={}, evulated from filter: {}: decision={}",
                    getUsername(principal), getObjectType(searchResultType), finalFilter, decision);
        }
        return decision;
    }

    /**
     * @return additional security filter. This filter is supposed to be added (operation "AND") to the original filter.
     *
     * See also {@link com.evolveum.midpoint.repo.common.query.SelectorToFilterTranslator} (should be merged eventually?)
     */
    private <T extends ObjectType, O extends ObjectType, F> F computeSecurityFilterPhase(MidPointPrincipal principal,
            String[] operationUrls, AuthorizationPhaseType phase, boolean includeNullPhase,
            Class<T> objectType, PrismObject<O> object, boolean includeSpecial, ObjectFilter origFilter,
            String limitAuthorizationAction, List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo, String desc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        Collection<Authorization> authorities = getAuthorities(principal);

        F securityFilterAllow = null;
        F securityFilterDeny = null;

        QueryAutzItemPaths queryItemsSpec = new QueryAutzItemPaths();
        queryItemsSpec.addRequiredItems(origFilter); // MID-3916
        LOGGER.trace("  phase={}, initial query items spec {}", phase, queryItemsSpec.shortDumpLazily());

        if (authorities != null) {
            for (Authorization authority : authorities) {
                String autzHumanReadableDesc = authority.getHumanReadableDesc();
                LOGGER.trace("    Evaluating {}", autzHumanReadableDesc);

                // action
                if (!isApplicableForActions(authority, operationUrls)) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("      Authorization not applicable for operation {}", prettyActionUrl(operationUrls));
                    }
                    continue;
                }

                // phase
                if (authority.getPhase() == phase || (includeNullPhase && authority.getPhase() == null)) {
                    LOGGER.trace("      Authorization is applicable for phases {} (continuing evaluation)", phase);
                } else {
                    LOGGER.trace("      Authorization is not applicable for phase {} (includeNullPhase={})", phase, includeNullPhase);
                    continue;
                }

                if (!isApplicableLimitations(authority, limitAuthorizationAction)) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("      Authorization is limited to other action, not applicable for operation {}", prettyActionUrl(operationUrls));
                    }
                    continue;
                }

                // orderConstraints
                if (!isApplicableOrderConstraints(authority, paramOrderConstraints)) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("      Authorization not applicable for orderConstraints {}",
                                SchemaDebugUtil.shortDumpOrderConstraintsList(paramOrderConstraints));
                    }
                    continue;
                }

                // object or target
                String objectTargetSpec;
                ObjectFilter autzObjSecurityFilter = null;
                List<OwnedObjectSelectorType> objectSpecTypes;
                if (object == null) {
                    // object not present. Therefore we are looking for object here
                    objectSpecTypes = authority.getObject();
                    objectTargetSpec = "object";
                } else {
                    // object present. Therefore we are looking for target
                    objectSpecTypes = authority.getTarget();
                    objectTargetSpec = "target";

                    // .. but we need to decide whether this authorization is applicable to the object
                    if (isApplicable(authority.getObject(), object, principal, null, "object", autzHumanReadableDesc, task, result)) {
                        LOGGER.trace("      Authorization is applicable for object {}", object);
                    } else {
                        LOGGER.trace("      Authorization is not applicable for object {}", object);
                        continue;
                    }
                }

                boolean applicable = true;
                if (objectSpecTypes == null || objectSpecTypes.isEmpty()) {

                    LOGGER.trace("      No {} specification in authorization (authorization is universally applicable)", objectTargetSpec);
                    autzObjSecurityFilter = FilterCreationUtil.createAll(prismContext);

                } else {

                    applicable = false;
                    for (OwnedObjectSelectorType objectSpecType : objectSpecTypes) {
                        ObjectFilter objSpecSecurityFilter = null;
                        TypeFilter objSpecTypeFilter = null;
                        SearchFilterType specFilterType = objectSpecType.getFilter();
                        ObjectReferenceType specOrgRef = objectSpecType.getOrgRef();
                        List<ObjectReferenceType> archetypeRefs = objectSpecType.getArchetypeRef();
                        OrgRelationObjectSpecificationType specOrgRelation = objectSpecType.getOrgRelation();
                        RoleRelationObjectSpecificationType specRoleRelation = objectSpecType.getRoleRelation();
                        TenantSelectorType specTenant = objectSpecType.getTenant();
                        QName specTypeQName = objectSpecType.getType();
                        PrismObjectDefinition<T> objectDefinition = null;

                        // Type
                        if (specTypeQName != null) {
                            specTypeQName = prismContext.getSchemaRegistry().qualifyTypeName(specTypeQName);
                            PrismObjectDefinition<?> specObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(specTypeQName);
                            if (specObjectDef == null) {
                                throw new SchemaException("Unknown object type " + specTypeQName + " in " + autzHumanReadableDesc);
                            }
                            Class<?> specObjectClass = specObjectDef.getCompileTimeClass();
                            if (objectType.equals(specObjectClass)) {
                                traceClassMatch("Authorization is applicable for object because of type exact match", specObjectClass, objectType);
                            } else if (!objectType.isAssignableFrom(specObjectClass)) {
                                traceClassMatch("Authorization not applicable for object because of type mismatch", specObjectClass, objectType);
                                continue;
                            } else {
                                traceClassMatch("Authorization is applicable for object because of type match, adding more specific type filter", specObjectClass, objectType);
                                // The spec type is a subclass of requested type. So it might be returned from the search.
                                // We need to use type filter.
                                objSpecTypeFilter = prismContext.queryFactory().createType(specTypeQName, null);
                                // and now we have a more specific object definition to use later in filter processing
                                objectDefinition = (PrismObjectDefinition<T>) specObjectDef;
                            }
                        }

                        // Owner
                        if (objectSpecType.getOwner() != null) {
                            if (objectDefinition == null) {
                                objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
                            }
                            // TODO: MID-3899
                            // TODO what if owner is specified not as "self" ?
                            if (TaskType.class.isAssignableFrom(objectType)) {
                                objSpecSecurityFilter = applyOwnerFilterOwnerRef(TaskType.F_OWNER_REF, objSpecSecurityFilter, principal, objectDefinition);
                            } else {
                                LOGGER.trace("      Authorization not applicable for object because it has owner specification (this is not applicable for search)");
                                continue;
                            }
                        }

                        // Requestor
                        if (objectSpecType.getRequester() != null) {
                            if (CaseType.class.isAssignableFrom(objectType)) {
                                objSpecSecurityFilter = applyRequestorFilter(objSpecSecurityFilter, principal);
                            } else {
                                LOGGER.trace("      Authorization not applicable for object because it has requester specification (this is not applicable for search for objects other than CaseType)");
                                continue;
                            }
                        }

                        // Related object
                        if (objectSpecType.getRelatedObject() != null) {
                            if (CaseType.class.isAssignableFrom(objectType) || TaskType.class.isAssignableFrom(objectType)) {
                                objSpecSecurityFilter = applyRelatedObjectFilter(objectType, objSpecSecurityFilter, principal);
                            } else {
                                LOGGER.trace("      Authorization not applicable for object because it has related object specification (this is not applicable for search for objects other than CaseType and TaskType)");
                                continue;
                            }
                        }

                        // Assignee
                        if (objectSpecType.getAssignee() != null) {
                            if (CaseType.class.isAssignableFrom(objectType)) {
                                objSpecSecurityFilter = applyAssigneeFilter(objSpecSecurityFilter, principal);
                            } else {
                                LOGGER.trace("      Authorization not applicable for object because it has assignee specification (this is not applicable for search for objects other than CaseType)");
                                continue;
                            }
                        }

                        // Delegator
                        if (objectSpecType.getDelegator() != null) {
                            // TODO: MID-3899
                            LOGGER.trace("      Authorization not applicable for object because it has delegator specification (this is not applicable for search)");
                            continue;
                        }

                        applicable = true;

                        // Special
                        List<SpecialObjectSpecificationType> specSpecial = objectSpecType.getSpecial();
                        if (specSpecial != null && !specSpecial.isEmpty()) {
                            if (!includeSpecial) {
                                LOGGER.trace("      Skipping authorization, because specials are present: {}", specSpecial);
                                applicable = false;
                            }
                            if (specFilterType != null || specOrgRef != null || specOrgRelation != null || specRoleRelation != null || specTenant != null || !archetypeRefs.isEmpty()) {
                                throw new SchemaException("Both filter/org/role/archetype/tenant and special object specification specified in authorization");
                            }
                            ObjectFilter specialFilter = null;
                            for (SpecialObjectSpecificationType special : specSpecial) {
                                if (special == SpecialObjectSpecificationType.SELF) {
                                    String principalOid = principal.getOid();
                                    specialFilter = ObjectQueryUtil.filterOr(specialFilter,
                                            prismContext.queryFactory().createInOid(principalOid), prismContext);
                                } else {
                                    throw new SchemaException("Unsupported special object specification specified in authorization: " + special);
                                }
                            }
                            objSpecSecurityFilter = specTypeQName != null ?
                                    prismContext.queryFactory().createType(specTypeQName, specialFilter) : specialFilter;
                        } else {
                            LOGGER.trace("      specials empty: {}", specSpecial);
                        }

                        // Filter
                        if (specFilterType != null) {
                            if (objectDefinition == null) {
                                objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
                            }
                            ObjectFilter specFilter = parseAndEvaluateFilter(principal, objectDefinition,
                                    specFilterType, objectTargetSpec, autzHumanReadableDesc, task, result);
                            if (specFilter != null) {
                                ObjectQueryUtil.assertNotRaw(specFilter, "Filter in authorization object has undefined items."
                                        + " Maybe a 'type' specification is missing in the authorization?");
                                ObjectQueryUtil.assertPropertyOnly(specFilter, "Filter in authorization object is not property-only filter");
                            }
                            LOGGER.trace("      applying property filter {}", specFilter);
                            objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, specFilter, prismContext);
                        } else {
                            LOGGER.trace("      filter empty (assuming \"all\")");
                            if (objSpecSecurityFilter == null) {
                                objSpecSecurityFilter = prismContext.queryFactory().createAll();
                            }
                        }

                        // Archetypes
                        if (!archetypeRefs.isEmpty()) {
                            ObjectFilter archsFilter = null;
                            for (ObjectReferenceType archetypeRef : archetypeRefs) {
                                ObjectFilter archFilter = prismContext.queryFor(AssignmentHolderType.class)
                                        .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(archetypeRef.getOid())
                                        .buildFilter();
                                archsFilter = ObjectQueryUtil.filterOr(archsFilter, archFilter, prismContext);
                            }
                            objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, archsFilter, prismContext);
                            LOGGER.trace("      applying archetype filter {}", archsFilter);
                        } else {
                            LOGGER.trace("      archetype empty");
                        }

                        // Org
                        if (specOrgRef != null) {
                            ObjectFilter orgFilter = prismContext.queryFor(ObjectType.class)
                                    .isChildOf(specOrgRef.getOid()).buildFilter();
                            objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, orgFilter, prismContext);
                            LOGGER.trace("      applying org filter {}", orgFilter);
                        } else {
                            LOGGER.trace("      org empty");
                        }

                        // orgRelation
                        if (specOrgRelation != null) {
                            ObjectFilter objSpecOrgRelationFilter = null;
                            QName subjectRelation = specOrgRelation.getSubjectRelation();
                            for (ObjectReferenceType subjectParentOrgRef : principal.getFocus().getParentOrgRef()) {
                                if (prismContext.relationMatches(subjectRelation, subjectParentOrgRef.getRelation())) {
                                    S_FilterEntryOrEmpty q = prismContext.queryFor(ObjectType.class);
                                    S_FilterExit q2;
                                    if (specOrgRelation.getScope() == null || specOrgRelation.getScope() == OrgScopeType.ALL_DESCENDANTS) {
                                        q2 = q.isChildOf(subjectParentOrgRef.getOid());
                                    } else if (specOrgRelation.getScope() == OrgScopeType.DIRECT_DESCENDANTS) {
                                        q2 = q.isDirectChildOf(subjectParentOrgRef.getOid());
                                    } else if (specOrgRelation.getScope() == OrgScopeType.ALL_ANCESTORS) {
                                        q2 = q.isParentOf(subjectParentOrgRef.getOid());
                                    } else {
                                        throw new UnsupportedOperationException("Unknown orgRelation scope " + specOrgRelation.getScope());
                                    }
                                    if (BooleanUtils.isTrue(specOrgRelation.isIncludeReferenceOrg())) {
                                        q2 = q2.or().id(subjectParentOrgRef.getOid());
                                    }
                                    objSpecOrgRelationFilter = ObjectQueryUtil.filterOr(objSpecOrgRelationFilter, q2.buildFilter(),
                                            prismContext);
                                }
                            }
                            if (objSpecOrgRelationFilter == null) {
                                objSpecOrgRelationFilter = FilterCreationUtil.createNone(prismContext);
                            }
                            objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecOrgRelationFilter,
                                    prismContext);
                            LOGGER.trace("      applying orgRelation filter {}", objSpecOrgRelationFilter);
                        } else {
                            LOGGER.trace("      orgRelation empty");
                        }

                        // roleRelation
                        if (specRoleRelation != null) {
                            ObjectFilter objSpecRoleRelationFilter = processRoleRelationFilter(
                                    principal, authority, specRoleRelation, queryItemsSpec, origFilter);
                            if (objSpecRoleRelationFilter == null) {
                                if (authority.maySkipOnSearch()) {
                                    LOGGER.trace("      not applying roleRelation filter {} because it is not efficient and maySkipOnSearch is set", objSpecRoleRelationFilter);
                                    applicable = false;
                                } else {
                                    objSpecRoleRelationFilter = FilterCreationUtil.createNone(prismContext);
                                }
                            }
                            if (objSpecRoleRelationFilter != null) {
                                objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecRoleRelationFilter,
                                        prismContext);
                                LOGGER.trace("      applying roleRelation filter {}", objSpecRoleRelationFilter);
                            }
                        } else {
                            LOGGER.trace("      roleRelation empty");
                        }

                        // tenant
                        if (specTenant != null) {
                            ObjectFilter objSpecTenantFilter = processTenantFilter(principal, authority, specTenant, queryItemsSpec, origFilter);
                            if (objSpecTenantFilter == null) {
                                if (authority.maySkipOnSearch()) {
                                    LOGGER.trace("      not applying tenant filter {} because it is not efficient and maySkipOnSearch is set", objSpecTenantFilter);
                                    applicable = false;
                                } else {
                                    objSpecTenantFilter = FilterCreationUtil.createNone(prismContext);
                                }
                            }
                            if (objSpecTenantFilter != null) {
                                objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecTenantFilter,
                                        prismContext);
                                LOGGER.trace("      applying tenant filter {}", objSpecTenantFilter);
                            }
                        } else {
                            LOGGER.trace("      tenant empty");
                        }

                        if (objSpecTypeFilter != null) {
                            objSpecTypeFilter.setFilter(objSpecSecurityFilter);
                            objSpecSecurityFilter = objSpecTypeFilter;
                        }

                        traceFilter("objSpecSecurityFilter", objectSpecType, objSpecSecurityFilter);
                        autzObjSecurityFilter = ObjectQueryUtil.filterOr(autzObjSecurityFilter, objSpecSecurityFilter,
                                prismContext);
                    }

                }

                traceFilter("autzObjSecurityFilter", authority, autzObjSecurityFilter);

                if (applicable) {
                    autzObjSecurityFilter = ObjectQueryUtil.simplify(autzObjSecurityFilter, prismContext);
                    F autzObjSecurityF = gizmo.adopt(autzObjSecurityFilter, authority);
                    // authority is applicable to this situation. now we can process the decision.
                    AuthorizationDecisionType decision = authority.getDecision();
                    if (decision == AuthorizationDecisionType.ALLOW) {
                        // allow
                        securityFilterAllow = gizmo.or(securityFilterAllow, autzObjSecurityF);
                        traceFilter("securityFilterAllow", authority, securityFilterAllow, gizmo);
                        if (!gizmo.isNone(autzObjSecurityF)) {
                            queryItemsSpec.collectItems(authority);
                        }
                    } else {
                        // deny
                        if (authority.hasItemSpecification()) {
                            // This is a tricky situation. We have deny authorization, but it only denies access to
                            // some items. Therefore we need to find the objects and then filter out the items.
                            // Therefore do not add this authorization into the filter.
                        } else {
                            if (gizmo.isAll(autzObjSecurityF)) {
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
                            securityFilterDeny = gizmo.or(securityFilterDeny, autzObjSecurityF);
                        }
                    }
                }

                traceFilter("securityFilterAllow", authority, securityFilterAllow, gizmo);
                traceFilter("securityFilterDeny", authority, securityFilterDeny, gizmo);
            }
        }

        traceFilter("securityFilterAllow", null, securityFilterAllow, gizmo);
        traceFilter("securityFilterDeny", null, securityFilterDeny, gizmo);

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

        securityFilterAllow = gizmo.simplify(securityFilterAllow);
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

        if (securityFilterDeny == null) {
            // Nothing has been denied. We have "allow" filter only.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("  phase={} done: principal={}, operation={}, {}: allow\n  Filter:\n{}",
                        phase, getUsername(principal), prettyActionUrl(operationUrls), desc,
                        securityFilterAllow == null ? "null" : gizmo.debugDumpFilter(securityFilterAllow, 2));
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

    private boolean isApplicableForActions(Authorization autz, String[] requiredActions) {
        List<String> autzActions = autz.getAction();
        if (autzActions.contains(AuthorizationConstants.AUTZ_ALL_URL)) {
            return true;
        }
        for (String requiredAction : requiredActions) {
            if (autzActions.contains(requiredAction)) {
                return true;
            }
        }
        return false;
    }

    private boolean isApplicableLimitations(Authorization autz, String limitAuthorizationAction) {
        if (limitAuthorizationAction == null) {
            return true;
        }
        AuthorizationLimitationsType limitations = autz.getLimitations();
        if (limitations == null) {
            return true;
        }
        List<String> limitationsActions = limitations.getAction();
        if (limitationsActions.isEmpty()) {
            return true;
        }
        return limitationsActions.contains(limitAuthorizationAction);
    }

    private boolean isApplicableRelation(Authorization autz, QName requestRelation) {
        List<QName> autzRelation = autz.getRelation();
        if (autzRelation == null || autzRelation.isEmpty()) {
            return true;
        }
        return QNameUtil.contains(autzRelation, requestRelation);
    }

    private boolean isApplicableOrderConstraints(Authorization autz, List<OrderConstraintsType> paramOrderConstraints) {
        if (autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
            // #all is always applicable
            // Compatibility note: in fact, this not really correct. We should not make
            // any special case for #all action - except for the fact that it applies to
            // all actions. Even for #all, the object and target specification should
            // still be processed. But orderConstraint is a bit different. For all other
            // authorization clauses holds that empty clause means that everything is
            // applicable. But it is different for orderConstraints. Due to compatibility
            // with midPoint 3.8 empty orderConstraints means min=0,max=0, i.e. it applies
            // only to assignment (not inducements). Therefore we need this exception for
            // #all, otherwise #all won't be applicable to inducements.
            return true;
        }
        OrderConstraintsType autzOrderConstraints = autz.getOrderConstraints();
        if (paramOrderConstraints == null || paramOrderConstraints.isEmpty()) {
            return autzOrderConstraints == null;
        }
        for (OrderConstraintsType paramOrderConstraint : paramOrderConstraints) {
            if (!isSubset(paramOrderConstraint, autzOrderConstraints)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSubset(OrderConstraintsType paramOrderConstraint, OrderConstraintsType autzOrderConstraints) {
        Integer autzOrderMin;
        Integer autzOrderMax;

        if (autzOrderConstraints == null) {
            autzOrderMin = 0;
            autzOrderMax = 0;
        } else {
            if (autzOrderConstraints.getRelation() != null) {
                throw new UnsupportedOperationException("Complex order constraints with relation not supported in authorizations");
            }
            if (autzOrderConstraints.getResetOrder() != null) {
                throw new UnsupportedOperationException("Complex order constraints with resetOrder not supported in authorizations");
            }

            Integer autzOrder = autzOrderConstraints.getOrder();
            autzOrderMin = XsdTypeMapper.multiplicityToInteger(autzOrderConstraints.getOrderMin());
            if (autzOrderMin == null) {
                if (autzOrder == null) {
                    autzOrderMin = 0;
                } else {
                    autzOrderMin = autzOrder;
                }
            }
            autzOrderMax = XsdTypeMapper.multiplicityToInteger(autzOrderConstraints.getOrderMax());
            if (autzOrderMax == null) {
                if (autzOrder == null) {
                    autzOrderMax = 0;
                } else {
                    autzOrderMax = autzOrder;
                }
            }
        }

        Integer paramOrder = paramOrderConstraint.getOrder();
        Integer paramOrderMin = XsdTypeMapper.multiplicityToInteger(paramOrderConstraint.getOrderMin());
        if (paramOrderMin == null) {
            paramOrderMin = paramOrder;
        }
        Integer paramOrderMax = XsdTypeMapper.multiplicityToInteger(paramOrderConstraint.getOrderMax());
        if (paramOrderMax == null) {
            paramOrderMax = paramOrder;
        }

//        LOGGER.info("OOO: A:{}-{} P:{}-{}", autzOrderMin, autzOrderMax, paramOrderMin, paramOrderMax);

        if (autzOrderMin < 0 || paramOrderMin < 0) {
            // minimum set to infinity, should not really happen
            return false;
        }

        if (paramOrderMin < autzOrderMin) {
            return false;
        }

        if (autzOrderMax < 0) {
            // required maximum set to infinity, everything allowed
            return true;
        }

        if (paramOrderMax < 0) {
            // parameter maximum set to infinity. You cannot pass now.
            return false;
        }

        if (paramOrderMax > autzOrderMax) {
            return false;
        }

        return true;
    }

    private void traceClassMatch(String message, Class<?> specObjectClass, Class<?> objectType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("      {}, authorization {}, query {}",
                    message, specObjectClass.getSimpleName(), objectType.getSimpleName());
        }
    }

    /**
     * Very rudimentary and experimental implementation.
     */
    private ObjectFilter processRoleRelationFilter(MidPointPrincipal principal, Authorization autz,
            RoleRelationObjectSpecificationType specRoleRelation, AutzItemPaths queryItemsSpec, ObjectFilter origFilter) {
        ObjectFilter refRoleFilter = null;
        if (BooleanUtils.isTrue(specRoleRelation.isIncludeReferenceRole())) {
            // This could mean that we will need to add filters for all roles in
            // subject's roleMembershipRef. There may be thousands of these.
            if (!autz.maySkipOnSearch()) {
                throw new UnsupportedOperationException("Inefficient roleRelation search (includeReferenceRole=true) is not supported yet");
            }
        }

        ObjectFilter membersFilter = null;
        if (!BooleanUtils.isFalse(specRoleRelation.isIncludeMembers())) {
            List<PrismReferenceValue> queryRoleRefs = getRoleOidsFromFilter(origFilter);
            if (queryRoleRefs == null || queryRoleRefs.isEmpty()) {
                // Cannot find specific role OID in original query. This could mean that we
                // will need to add filters for all roles in subject's roleMembershipRef.
                // There may be thousands of these.
                if (!autz.maySkipOnSearch()) {
                    throw new UnsupportedOperationException("Inefficient roleRelation search (includeMembers=true without role in the original query) is not supported yet");
                }
            } else {
                List<QName> subjectRelation = specRoleRelation.getSubjectRelation();
                boolean isRoleOidOk = false;
                for (ObjectReferenceType subjectRoleMembershipRef : principal.getFocus().getRoleMembershipRef()) {
                    if (!prismContext.relationMatches(subjectRelation, subjectRoleMembershipRef.getRelation())) {
                        continue;
                    }
                    if (!PrismValueCollectionsUtil.containsOid(queryRoleRefs, subjectRoleMembershipRef.getOid())) {
                        continue;
                    }
                    isRoleOidOk = true;
                    break;
                }
                if (isRoleOidOk) {
                    // There is already a good filter in the origFilter
                    // TODO: mind the objectRelation
                    membersFilter = FilterCreationUtil.createAll(prismContext);
                } else {
                    membersFilter = FilterCreationUtil.createNone(prismContext);
                }
            }
        }

        return ObjectQueryUtil.filterOr(refRoleFilter, membersFilter, prismContext);
    }

    private ObjectFilter processTenantFilter(MidPointPrincipal principal, Authorization autz,
            TenantSelectorType specTenant, AutzItemPaths queryItemsSpec, ObjectFilter origFilter) {
        ObjectFilter tenantFilter;
        if (BooleanUtils.isTrue(specTenant.isSameAsSubject())) {
            ObjectReferenceType subjectTenantRef = principal.getFocus().getTenantRef();
            if (subjectTenantRef == null || subjectTenantRef.getOid() == null) {
                LOGGER.trace("    subject tenant empty (none filter)");
                tenantFilter = FilterCreationUtil.createNone(prismContext);
            } else {
                tenantFilter = prismContext.queryFor(ObjectType.class)
                        .item(ObjectType.F_TENANT_REF).ref(subjectTenantRef.getOid())
                        .buildFilter();
            }
            if (!BooleanUtils.isTrue(specTenant.isIncludeTenantOrg())) {
                ObjectFilter notTenantFilter = prismContext.queryFor(ObjectType.class)
                        .not()
                        .type(OrgType.class)
                        .item(OrgType.F_TENANT).eq(true)
                        .buildFilter();
                tenantFilter = ObjectQueryUtil.filterAnd(tenantFilter, notTenantFilter, prismContext);
            }
            LOGGER.trace("    applying tenant filter {}", tenantFilter);
        } else {
            tenantFilter = FilterCreationUtil.createNone(prismContext);
            LOGGER.trace("    tenant authorization empty (none filter)");
        }

        return tenantFilter;
    }

    private List<PrismReferenceValue> getRoleOidsFromFilter(ObjectFilter origFilter) {
        if (origFilter == null) {
            return null;
        }
        if (origFilter instanceof RefFilter) {
            ItemPath path = ((RefFilter) origFilter).getPath();
            if (path.equivalent(SchemaConstants.PATH_ROLE_MEMBERSHIP_REF)) {
                return ((RefFilter) origFilter).getValues();
            }
        }
        if (origFilter instanceof AndFilter) {
            for (ObjectFilter condition : ((AndFilter) origFilter).getConditions()) {
                List<PrismReferenceValue> refs = getRoleOidsFromFilter(condition);
                if (refs != null && !refs.isEmpty()) {
                    return refs;
                }
            }
        }
        return null;
    }

    private <T extends ObjectType> ObjectFilter applyOwnerFilterOwnerRef(ItemPath ownerRefPath,
            ObjectFilter objSpecSecurityFilter, MidPointPrincipal principal, PrismObjectDefinition<T> objectDefinition) {
        PrismReferenceDefinition ownerRefDef = objectDefinition.findReferenceDefinition(ownerRefPath);
        S_FilterExit builder = prismContext.queryFor(AbstractRoleType.class)
                .item(ownerRefPath, ownerRefDef).ref(principal.getFocus().getOid());
        // TODO don't understand this code
        for (ObjectReferenceType subjectParentOrgRef : principal.getFocus().getParentOrgRef()) {
            if (prismContext.isDefaultRelation(subjectParentOrgRef.getRelation())) {
                builder = builder.or().item(ownerRefPath, ownerRefDef).ref(subjectParentOrgRef.getOid());
            }
        }
        ObjectFilter objSpecOwnerFilter = builder.buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecOwnerFilter, prismContext);
        LOGGER.trace("  applying owner filter {}", objSpecOwnerFilter);
        return objSpecSecurityFilter;
    }

    private ObjectFilter applyRequestorFilter(ObjectFilter objSpecSecurityFilter,
            MidPointPrincipal principal) {
        ObjectFilter filter = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_REQUESTOR_REF).ref(getSelfAndOtherOids(principal, getDelegatorsForRequestor(principal)))
                .buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, filter, prismContext);
        LOGGER.trace("  applying requestor filter {}", filter);
        return objSpecSecurityFilter;
    }

    private ObjectFilter applyRelatedObjectFilter(Class<? extends ObjectType> objectType, ObjectFilter objSpecSecurityFilter,
            MidPointPrincipal principal) {
        // we assume CaseType.F_OBJECT_REF == TaskType.F_OBJECT_REF here
        ObjectFilter filter = prismContext.queryFor(objectType)
                .item(CaseType.F_OBJECT_REF).ref(getSelfAndOtherOids(principal, getDelegatorsForRelatedObjects(principal)))
                .buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, filter, prismContext);
        LOGGER.trace("  applying related object filter {}", filter);
        return objSpecSecurityFilter;
    }

    private ObjectFilter applyAssigneeFilter(ObjectFilter objSpecSecurityFilter,
            MidPointPrincipal principal) {
        ObjectFilter filter = prismContext.queryFor(CaseType.class)
                .exists(CaseType.F_WORK_ITEM)
                .block()
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull()
                .and().item(CaseWorkItemType.F_ASSIGNEE_REF)
                .ref(getSelfAndOtherOids(principal, getDelegatorsForAssignee(principal)))
                .endBlock()
                .buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, filter, prismContext);
        LOGGER.trace("  applying assignee filter {}", filter);
        return objSpecSecurityFilter;
    }

    private String[] getSelfAndOtherOids(MidPointPrincipal principal, Collection<String> otherOids) {
        List<String> rv = new ArrayList<>(otherOids.size() + 1);
        CollectionUtils.addIgnoreNull(rv, principal.getOid());
        rv.addAll(otherOids);
        return rv.toArray(new String[0]);
    }

    private void traceFilter(String message, Object forObj, ObjectFilter filter) {
        if (FILTER_TRACE_ENABLED) {
            LOGGER.trace("FILTER {} for {}:\n{}", message, forObj, filter == null ? DebugDumpable.INDENT_STRING + "null" : filter.debugDump(1));
        }
    }

    private <F> void traceFilter(String message, Object forObj, F filter, FilterGizmo<F> gizmo) {
        if (FILTER_TRACE_ENABLED) {
            LOGGER.trace("FILTER {} for {}:\n{}", message, forObj, gizmo.debugDumpFilter(filter, 1));
        }
    }

    private String getUsername(MidPointPrincipal principal) {
        return principal == null ? null : principal.getUsername();
    }

    private String prettyActionUrl(String fullUrl) {
        return DebugUtil.shortenUrl(AuthorizationConstants.NS_SECURITY_PREFIX, fullUrl);
    }

    private String prettyActionUrl(String[] fullUrls) {
        if (fullUrls.length == 1) {
            return DebugUtil.shortenUrl(AuthorizationConstants.NS_SECURITY_PREFIX, fullUrls[0]);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fullUrls.length; i++) {
                sb.append(DebugUtil.shortenUrl(AuthorizationConstants.NS_SECURITY_PREFIX, fullUrls[i]));
                if (i < fullUrls.length - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }
    }

    private <O extends ObjectType> String getObjectType(Class<O> type) {
        if (type == null) {
            return null;
        }
        return type.getSimpleName();
    }

    @Override
    public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(
            MidPointPrincipal midPointPrincipal, String operationUrl, PrismObject<O> object,
            PrismObject<R> target, OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        ItemSecurityConstraintsImpl itemConstraints = new ItemSecurityConstraintsImpl();

        for (Authorization autz : getAuthorities(midPointPrincipal)) {
            String autzHumanReadableDesc = autz.getHumanReadableDesc();
            LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

            // First check if the authorization is applicable.

            // action
            if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
                LOGGER.trace("    {} not applicable for operation {}", autzHumanReadableDesc, operationUrl);
                continue;
            }

            // phase
            if (autz.getPhase() != null && autz.getPhase() != AuthorizationPhaseType.REQUEST) {
                LOGGER.trace("    {} is not applicable for phase {} (breaking evaluation)", autzHumanReadableDesc, AuthorizationPhaseType.REQUEST);
                continue;
            }

            // object
            if (isApplicable(autz.getObject(), object, midPointPrincipal, ownerResolver, "object", autzHumanReadableDesc, task, result)) {
                LOGGER.trace("    {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
            } else {
                LOGGER.trace("    {} not applicable for object {}, none of the object specifications match (breaking evaluation)",
                        autzHumanReadableDesc, object);
                continue;
            }

            // target
            if (isApplicable(autz.getTarget(), target, midPointPrincipal, ownerResolver, "target", autzHumanReadableDesc, task, result)) {
                LOGGER.trace("    {} applicable for target {} (continuing evaluation)", autzHumanReadableDesc, object);
            } else {
                LOGGER.trace("    {} not applicable for target {}, none of the target specifications match (breaking evaluation)",
                        autzHumanReadableDesc, object);
                continue;
            }

            // authority is applicable to this situation. now we can process the decision.
            itemConstraints.collectItems(autz);
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
        AccessDecision decision = isAuthorizedInternal(attorneyPrincipal, attorneyAuthorizationAction, null, autzParams, null, limitationsCollector, task, result);
        if (!decision.equals(AccessDecision.ALLOW)) {
            failAuthorization(attorneyAuthorizationAction, null, autzParams, result);
        }

        MidPointPrincipal donorPrincipal = securityContextManager.getUserProfileService().getPrincipal(donor, limitationsCollector, result);
        donorPrincipal.setAttorney(attorneyPrincipal.getFocus());

        // chain principals so we can easily drop the power of attorney and return back to original identity
        donorPrincipal.setPreviousPrincipal(attorneyPrincipal);

        return donorPrincipal;
    }

    @Override
    public <O extends ObjectType> AccessDecision determineSubitemDecision(
            ObjectSecurityConstraints securityConstraints, ObjectDelta<O> delta, PrismObject<O> currentObject, String operationUrl,
            AuthorizationPhaseType phase, ItemPath subitemRootPath) {
        ItemDecisionFunction itemDecisionFunction = (nameOnlyItemPath, removingContainer) ->
                subitemDecide(nameOnlyItemPath, removingContainer, securityConstraints, operationUrl, phase, subitemRootPath);
        ItemDecisionFunction itemDecisionFunctionDelete = (nameOnlyItemPath, removingContainer) ->
                subitemDecide(nameOnlyItemPath, removingContainer, securityConstraints, operationUrl, AuthorizationPhaseType.EXECUTION, subitemRootPath);
        return determineDeltaDecision(delta, currentObject, itemDecisionFunction, itemDecisionFunctionDelete);
    }

    private AccessDecision subitemDecide(
            ItemPath nameOnlyItemPath, boolean removingContainer, ObjectSecurityConstraints securityConstraints,
            String operationUrl, AuthorizationPhaseType phase, ItemPath subitemRootPath) {
        if (removingContainer && isInList(nameOnlyItemPath, AuthorizationConstants.OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE)) {
            return null;
        }
        if (AuthorizationPhaseType.EXECUTION.equals(phase) && isInList(nameOnlyItemPath, AuthorizationConstants.EXECUTION_ITEMS_ALLOWED_BY_DEFAULT)) {
            return null;
        }
        if (subitemRootPath != null && !subitemRootPath.isSubPathOrEquivalent(nameOnlyItemPath)) {
//            LOGGER.trace("subitem decision: {} <=> {} (not under root) : {}", subitemRootPath, nameOnlyItemPath, null);
            return null;
        }

        AuthorizationDecisionType authorizationDecisionType = securityConstraints.findItemDecision(nameOnlyItemPath, operationUrl, phase);
        AccessDecision decision = AccessDecision.translate(authorizationDecisionType);
//        LOGGER.trace("subitem decision: {} <=> {} : {}", subitemRootPath, nameOnlyItemPath, decision);
        return decision;
    }

    @Override
    public <C extends Containerable> AccessDecision determineSubitemDecision(
            ObjectSecurityConstraints securityConstraints, PrismContainerValue<C> containerValue, String operationUrl,
            AuthorizationPhaseType phase, ItemPath subitemRootPath, PlusMinusZero plusMinusZero, String decisionContextDesc) {
        boolean removingContainer = false;
        if (plusMinusZero == PlusMinusZero.MINUS) {
            removingContainer = true;
        }
        return determineContainerDecision(containerValue,
                (nameOnlyItemPath, lRemovingContainer) -> {
                    if (lRemovingContainer && isInList(nameOnlyItemPath, AuthorizationConstants.OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE)) {
                        return null;
                    }
                    if (AuthorizationPhaseType.EXECUTION.equals(phase) && isInList(nameOnlyItemPath, AuthorizationConstants.EXECUTION_ITEMS_ALLOWED_BY_DEFAULT)) {
                        return null;
                    }
                    if (subitemRootPath != null && !subitemRootPath.isSubPathOrEquivalent(nameOnlyItemPath)) {
//                        LOGGER.trace("subitem decision: {} <=> {} (not under root) : {}", subitemRootPath, nameOnlyItemPath, null);
                        return null;
                    }

                    AuthorizationDecisionType authorizationDecisionType = securityConstraints.findItemDecision(nameOnlyItemPath, operationUrl, phase);
                    AccessDecision decision = AccessDecision.translate(authorizationDecisionType);
//                    LOGGER.trace("subitem decision: {} <=> {} : {}", subitemRootPath, nameOnlyItemPath, decision);
                    return decision;
                }, removingContainer, decisionContextDesc);
    }
}
