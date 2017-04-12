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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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

    @NotNull
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

    // to be used in tests (beware: there could be more work items)
    // TODO move to a test class
    public static AccessCertificationWorkItemType findWorkItem(AccessCertificationCaseType _case, int stageNumber, String reviewerOid) {
        return _case.getWorkItem().stream()
                .filter(wi -> wi.getStageNumber() == stageNumber && ObjectTypeUtil.containsOid(wi.getAssigneeRef(), reviewerOid))
                .findFirst().orElse(null);
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
            if (aCase.getStageNumber() != campaignStageNumber) {
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
        if (state == AccessCertificationCampaignStateType.IN_REMEDIATION || state == AccessCertificationCampaignStateType.CLOSED) {
            campaignStageNumber = campaignStageNumber - 1;          // move to last campaign state
        }
        for (AccessCertificationCaseType aCase : caseList) {
			for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
				if (workItem.getStageNumber() == campaignStageNumber
                        && workItem.getCloseTimestamp() == null
                        && WorkItemTypeUtil.getOutcome(workItem) == null) {
					unansweredCases++;
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
            if (QNameUtil.matchUri(aCase.getOutcome(), SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT)
                    || QNameUtil.matchUri(aCase.getOutcome(), SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REVOKE)
                    || QNameUtil.matchUri(aCase.getOutcome(), SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REDUCE)) {
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
				if (WorkItemTypeUtil.getOutcome(workItem) != null) {
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
            Date responseDate = XmlTypeConverter.toDate(workItem.getOutputChangeTimestamp());
            if (lastDate == null || responseDate.after(lastDate)) {
                lastDate = responseDate;
            }
        }
        return lastDate;
    }

    private static boolean hasNoResponse(AccessCertificationWorkItemType workItem) {
        return WorkItemTypeUtil.getOutcome(workItem) == null && StringUtils.isEmpty(WorkItemTypeUtil.getComment(workItem));
    }

    public static List<ObjectReferenceType> getReviewedBy(List<AccessCertificationWorkItemType> workItems) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : workItems) {
            if (hasNoResponse(workItem)) {
                continue;
            }
            rv.add(workItem.getPerformerRef());
        }
        return rv;
    }

    public static List<String> getComments(List<AccessCertificationWorkItemType> workItems) {
        List<String> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : workItems) {
            if (StringUtils.isEmpty(WorkItemTypeUtil.getComment(workItem))) {
                continue;
            }
            rv.add(WorkItemTypeUtil.getComment(workItem));
        }
        return rv;
    }

//    // TODO find a better place for this
//	@Deprecated
//    public static ItemPath getOrderBy(QName oldName) {
//        if (QNameUtil.match(oldName, AccessCertificationCaseType.F_TARGET_REF)) {
//            return new ItemPath(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
//        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_OBJECT_REF)) {
//            return new ItemPath(AccessCertificationCaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
//        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_TENANT_REF)) {
//            return new ItemPath(AccessCertificationCaseType.F_TENANT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
//        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_ORG_REF)) {
//            return new ItemPath(AccessCertificationCaseType.F_ORG_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
//        } else {
//            return new ItemPath(oldName);
//        }
//    }

    public static ObjectQuery createCasesForCampaignQuery(String campaignOid, PrismContext prismContext) throws SchemaException {
        return QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .ownerId(campaignOid)
                .build();
    }

    public static ObjectQuery createWorkItemsForCampaignQuery(String campaignOid, PrismContext prismContext) throws SchemaException {
        return QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
                .exists(PrismConstants.T_PARENT)
                   .ownerId(campaignOid)
                .build();
    }

    public static String getStageOutcome(AccessCertificationCaseType aCase, int stageNumber) {
        StageCompletionEventType event = aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType && e.getStageNumber() == stageNumber)
                .map(e -> (StageCompletionEventType) e)
                .findAny().orElseThrow(
                        () -> new IllegalStateException("No outcome registered for stage " + stageNumber + " in case " + aCase));
        return event.getOutcome();
    }

    public static List<AccessCertificationResponseType> getOutcomesToStopOn(List<AccessCertificationResponseType> stopReviewOn, List<AccessCertificationResponseType> advanceToNextStageOn) {
        if (!stopReviewOn.isEmpty()) {
            return stopReviewOn;
        }
        List<AccessCertificationResponseType> rv = new ArrayList<>(Arrays.asList(AccessCertificationResponseType.values()));
        rv.removeAll(advanceToNextStageOn);
        return rv;
    }

    // useful e.g. for tests
    public static Set<ObjectReferenceType> getCurrentReviewers(AccessCertificationCaseType aCase) {
        return aCase.getWorkItem().stream()
                // TODO check also with campaign stage?
                .filter(wi -> wi.getStageNumber() == aCase.getStageNumber() && wi.getCloseTimestamp() == null)
                .flatMap(wi -> wi.getAssigneeRef().stream())
                .collect(Collectors.toSet());
    }

    @NotNull
    public static AccessCertificationCaseType getCaseChecked(AccessCertificationWorkItemType workItem) {
        AccessCertificationCaseType aCase = getCase(workItem);
        if (aCase == null) {
            throw new IllegalStateException("No certification case for work item " + workItem);
        }
        return aCase;
    }

    @NotNull
    public static AccessCertificationCampaignType getCampaignChecked(AccessCertificationCaseType aCase) {
        AccessCertificationCampaignType campaign = getCampaign(aCase);
        if (campaign == null) {
            throw new IllegalStateException("No certification campaign for case " + aCase);
        }
        return campaign;
    }

    @NotNull
    public static AccessCertificationCampaignType getCampaignChecked(AccessCertificationWorkItemType workItem) {
        return getCampaignChecked(getCaseChecked(workItem));
    }

    public static AccessCertificationCaseType getCase(AccessCertificationWorkItemType workItem) {
        @SuppressWarnings({"unchecked", "raw"})
        PrismContainerable<AccessCertificationWorkItemType> parent = workItem.asPrismContainerValue().getParent();
        if (!(parent instanceof PrismContainer)) {
            return null;
        }
        PrismValue parentParent = ((PrismContainer<AccessCertificationWorkItemType>) parent).getParent();
        if (!(parentParent instanceof PrismContainerValue)) {
            return null;
        }
        @SuppressWarnings({"unchecked", "raw"})
        PrismContainerValue<AccessCertificationCaseType> parentParentPcv = (PrismContainerValue<AccessCertificationCaseType>) parentParent;
        return parentParentPcv.asContainerable();
    }

    public static AccessCertificationCampaignType getCampaign(AccessCertificationCaseType aCase) {
        @SuppressWarnings({"unchecked", "raw"})
        PrismContainer<AccessCertificationCaseType> caseContainer = (PrismContainer<AccessCertificationCaseType>) aCase.asPrismContainerValue().getParent();
        if (caseContainer == null) {
            return null;
        }
        @SuppressWarnings({"unchecked", "raw"})
        PrismContainerValue<AccessCertificationCampaignType> campaignValue = (PrismContainerValue<AccessCertificationCampaignType>) caseContainer.getParent();
        if (campaignValue == null) {
            return null;
        }
        PrismObject<AccessCertificationCampaignType> campaign = (PrismObject<AccessCertificationCampaignType>) campaignValue.getParent();
        return campaign != null ? campaign.asObjectable() : null;
    }

    public static List<String> getOutcomesFromCompletedStages(AccessCertificationCaseType aCase) {
        return aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType)
                .map(e -> ((StageCompletionEventType) e).getOutcome())
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<StageCompletionEventType> getCompletedStageEvents(AccessCertificationCaseType aCase) {
        return aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType)
                .map(e -> (StageCompletionEventType) e)
                .collect(Collectors.toList());
    }

    public static int getCurrentStageEscalationLevelNumber(@NotNull AccessCertificationCampaignType campaign) {
        AccessCertificationStageType currentStage = getCurrentStage(campaign);
        if (currentStage == null) {
            throw new IllegalStateException("No current stage for " + campaign);
        }
        if (currentStage.getEscalationLevel() != null && currentStage.getEscalationLevel().getNumber() != null) {
            return currentStage.getEscalationLevel().getNumber();
        } else {
            return 0;
        }
    }
}
