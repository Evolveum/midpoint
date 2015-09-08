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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AccCertQueryHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertQueryHelper.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelService modelService;

    @Autowired
    private MatchingRuleRegistry matchingRuleRegistry;

    @Autowired
    protected AccCertGeneralHelper helper;

    // TODO temporary hack because of some problems in model service...
    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    protected List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectFilter filter = query != null ? query.getFilter() : null;
        ObjectPaging paging = query != null ? query.getPaging() : null;
        AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, options, task, result);
        List<AccessCertificationCaseType> caseList = getCases(campaign, filter, task, result);
        caseList = doSortingAndPaging(caseList, paging);
        return caseList;
    }

    protected List<AccessCertificationCaseType> searchDecisions(ObjectQuery campaignQuery, ObjectQuery caseQuery, String reviewerOid, boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        // enhance filter with reviewerRef
        ObjectFilter enhancedFilter;
        ObjectReferenceType reviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
        ObjectFilter reviewerFilter = RefFilter.createReferenceEqual(
                new ItemPath(AccessCertificationCaseType.F_REVIEWER_REF),
                AccessCertificationCaseType.class, prismContext, reviewerRef.asReferenceValue());
        ObjectFilter enabledFilter = EqualFilter.createEqual(
                AccessCertificationCaseType.F_ENABLED, AccessCertificationCaseType.class, prismContext, Boolean.TRUE);
        ObjectFilter andFilter = AndFilter.createAnd(reviewerFilter, enabledFilter);

        if (caseQuery == null || caseQuery.getFilter() == null) {
            enhancedFilter = andFilter;
        } else {
            enhancedFilter = AndFilter.createAnd(caseQuery.getFilter(), andFilter);
        }

        // retrieve cases, filtered
        List<PrismObject<AccessCertificationCampaignType>> campaignObjects = modelService.searchObjects(AccessCertificationCampaignType.class, campaignQuery, options, task, result);
        List<AccessCertificationCaseType> caseList = new ArrayList<>();
        for (PrismObject<AccessCertificationCampaignType> campaignObject : campaignObjects) {
            AccessCertificationCampaignType campaign = campaignObject.asObjectable();
            if (!AccessCertificationCampaignStateType.IN_REVIEW_STAGE.equals(campaign.getState())) {
                continue;
            }
            List<AccessCertificationCaseType> campaignCases = getCases(campaign, enhancedFilter, task, result);

            // remove irrelevant decisions from each case
            // and add campaignRef
            int stage = campaign.getCurrentStageNumber();
            for (AccessCertificationCaseType _case : campaignCases) {
                Iterator<AccessCertificationDecisionType> decisionIterator = _case.getDecision().iterator();
                while (decisionIterator.hasNext()) {
                    AccessCertificationDecisionType decision = decisionIterator.next();
                    if (decision.getStageNumber() != stage || !decision.getReviewerRef().getOid().equals(reviewerOid)) {
                        decisionIterator.remove();
                    }
                }

                if (!notDecidedOnly || !isDecided(_case)) {
                    ObjectReferenceType campaignRef = ObjectTypeUtil.createObjectRef(campaignObject);
                    campaignRef.asReferenceValue().setObject(campaignObject);
                    _case.setCampaignRef(campaignRef);
                    caseList.add(_case);
                }
            }
        }

        // sort and page cases
        ObjectPaging paging = caseQuery != null ? caseQuery.getPaging() : null;
        caseList = doSortingAndPaging(caseList, paging);

        return caseList;
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
        return response != null && response != AccessCertificationResponseType.NO_RESPONSE;
    }


    protected List<AccessCertificationCaseType> doSortingAndPaging(List<AccessCertificationCaseType> caseList, ObjectPaging paging) {
        if (paging == null) {
            return caseList;
        }

        // sorting
        if (paging.getOrderBy() != null) {
            Comparator<AccessCertificationCaseType> comparator = createComparator(paging.getOrderBy(), paging.getDirection());
            if (comparator != null) {
                caseList = new ArrayList<>(caseList);
                Collections.sort(caseList, comparator);
            }
        }
        // paging
        if (paging.getOffset() != null || paging.getMaxSize() != null) {
            int offset = paging.getOffset() != null ? paging.getOffset() : 0;
            int maxSize = paging.getMaxSize() != null ? paging.getMaxSize() : Integer.MAX_VALUE;
            if (offset >= caseList.size()) {
                caseList = new ArrayList<>();
            } else {
                if (maxSize > caseList.size() - offset) {
                    maxSize = caseList.size() - offset;
                }
                caseList = caseList.subList(offset, offset+maxSize);
            }
        }
        return caseList;
    }

    protected List<AccessCertificationCaseType> getCases(AccessCertificationCampaignType campaign, ObjectFilter filter, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        // temporary implementation: simply fetches the whole campaign and selects requested items by itself
        List<AccessCertificationCaseType> caseList = campaign.getCase();

        // filter items
        if (filter != null) {
            Iterator<AccessCertificationCaseType> caseIterator = caseList.iterator();
            while (caseIterator.hasNext()) {
                AccessCertificationCaseType _case = caseIterator.next();
                if (!ObjectQuery.match(_case, filter, matchingRuleRegistry)) {
                    caseIterator.remove();
                }
            }
        }

        return caseList;
    }

    /**
     * Experimental implementation: we support ordering by object object name, target object name.
     * However, there are no QNames that exactly match these options. So we map the following QNames to ordering attributes:
     *
     * F_OBJECT_REF -> ordering by object name
     * F_TARGET_REF -> ordering by target name
     *
     * The requirement is that object names were fetched as well (resolveNames option)
     *
     */
    private Comparator<AccessCertificationCaseType> createComparator(QName orderBy, OrderDirection direction) {
        if (QNameUtil.match(orderBy, AccessCertificationCaseType.F_OBJECT_REF)) {
            return createObjectNameComparator(direction);
        } else if (QNameUtil.match(orderBy, AccessCertificationCaseType.F_TARGET_REF)) {
            return createTargetNameComparator(direction);
        } else if (QNameUtil.match(orderBy, AccessCertificationCaseType.F_CAMPAIGN_REF)) {
            return createCampaignNameComparator(direction);
        } else if (QNameUtil.match(orderBy, AccessCertificationCaseType.F_REVIEW_REQUESTED_TIMESTAMP)) {
            return createReviewRequestedComparator(direction);
        } else if (QNameUtil.match(orderBy, AccessCertificationCaseType.F_REVIEW_DEADLINE)) {
            return createReviewDeadlineComparator(direction);
        } else {
            LOGGER.warn("Unsupported sorting attribute {}. Results will not be sorted.", orderBy);
            return null;
        }
    }

    private Comparator<AccessCertificationCaseType> createObjectNameComparator(final OrderDirection direction) {
        return new Comparator<AccessCertificationCaseType>() {
            @Override
            public int compare(AccessCertificationCaseType o1, AccessCertificationCaseType o2) {
                return compareRefNames(o1.getObjectRef(), o2.getObjectRef(), direction);
            }
        };
    }

    private Comparator<AccessCertificationCaseType> createTargetNameComparator(final OrderDirection direction) {
        return new Comparator<AccessCertificationCaseType>() {
            @Override
            public int compare(AccessCertificationCaseType o1, AccessCertificationCaseType o2) {
                return compareRefNames(o1.getTargetRef(), o2.getTargetRef(), direction);
            }
        };
    }

    private Comparator<AccessCertificationCaseType> createCampaignNameComparator(final OrderDirection direction) {
        return new Comparator<AccessCertificationCaseType>() {
            @Override
            public int compare(AccessCertificationCaseType o1, AccessCertificationCaseType o2) {
                AccessCertificationCampaignType c1 = getCampaign(o1);
                AccessCertificationCampaignType c2 = getCampaign(o2);
                if (c1 == null) {
                    return respectDirection(-1, direction);
                } else if (c2 == null) {
                    return respectDirection(1, direction);
                }
                int ordering = c1.getName().getNorm().compareTo(c2.getName().getNorm());
                //int ordering = c1.getName().getOrig().compareTo(c2.getName().getOrig());
                return respectDirection(ordering, direction);
            }
        };
    }

    private Comparator<AccessCertificationCaseType> createReviewRequestedComparator(final OrderDirection direction) {
        return new Comparator<AccessCertificationCaseType>() {
            @Override
            public int compare(AccessCertificationCaseType o1, AccessCertificationCaseType o2) {
                return compareDates(o1.getReviewRequestedTimestamp(), o2.getReviewRequestedTimestamp(), direction);
            }
        };
    }

    private Comparator<AccessCertificationCaseType> createReviewDeadlineComparator(final OrderDirection direction) {
        return new Comparator<AccessCertificationCaseType>() {
            @Override
            public int compare(AccessCertificationCaseType o1, AccessCertificationCaseType o2) {
                return compareDates(o1.getReviewDeadline(), o2.getReviewDeadline(), direction);
            }
        };
    }

    private int compareDates(XMLGregorianCalendar d1, XMLGregorianCalendar d2, OrderDirection direction) {
        if (d1 == null) {
            return respectDirection(1, direction);          // null is later
        } else if (d2 == null) {
            return respectDirection(-1, direction);
        } else {
            return respectDirection(d1.compare(d2), direction);
        }
    }


    private AccessCertificationCampaignType getCampaign(AccessCertificationCaseType c) {
        if (c == null || c.getCampaignRef() == null || c.getCampaignRef().asReferenceValue().getObject() == null) {
            return null;
        }
        return (AccessCertificationCampaignType) c.getCampaignRef().asReferenceValue().getObject().asObjectable();
    }


    private int compareRefNames(ObjectReferenceType leftRef, ObjectReferenceType rightRef, OrderDirection direction) {
        if (leftRef == null) {
            return respectDirection(1, direction);      // null > anything
        } else if (rightRef == null) {
            return respectDirection(-1, direction);     // anything < null
        }

        // brutal hack - we (mis)use the fact that names are serialized as XNode comments
//        String leftName = (String) leftRef.asReferenceValue().getUserData(XNodeSerializer.USER_DATA_KEY_COMMENT);
//        String rightName = (String) rightRef.asReferenceValue().getUserData(XNodeSerializer.USER_DATA_KEY_COMMENT);
        String leftName = leftRef.asReferenceValue().getTargetName() != null ? leftRef.asReferenceValue().getTargetName().getOrig() : null;
        String rightName = rightRef.asReferenceValue().getTargetName() != null ? rightRef.asReferenceValue().getTargetName().getOrig() : null;
        if (leftName == null) {
            return respectDirection(1, direction);      // null > anything
        } else if (rightName == null) {
            return respectDirection(-1, direction);     // anything < null
        }

        int ordering = leftName.toUpperCase().compareTo(rightName.toUpperCase());           // brutal hack (we need to compare on normalized values)
        return respectDirection(ordering, direction);
    }

    private int respectDirection(int ordering, OrderDirection direction) {
        if (direction == OrderDirection.ASCENDING) {
            return ordering;
        } else {
            return -ordering;
        }
    }

}
