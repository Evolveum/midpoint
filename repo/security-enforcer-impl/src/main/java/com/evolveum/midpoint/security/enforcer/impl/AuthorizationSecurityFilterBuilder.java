/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.traceFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

class AuthorizationSecurityFilterBuilder<T extends ObjectType> extends AuthorizationProcessor {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull private final Class<T> objectType;
    @NotNull private final List<OwnedObjectSelectorType> objectSpecTypes;
    @NotNull private final String objectTargetSpec;
    private final boolean includeSpecial;
    @NotNull private final QueryAutzItemPaths queryItemsSpec;
    @Nullable private final ObjectFilter origFilter;
    private ObjectFilter autzObjSecurityFilter = null;
    private boolean applicable = true;

    AuthorizationSecurityFilterBuilder(
            @Nullable MidPointPrincipal principal,
            @NotNull Class<T> objectType,
            @NotNull Authorization authorization,
            @NotNull List<OwnedObjectSelectorType> objectSelectors,
            @NotNull String selectorLabel,
            boolean includeSpecial,
            @NotNull QueryAutzItemPaths queryItemsSpec,
            @Nullable ObjectFilter origFilter,
            @NotNull Beans beans,
            @NotNull Task task,
            @NotNull OperationResult result) {
        super(authorization, principal, null, beans, task, result);
        this.objectType = objectType;
        this.includeSpecial = includeSpecial;
        this.queryItemsSpec = queryItemsSpec;
        this.origFilter = origFilter;
        this.objectSpecTypes = objectSelectors;
        this.objectTargetSpec = selectorLabel;
    }

    public void build()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var prismContext = PrismContext.get();

        applicable = true;
        if (objectSpecTypes.isEmpty()) {

            LOGGER.trace("      No {} specification in authorization (authorization is universally applicable)", objectTargetSpec);
            autzObjSecurityFilter = FilterCreationUtil.createAll();

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
                    specTypeQName = PrismContext.get().getSchemaRegistry().qualifyTypeName(specTypeQName);
                    PrismObjectDefinition<?> specObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(specTypeQName);
                    if (specObjectDef == null) {
                        throw new SchemaException("Unknown object type " + specTypeQName + " in " + getDesc());
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
                        objSpecSecurityFilter = applyOwnerFilterOwnerRef(TaskType.F_OWNER_REF, objSpecSecurityFilter, objectDefinition);
                    } else {
                        LOGGER.trace("      Authorization not applicable for object because it has owner specification (this is not applicable for search)");
                        continue;
                    }
                }

                // Requestor
                if (objectSpecType.getRequester() != null) {
                    if (CaseType.class.isAssignableFrom(objectType)) {
                        objSpecSecurityFilter = applyRequestorFilter(objSpecSecurityFilter);
                    } else {
                        LOGGER.trace("      Authorization not applicable for object because it has requester specification (this is not applicable for search for objects other than CaseType)");
                        continue;
                    }
                }

                // Related object
                if (objectSpecType.getRelatedObject() != null) {
                    if (CaseType.class.isAssignableFrom(objectType) || TaskType.class.isAssignableFrom(objectType)) {
                        objSpecSecurityFilter = applyRelatedObjectFilter(objectType, objSpecSecurityFilter);
                    } else {
                        LOGGER.trace("      Authorization not applicable for object because it has related object specification (this is not applicable for search for objects other than CaseType and TaskType)");
                        continue;
                    }
                }

                // Assignee
                if (objectSpecType.getAssignee() != null) {
                    if (CaseType.class.isAssignableFrom(objectType)) {
                        objSpecSecurityFilter = applyAssigneeFilter(objSpecSecurityFilter);
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
                            String principalOid = principal != null ? principal.getOid() : null;
                            specialFilter = ObjectQueryUtil.filterOr(specialFilter,
                                    prismContext.queryFactory().createInOid(principalOid));
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
                    ObjectFilter specFilter = parseAndEvaluateFilter(objectDefinition, specFilterType, objectTargetSpec);
                    if (specFilter != null) {
                        ObjectQueryUtil.assertNotRaw(specFilter, "Filter in authorization object has undefined items."
                                + " Maybe a 'type' specification is missing in the authorization?");
                        ObjectQueryUtil.assertPropertyOnly(specFilter, "Filter in authorization object is not property-only filter");
                    }
                    LOGGER.trace("      applying property filter {}", specFilter);
                    objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, specFilter);
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
                        archsFilter = ObjectQueryUtil.filterOr(archsFilter, archFilter);
                    }
                    objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, archsFilter);
                    LOGGER.trace("      applying archetype filter {}", archsFilter);
                } else {
                    LOGGER.trace("      archetype empty");
                }

                // Org
                if (specOrgRef != null) {
                    ObjectFilter orgFilter = prismContext.queryFor(ObjectType.class)
                            .isChildOf(specOrgRef.getOid()).buildFilter();
                    objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, orgFilter);
                    LOGGER.trace("      applying org filter {}", orgFilter);
                } else {
                    LOGGER.trace("      org empty");
                }

                // orgRelation
                if (specOrgRelation != null) {
                    ObjectFilter objSpecOrgRelationFilter = null;
                    QName subjectRelation = specOrgRelation.getSubjectRelation();
                    if (principal != null) {
                        for (ObjectReferenceType subjectParentOrgRef : principal.getFocus().getParentOrgRef()) {
                            if (prismContext.relationMatches(subjectRelation, subjectParentOrgRef.getRelation())) {
                                S_FilterEntryOrEmpty q = prismContext.queryFor(ObjectType.class);
                                S_FilterExit q2;
                                OrgScopeType scope = specOrgRelation.getScope();
                                if (scope == null || scope == OrgScopeType.ALL_DESCENDANTS) {
                                    q2 = q.isChildOf(subjectParentOrgRef.getOid());
                                } else if (scope == OrgScopeType.DIRECT_DESCENDANTS) {
                                    q2 = q.isDirectChildOf(subjectParentOrgRef.getOid());
                                } else if (scope == OrgScopeType.ALL_ANCESTORS) {
                                    q2 = q.isParentOf(subjectParentOrgRef.getOid());
                                } else {
                                    throw new UnsupportedOperationException("Unknown orgRelation scope " + scope);
                                }
                                if (BooleanUtils.isTrue(specOrgRelation.isIncludeReferenceOrg())) {
                                    q2 = q2.or().id(subjectParentOrgRef.getOid());
                                }
                                objSpecOrgRelationFilter = ObjectQueryUtil.filterOr(objSpecOrgRelationFilter, q2.buildFilter());
                            }
                        }
                    }
                    if (objSpecOrgRelationFilter == null) {
                        objSpecOrgRelationFilter = FilterCreationUtil.createNone();
                    }
                    objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecOrgRelationFilter);
                    LOGGER.trace("      applying orgRelation filter {}", objSpecOrgRelationFilter);
                } else {
                    LOGGER.trace("      orgRelation empty");
                }

                // roleRelation
                if (specRoleRelation != null) {
                    ObjectFilter objSpecRoleRelationFilter = processRoleRelationFilter(
                            principal, authorization, specRoleRelation, origFilter);
                    if (objSpecRoleRelationFilter == null) {
                        if (authorization.maySkipOnSearch()) {
                            LOGGER.trace("      not applying roleRelation filter {} because it is not efficient and maySkipOnSearch is set", objSpecRoleRelationFilter);
                            applicable = false;
                        } else {
                            objSpecRoleRelationFilter = FilterCreationUtil.createNone();
                        }
                    }
                    if (objSpecRoleRelationFilter != null) {
                        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecRoleRelationFilter);
                        LOGGER.trace("      applying roleRelation filter {}", objSpecRoleRelationFilter);
                    }
                } else {
                    LOGGER.trace("      roleRelation empty");
                }

                // tenant
                if (specTenant != null) {
                    ObjectFilter objSpecTenantFilter = processTenantFilter(principal, specTenant);
                    if (objSpecTenantFilter == null) {
                        if (authorization.maySkipOnSearch()) {
                            LOGGER.trace("      not applying tenant filter {} because it is not efficient and maySkipOnSearch is set", objSpecTenantFilter);
                            applicable = false;
                        } else {
                            objSpecTenantFilter = FilterCreationUtil.createNone();
                        }
                    }
                    if (objSpecTenantFilter != null) {
                        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecTenantFilter);
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
                autzObjSecurityFilter = ObjectQueryUtil.filterOr(autzObjSecurityFilter, objSpecSecurityFilter);
            }

        }
        traceFilter("autzObjSecurityFilter", authorization, autzObjSecurityFilter);
    }

    /**
     * Very rudimentary and experimental implementation.
     */
    private ObjectFilter processRoleRelationFilter(MidPointPrincipal principal, Authorization autz,
            RoleRelationObjectSpecificationType specRoleRelation, ObjectFilter origFilter) {
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
                    if (!b.prismContext.relationMatches(subjectRelation, subjectRoleMembershipRef.getRelation())) {
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
                    membersFilter = FilterCreationUtil.createAll();
                } else {
                    membersFilter = FilterCreationUtil.createNone();
                }
            }
        }

        return ObjectQueryUtil.filterOr(refRoleFilter, membersFilter);
    }

    private ObjectFilter processTenantFilter(MidPointPrincipal principal, TenantSelectorType specTenant) {
        ObjectFilter tenantFilter;
        if (BooleanUtils.isTrue(specTenant.isSameAsSubject())) {
            ObjectReferenceType subjectTenantRef = principal.getFocus().getTenantRef();
            if (subjectTenantRef == null || subjectTenantRef.getOid() == null) {
                LOGGER.trace("    subject tenant empty (none filter)");
                tenantFilter = FilterCreationUtil.createNone();
            } else {
                tenantFilter = b.prismContext.queryFor(ObjectType.class)
                        .item(ObjectType.F_TENANT_REF).ref(subjectTenantRef.getOid())
                        .buildFilter();
            }
            if (!BooleanUtils.isTrue(specTenant.isIncludeTenantOrg())) {
                ObjectFilter notTenantFilter = b.prismContext.queryFor(ObjectType.class)
                        .not()
                        .type(OrgType.class)
                        .item(OrgType.F_TENANT).eq(true)
                        .buildFilter();
                tenantFilter = ObjectQueryUtil.filterAnd(tenantFilter, notTenantFilter);
            }
            LOGGER.trace("    applying tenant filter {}", tenantFilter);
        } else {
            tenantFilter = FilterCreationUtil.createNone();
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

    private ObjectFilter applyOwnerFilterOwnerRef(ItemPath ownerRefPath,
            ObjectFilter objSpecSecurityFilter, PrismObjectDefinition<T> objectDefinition) {
        PrismReferenceDefinition ownerRefDef = objectDefinition.findReferenceDefinition(ownerRefPath);
        S_FilterExit builder = b.prismContext.queryFor(AbstractRoleType.class)
                .item(ownerRefPath, ownerRefDef).ref(principal.getFocus().getOid());
        // TODO don't understand this code
        for (ObjectReferenceType subjectParentOrgRef : principal.getFocus().getParentOrgRef()) {
            if (b.prismContext.isDefaultRelation(subjectParentOrgRef.getRelation())) {
                builder = builder.or().item(ownerRefPath, ownerRefDef).ref(subjectParentOrgRef.getOid());
            }
        }
        ObjectFilter objSpecOwnerFilter = builder.buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecOwnerFilter);
        LOGGER.trace("  applying owner filter {}", objSpecOwnerFilter);
        return objSpecSecurityFilter;
    }

    private ObjectFilter applyRequestorFilter(ObjectFilter objSpecSecurityFilter) {
        ObjectFilter filter = b.prismContext.queryFor(CaseType.class)
                .item(CaseType.F_REQUESTOR_REF).ref(getSelfAndOtherOids(getDelegatorsForRequestor()))
                .buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, filter);
        LOGGER.trace("  applying requestor filter {}", filter);
        return objSpecSecurityFilter;
    }

    private ObjectFilter applyRelatedObjectFilter(Class<? extends ObjectType> objectType, ObjectFilter objSpecSecurityFilter) {
        // we assume CaseType.F_OBJECT_REF == TaskType.F_OBJECT_REF here
        ObjectFilter filter = b.prismContext.queryFor(objectType)
                .item(CaseType.F_OBJECT_REF).ref(getSelfAndOtherOids(getDelegatorsForRelatedObjects()))
                .buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, filter);
        LOGGER.trace("  applying related object filter {}", filter);
        return objSpecSecurityFilter;
    }

    private ObjectFilter applyAssigneeFilter(ObjectFilter objSpecSecurityFilter) {
        ObjectFilter filter = b.prismContext.queryFor(CaseType.class)
                .exists(CaseType.F_WORK_ITEM)
                .block()
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull()
                .and().item(CaseWorkItemType.F_ASSIGNEE_REF)
                .ref(getSelfAndOtherOids(getDelegatorsForAssignee()))
                .endBlock()
                .buildFilter();
        objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, filter);
        LOGGER.trace("  applying assignee filter {}", filter);
        return objSpecSecurityFilter;
    }

    private String[] getSelfAndOtherOids(Collection<String> otherOids) {
        if (principal == null) {
            return new String[0];
        }
        List<String> rv = new ArrayList<>(otherOids.size() + 1);
        CollectionUtils.addIgnoreNull(rv, principal.getOid());
        rv.addAll(otherOids);
        return rv.toArray(new String[0]);
    }

    private <O extends ObjectType> ObjectFilter parseAndEvaluateFilter(
            PrismObjectDefinition<O> objectDefinition, SearchFilterType specFilterType, String objectTargetDesc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectFilter specFilter = b.prismContext.getQueryConverter().createObjectFilter(objectDefinition, specFilterType);
        if (specFilter == null) {
            return null;
        }
        ObjectFilterExpressionEvaluator filterEvaluator = createFilterEvaluator(objectTargetDesc);
        return filterEvaluator.evaluate(specFilter);
    }

    private void traceClassMatch(String message, Class<?> specObjectClass, Class<?> objectType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("      {}, authorization {}, query {}",
                    message, specObjectClass.getSimpleName(), objectType.getSimpleName());
        }
    }

    boolean isApplicable() {
        return applicable;
    }

    ObjectFilter getAutzObjSecurityFilter() {
        return autzObjSecurityFilter;
    }
}
