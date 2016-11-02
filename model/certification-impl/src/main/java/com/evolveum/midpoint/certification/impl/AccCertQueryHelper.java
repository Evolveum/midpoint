/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

/**
 * @author mederly
 */
@Component
public class AccCertQueryHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertQueryHelper.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    protected AccCertGeneralHelper helper;

    // public because of certification tests
    public List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query,
                                                            Collection<SelectorOptions<GetOperationOptions>> options,
                                                            OperationResult result) throws SchemaException {
        ObjectQuery newQuery;
        InOidFilter inOidFilter = InOidFilter.createOwnerHasOidIn(campaignOid);
        newQuery = replaceFilter(query, inOidFilter);

        List<AccessCertificationCaseType> caseList = repositoryService.searchContainers(AccessCertificationCaseType.class, newQuery, options, result);
        return caseList;
    }

    ObjectQuery replaceFilter(ObjectQuery query, ObjectFilter newFilter) {
        ObjectQuery newQuery;
        if (query == null) {
            newQuery = ObjectQuery.createObjectQuery(newFilter);
        } else {
            newQuery = query.clone();
            if (query.getFilter() == null) {
                newQuery.setFilter(newFilter);
            } else {
                newQuery.setFilter(AndFilter.createAnd(query.getFilter(), newFilter));
            }
        }
        return newQuery;
    }

    // public because of testing
    public List<AccessCertificationCaseType> searchDecisions(ObjectQuery query, String reviewerOid, boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        // enhance filter with reviewerRef + enabled
        ObjectQuery newQuery;

        PrismReferenceValue reviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER).asReferenceValue();
        ObjectFilter reviewerAndEnabledFilter = getReviewerAndEnabledFilter(reviewerOid);

        ObjectFilter filterToAdd;
        if (notDecidedOnly) {
            /*
             * This filter is intended to return all cases that do not have a decision for a current stage.
             *
             * Unfortunately, what it really says is "return all cases that have a NULL or NO_RESPONSE decision
             * for a current stage. In order to write original filter we'd need to have NOT EXISTS filter
             * that would probably require using nested SELECTs (that is not possible now, and overall, it is
             * questionable from the point of view of performance).
             *
             * So, until it's fixed, we assume that on stage opening, NULL decisions are created for all
             * cases and all reviewers.
             */
            ObjectFilter noResponseFilter = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .exists(F_DECISION)
                    .block()
                        .item(AccessCertificationDecisionType.F_REVIEWER_REF).ref(reviewerRef)
                        .and().item(F_STAGE_NUMBER).eq().item(T_PARENT, F_CURRENT_STAGE_NUMBER)
                        .and().block()
                            .item(F_RESPONSE).eq(NO_RESPONSE)
                            .or().item(F_RESPONSE).isNull()
                        .endBlock()
                    .endBlock()
                    .buildFilter();
            filterToAdd = AndFilter.createAnd(reviewerAndEnabledFilter, noResponseFilter);
        } else {
            filterToAdd = reviewerAndEnabledFilter;
        }

        newQuery = replaceFilter(query, filterToAdd);

        // retrieve cases, filtered
        List<AccessCertificationCaseType> caseList = repositoryService.searchContainers(AccessCertificationCaseType.class, newQuery, options, result);

        // campaigns already loaded
        Map<String,AccessCertificationCampaignType> campaigns = new HashMap<>();

        // remove irrelevant decisions from each case and add campaignRef
        for (AccessCertificationCaseType _case : caseList) {
            if (_case.getCampaignRef() == null) {
                LOGGER.warn("AccessCertificationCaseType {} has no campaignRef -- skipping it", _case);
                continue;
            }
            // obtain campaign object
            String campaignOid = _case.getCampaignRef().getOid();
            AccessCertificationCampaignType campaign = campaigns.get(campaignOid);
            if (campaign == null) {
                campaign = repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result).asObjectable();    // TODO error checking
                campaigns.put(campaignOid, campaign);
            }

            int stage = campaign.getStageNumber();
            Iterator<AccessCertificationDecisionType> decisionIterator = _case.getDecision().iterator();
            while (decisionIterator.hasNext()) {
                AccessCertificationDecisionType decision = decisionIterator.next();
                if (decision.getStageNumber() != stage || !decision.getReviewerRef().getOid().equals(reviewerOid)) {
                    decisionIterator.remove();
                }
            }

            PrismObject<AccessCertificationCampaignType> campaignObject = campaign.asPrismObject();
            ObjectReferenceType campaignRef = ObjectTypeUtil.createObjectRef(campaignObject);
            _case.setCampaignRef(campaignRef);
            _case.getCampaignRef().asReferenceValue().setObject(campaignObject);    // has to be done AFTER setCampaignRef in order to preserve the value!
        }

        return caseList;
    }

    private ObjectFilter getReviewerAndEnabledFilter(String reviewerOid) throws SchemaException {
        // we have to find definition ourselves, as ../state cannot be currently resolved by query builder
        return QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .item(F_CURRENT_REVIEWER_REF).ref(reviewerOid, UserType.COMPLEX_TYPE)
                    .and().item(F_CURRENT_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .and().item(T_PARENT, F_STATE).eq(IN_REVIEW_STAGE)
                    .buildFilter();
    }

    // we expect that only one decision item (the relevant one) is present
    private boolean isDecided(AccessCertificationCaseType _case) {
        if (_case.getDecision() == null || _case.getDecision().isEmpty()) {
            return false;
        }
        if (_case.getDecision().size() > 1) {
            throw new IllegalStateException("More than 1 decision in case");
        }
        AccessCertificationResponseType response = _case.getDecision().get(0).getResponse();
        return response != null && response != NO_RESPONSE;
    }

    public List<AccessCertificationCaseType> getCasesForReviewer(AccessCertificationCampaignType campaign,
                                                                 String reviewerOid, Task task, OperationResult result) throws SchemaException {

        ObjectFilter filter = getReviewerAndEnabledFilter(reviewerOid);

        List<AccessCertificationCaseType> caseList = searchCases(campaign.getOid(), ObjectQuery.createObjectQuery(filter), null, result);
        return caseList;
    }

    public AccessCertificationCaseType getCase(String campaignOid, long caseId, Task task, OperationResult result) throws SchemaException, SecurityViolationException {
        ObjectFilter filter = AndFilter.createAnd(
                InOidFilter.createOwnerHasOidIn(campaignOid),
                InOidFilter.createInOid(String.valueOf(caseId))
        );
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<AccessCertificationCaseType> caseList = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);

        if (caseList.isEmpty()) {
            return null;
        } else if (caseList.size() == 1) {
            return caseList.get(0);
        } else {
            throw new IllegalStateException("More than one certification case with ID " + caseId + " in campaign " + campaignOid);
        }
    }

    public List<AccessCertificationCaseType> selectCasesForReviewer(AccessCertificationCampaignType campaign, List<AccessCertificationCaseType> caseList, String reviewerOid) {

        List<AccessCertificationCaseType> rv = new ArrayList<>();
        for (AccessCertificationCaseType aCase : caseList) {
            if (aCase.getCurrentStageNumber() == campaign.getStageNumber()) {
                for (ObjectReferenceType reviewerRef : aCase.getCurrentReviewerRef()) {
                    if (reviewerOid.equals(reviewerRef.getOid())) {
                        rv.add(aCase.clone());
                        break;
                    }
                }
            }
        }
        return rv;
    }
}
