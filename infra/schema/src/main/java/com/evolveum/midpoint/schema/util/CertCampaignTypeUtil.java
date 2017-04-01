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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 * @author mederly
 */
public class CertCampaignTypeUtil {

    public static AccessCertificationStageType getCurrentStage(AccessCertificationCampaignType campaign) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (stage.getNumber() == campaign.getStageNumber()) {
                return stage;
            }
        }
        return null;
    }

    public static AccessCertificationStageDefinitionType getCurrentStageDefinition(AccessCertificationCampaignType campaign) {
        return findStageDefinition(campaign, campaign.getStageNumber());
    }

    public static AccessCertificationStageDefinitionType findStageDefinition(AccessCertificationCampaignType campaign, int stageNumber) {
        for (AccessCertificationStageDefinitionType stage : campaign.getStageDefinition()) {
            if (stage.getNumber() == stageNumber) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " in " + ObjectTypeUtil.toShortString(campaign));
    }

    public static AccessCertificationStageType findStage(AccessCertificationCampaignType campaign, int stageNumber) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (stage.getNumber() == stageNumber) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " in " + ObjectTypeUtil.toShortString(campaign));
    }

    public static AccessCertificationCaseType findCase(AccessCertificationCampaignType campaign, long caseId) {
        for (AccessCertificationCaseType _case : campaign.getCase()) {
            if (_case.asPrismContainerValue().getId() != null && _case.asPrismContainerValue().getId() == caseId) {
                return _case;
            }
        }
        return null;
    }

    public static void findDecision(AccessCertificationCaseType _case, int stageNumber, String reviewerOid) {
//        for (AccessCertificationDecisionType d : _case.getDecision()) {
//            if (d.getStageNumber() == stageNumber && d.getReviewerRef().getOid().equals(reviewerOid)) {
//                return d;
//            }
//        }
//        return null;
    }

    public static AccessCertificationWorkItemType findWorkItem(AccessCertificationCaseType _case, long workItemId) {
		return _case.getWorkItem().stream()
				.filter(wi -> wi.getId() != null && wi.getId() == workItemId)
				.findFirst().orElse(null);
    }

    public static int getNumberOfStages(AccessCertificationCampaignType campaign) {
        return campaign.getStageDefinition().size();
    }

    public static AccessCertificationDefinitionType getDefinition(AccessCertificationCampaignType campaign) {
        if (campaign.getDefinitionRef() == null) {
            throw new IllegalStateException("No definition reference in " + ObjectTypeUtil.toShortString(campaign));
        }
        PrismReferenceValue referenceValue = campaign.getDefinitionRef().asReferenceValue();
        if (referenceValue.getObject() == null) {
            throw new IllegalStateException("No definition object in " + ObjectTypeUtil.toShortString(campaign));
        }
        return (AccessCertificationDefinitionType) (referenceValue.getObject().asObjectable());
    }

    public static boolean isRemediationAutomatic(AccessCertificationCampaignType campaign) {
        return campaign.getRemediationDefinition() != null &&
                AccessCertificationRemediationStyleType.AUTOMATED.equals(campaign.getRemediationDefinition().getStyle());
    }

    public static boolean isCampaignClosed(AccessCertificationCampaignType campaign) {
        int currentStage = campaign.getStageNumber();
        int stages = getNumberOfStages(campaign);
        return AccessCertificationCampaignStateType.CLOSED.equals(campaign.getState()) || currentStage > stages;
    }

    // TODO rework signalling problems
    public static void checkStageDefinitionConsistency(List<AccessCertificationStageDefinitionType> stages) {
        int count = stages.size();
        boolean[] numberPresent = new boolean[count];
        for (AccessCertificationStageDefinitionType stage : stages) {
            int num = stage.getNumber();
            if (num < 1 || num > count) {
                throw new IllegalStateException("Invalid stage number: " + num + " (stage count: " + count +")");
            }
            if (numberPresent[num-1]) {
                throw new IllegalStateException("Stage with number " + num + " is defined multiple times");
            }
            numberPresent[num-1] = true;
        }
        // maybe redundant test
        for (int i = 0; i < numberPresent.length; i++) {
            if (!numberPresent[i]) {
                throw new IllegalStateException("Stage with number " + (i+1) + " was not defined");
            }
        }
    }

    // expects that the currentStageNumber is reasonable
    public static AccessCertificationStageType findCurrentStage(AccessCertificationCampaignType campaign) {
        return findStage(campaign, campaign.getStageNumber());
    }

    // active cases = cases that are to be responded to in this stage
    public static int getActiveCases(List<AccessCertificationCaseType> caseList, int campaignStageNumber, AccessCertificationCampaignStateType state) {
        int open = 0;
        if (state == AccessCertificationCampaignStateType.IN_REMEDIATION || state == AccessCertificationCampaignStateType.CLOSED) {
            campaignStageNumber = campaignStageNumber - 1;          // move to last campaign state
        }
        for (AccessCertificationCaseType aCase : caseList) {
            if (aCase.getCurrentStageNumber() != campaignStageNumber) {
                continue;
            }
            open++;
        }
        return open;
    }

    // unanswered cases = cases where one or more answers from reviewers are missing
    // "no reviewers" cases are treated as answered, because no answer can be provided
    public static int getUnansweredCases(List<AccessCertificationCaseType> caseList, int campaignStageNumber, AccessCertificationCampaignStateType state) {
        int unansweredCases = 0;
        for (AccessCertificationCaseType aCase : caseList) {
			for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
				if (workItem.getStageNumber() != campaignStageNumber) {
					continue;
				}
				if (workItem.getClosedTimestamp() == null && (workItem.getResponse() == null || workItem.getResponse() == NO_RESPONSE)) {
					unansweredCases++; // TODO what about delegations?
					break;
				}
			}
        }
        return unansweredCases;
    }

    // backwards compatibility (for reports)
    public static int getPercentComplete(List<AccessCertificationCaseType> caseList, int campaignStageNumber, AccessCertificationCampaignStateType state) {
        return Math.round(getCasesCompletedPercentage(caseList, campaignStageNumber, state));
    }

    // what % of cases is fully decided? (i.e. either they have all the decisions or they are not in the current stage at all)
    public static float getCasesCompletedPercentage(AccessCertificationCampaignType campaign) {
        return getCasesCompletedPercentage(campaign.getCase(), campaign.getStageNumber(), campaign.getState());
    }

    public static float getCasesCompletedPercentage(List<AccessCertificationCaseType> caseList, int campaignStageNumber, AccessCertificationCampaignStateType state) {
        int cases = caseList.size();
        if (cases > 0) {
            int unanswered = getUnansweredCases(caseList, campaignStageNumber, state);
            return 100 * ((float) cases - (float) unanswered) / (float) cases;
        } else {
            return 100.0f;
        }
    }

    // what % of cases is effectively decided? (i.e. their preliminary outcome is ACCEPT, REJECT, or REVOKE)
    public static float getCasesDecidedPercentage(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase());
    }

    public static float getCasesDecidedPercentage(List<AccessCertificationCaseType> caseList) {
        if (caseList.isEmpty()) {
            return 100.0f;
        }
        int decided = 0;
        for (AccessCertificationCaseType aCase : caseList) {
            if (aCase.getOverallOutcome() == ACCEPT || aCase.getOverallOutcome() == REVOKE || aCase.getOverallOutcome() == REDUCE) {
                decided++;
            }
        }
        return 100.0f * (float) decided / (float) caseList.size();
    }

    // what % of decisions is complete?
    public static float getDecisionsDonePercentage(AccessCertificationCampaignType campaign) {
        return getDecisionsDonePercentage(campaign.getCase(), campaign.getStageNumber(), campaign.getState());
    }

    public static float getDecisionsDonePercentage(List<AccessCertificationCaseType> caseList, int campaignStageNumber, AccessCertificationCampaignStateType state) {
        int decisionsRequested = 0;
        int decisionsDone = 0;
        if (state == AccessCertificationCampaignStateType.IN_REMEDIATION || state == AccessCertificationCampaignStateType.CLOSED) {
            campaignStageNumber = campaignStageNumber - 1;          // move to last campaign state
        }

        for (AccessCertificationCaseType aCase : caseList) {
			for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
				if (workItem.getStageNumber() != campaignStageNumber) {
					continue;
				}
				decisionsRequested++;
				if (workItem.getResponse() != null && workItem.getResponse() != NO_RESPONSE) {	// TODO what about delegations?
					decisionsDone++;
				}
			}
        }
        if (decisionsRequested == 0) {
            return 100.0f;
        } else {
            return 100.0f * (float) decisionsDone / (float) decisionsRequested;
        }
    }

    public static Date getReviewedTimestamp(List<AccessCertificationWorkItemType> workItems) {
        Date lastDate = null;
        for (AccessCertificationWorkItemType workItem : workItems) {
            if (hasNoResponse(workItem)) {
                continue;
            }
            Date responseDate = XmlTypeConverter.toDate(workItem.getTimestamp());
            if (lastDate == null || responseDate.after(lastDate)) {
                lastDate = responseDate;
            }
        }
        return lastDate;
    }

    private static boolean hasNoResponse(AccessCertificationWorkItemType workItem) {
        return (workItem.getResponse() == null || workItem.getResponse() == NO_RESPONSE) && StringUtils.isEmpty(workItem.getComment());
    }

    public static List<ObjectReferenceType> getReviewedBy(List<AccessCertificationWorkItemType> workItems) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : workItems) {
            if (hasNoResponse(workItem)) {
                continue;
            }
            rv.add(workItem.getResponderRef());
        }
        return rv;
    }

    public static List<String> getComments(List<AccessCertificationWorkItemType> workItems) {
        List<String> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : workItems) {
            if (StringUtils.isEmpty(workItem.getComment())) {
                continue;
            }
            rv.add(workItem.getComment());
        }
        return rv;
    }

    // TODO find a better place for this
    public static ItemPath getOrderBy(QName oldName) {
        if (QNameUtil.match(oldName, AccessCertificationCaseType.F_TARGET_REF)) {
            return new ItemPath(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_OBJECT_REF)) {
            return new ItemPath(AccessCertificationCaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_TENANT_REF)) {
            return new ItemPath(AccessCertificationCaseType.F_TENANT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_ORG_REF)) {
            return new ItemPath(AccessCertificationCaseType.F_ORG_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_CAMPAIGN_REF)) {
            return new ItemPath(T_PARENT, ObjectType.F_NAME);
        } else {
            return new ItemPath(oldName);
        }
    }

    public static ObjectQuery createCasesForCampaignQuery(String campaignOid, PrismContext prismContext) throws SchemaException {
        return QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .ownerId(campaignOid)
                .build();
    }

    public static AccessCertificationCaseStageOutcomeType getStageOutcome(AccessCertificationCaseType aCase, int stageNumber) {
        for (AccessCertificationCaseStageOutcomeType outcome : aCase.getCompletedStageOutcome()) {
            if (outcome.getStageNumber() == stageNumber) {
                return outcome;
            }
        }
        throw new IllegalStateException("No outcome registered for stage " + stageNumber + " in case " + aCase);
    }

    public static List<AccessCertificationResponseType> getOutcomesToStopOn(List<AccessCertificationResponseType> stopReviewOn, List<AccessCertificationResponseType> advanceToNextStageOn) {
        if (!stopReviewOn.isEmpty()) {
            return stopReviewOn;
        }
        List<AccessCertificationResponseType> rv = new ArrayList<>(Arrays.asList(AccessCertificationResponseType.values()));
        rv.removeAll(advanceToNextStageOn);
        return rv;
    }

    // TODO temporary implementation: replace by work items based approach where possible
//    public static List<ObjectReferenceType> getReviewers(AccessCertificationCaseType _case) {
//        throw new UnsupportedOperationException("TODO");
//    }
}
