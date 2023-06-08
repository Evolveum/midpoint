/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.*;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
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

    @VisibleForTesting // public because of certification tests
    public List<AccessCertificationCaseType> searchCases(
            String campaignOid, ObjectQuery query, OperationResult result) throws SchemaException {
        return repositoryService.searchContainers(
                AccessCertificationCaseType.class,
                QueryUtils.addFilter(
                        query,
                        prismContext.queryFactory().createOwnerHasOidIn(campaignOid)),
                null,
                result);
    }

    List<AccessCertificationCaseType> getAllCurrentIterationCases(
            String campaignOid, int iteration, OperationResult result) throws SchemaException {
        return repositoryService.searchContainers(
                AccessCertificationCaseType.class,
                prismContext.queryFor(AccessCertificationCaseType.class)
                        .ownerId(campaignOid)
                        .and().item(AccessCertificationCaseType.F_ITERATION).eq(iteration)
                        .build(),
                null,
                result);
    }

    List<AccessCertificationWorkItemType> searchOpenWorkItems(
            ObjectQuery baseWorkItemsQuery, boolean notDecidedOnly, OperationResult result)
            throws SchemaException {
        return repositoryService.searchContainers(
                AccessCertificationWorkItemType.class,
                QueryUtils.createQueryForOpenWorkItems(baseWorkItemsQuery, null, notDecidedOnly),
                null,
                result);
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

    // TODO get work items for reviewer/campaign
    List<AccessCertificationCaseType> getOpenCasesForReviewer(
            AccessCertificationCampaignType campaign, String reviewerOid, OperationResult result) throws SchemaException {
        // note: this is OK w.r.t. iterations, as we are looking for cases with non-closed work items here
        ObjectFilter filter = getReviewerAndEnabledFilter(reviewerOid);
        return searchCases(campaign.getOid(), prismContext.queryFactory().createQuery(filter), result);
    }

    public AccessCertificationCaseType getCase(String campaignOid, long caseId, @SuppressWarnings("unused") Task task,
            OperationResult result) throws SchemaException {
        QueryFactory queryFactory = prismContext.queryFactory();
        ObjectFilter filter = queryFactory.createAnd(
                queryFactory.createOwnerHasOidIn(campaignOid),
                queryFactory.createInOid(String.valueOf(caseId))
        );
        ObjectQuery query = queryFactory.createQuery(filter);

        List<AccessCertificationCaseType> caseList =
                repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
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
