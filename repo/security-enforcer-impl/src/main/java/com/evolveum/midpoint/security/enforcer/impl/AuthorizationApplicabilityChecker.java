/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.prism.PrismObjectValue.asObjectable;
import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.prettyActionUrl;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.emptySet;

/**
 * Checks an applicability of given {@link Authorization} to the current situation, represented by action (operation URL),
 * phase, object, assignment target and related aspects, and so on.
 *
 * It was created because this functionality is present in {@link SecurityEnforcerImpl} at various places, leading to
 * code duplication and calling of methods with multitude of parameters.
 */
class AuthorizationApplicabilityChecker extends AuthorizationProcessor {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    private final boolean traceEnabled = LOGGER.isTraceEnabled();

    AuthorizationApplicabilityChecker(
            @NotNull Authorization authorization,
            @Nullable MidPointPrincipal principal,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Beans beans,
            @NotNull Task task,
            @NotNull OperationResult result) {
        super(authorization, principal, ownerResolver, beans, task, result);
        if (traceEnabled) {
            LOGGER.trace("    Evaluating applicability of {}", getDesc());
        }
    }

    boolean isApplicableToAction(@NotNull String operationUrl) {
        List<String> autzActions = authorization.getAction();
        if (autzActions.contains(operationUrl) || autzActions.contains(AuthorizationConstants.AUTZ_ALL_URL)) {
            return true;
        }
        if (traceEnabled) {
            LOGGER.trace("      Authorization not applicable for operation {}", prettyActionUrl(operationUrl));
        }
        return false;
    }

    boolean isApplicableToActions(String[] requiredActions) {
        List<String> autzActions = authorization.getAction();
        if (autzActions.contains(AuthorizationConstants.AUTZ_ALL_URL)) {
            return true;
        }
        for (String requiredAction : requiredActions) {
            if (autzActions.contains(requiredAction)) {
                return true;
            }
        }
        if (traceEnabled) {
            LOGGER.trace("      Authorization not applicable for operation {}", prettyActionUrl(requiredActions));
        }
        return false;
    }

    /** No AUTZ_ALL exception here! */
    boolean isApplicableToActions(Collection<String> requiredActions) {
        List<String> autzActions = authorization.getAction();
        for (String requiredAction : requiredActions) {
            if (autzActions.contains(requiredAction)) {
                return true;
            }
        }
        if (traceEnabled) {
            LOGGER.trace("      Authorization not applicable for operation {}", prettyActionUrl(requiredActions));
        }
        return false;
    }

    boolean isApplicableToPhase(AuthorizationPhaseType phase, boolean includeNullPhase) {
        AuthorizationPhaseType autzPhase = authorization.getPhase();
        if (autzPhase == phase || (includeNullPhase && autzPhase == null)) {
            LOGGER.trace("      Authorization is applicable for phases {} (continuing evaluation)", phase);
            return true;
        } else {
            LOGGER.trace("      Authorization is not applicable for phase {} (includeNullPhase={})", phase, includeNullPhase);
            return false;
        }
    }

    boolean isApplicableToLimitations(String limitAuthorizationAction, String[] operationUrls) {
        if (limitAuthorizationAction == null) {
            return true;
        }
        AuthorizationLimitationsType autzLimitations = authorization.getLimitations();
        if (autzLimitations == null) {
            return true;
        }
        List<String> autzLimitationsActions = autzLimitations.getAction();
        if (autzLimitationsActions.isEmpty() || autzLimitationsActions.contains(limitAuthorizationAction)) {
            return true;
        }
        if (traceEnabled) {
            LOGGER.trace("      Authorization is limited to other action, not applicable for operation {}",
                    prettyActionUrl(operationUrls));
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isApplicableToOrderConstraints(List<OrderConstraintsType> paramOrderConstraints) {
        var applies = getOrderConstraintsApplicability(paramOrderConstraints);
        if (!applies && traceEnabled) {
            LOGGER.trace("      Authorization not applicable for orderConstraints {}",
                    SchemaDebugUtil.shortDumpOrderConstraintsList(paramOrderConstraints));
        }
        return applies;
    }

    private boolean getOrderConstraintsApplicability(List<OrderConstraintsType> paramOrderConstraints) {
        if (authorization.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
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
        OrderConstraintsType autzOrderConstraints = authorization.getOrderConstraints();
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

    private static boolean isSubset(OrderConstraintsType paramOrderConstraint, OrderConstraintsType autzOrderConstraints) {
        int autzOrderMin;
        int autzOrderMax;

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

            int autzOrder = or0(autzOrderConstraints.getOrder());
            autzOrderMin = Objects.requireNonNullElse(
                    XsdTypeMapper.multiplicityToInteger(autzOrderConstraints.getOrderMin()),
                    autzOrder);
            autzOrderMax = Objects.requireNonNullElse(
                    XsdTypeMapper.multiplicityToInteger(autzOrderConstraints.getOrderMax()),
                    autzOrder);
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

        return paramOrderMax <= autzOrderMax;
    }

    boolean isApplicableToRelation(QName relation) {
        List<QName> autzRelation = authorization.getRelation();
        if (autzRelation.isEmpty() || QNameUtil.contains(autzRelation, relation)) {
            return true;
        } else {
            LOGGER.trace("      Authorization is not applicable for relation {}", relation);
            return false;
        }
    }

    <O extends ObjectType> boolean isApplicableToObject(ObjectDeltaObject<O> odo)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        var anyObject = odo != null ? odo.getAnyObject() : null;
        if (isApplicableToObjectDeltaObjectInternal(odo)) {
            LOGGER.trace("    Authorization is applicable for object {} (continuing evaluation)", anyObject);
            return true;
        } else {
            LOGGER.trace("    Authorization is not applicable for object {}, none of the object specifications match (breaking evaluation)",
                    anyObject);
            return false;
        }
    }

    private <O extends ObjectType> boolean isApplicableToObjectDeltaObjectInternal(ObjectDeltaObject<O> odo)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        List<OwnedObjectSelectorType> objectSpecTypes = authorization.getObject();
        if (!objectSpecTypes.isEmpty()) {
            if (odo == null) {
                if (traceEnabled) {
                    LOGGER.trace("  object not applicable for null {}", getDesc());
                }
                return false;
            }
            ObjectDelta<O> objectDelta = odo.getObjectDelta();
            if (objectDelta != null && objectDelta.isModify()) {
                if (authorization.keepZoneOfControl()) {
                    PrismObject<O> oldObject = odo.getOldObject();
                    if (oldObject == null) {
                        throw new IllegalStateException("No old object in odo " + odo);
                    }
                    if (!isApplicable(objectSpecTypes, oldObject, "object(old)")) {
                        return false;
                    }
                    PrismObject<O> newObject = odo.getNewObject();
                    if (newObject == null) {
                        throw new IllegalStateException("No new object in odo " + odo);
                    }
                    return isApplicable(objectSpecTypes, newObject, "object(new)");
                } else {
                    PrismObject<O> object = odo.getOldObject();
                    if (object == null) {
                        throw new IllegalStateException("No old object in odo " + odo);
                    }
                    return isApplicable(objectSpecTypes, object, "object(old)");
                }
            } else {
                // Old and new object should be the same. Or there is just one of them. Any one of them will do.
                PrismObject<O> object = odo.getAnyObject();
                if (object == null) {
                    throw new IllegalStateException("No object in odo " + odo);
                }
                return isApplicable(objectSpecTypes, object, "object");
            }
        } else {
            LOGGER.trace("    {}: No object specification in authorization (authorization is applicable)", getDesc());
            return true;
        }
    }

    private <O extends ObjectType> boolean isApplicable(
            List<OwnedObjectSelectorType> objectSpecTypes, PrismObject<O> object, String desc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
            if (object == null) {
                if (traceEnabled) {
                    LOGGER.trace("  Authorization is not applicable for null {}", desc);
                }
                return false;
            }
            for (OwnedObjectSelectorType autzObject : objectSpecTypes) {
                if (isApplicable(autzObject, object, emptySet(), desc)) {
                    return true;
                }
            }
            return false;
        } else {
            LOGGER.trace("    Authorization: No {} specification in authorization (authorization is applicable)", desc);
            return true;
        }
    }

    /**
     * @param otherSelfOids Which OIDs should match "self" in addition to the current principal OID. Usually these could be
     * some or all of delegators' OIDs, i.e. people that delegated privileges to the current principal.
     * The reason is that if we want to match assignee or requestor (probably targetObject and owner as well)
     * we want to give appropriate privileges also to assignee/requestor delegates.
     */
    private <O extends ObjectType> boolean isApplicable(
            SubjectedObjectSelectorType objectSelector, PrismObject<O> object, Collection<String> otherSelfOids, String desc)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectFilterExpressionEvaluator filterExpressionEvaluator = createFilterEvaluator(desc);
        if (!b.repositoryService.selectorMatches(
                objectSelector, object, filterExpressionEvaluator, LOGGER,
                "    authorization not applicable for " + desc + " because of")) {
            // No need to log inapplicability here. It should be logged inside repositoryService.selectorMatches()
            return false;
        }

        OrgRelationObjectSpecificationType specOrgRelation = objectSelector.getOrgRelation();
        RoleRelationObjectSpecificationType specRoleRelation = objectSelector.getRoleRelation();

        // Special
        List<SpecialObjectSpecificationType> specSpecial = objectSelector.getSpecial();
        if (specSpecial != null && !specSpecial.isEmpty()) {
            if (objectSelector.getFilter() != null || objectSelector.getOrgRef() != null || specOrgRelation != null || specRoleRelation != null) {
                throw new SchemaException("Both filter/org/role/archetype and special " + desc + " specification specified in " + getDesc());
            }
            for (SpecialObjectSpecificationType special : specSpecial) {
                if (special == SpecialObjectSpecificationType.SELF) {
                    String principalOid = principal != null ? principal.getOid() : null;
                    if (principalOid == null) {
                        // This is a rare case. It should not normally happen. But it may happen in tests
                        // or during initial import. Therefore we are not going to die here. Just ignore it.
                    } else {
                        if (principalOid.equals(object.getOid())) {
                            LOGGER.trace("    'self' authorization applicable for {} - match on principal OID ({})",
                                    desc, principalOid);
                            return true;
                        } else if (otherSelfOids != null && otherSelfOids.contains(object.getOid())) {
                            LOGGER.trace("    'self' authorization applicable for {} - match on other 'self OID' ({})",
                                    desc, object.getOid());
                            return true;
                        } else {
                            LOGGER.trace("    'self' authorization not applicable for {}, principal OID: {} (other accepted self OIDs: {}), {} OID {}",
                                    desc, principalOid, otherSelfOids, desc, object.getOid());
                        }
                    }
                } else {
                    throw new SchemaException("Unsupported special " + desc + " specification specified in " + getDesc() + ": " + special);
                }
            }
            LOGGER.trace("    special authorization not applicable for {}", desc);
            return false;
        } else {
            LOGGER.trace("    specials empty: {}", specSpecial);
        }

        // orgRelation
        if (specOrgRelation != null) {
            boolean match = false;
            if (principal != null) {
                for (ObjectReferenceType subjectParentOrgRef : principal.getFocus().getParentOrgRef()) {
                    if (matchesOrgRelation(object, subjectParentOrgRef, specOrgRelation)) {
                        LOGGER.trace("    org applicable for {}, object OID {} because subject org {} matches",
                                desc, object.getOid(), subjectParentOrgRef.getOid());
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                LOGGER.trace("    org not applicable for {}, object OID {} because none of the subject orgs matches",
                        desc, object.getOid());
                return false;
            }
        }

        // roleRelation
        if (specRoleRelation != null) {
            boolean match = false;
            if (principal != null) {
                for (ObjectReferenceType subjectRoleMembershipRef : principal.getFocus().getRoleMembershipRef()) {
                    if (matchesRoleRelation(object, subjectRoleMembershipRef, specRoleRelation)) {
                        LOGGER.trace("    applicable for {}, object OID {} because subject role relation {} matches",
                                desc, object.getOid(), subjectRoleMembershipRef.getOid());
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                LOGGER.trace("    not applicable for {}, object OID {} because none of the subject roles matches",
                        desc, object.getOid());
                return false;
            }
        }

        if (objectSelector instanceof OwnedObjectSelectorType) {
            OwnedObjectSelectorType ownedObjectSelector = (OwnedObjectSelectorType) objectSelector;

            // Owner
            SubjectedObjectSelectorType ownerSpec = ownedObjectSelector.getOwner();
            if (ownerSpec != null) {
                if (ownerResolver == null) {
                    LOGGER.trace("    owner object spec not applicable for {}, object OID {} because there is no owner resolver",
                            desc, object.getOid());
                    return false;
                }
                PrismObject<? extends FocusType> owner = ownerResolver.resolveOwner(object);
                if (owner == null) {
                    LOGGER.trace("    owner object spec not applicable for {}, object OID {} because it has no owner",
                            desc, object.getOid());
                    return false;
                }
                boolean ownerApplicable = isApplicable(ownerSpec, owner, emptySet(), "owner of " + desc);
                if (!ownerApplicable) {
                    LOGGER.trace("    owner object spec not applicable for {}, object OID {} because owner does not match (owner={})",
                            desc, object.getOid(), owner);
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
                    LOGGER.trace("    delegator object spec not applicable for {}, because the object is not user", desc);
                    return false;
                }
                boolean found = false;
                String principalOid = principal != null ? principal.getOid() : null;
                if (principalOid != null) {
                    for (ObjectReferenceType objectDelegatedRef : ((UserType) object.asObjectable()).getDelegatedRef()) {
                        if (principalOid.equals(objectDelegatedRef.getOid())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    if (BooleanUtils.isTrue(delegatorSpec.isAllowInactive())) {
                        for (AssignmentType objectAssignment : ((UserType) object.asObjectable()).getAssignment()) {
                            ObjectReferenceType objectAssignmentTargetRef = objectAssignment.getTargetRef();
                            if (objectAssignmentTargetRef == null) {
                                continue;
                            }
                            if (principalOid != null && principalOid.equals(objectAssignmentTargetRef.getOid())) {
                                if (b.relationRegistry.isDelegation(objectAssignmentTargetRef.getRelation())) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!found) {
                        LOGGER.trace("    delegator object spec not applicable for {}, object OID {} because delegator does not match",
                                desc, object.getOid());
                        return false;
                    }
                }
            }

            // Requestor
            SubjectedObjectSelectorType requestorSpec = ownedObjectSelector.getRequester();
            if (requestorSpec != null) {
                PrismObject<? extends ObjectType> requestor = getRequestor(object, result);
                if (requestor == null) {
                    LOGGER.trace("    requester object spec not applicable for {}, object OID {} because it has no requestor",
                            desc, object.getOid());
                    return false;
                }
                boolean requestorApplicable =
                        isApplicable(requestorSpec, requestor, getDelegatorsForRequestor(), "requestor of " + desc);
                if (!requestorApplicable) {
                    LOGGER.trace("    requester object spec not applicable for {}, object OID {} because requestor does not match (requestor={})",
                            desc, object.getOid(), requestor);
                    return false;
                }
            }

            // Requestor
            SubjectedObjectSelectorType relatedObjectSpec = ownedObjectSelector.getRelatedObject();
            if (relatedObjectSpec != null) {
                PrismObject<? extends ObjectType> relatedObject = getRelatedObject(object, result);
                if (relatedObject == null) {
                    LOGGER.trace("    related object spec not applicable for {}, object OID {} because it has no related object",
                            desc, object.getOid());
                    return false;
                }
                boolean relatedObjectApplicable = isApplicable(
                        relatedObjectSpec, relatedObject, getDelegatorsForRelatedObjects(),
                        "related object of " + desc);
                if (!relatedObjectApplicable) {
                    LOGGER.trace("    related object spec not applicable for {}, object OID {} because related object does not match (related object={})",
                            desc, object.getOid(), relatedObject);
                    return false;
                }
            }

            // Assignee
            SubjectedObjectSelectorType assigneeSpec = ownedObjectSelector.getAssignee();
            if (assigneeSpec != null) {
                List<PrismObject<? extends ObjectType>> assignees = getAssignees(object, result);
                if (assignees.isEmpty()) {
                    LOGGER.trace("    assignee spec not applicable for {}, object OID {} because it has no assignees",
                            desc, object.getOid());
                    return false;
                }
                Collection<String> relevantDelegators = getDelegatorsForAssignee();
                boolean assigneeApplicable = false;
                for (PrismObject<? extends ObjectType> assignee : assignees) {
                    if (isApplicable(assigneeSpec, assignee, relevantDelegators, "assignee of " + desc)) {
                        assigneeApplicable = true;
                        break;
                    }
                }
                if (!assigneeApplicable) {
                    LOGGER.trace("    assignee spec not applicable for {}, object OID {} because none of the assignees match (assignees={})",
                            desc, object.getOid(), assignees);
                    return false;
                }
            }

            // Tenant
            TenantSelectorType tenantSpec = ownedObjectSelector.getTenant();
            if (tenantSpec != null) {
                if (BooleanUtils.isTrue(tenantSpec.isSameAsSubject())) {
                    ObjectReferenceType subjectTenantRef = principal != null ? principal.getFocus().getTenantRef() : null;
                    if (subjectTenantRef == null || subjectTenantRef.getOid() == null) {
                        LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because subject does not have tenantRef",
                                desc, object.getOid());
                        return false;
                    }
                    ObjectReferenceType objectTenantRef = object.asObjectable().getTenantRef();
                    if (objectTenantRef == null || objectTenantRef.getOid() == null) {
                        LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because object does not have tenantRef",
                                desc, object.getOid());
                        return false;
                    }
                    if (!subjectTenantRef.getOid().equals(objectTenantRef.getOid())) {
                        LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because of tenant mismatch",
                                desc, object.getOid());
                        return false;
                    }
                    if (!BooleanUtils.isTrue(tenantSpec.isIncludeTenantOrg())) {
                        O objectType = object.asObjectable();
                        if (objectType instanceof OrgType) {
                            if (BooleanUtils.isTrue(((OrgType) objectType).isTenant())) {
                                LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because it is a tenant org and it is not included",
                                        desc, object.getOid());
                                return false;
                            }
                        }
                    }
                } else {
                    LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because there is a strange tenant specification in authorization",
                            desc, object.getOid());
                    return false;
                }
            }
        }

        LOGGER.trace("    {} applicable for {} (filter)", getDesc(), desc);
        return true;
    }

    <T extends ObjectType> boolean isApplicableToObject(PrismObject<T> object)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (isApplicable(authorization.getObject(), object, "object")) {
            LOGGER.trace("    applicable for object {} (continuing evaluation)", object);
            return true;
        } else {
            LOGGER.trace("    not applicable for object {}, none of the object specifications match (breaking evaluation)",
                    object);
            return false;
        }
    }

    <T extends ObjectType> boolean isApplicableToTarget(PrismObject<T> target)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (isApplicable(authorization.getTarget(), target, "target")) {
            LOGGER.trace("    applicable for target {} (continuing evaluation)", target);
            return true;
        } else {
            LOGGER.trace("    not applicable for target {}, none of the target specifications match (breaking evaluation)",
                    target);
            return false;
        }
    }

    <O extends ObjectType> boolean isApplicableItem(PrismObject<O> object, ObjectDelta<O> delta) throws SchemaException {
        List<ItemPathType> itemPaths = authorization.getItem();
        if (itemPaths.isEmpty()) {
            List<ItemPathType> exceptItems = authorization.getExceptItem();
            if (exceptItems.isEmpty()) {
                // No item constraints. Applicable for all items.
                LOGGER.trace("  items empty");
                return true;
            } else {
                return isApplicableItem(object, delta, exceptItems, false);
            }
        } else {
            return isApplicableItem(object, delta, itemPaths, true);
        }
    }

    private static <O extends ObjectType> boolean isApplicableItem(
            PrismObject<O> object, ObjectDelta<O> delta, List<ItemPathType> itemPaths, boolean positive)
            throws SchemaException {
        for (ItemPathType itemPathType : itemPaths) {
            ItemPath itemPath = itemPathType.getItemPath();
            if (delta == null) {
                if (object != null) {
                    if (object.containsItem(itemPath, false)) {
                        if (positive) {
                            LOGGER.trace("  applicable object item {}", itemPath);
                            return true;
                        } else {
                            LOGGER.trace("  excluded object item {}", itemPath);
                            return false;
                        }
                    }
                }
            } else {
                ItemDelta<?, ?> itemDelta = delta.findItemDelta(itemPath);
                if (itemDelta != null && !itemDelta.isEmpty()) {
                    if (positive) {
                        LOGGER.trace("  applicable delta item {}", itemPath);
                        return true;
                    } else {
                        LOGGER.trace("  excluded delta item {}", itemPath);
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

    private <O extends ObjectType> boolean matchesOrgRelation(
            PrismObject<O> object, ObjectReferenceType subjectParentOrgRef, OrgRelationObjectSpecificationType specOrgRelation)
            throws SchemaException {
        if (!b.prismContext.relationMatches(specOrgRelation.getSubjectRelation(), subjectParentOrgRef.getRelation())) {
            return false;
        }
        if (BooleanUtils.isTrue(specOrgRelation.isIncludeReferenceOrg()) && subjectParentOrgRef.getOid().equals(object.getOid())) {
            return true;
        }
        if (specOrgRelation.getScope() == null) {
            return b.repositoryService.isDescendant(object, subjectParentOrgRef.getOid());
        }
        switch (specOrgRelation.getScope()) {
            case ALL_DESCENDANTS:
                return b.repositoryService.isDescendant(object, subjectParentOrgRef.getOid());
            case DIRECT_DESCENDANTS:
                return hasParentOrgRef(object, subjectParentOrgRef.getOid());
            case ALL_ANCESTORS:
                return b.repositoryService.isAncestor(object, subjectParentOrgRef.getOid());
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
        if (!b.prismContext.relationMatches(specRoleRelation.getSubjectRelation(), subjectRoleMembershipRef.getRelation())) {
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
                if (!b.prismContext.relationMatches(specRoleRelation.getObjectRelation(), objectRoleMembershipRef.getRelation())) {
                    continue;
                }
                return true;
            }
        }
        return false;
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
                    b.prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()) : UserType.class;
            try {
                return b.repositoryService.getObject(type, ref.getOid(), null, result);
            } catch (ObjectNotFoundException | SchemaException e) {
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't resolve {} of {}", e, referenceName, object);
                return null;
            }
        } else {
            return null;
        }
    }
}
