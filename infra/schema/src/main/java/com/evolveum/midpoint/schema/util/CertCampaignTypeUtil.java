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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;

/**
 * @author mederly
 */
public class CertCampaignTypeUtil {

    public static AccessCertificationStageType getCurrentStage(AccessCertificationCampaignType campaign) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (stage.getNumber() == campaign.getStageNumber() && stage.getIteration() == campaign.getIteration()) {
                return stage;
            }
        }
        return null;
    }

    @NotNull
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

    @NotNull
    public static AccessCertificationStageType findStage(AccessCertificationCampaignType campaign, int stageNumber) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (stage.getNumber() == stageNumber && stage.getIteration() == campaign.getIteration()) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " (iteration " + campaign.getIteration() + " in "
                + ObjectTypeUtil.toShortString(campaign));
    }

    @SuppressWarnings("unused")
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
    public static AccessCertificationWorkItemType findWorkItem(AccessCertificationCaseType _case, int stageNumber, int iteration,
            String reviewerOid) {
        return _case.getWorkItem().stream()
                .filter(wi -> wi.getStageNumber() == stageNumber && wi.getIteration() == iteration
                        && ObjectTypeUtil.containsOid(wi.getAssigneeRef(), reviewerOid))
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

    @SuppressWarnings("unused")
    public static boolean isCampaignClosed(AccessCertificationCampaignType campaign) {
        int currentStage = campaign.getStageNumber();
        int stages = getNumberOfStages(campaign);
        return CLOSED.equals(campaign.getState()) || currentStage > stages;
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
    @SuppressWarnings("unused")         // used e.g. by campaigns report
    public static int getActiveCases(List<AccessCertificationCaseType> caseList, int campaignStageNumber, AccessCertificationCampaignStateType state) {
        int open = 0;
        for (AccessCertificationCaseType aCase : caseList) {
            if (aCase.getReviewFinishedTimestamp() == null) {
                open++;
            }
        }
        return open;
    }

    // what % of cases is fully decided? (i.e. either they have all the decisions or they are review-completed)
    public static float getCasesCompletedPercentageAllStagesAllIterations(AccessCertificationCampaignType campaign) {
        return getCasesCompletedPercentage(campaign.getCase(), null, null, null);
    }

    public static float getCasesCompletedPercentageCurrStageCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesCompletedPercentage(campaign.getCase(), campaign.getStageNumber(), campaign.getIteration(), campaign.getState());
    }

    public static float getCasesCompletedPercentageAllStagesCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesCompletedPercentage(campaign.getCase(), null, campaign.getIteration(), campaign.getState());
    }

    private static float getCasesCompletedPercentage(List<AccessCertificationCaseType> caseList, Integer campaignStage, Integer iteration,
            AccessCertificationCampaignStateType state) {
        Integer stage = accountForClosingStates(campaignStage, state);
        int allCases = 0;
        int completedCases = 0;
        for (AccessCertificationCaseType aCase : caseList) {
            if (!caseMatches(aCase, stage, iteration)) {
                continue;
            }
            allCases++;
            List<AccessCertificationWorkItemType> workItems = aCase.getWorkItem().stream().filter(wi -> workItemMatches(wi, stage, iteration)).collect(Collectors.toList());
            if (iteration == null) {
                // remove work items that were completed in later iterations
                MultiValuedMap<Integer, String> reviewersAnswered = new HashSetValuedHashMap<>();
                for (AccessCertificationWorkItemType workItem : workItems) {
                    if (WorkItemTypeUtil.getOutcome(workItem) != null) {
                        reviewersAnswered.put(workItem.getStageNumber(), workItem.getOriginalAssigneeRef().getOid());
                    }
                }
                for (Iterator<AccessCertificationWorkItemType> iterator = workItems.iterator(); iterator.hasNext(); ) {
                    AccessCertificationWorkItemType workItem = iterator.next();
                    String outcome = WorkItemTypeUtil.getOutcome(workItem);
                    if (outcome == null && reviewersAnswered.containsMapping(workItem.getStageNumber(), workItem.getOriginalAssigneeRef().getOid())) {
                        iterator.remove();
                    }
                }
            }
            // now check whether all (remaining) work items have outcome
            if (workItems.stream().allMatch(wi -> WorkItemTypeUtil.getOutcome(wi) != null)) {
                completedCases++;
            }
        }
        return allCases > 0 ? 100.0f * ((float) completedCases) / (float) allCases : 100.0f;
    }

    // what % of cases is effectively decided? (i.e. their preliminary outcome is ACCEPT, REJECT, or REVOKE)
    public static float getCasesDecidedPercentageAllStagesAllIterations(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase(), null, null, null);
    }

    public static float getCasesDecidedPercentageCurrStageCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase(), campaign.getStageNumber(), campaign.getIteration(), campaign.getState());
    }

    public static float getCasesDecidedPercentageAllStagesCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase(), null, campaign.getIteration(), null);
    }

    public static float getCasesDecidedPercentage(List<AccessCertificationCaseType> caseList, Integer stage, Integer iteration,
            AccessCertificationCampaignStateType state) {
        stage = accountForClosingStates(stage, state);
        int allCases = 0;
        int decidedCases = 0;
        for (AccessCertificationCaseType aCase : caseList) {
            if (!caseMatches(aCase, stage, iteration)) {
                continue;
            }
            allCases++;
            String outcome = aCase.getOutcome();
            if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT)
                    || QNameUtil.matchUri(outcome, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REVOKE)
                    || QNameUtil.matchUri(outcome, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REDUCE)) {
                decidedCases++;
            }
        }
        return allCases > 0 ? 100.0f * (float) decidedCases / (float) allCases : 100.0f;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean caseMatches(AccessCertificationCaseType aCase, Integer stage, Integer iteration) {
        return (stage == null || stage == aCase.getStageNumber())
                && (iteration == null || iteration.intValue() == aCase.getIteration()); // TODO .intValue
    }

    private static boolean workItemMatches(AccessCertificationWorkItemType workItem, Integer stage, Integer iteration) {
        return (stage == null || stage.intValue() == workItem.getStageNumber().intValue())
                && (iteration == null || iteration.intValue() == workItem.getIteration()); // TODO .intValue
    }

    // what % of work items is complete?
    public static float getWorkItemsCompletedPercentageAllStagesAllIterations(AccessCertificationCampaignType campaign) {
        return getWorkItemsCompletedPercentage(campaign.getCase(), null, null, campaign.getIteration(), null);
    }

    public static float getWorkItemsCompletedPercentageCurrStageCurrIteration(AccessCertificationCampaignType campaign) {
        return getWorkItemsCompletedPercentage(campaign.getCase(), campaign.getStageNumber(), campaign.getIteration(), // todo default value
                campaign.getIteration(), campaign.getState());
    }

    public static float getWorkItemsCompletedPercentageAllStagesCurrIteration(AccessCertificationCampaignType campaign) {
        return getWorkItemsCompletedPercentage(campaign.getCase(), null, campaign.getIteration(), // todo default value
                campaign.getIteration(), null);
    }

    public static float getWorkItemsCompletedPercentage(List<AccessCertificationCaseType> caseList, Integer stage,
            Integer iteration, int iterations, AccessCertificationCampaignStateType state) {
        int allWorkItems = 0;
        int decidedWorkItems = 0;
        stage = accountForClosingStates(stage, state);

        for (AccessCertificationCaseType aCase : caseList) {
			for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
                String outcome = WorkItemTypeUtil.getOutcome(workItem);
                if (iteration != null) {
			        if (workItem.getIteration() != iteration) {
                        continue;
                    }
                } else {
                    // we are interested in all iterations -- so we skip outcome-less work items from earlier iterations
			        if (workItem.getIteration() < iterations && outcome == null) {
			            continue;
                    }
                }
				if (stage != null && workItem.getStageNumber().intValue() != stage.intValue()) {
					continue;
				}
				allWorkItems++;
				if (outcome != null) {
					decidedWorkItems++;
				}
			}
        }
        if (allWorkItems == 0) {
            return 100.0f;
        } else {
            return 100.0f * (float) decidedWorkItems / (float) allWorkItems;
        }
    }

    protected static Integer accountForClosingStates(Integer stage, AccessCertificationCampaignStateType state) {
        if (stage != null && (state == IN_REMEDIATION || state == CLOSED)) {
            return stage - 1;          // move to last campaign state
        } else {
            return stage;
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

    // TODO use this also from GUI and maybe notifications
    @SuppressWarnings("unused")         // used by certification cases report
    public static List<ObjectReferenceType> getCurrentlyAssignedReviewers(PrismContainerValue<AccessCertificationCaseType> pcv) {
        AccessCertificationCaseType aCase = pcv.asContainerable();
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
            for (ObjectReferenceType assigneeRef : workItem.getAssigneeRef()) {
                if (workItem.getCloseTimestamp() == null
                        && java.util.Objects.equals(workItem.getStageNumber(), aCase.getStageNumber())) {
                    rv.add(assigneeRef);
                }
            }
        }
        return rv;
    }

    @SuppressWarnings("unused")         // used by certification cases report
    public static Date getLastReviewedOn(PrismContainerValue<AccessCertificationCaseType> pcv) {
        return getReviewedTimestamp(pcv.asContainerable().getWorkItem());
    }

//    @SuppressWarnings("unused")         // used by certification cases report
//    public static XMLGregorianCalendar getLastReviewedOn(PrismContainerValue<AccessCertificationCaseType> pcv) {
//        AccessCertificationCaseType aCase = pcv.asContainerable();
//        XMLGregorianCalendar max = null;
//        for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
//            if (workItem.getOutputChangeTimestamp() != null &&
//                    (max == null || max.compare(workItem.getOutputChangeTimestamp()) == DatatypeConstants.GREATER)) {
//                max = workItem.getOutputChangeTimestamp();
//            }
//        }
//        return max;
//    }

    @SuppressWarnings("unused")         // used by certification cases report
    public static List<ObjectReferenceType> getReviewedBy(PrismContainerValue<AccessCertificationCaseType> pcv) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : pcv.asContainerable().getWorkItem()) {
            if (!hasNoResponse(workItem)) {
                rv.add(workItem.getPerformerRef());
            }
        }
        return rv;
    }

    @SuppressWarnings("unused")         // used by certification cases report
    public static List<String> getComments(PrismContainerValue<AccessCertificationCaseType> pcv) {
        List<String> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : pcv.asContainerable().getWorkItem()) {
            if (!StringUtils.isEmpty(WorkItemTypeUtil.getComment(workItem))) {
                rv.add(WorkItemTypeUtil.getComment(workItem));
            }
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

    public static ObjectQuery createCasesForCampaignQuery(String campaignOid, PrismContext prismContext) {
        return QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .ownerId(campaignOid)
                .build();
    }

    public static ObjectQuery createWorkItemsForCampaignQuery(String campaignOid, PrismContext prismContext) {
        return QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
                .exists(PrismConstants.T_PARENT)
                   .ownerId(campaignOid)
                .build();
    }

//    // some methods, like searchOpenWorkItems, engage their own "openness" filter
//    public static ObjectQuery createOpenWorkItemsForCampaignQuery(String campaignOid, PrismContext prismContext) throws SchemaException {
//        return QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
//                .item(AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP).isNull()
//                .and().exists(PrismConstants.T_PARENT)
//                   .ownerId(campaignOid)
//                .build();
//    }

//    public static String getStageOutcome(AccessCertificationCaseType aCase, int stageNumber, int iteration) {
//        StageCompletionEventType event = aCase.getEvent().stream()
//                .filter(e -> e instanceof StageCompletionEventType && e.getStageNumber() == stageNumber
//                        && e.getIteration() != null && e.getIteration() == iteration)
//                .map(e -> (StageCompletionEventType) e)
//                .findAny().orElseThrow(
//                        () -> new IllegalStateException("No outcome registered for stage " + stageNumber + " in case " + aCase));
//        return event.getOutcome();
//    }

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
                .filter(wi -> wi.getStageNumber() == aCase.getStageNumber())
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

    @SuppressWarnings("unused")
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

    @NotNull
    public static List<StageCompletionEventType> getCompletedStageEvents(AccessCertificationCaseType aCase, int iteration) {
        return aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType)
                .filter(e -> e.getIteration() != null && e.getIteration() == iteration)
                .map(e -> (StageCompletionEventType) e)
                .collect(Collectors.toList());
    }

    public static int getCurrentStageEscalationLevelNumberSafe(@NotNull AccessCertificationCampaignType campaign) {
        AccessCertificationStageType currentStage = getCurrentStage(campaign);
        return currentStage != null ? getEscalationLevelNumber(currentStage) : 0;
    }

    public static int getCurrentStageEscalationLevelNumber(@NotNull AccessCertificationCampaignType campaign) {
        AccessCertificationStageType currentStage = getCurrentStage(campaign);
        if (currentStage == null) {
            throw new IllegalStateException("No current stage for " + campaign);
        }
        return getEscalationLevelNumber(currentStage);
    }

    private static int getEscalationLevelNumber(AccessCertificationStageType currentStage) {
        if (currentStage.getEscalationLevel() != null && currentStage.getEscalationLevel().getNumber() != null) {
            return currentStage.getEscalationLevel().getNumber();
        } else {
            return 0;
        }
    }

    // see WfContextUtil.getEscalationLevelInfo
    @Nullable
    public static String getEscalationLevelInfo(AccessCertificationCampaignType campaign) {
        if (campaign == null) {
            return null;
        }
        AccessCertificationStageType stage = getCurrentStage(campaign);
        return stage != null ? WfContextUtil.getEscalationLevelInfo(stage.getEscalationLevel()) : null;
    }

    // returns reviewers for non-closed work items
    public static Collection<String> getActiveReviewers(List<AccessCertificationCaseType> caseList) {
        Set<String> oids = new HashSet<>();
        for (AccessCertificationCaseType aCase : caseList) {
			for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
				if (workItem.getCloseTimestamp() == null) {
					for (ObjectReferenceType reviewerRef : workItem.getAssigneeRef()) {
						oids.add(reviewerRef.getOid());
					}
				}
			}
        }
        return oids;
    }
}
