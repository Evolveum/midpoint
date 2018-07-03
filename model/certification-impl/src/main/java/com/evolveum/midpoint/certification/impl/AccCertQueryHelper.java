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
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType.F_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.*;

/**
 * @author mederly
 */
@Component
public class AccCertQueryHelper {

    @SuppressWarnings("unused")
    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertQueryHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired protected AccCertGeneralHelper helper;
	@Autowired @Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

    // public because of certification tests
    public List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        InOidFilter campaignFilter = InOidFilter.createOwnerHasOidIn(campaignOid);
	    ObjectQuery newQuery = addFilter(query, campaignFilter);
		return repositoryService.searchContainers(AccessCertificationCaseType.class, newQuery, options, result);
    }

    public List<AccessCertificationCaseType> getAllCurrentIterationCases(String campaignOid, int iteration,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
	    ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
			    .ownerId(campaignOid)
			    .and().item(AccessCertificationCaseType.F_ITERATION).eq(iteration)
			    .build();
		return repositoryService.searchContainers(AccessCertificationCaseType.class, query, options, result);
    }

    private ObjectQuery addFilter(ObjectQuery query, ObjectFilter additionalFilter) {
        ObjectQuery newQuery;
        if (query == null) {
            newQuery = ObjectQuery.createObjectQuery(additionalFilter);
        } else {
            newQuery = query.clone();
            if (query.getFilter() == null) {
                newQuery.setFilter(additionalFilter);
            } else {
                newQuery.setFilter(AndFilter.createAnd(query.getFilter(), additionalFilter));
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

	private ObjectQuery createQueryForOpenWorkItems(ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal,
			boolean notDecidedOnly) throws SchemaException {
		ObjectFilter reviewerAndEnabledFilter = getReviewerAndEnabledFilterForWI(principal);

		ObjectFilter filter;
		if (notDecidedOnly) {
			ObjectFilter noResponseFilter = QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
					.item(F_OUTPUT, F_OUTCOME).isNull()
					.buildFilter();
			filter = AndFilter.createAnd(reviewerAndEnabledFilter, noResponseFilter);
		} else {
			filter = reviewerAndEnabledFilter;
		}
		return addFilter(baseWorkItemsQuery, filter);
	}

    // principal == null => take all work items
	int countOpenWorkItems(ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal,
			boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
			throws SchemaException {
		ObjectQuery newQuery = createQueryForOpenWorkItems(baseWorkItemsQuery, principal, notDecidedOnly);
        return repositoryService.countContainers(AccessCertificationWorkItemType.class, newQuery, options, result);
    }

    private ObjectFilter getReviewerAndEnabledFilter(String reviewerOid) {
        return QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
					.exists(F_WORK_ITEM)
					.block()
						.item(F_ASSIGNEE_REF).ref(reviewerOid, UserType.COMPLEX_TYPE)
						.and().item(F_CLOSE_TIMESTAMP).isNull()
					.endBlock()
                    .buildFilter();
    }

    private ObjectFilter getReviewerAndEnabledFilterForWI(MidPointPrincipal principal) throws SchemaException {
        if (principal != null) {
			return QueryUtils.filterForAssignees(
						QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext),
						principal,
						OtherPrivilegesLimitationType.F_CERTIFICATION_WORK_ITEMS)
					.and().item(F_CLOSE_TIMESTAMP).isNull()
					.buildFilter();
        } else {
            return QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
                    .item(F_CLOSE_TIMESTAMP).isNull()
                    .buildFilter();
        }
    }

    // TODO get work items for reviewer/campaign
    List<AccessCertificationCaseType> getOpenCasesForReviewer(AccessCertificationCampaignType campaign,
		    String reviewerOid, OperationResult result) throws SchemaException {
	    // note: this is OK w.r.t. iterations, as we are looking for cases with non-closed work items here
        ObjectFilter filter = getReviewerAndEnabledFilter(reviewerOid);
		return searchCases(campaign.getOid(), ObjectQuery.createObjectQuery(filter), null, result);
    }

    public AccessCertificationCaseType getCase(String campaignOid, long caseId, @SuppressWarnings("unused") Task task,
		    OperationResult result) throws SchemaException {
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
}
