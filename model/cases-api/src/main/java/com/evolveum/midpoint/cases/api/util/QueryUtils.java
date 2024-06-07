/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.cases.api.util;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OtherPrivilegesLimitations;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType.F_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CLOSE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_OUTPUT;

/**
 * TODO move to more appropriate place (common for both wf and certifications)
 */
public class QueryUtils {

    /**
     * The call to {@link #filterForAssignees(S_FilterEntryOrEmpty, MidPointPrincipal, OtherPrivilegesLimitations.Type)},
     * for case management work items.
     */
    public static S_FilterExit filterForCaseAssignees(@NotNull S_FilterEntryOrEmpty q, @Nullable MidPointPrincipal principal) {
        return filterForAssignees(q, principal, OtherPrivilegesLimitations.Type.CASES);
    }

    /**
     * The call to {@link #filterForAssignees(S_FilterEntryOrEmpty, MidPointPrincipal, OtherPrivilegesLimitations.Type)},
     * for access certification work items.
     */
    public static S_FilterExit filterForCertificationAssignees(
            @NotNull S_FilterEntryOrEmpty q, @Nullable MidPointPrincipal principal) {
        return filterForAssignees(q, principal, OtherPrivilegesLimitations.Type.ACCESS_CERTIFICATION);
    }

    /**
     * Augments work item query by including filter to see only work items assigned to the current user or any of his delegators,
     * providing that the limitation(s) allow it.
     *
     * Note that work item limitations are supported only in the current (crude) form: all or none.
     */
    @SuppressWarnings("WeakerAccess") // for compatibility purposes
    public static S_FilterExit filterForAssignees(
            @NotNull S_FilterEntryOrEmpty q,
            @Nullable MidPointPrincipal principal,
            @NotNull OtherPrivilegesLimitations.Type limitationType) {
        if (principal == null) {
            return q.none();
        } else {
            return q.item(AbstractWorkItemType.F_ASSIGNEE_REF)
                    .ref(getPotentialAssigneesForUser(principal, limitationType));
        }
    }

    public static S_FilterExit filterForNotClosedStateAndAssignees(
            @NotNull S_FilterEntryOrEmpty q,
            @Nullable MidPointPrincipal principal,
            @NotNull OtherPrivilegesLimitations.Type limitationType) {
        if (principal == null) {
            return q.none();
        } else {
            return q.item(CaseWorkItemType.F_ASSIGNEE_REF)
                    .ref(getPotentialAssigneesForUser(principal, limitationType))
                    .and()
                    .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                    .isNull();
        }
    }

    public static S_FilterExit filterForClaimableItems(S_FilterEntryOrEmpty q, String userOid, RepositoryService repositoryService,
            RelationRegistry relationRegistry, OperationResult result)
            throws SchemaException {
        List<PrismReferenceValue> candidates = getCandidatesForUser(userOid, repositoryService, relationRegistry, result);
        return q.item(CaseWorkItemType.F_CANDIDATE_REF).ref(candidates)
                .and()
                .item(CaseWorkItemType.F_ASSIGNEE_REF)
                .isNull()
                .and()
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull();
    }

    private static List<PrismReferenceValue> getPotentialAssigneesForUser(
            @NotNull MidPointPrincipal principal, @NotNull OtherPrivilegesLimitations.Type limitationType) {
        // As for relations, WorkItem.assigneeRef should contain only the default ones.
        QName defaultRelation = SchemaService.get().relationRegistry().getDefaultRelation();
        List<PrismReferenceValue> rv = new ArrayList<>();
        rv.add(createUserRef(principal.getOid(), defaultRelation));
        principal.getDelegatorsFor(limitationType).forEach(
                oid -> rv.add(createUserRef(oid, defaultRelation)));
        return rv;
    }

    private static PrismReferenceValue createUserRef(String oid, QName relation) {
        return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.USER)
                .relation(relation)
                .asReferenceValue();
    }

    /**
     * Returns values to look for in candidateRef field. Basically, all the groups a user is member of should be present here.
     * The question is what to do if candidateRef points to another user or users. This case is obviously not supported yet.
     */
    private static List<PrismReferenceValue> getCandidatesForUser(String userOid, RepositoryService repositoryService,
            RelationRegistry relationRegistry, OperationResult result) throws SchemaException {
        List<PrismReferenceValue> rv = new ArrayList<>();
        UserType user;
        try {
            user = repositoryService.getObject(UserType.class, userOid, null, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            return rv;
        }
        user.getRoleMembershipRef().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .forEach(ref -> rv.add(ref.clone().asReferenceValue()));
        user.getDelegatedRef().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .filter(ref -> !QNameUtil.match(ref.getType(), UserType.COMPLEX_TYPE))   // we are not interested in deputies (but this should be treated above)
                .forEach(ref -> rv.add(ref.clone().asReferenceValue()));
        return rv;
    }

    public static S_FilterExit filterForMyRequests(S_FilterEntryOrEmpty q, String principalUserOid) {
        return q.item(CaseType.F_REQUESTOR_REF)
                .ref(principalUserOid)
                .and()
                .item(CaseType.F_ARCHETYPE_REF)
                .ref(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value());
    }

    public static S_FilterExit filterForCasesOverObject(S_FilterEntryOrEmpty q, String objectOid) {
        return q.item(CaseType.F_OBJECT_REF).ref(objectOid)
                .and()
                .item(CaseType.F_ARCHETYPE_REF)
                .ref(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value())
                .and()
                .not()
                .item(CaseType.F_STATE)
                .eq(SchemaConstants.CASE_STATE_CLOSED);
    }

    private static ObjectFilter getReviewerAndEnabledFilterForWI(MidPointPrincipal principal) {
        if (principal != null) {
            return filterForCertificationAssignees(
                        PrismContext.get().queryFor(AccessCertificationWorkItemType.class),
                        principal)
                    .and().item(F_CLOSE_TIMESTAMP).isNull()
                    .buildFilter();
        } else {
            return PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .item(F_CLOSE_TIMESTAMP).isNull()
                    .buildFilter();
        }
    }

    public static ObjectQuery addFilter(ObjectQuery query, ObjectFilter additionalFilter) {
        ObjectQuery newQuery;
        QueryFactory queryFactory = PrismContext.get().queryFactory();
        if (query == null) {
            newQuery = queryFactory.createQuery(additionalFilter);
        } else {
            newQuery = query.clone();
            if (query.getFilter() == null) {
                newQuery.setFilter(additionalFilter);
            } else {
                newQuery.setFilter(queryFactory.createAnd(query.getFilter(), additionalFilter));
            }
        }
        return newQuery;
    }

    public static ObjectQuery createQueryForOpenWorkItemsForCampaigns(
            List<String> campaignOids, MidPointPrincipal principal, boolean notDecidedOnly) {
        if (campaignOids == null || campaignOids.isEmpty()) {
            return null;
        }
        S_FilterEntry queryPrefix = PrismContext.get().queryFor(AccessCertificationWorkItemType.class).block();
        S_FilterExit filterExit = null;
        for (String oid : campaignOids) {
             filterExit = queryPrefix
                    .exists(PrismConstants.T_PARENT)
                    .ownerId(oid);
             if (campaignOids.indexOf(oid) < campaignOids.size() - 1) {
                 queryPrefix = filterExit.or();
             }
        }
        if (filterExit == null) {
            return null;
        }
        ObjectQuery query = filterExit.endBlock().build();
        return createQueryForOpenWorkItems(query, principal, notDecidedOnly);
    }

   public static ObjectQuery createQueryForOpenWorkItems(
            ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal, boolean notDecidedOnly) {
        ObjectFilter reviewerAndEnabledFilter = getReviewerAndEnabledFilterForWI(principal);

        ObjectFilter filter;
        if (notDecidedOnly) {
            ObjectFilter noResponseFilter = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .item(F_OUTPUT, F_OUTCOME).isNull()
                    .buildFilter();
            filter = PrismContext.get().queryFactory().createAnd(reviewerAndEnabledFilter, noResponseFilter);
        } else {
            filter = reviewerAndEnabledFilter;
        }
        return addFilter(baseWorkItemsQuery, filter);
    }
}
