/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType.F_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class AccCertQueryHelper {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(AccCertQueryHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired protected AccCertGeneralHelper helper;
    @Autowired @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    // public because of certification tests
    public List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        InOidFilter campaignFilter = prismContext.queryFactory().createOwnerHasOidIn(campaignOid);
        ObjectQuery newQuery = addFilter(query, campaignFilter);
        return repositoryService.searchContainers(AccessCertificationCaseType.class, newQuery, options, result);
    }

    List<AccessCertificationCaseType> getAllCurrentIterationCases(String campaignOid, int iteration,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                .ownerId(campaignOid)
                .and().item(AccessCertificationCaseType.F_ITERATION).eq(iteration)
                .build();
        return repositoryService.searchContainers(AccessCertificationCaseType.class, query, options, result);
    }

    private ObjectQuery addFilter(ObjectQuery query, ObjectFilter additionalFilter) {
        ObjectQuery newQuery;
        QueryFactory queryFactory = prismContext.queryFactory();
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

    // public because of testing
    // principal == null => take all work items
    public List<AccessCertificationWorkItemType> searchOpenWorkItems(ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal,
            boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {
        ObjectQuery newQuery = createQueryForOpenWorkItems(baseWorkItemsQuery, principal, notDecidedOnly);
        return repositoryService.searchContainers(AccessCertificationWorkItemType.class, newQuery, options, result);
    }

    private ObjectQuery createQueryForOpenWorkItems(
            ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal, boolean notDecidedOnly) {
        ObjectFilter reviewerAndEnabledFilter = getReviewerAndEnabledFilterForWI(principal);

        ObjectFilter filter;
        if (notDecidedOnly) {
            ObjectFilter noResponseFilter = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_OUTPUT, F_OUTCOME).isNull()
                    .buildFilter();
            filter = prismContext.queryFactory().createAnd(reviewerAndEnabledFilter, noResponseFilter);
        } else {
            filter = reviewerAndEnabledFilter;
        }
        return addFilter(baseWorkItemsQuery, filter);
    }

    // principal == null => take all work items
    int countOpenWorkItems(ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal,
            boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
        ObjectQuery newQuery = createQueryForOpenWorkItems(baseWorkItemsQuery, principal, notDecidedOnly);
        return repositoryService.countContainers(AccessCertificationWorkItemType.class, newQuery, options, result);
    }

    private ObjectFilter getReviewerAndEnabledFilter(String reviewerOid) {
        return prismContext.queryFor(AccessCertificationCaseType.class)
                    .exists(F_WORK_ITEM)
                    .block()
                        .item(F_ASSIGNEE_REF).ref(reviewerOid, UserType.COMPLEX_TYPE)
                        .and().item(F_CLOSE_TIMESTAMP).isNull()
                    .endBlock()
                    .buildFilter();
    }

    private ObjectFilter getReviewerAndEnabledFilterForWI(MidPointPrincipal principal) {
        if (principal != null) {
            return QueryUtils.filterForCertificationAssignees(
                        prismContext.queryFor(AccessCertificationWorkItemType.class),
                        principal)
                    .and().item(F_CLOSE_TIMESTAMP).isNull()
                    .buildFilter();
        } else {
            return prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_CLOSE_TIMESTAMP).isNull()
                    .buildFilter();
        }
    }

    // TODO get work items for reviewer/campaign
    List<AccessCertificationCaseType> getOpenCasesForReviewer(AccessCertificationCampaignType campaign,
            String reviewerOid, OperationResult result) throws SchemaException {
        // note: this is OK w.r.t. iterations, as we are looking for cases with non-closed work items here
        ObjectFilter filter = getReviewerAndEnabledFilter(reviewerOid);
        return searchCases(campaign.getOid(), prismContext.queryFactory().createQuery(filter), null, result);
    }

    public AccessCertificationCaseType getCase(String campaignOid, long caseId, @SuppressWarnings("unused") Task task,
            OperationResult result) throws SchemaException {
        QueryFactory queryFactory = prismContext.queryFactory();
        ObjectFilter filter = queryFactory.createAnd(
                queryFactory.createOwnerHasOidIn(campaignOid),
                queryFactory.createInOid(String.valueOf(caseId))
        );
        ObjectQuery query = queryFactory.createQuery(filter);

        List<AccessCertificationCaseType> caseList = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
        if (caseList.isEmpty()) {
            return null;
        } else if (caseList.size() == 1) {
            return caseList.get(0);
        } else {
            throw new IllegalStateException("More than one certification case with ID " + caseId + " in campaign " + campaignOid);
        }
    }

    List<AccessCertificationCaseType> selectOpenCasesForReviewer(List<AccessCertificationCaseType> caseList, String reviewerOid) {
        List<AccessCertificationCaseType> rv = new ArrayList<>();
        cases: for (AccessCertificationCaseType aCase : caseList) {
            for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
                if (workItem.getCloseTimestamp() == null) {
                    for (ObjectReferenceType reviewerRef : workItem.getAssigneeRef()) {
                        if (reviewerOid.equals(reviewerRef.getOid())) {
                            rv.add(aCase.clone());
                            continue cases;
                        }
                    }
                }
            }
        }
        return rv;
    }

    boolean hasNoResponseCases(String campaignOid, OperationResult result) {
        ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                .ownerId(campaignOid)
                .and().item(AccessCertificationCaseType.F_OUTCOME).eq(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
                .build();
        return repositoryService.countContainers(AccessCertificationCaseType.class, query, null, result) > 0;
    }
}
