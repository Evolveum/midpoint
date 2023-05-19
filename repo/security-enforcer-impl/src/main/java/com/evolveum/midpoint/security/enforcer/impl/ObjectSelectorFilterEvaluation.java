/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.EnforcerFilterOperation.traceFilter;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.enforcer.impl.clauses.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

class ObjectSelectorFilterEvaluation<T>
        extends ObjectSelectorEvaluation<OwnedObjectSelectorType>
        implements ClauseFilterEvaluationContext {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull private final Class<T> objectType;
    @NotNull private final Class<T> refinedType;
    @Nullable private final ObjectFilter originalFilter;
    private final String selectorLabel;

    @Nullable private final QName selectorTypeName;

    /** The result */
    private ObjectFilter securityFilter;

    /** The type filter - applied at the end. */
    private TypeFilter securityTypeFilter;

    /** TODO */
    private boolean applicable;

    ObjectSelectorFilterEvaluation(
            @NotNull OwnedObjectSelectorType selector,
            @NotNull Class<T> objectType,
            @Nullable ObjectFilter originalFilter,
            @NotNull Collection<String> otherSelfOids,
            @NotNull String desc,
            String selectorLabel,
            @NotNull AuthorizationEvaluation authorizationEvaluation,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException {
        super(selector, null, otherSelfOids, desc, authorizationEvaluation, result);
        this.selectorTypeName = PrismContext.get().getSchemaRegistry().qualifyTypeName(selector.getType());
        this.objectType = objectType;
        this.refinedType = determineRefinedType();
        this.originalFilter = originalFilter;
        this.selectorLabel = selectorLabel;
    }

    private Class<T> determineRefinedType() throws ConfigurationException {
        if (selectorTypeName != null) {
            var typeDef = configNonNull(
                    b.prismContext.getSchemaRegistry().findTypeDefinitionByType(selectorTypeName),
                    () -> "Unknown object type " + selectorTypeName + " in " + getAutzDesc());
            //noinspection unchecked
            return (Class<T>) requireNonNull(
                    typeDef.getCompileTimeClass(),
                    () -> new UnsupportedOperationException(String.format(
                            "Only statically defined types are supported in authorizations."
                                    + "Type '%s' is not statically defined; in %s", selectorTypeName, getAutzDesc())));
        } else {
            return objectType;
        }
    }

    void processFilter(boolean includeSpecial)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        SearchFilterType selectorFilter = selector.getFilter();
        ObjectReferenceType selectorOrgRef = selector.getOrgRef();
        List<ObjectReferenceType> selectorArchetypeRefs = selector.getArchetypeRef();
        OrgRelationObjectSpecificationType selectorOrgRelation = selector.getOrgRelation();
        RoleRelationObjectSpecificationType selectorRoleRelation = selector.getRoleRelation();
        TenantSelectorType selectorTenant = selector.getTenant();

        // Type (not supported by specific clause processor)
        if (selectorTypeName != null) {
            if (objectType.equals(refinedType)) {
                traceClassMatch("Object selector is applicable because of type exact match");
            } else if (!objectType.isAssignableFrom(refinedType)) {
                traceClassMatch("Object selector is not applicable because of type mismatch");
                return;
            } else {
                traceClassMatch("Object selector is applicable because of type match, adding more specific type filter");
                // The spec type is a subclass of requested type. So it might be returned from the search.
                // We need to use type filter.
                securityTypeFilter = b.prismContext.queryFactory().createType(selectorTypeName, null);
            }
        }

        var ownerSelector = selector.getOwner();
        if (ownerSelector != null) {
            if (!new Owner(ownerSelector, this).applyFilter()) {
                return;
            }
        }

        var requesterSelector = selector.getRequester();
        if (requesterSelector != null) {
            if (!new Requester(requesterSelector, this).applyFilter()) {
                return;
            }
        }

        var relatedObjectSelector = selector.getRelatedObject();
        if (relatedObjectSelector != null) {
            if (!new RelatedObject(relatedObjectSelector, this).applyFilter()) {
                return;
            }
        }

        var assigneeSelector = selector.getAssignee();
        if (assigneeSelector != null) {
            if (!new Assignee(assigneeSelector, this).applyFilter()) {
                return;
            }
        }

        var delegatorSelector = selector.getDelegator();
        if (delegatorSelector != null) {
            // TODO: MID-3899
            LOGGER.trace("      Authorization not applicable for object because it has delegator specification (this is not applicable for search)");
            return;
        }

        applicable = true;

        // Special
        List<SpecialObjectSpecificationType> selectorSpecialList = selector.getSpecial();
        if (!selectorSpecialList.isEmpty()) {
            if (!includeSpecial) {
                LOGGER.trace("      Skipping authorization, because specials are present: {}", selectorSpecialList);
                applicable = false; // TODO shouldn't we exit immediately?
            }
            if (selectorFilter != null
                    || selectorOrgRef != null
                    || selectorOrgRelation != null
                    || selectorRoleRelation != null
                    || selectorTenant != null
                    || !selectorArchetypeRefs.isEmpty()) {
                throw new SchemaException(
                        "Both filter/org/role/archetype/tenant and special object specification specified in authorization");
            }
            ObjectFilter increment = new Special(selectorSpecialList, this).createFilter();
            // TODO why do we throw away existing value of securityFilter here?
            //  and why we do not use existing type filter (objSpecTypeFilter)?
            securityFilter = selectorTypeName != null ?
                    b.prismContext.queryFactory().createType(selectorTypeName, increment) : increment;
        }

        // Filter
        if (selectorFilter != null) {
            new Filter(selectorFilter, this).applyFilter();
        } else {
            LOGGER.trace("      filter empty (assuming \"all\")");
            if (securityFilter == null) { // TODO is this necessary?
                securityFilter = b.prismContext.queryFactory().createAll();
            }
        }

        // Archetypes
        if (!selectorArchetypeRefs.isEmpty()) {
            new ArchetypeRef(selectorArchetypeRefs, this).applyFilter();
        }

        // Org ref
        if (selectorOrgRef != null) {
            new OrgRef(selectorOrgRef, this).applyFilter();
        }

        // orgRelation
        if (selectorOrgRelation != null) {
            new OrgRelation(selectorOrgRelation, this).applyFilter();
        }

        // roleRelation
        if (selectorRoleRelation != null) {
            if (!new RoleRelation(selectorRoleRelation, this).applyFilter()) {
                applicable = false;
                return;
            }
        }

        // tenant
        if (selectorTenant != null) {
            new Tenant(selectorTenant, this).applyFilter();
        }

        if (securityTypeFilter != null) {
            securityTypeFilter.setFilter(securityFilter);
            securityFilter = securityTypeFilter;
        }

        // TODO check if applicability flag can be false here
        traceFilter(enforcerOp, "for object selector (applicable: " + applicable + ")", selector, securityFilter);
    }

    private void traceClassMatch(String message) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("      {}, authorization {}, query {}",
                    message, refinedType.getSimpleName(), objectType.getSimpleName());
        }
    }

    public boolean isApplicable() {
        return applicable;
    }

    public void addConjunction(ObjectFilter increment) {
        securityFilter = ObjectQueryUtil.filterAnd(securityFilter, increment);
    }

    ObjectFilter getSecurityFilter() {
        return securityFilter;
    }

    @Override
    public @NotNull ObjectFilterExpressionEvaluator createFilterEvaluator() {
        return authorizationEvaluation.createFilterEvaluator(selectorLabel);
    }

    @Override
    public @NotNull Class<?> getRefinedType() {
        return refinedType;
    }

    @Override
    public @Nullable ObjectFilter getOriginalFilter() {
        return originalFilter;
    }

    @Override
    public boolean maySkipOnSearch() {
        return authorizationEvaluation.authorization.maySkipOnSearch();
    }
}
