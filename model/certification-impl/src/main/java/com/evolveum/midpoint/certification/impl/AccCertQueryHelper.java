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
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
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

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertQueryHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired protected AccCertGeneralHelper helper;
	@Autowired @Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

    // public because of certification tests
    public List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {

        ObjectQuery newQuery;
        InOidFilter inOidFilter = InOidFilter.createOwnerHasOidIn(campaignOid);
        newQuery = replaceFilter(query, inOidFilter);

		return repositoryService.searchContainers(AccessCertificationCaseType.class, newQuery, options, result);
    }

    private ObjectQuery replaceFilter(ObjectQuery query, ObjectFilter newFilter) {
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
    // principal == null => take all work items
    public List<AccessCertificationWorkItemType> searchOpenWorkItems(ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal,
			boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		ObjectQuery newQuery = createQueryForOpenWorkItems(baseWorkItemsQuery, principal, notDecidedOnly);

		// retrieve cases, filtered
		return repositoryService.searchContainers(AccessCertificationWorkItemType.class, newQuery, options, result);
    }

	private ObjectQuery createQueryForOpenWorkItems(ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal,
			boolean notDecidedOnly) throws SchemaException {
		// enhance filter with reviewerRef + enabled
		ObjectQuery newQuery;

		ObjectFilter reviewerAndEnabledFilter = getReviewerAndEnabledFilterForWI(principal);

		ObjectFilter filterToAdd;
		if (notDecidedOnly) {
			ObjectFilter noResponseFilter = QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
					.item(F_OUTPUT, F_OUTCOME).isNull()
					.buildFilter();
			filterToAdd = AndFilter.createAnd(reviewerAndEnabledFilter, noResponseFilter);
		} else {
			filterToAdd = reviewerAndEnabledFilter;
		}
		newQuery = replaceFilter(baseWorkItemsQuery, filterToAdd);
		return newQuery;
	}

    // principal == null => take all work items
	int countOpenWorkItems(ObjectQuery baseWorkItemsQuery, MidPointPrincipal principal,
			boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
			throws SchemaException, ObjectNotFoundException {

        // enhance filter with reviewerRef + enabled
		ObjectQuery newQuery = createQueryForOpenWorkItems(baseWorkItemsQuery, principal, notDecidedOnly);

        return repositoryService.countContainers(AccessCertificationWorkItemType.class, newQuery, options, result);
    }

    private ObjectFilter getReviewerAndEnabledFilter(String reviewerOid) throws SchemaException {
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
    public List<AccessCertificationCaseType> getCasesForReviewer(AccessCertificationCampaignType campaign,
			String reviewerOid, Task task, OperationResult result) throws SchemaException {
        ObjectFilter filter = getReviewerAndEnabledFilter(reviewerOid);
		return searchCases(campaign.getOid(), ObjectQuery.createObjectQuery(filter), null, result);
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

    public List<AccessCertificationCaseType> selectOpenCasesForReviewer(List<AccessCertificationCaseType> caseList, String reviewerOid) {
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
