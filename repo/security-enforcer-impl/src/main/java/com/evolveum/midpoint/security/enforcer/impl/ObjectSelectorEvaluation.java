/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.impl.clauses.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Treats given {@link SubjectedObjectSelectorType} or {@link OwnedObjectSelectorType}: evaluates its applicability
 * or produces security filters based on it.
 *
 * Instantiated and used as part of an {@link EnforcerOperation}.
 */
class ObjectSelectorEvaluation<S extends SubjectedObjectSelectorType>
        implements ClauseEvaluationContext {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull final S selector;
    @Nullable private final PrismValue value;
    @NotNull private final Collection<String> otherSelfOids;
    @NotNull private final String desc;
    @NotNull final AuthorizationEvaluation authorizationEvaluation;
    @NotNull final EnforcerOperation<?> enforcerOp;
    @NotNull final Beans b;
    @NotNull private final OperationResult result;

    ObjectSelectorEvaluation(
            @NotNull S selector,
            @Nullable PrismValue value,
            @NotNull Collection<String> otherSelfOids,
            @NotNull String desc,
            @NotNull AuthorizationEvaluation authorizationEvaluation,
            @NotNull OperationResult result) {
        this.selector = selector;
        this.value = value;
        this.otherSelfOids = otherSelfOids;
        this.desc = desc;
        this.authorizationEvaluation = authorizationEvaluation;
        this.enforcerOp = authorizationEvaluation.op;
        this.b = enforcerOp.b;
        this.result = result;
    }

    boolean isSelectorApplicable()
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        assert value != null; // TODO sure?

        if (!(value instanceof PrismObjectValue<?>)) {
            // FIXME TEMPORARY HACK
            return new ValueSelectorEvaluation(value, selector, authorizationEvaluation)
                    .matches();
        }

        PrismObject<? extends ObjectType> object = asObject(value);

        ObjectFilterExpressionEvaluator filterExpressionEvaluator = authorizationEvaluation.createFilterEvaluator(desc);
        if (!b.repositoryService.selectorMatches(
                selector, object, filterExpressionEvaluator, LOGGER,
                "    authorization not applicable for " + desc + " because of")) {
            // No need to log inapplicability here. It should be logged inside repositoryService.selectorMatches()
            return false;
        }

        List<SpecialObjectSpecificationType> specSpecial = selector.getSpecial();
        OrgRelationObjectSpecificationType specOrgRelation = selector.getOrgRelation();
        RoleRelationObjectSpecificationType specRoleRelation = selector.getRoleRelation();

        if (!specSpecial.isEmpty()) {
            if (selector.getFilter() != null
                    || selector.getOrgRef() != null
                    || specOrgRelation != null
                    || specRoleRelation != null) {
                throw new SchemaException(String.format(
                        "Both filter/org/role/archetype and special %s specification specified in %s", desc, getAutzDesc()));
            }
            // As the "special" declaration is exclusive, we may return here immediately
            return new Special(specSpecial, this)
                    .isApplicable(object);
        }

        if (specOrgRelation != null) {
            if (!new OrgRelation(specOrgRelation, this).isApplicable(object)) {
                return false;
            }
        }

        // roleRelation
        if (specRoleRelation != null) {
            if (!new RoleRelation(specRoleRelation, this).isApplicable(object)) {
                return false;
            }
        }

        if (selector instanceof OwnedObjectSelectorType) {
            OwnedObjectSelectorType ownedObjectSelector = (OwnedObjectSelectorType) selector;

            SubjectedObjectSelectorType ownerSelector = ownedObjectSelector.getOwner();
            if (ownerSelector != null) {
                if (!new Owner(ownerSelector, this).isApplicable(object)) {
                    return false;
                }
            }

            // Delegator
            SubjectedObjectSelectorType delegatorSpec = ownedObjectSelector.getDelegator();
            if (delegatorSpec != null) {
                if (!new Delegator(delegatorSpec, this).isApplicable(object)) {
                    return false;
                }
            }

            // Requestor
            SubjectedObjectSelectorType requestorSpec = ownedObjectSelector.getRequester();
            if (requestorSpec != null) {
                if (!new Requester(requestorSpec, this).isApplicable(object)) {
                    return false;
                }
            }

            // Related object
            SubjectedObjectSelectorType relatedObjectSpec = ownedObjectSelector.getRelatedObject();
            if (relatedObjectSpec != null) {
                if (!new RelatedObject(relatedObjectSpec, this).isApplicable(object)) {
                    return false;
                }
            }

            // Assignee
            SubjectedObjectSelectorType assigneeSpec = ownedObjectSelector.getAssignee();
            if (assigneeSpec != null) {
                if (!new Assignee(assigneeSpec, this).isApplicable(object)) {
                    return false;
                }
            }

            // Tenant
            TenantSelectorType tenantSpec = ownedObjectSelector.getTenant();
            if (tenantSpec != null) {
                if (!new Tenant(tenantSpec, this).isApplicable(object)) {
                    return false;
                }
            }
        }

        LOGGER.trace("    {} applicable for {} (filter)", getAutzDesc(), desc);
        return true;
    }

    private @NotNull PrismObject<? extends ObjectType> asObject(PrismValue value) {
        //noinspection unchecked
        return ((PrismObjectValue<? extends ObjectType>) value).asPrismObject();
    }

    @Override
    public String getPrincipalOid() {
        return enforcerOp.getPrincipalOid();
    }

    @Override
    public FocusType getPrincipalFocus() {
        return enforcerOp.getPrincipalFocus();
    }

    @Override
    public Object getDesc() {
        return desc;
    }

    @Override
    public @NotNull Collection<String> getOtherSelfOids() {
        return otherSelfOids;
    }

    public String getAutzDesc() {
        return authorizationEvaluation.getDesc();
    }

    @Override
    public @Nullable OwnerResolver getOwnerResolver() {
        return enforcerOp.ownerResolver;
    }

    @Override
    public boolean isSelectorApplicable(
            @NotNull SubjectedObjectSelectorType selector,
            @Nullable PrismObject<? extends ObjectType> object,
            @NotNull Collection<String> otherSelfOids,
            @NotNull String desc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return authorizationEvaluation.isSelectorApplicable(selector, object, otherSelfOids, desc);
    }

    @Override
    public Collection<String> getDelegatorsForAssignee() {
        return enforcerOp.getDelegatorsForAssignee();
    }

    @Override
    public Collection<String> getDelegatorsForRelatedObjects() {
        return enforcerOp.getDelegatorsForRelatedObjects();
    }

    @Override
    public Collection<String> getDelegatorsForRequestor() {
        return enforcerOp.getDelegatorsForRequestor();
    }

    @Override
    public String[] getSelfAndOtherOids(Collection<String> otherOids) {
        return enforcerOp.getSelfAndOtherOids(otherOids);
    }

    @Override
    public @NotNull RepositoryService getRepositoryService() {
        return b.repositoryService;
    }

    /** TODO */
    public PrismObject<? extends ObjectType> resolveReference(
            ObjectReferenceType ref, PrismObject<? extends ObjectType> object, String referenceName) {
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
