/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.util.AccessCertificationCaseId;

import com.evolveum.midpoint.schema.util.AccessCertificationWorkItemId;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import org.jetbrains.annotations.NotNull;
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
        final ObjectQuery queryWithOwnerIdFilter = QueryUtils.addFilter(query,
                prismContext.queryFor(AccessCertificationCaseType.class)
                        .ownerId(campaignOid)
                        .buildFilter());
        return repositoryService.searchContainers(AccessCertificationCaseType.class, queryWithOwnerIdFilter, null,
                result);
    }

    public List<AccessCertificationCaseType> getAllCurrentIterationCases(
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

    /**
     * Search all campaign cases iteratively.
     *
     * **Note:** In some cases (when backend does not support it), the search may be done non iteratively, using just
     * one select query with limit of 10 000 records.
     *
     * @param campaignOid oid of campaign in to which cases belongs
     * @param filter additional filter with more filtering criteria
     * @param certCaseHandler handler used to process returned cases
     * @param result parent operational result
     * @throws SchemaException when exception occurs during the read from database
     */
    public void iterateAllCampaignCases(String campaignOid, ObjectFilter filter,
            ObjectHandler<AccessCertificationCaseType> certCaseHandler, OperationResult result) throws SchemaException {

        final ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                .ownerId(campaignOid)
                .and().filter(filter)
                .build();
        if (this.repositoryService.isNative()) {
            this.repositoryService.searchContainersIterative(AccessCertificationCaseType.class, query, certCaseHandler,
                    Collections.emptyList(), result);
        } else {
            final SearchResultList<AccessCertificationCaseType> certCases = this.repositoryService.searchContainers(
                    AccessCertificationCaseType.class, query, Collections.emptyList(), result);
            certCases.forEach(aCase -> certCaseHandler.handle(aCase, result));
        }
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

    Map<String, List<AccessCertificationCaseType>> getOpenedCasesMappedToReviewers(String campaignOid,
            boolean onlyCasesWithoutReviewerAnswer, OperationResult result) throws SchemaException {
        final Map<String, List<AccessCertificationCaseType>> reviewerCases = new HashMap<>();
        final boolean allCases = !onlyCasesWithoutReviewerAnswer;
        final ObjectHandler<AccessCertificationCaseType> casesHandler = (aCase, parentResult) -> {
            aCase.getWorkItem().stream()
                    .filter(workItem -> allCases || WorkItemTypeUtil.isWithoutOutcome(workItem))
                    .flatMap(workItem -> workItem.getAssigneeRef().stream())
                    .map(AbstractReferencable::getOid)
                    .forEach(reviewerOid -> reviewerCases.computeIfAbsent(reviewerOid, key -> new ArrayList<>())
                            .add(aCase));
            return true;
        };

        iterateAllCampaignCases(campaignOid, filterForCasesWithOpenedWorkItems(), casesHandler, result);
        return reviewerCases;
    }

    @NotNull WorkItemInContext getWorkItemInContext(AccessCertificationWorkItemId workItemId, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        AccessCertificationCaseId caseId = workItemId.caseId();
        AccessCertificationCaseType aCase = getCase(caseId, result);
        if (aCase == null) {
            throw new ObjectNotFoundException(
                    "Case " + caseId + " was not found",
                    AccessCertificationCaseType.class,
                    caseId.toString());
        }
        AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(aCase);
        if (campaign == null) {
            throw new IllegalStateException("No owning campaign present in case " + aCase);
        }
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(aCase, workItemId.workItemId());
        if (workItem == null) {
            throw new ObjectNotFoundException(
                    "Work item %d was not found in campaign %s, case %s"
                            .formatted(workItemId.workItemId(), toShortString(campaign), caseId),
                    AccessCertificationWorkItemType.class,
                    workItemId.toString());
        }
        return new WorkItemInContext(campaign, aCase, workItem);
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

    private AccessCertificationCaseType getCase(
            @NotNull AccessCertificationCaseId caseId, OperationResult result) throws SchemaException {
        QueryFactory queryFactory = prismContext.queryFactory();
        ObjectFilter filter = queryFactory.createAnd(
                queryFactory.createOwnerHasOidIn(caseId.campaignOid()),
                queryFactory.createInOid(String.valueOf(caseId.caseId()))
        );
        ObjectQuery query = queryFactory.createQuery(filter);

        List<AccessCertificationCaseType> caseList =
                repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
        if (caseList.isEmpty()) {
            return null;
        } else if (caseList.size() == 1) {
            return caseList.get(0);
        } else {
            throw new IllegalStateException("More than one certification case with ID " + caseId);
        }
    }

    private ObjectFilter filterForCasesWithOpenedWorkItems() {
        return this.prismContext.queryFor(AccessCertificationCaseType.class)
                .exists(F_WORK_ITEM)
                .block()
                .item(F_CLOSE_TIMESTAMP).isNull()
                .endBlock()
                .buildFilter();
    }
}
