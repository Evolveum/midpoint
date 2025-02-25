/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static java.util.stream.Collectors.toList;

public class CertCampaignTypeUtil {

    public static AccessCertificationStageType getCurrentStage(AccessCertificationCampaignType campaign) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (or0(stage.getNumber()) == or0(campaign.getStageNumber()) && norm(stage.getIteration()) == norm(campaign.getIteration())) {
                return stage;
            }
        }
        return null;
    }

    @NotNull
    public static AccessCertificationStageDefinitionType getCurrentStageDefinition(AccessCertificationCampaignType campaign) {
        return findStageDefinition(campaign, or0(campaign.getStageNumber()));
    }

    @NotNull
    public static AccessCertificationStageDefinitionType findStageDefinition(AccessCertificationCampaignType campaign, int stageNumber) {
        for (AccessCertificationStageDefinitionType stage : campaign.getStageDefinition()) {
            if (or0(stage.getNumber()) == stageNumber) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " in " + ObjectTypeUtil.toShortString(campaign));
    }

    @NotNull
    public static AccessCertificationStageType findStage(AccessCertificationCampaignType campaign, int stageNumber) {
        for (AccessCertificationStageType stage : campaign.getStage()) {
            if (or0(stage.getNumber()) == stageNumber && norm(stage.getIteration()) == norm(campaign.getIteration())) {
                return stage;
            }
        }
        throw new IllegalStateException("No stage " + stageNumber + " (iteration " + norm(campaign.getIteration()) + " in "
                + ObjectTypeUtil.toShortString(campaign));
    }

    @SuppressWarnings("unused")
    public static AccessCertificationCaseType findCase(AccessCertificationCampaignType campaign, long caseId) {
        for (AccessCertificationCaseType acase : campaign.getCase()) {
            if (acase.asPrismContainerValue().getId() != null && acase.asPrismContainerValue().getId() == caseId) {
                return acase;
            }
        }
        return null;
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
        int currentStage = or0(campaign.getStageNumber());
        int stages = getNumberOfStages(campaign);
        return CLOSED.equals(campaign.getState()) || currentStage > stages;
    }

    // TODO rework signalling problems
    public static void checkStageDefinitionConsistency(List<AccessCertificationStageDefinitionType> stages) {
        int count = stages.size();
        boolean[] numberPresent = new boolean[count];
        for (AccessCertificationStageDefinitionType stage : stages) {
            int num = or0(stage.getNumber());
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
        return findStage(campaign, or0(campaign.getStageNumber()));
    }

    // active cases = cases that are to be responded to in this stage
    @SuppressWarnings("unused")         // used e.g. by campaigns report
    public static int getActiveCases(List<AccessCertificationCaseType> caseList, Integer campaignStageNumber, AccessCertificationCampaignStateType state) {
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
        return getCasesCompletedPercentage(campaign.getCase(), null, null);
    }

    public static float getCasesCompletedPercentageCurrStageCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesCompletedPercentage(campaign.getCase(),
                accountForClosingStates(or0(campaign.getStageNumber()), campaign.getState()), norm(campaign.getIteration()));
    }

    public static float getCasesCompletedPercentageCurrStageAllIterations(AccessCertificationCampaignType campaign) {
        return getCasesCompletedPercentage(campaign.getCase(),
                accountForClosingStates(or0(campaign.getStageNumber()), campaign.getState()), null);
    }

    public static float getCasesCompletedPercentageAllStagesCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesCompletedPercentage(campaign.getCase(), null, norm(campaign.getIteration()));
    }

    private static float getCasesCompletedPercentage(List<AccessCertificationCaseType> caseList, Integer stage, Integer iteration) {
        int allCases = 0;
        int completedCases = 0;
        for (AccessCertificationCaseType aCase : caseList) {
            if (!caseMatches(aCase, stage, iteration)) {
                continue;
            }
            allCases++;
            List<AccessCertificationWorkItemType> workItems = aCase.getWorkItem().stream().filter(wi -> workItemMatches(wi, stage, iteration)).collect(toList());
            if (iteration == null) {
                removeRedundantWorkItems(workItems);
            }
            // now check whether all (remaining) work items have outcome
            if (workItems.stream().allMatch(wi -> WorkItemTypeUtil.getOutcome(wi) != null)) {
                completedCases++;
            }
        }
        return allCases > 0 ? 100.0f * ((float) completedCases) / (float) allCases : 100.0f;
    }

    private static void removeRedundantWorkItems(List<AccessCertificationWorkItemType> workItems) {
        Map<Integer,Map<String, Integer>> stagesReviewersIterations = new HashMap<>();           // stage -> reviewer oid -> last iteration where present
        for (AccessCertificationWorkItemType workItem : workItems) {
            Map<String,Integer> reviewersIterations = stagesReviewersIterations.computeIfAbsent(workItem.getStageNumber(), (stage) -> new HashMap<>());
            String reviewer = workItem.getOriginalAssigneeRef().getOid();
            Integer oldIteration = reviewersIterations.get(reviewer);
            if (oldIteration == null || oldIteration < norm(workItem.getIteration())) {
                reviewersIterations.put(reviewer, norm(workItem.getIteration()));
            }
        }
        for (Iterator<AccessCertificationWorkItemType> iterator = workItems.iterator(); iterator.hasNext(); ) {
            AccessCertificationWorkItemType workItem = iterator.next();
            int lastIteration = stagesReviewersIterations.get(workItem.getStageNumber()).get(workItem.getOriginalAssigneeRef().getOid());
            if (norm(workItem.getIteration()) < lastIteration) {
                iterator.remove();
            }
        }
    }

//    private static void removeRedundantWorkItems(List<AccessCertificationWorkItemType> workItems) {
//        MultiValuedMap<Integer, String> reviewersAnswered = new HashSetValuedHashMap<>();
//        for (AccessCertificationWorkItemType workItem : workItems) {
//            if (WorkItemTypeUtil.getOutcome(workItem) != null) {
//                reviewersAnswered.put(workItem.getStageNumber(), workItem.getOriginalAssigneeRef().getOid());
//            }
//        }
//        for (Iterator<AccessCertificationWorkItemType> iterator = workItems.iterator(); iterator.hasNext(); ) {
//            AccessCertificationWorkItemType workItem = iterator.next();
//            String outcome = WorkItemTypeUtil.getOutcome(workItem);
//            if (outcome == null && reviewersAnswered.containsMapping(workItem.getStageNumber(), workItem.getOriginalAssigneeRef().getOid())) {
//                iterator.remove();
//            }
//        }
//    }

    // what % of cases is effectively decided? (i.e. their preliminary outcome is ACCEPT, REJECT, or REVOKE)
    public static float getCasesDecidedPercentageAllStagesAllIterations(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase(), null, null, null);
    }

    public static float getCasesDecidedPercentageCurrStageCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase(), or0(campaign.getStageNumber()), norm(campaign.getIteration()), campaign.getState());
    }

    public static float getCasesDecidedPercentageCurrStageAllIterations(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase(), or0(campaign.getStageNumber()), null, campaign.getState());
    }

    public static float getCasesDecidedPercentageAllStagesCurrIteration(AccessCertificationCampaignType campaign) {
        return getCasesDecidedPercentage(campaign.getCase(), null, norm(campaign.getIteration()), null);
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
        if (iteration != null && iteration != norm(aCase.getIteration())) {
            return false;
        }
        return stage == null || or0(stage) == or0(aCase.getStageNumber())
                || !CertCampaignTypeUtil.getCompletedStageEvents(aCase, stage, iteration).isEmpty();
    }

    private static boolean workItemMatches(AccessCertificationWorkItemType workItem, Integer stage, Integer iteration) {
        return (stage == null || stage.intValue() == workItem.getStageNumber().intValue())
                && (iteration == null || iteration == norm(workItem.getIteration()));
    }

    // what % of work items is complete?
    public static float getWorkItemsCompletedPercentageAllStagesAllIterations(AccessCertificationCampaignType campaign) {
        return getWorkItemsCompletedPercentage(campaign.getCase(), null, null);
    }

    public static float getWorkItemsCompletedPercentageCurrStageCurrIteration(AccessCertificationCampaignType campaign) {
        return getWorkItemsCompletedPercentage(campaign.getCase(),
                accountForClosingStates(or0(campaign.getStageNumber()), campaign.getState()), norm(campaign.getIteration()));
    }

    public static float getWorkItemsCompletedPercentageCurrStageAllIterations(AccessCertificationCampaignType campaign) {
        return getWorkItemsCompletedPercentage(campaign.getCase(),
                accountForClosingStates(or0(campaign.getStageNumber()), campaign.getState()), null);
    }

    public static float getWorkItemsCompletedPercentageAllStagesCurrIteration(AccessCertificationCampaignType campaign) {
        return getWorkItemsCompletedPercentage(campaign.getCase(), null, norm(campaign.getIteration()));
    }

    public static float getWorkItemsCompletedPercentage(List<AccessCertificationCaseType> caseList, Integer stage, Integer iteration) {
        int allWorkItems = 0;
        int decidedWorkItems = 0;
        for (AccessCertificationCaseType aCase : caseList) {
            List<AccessCertificationWorkItemType> workItems = aCase.getWorkItem().stream().filter(wi -> workItemMatches(wi, stage, iteration)).collect(toList());
            if (iteration == null) {
                removeRedundantWorkItems(workItems);
            }
            for (AccessCertificationWorkItemType workItem : workItems) {
                allWorkItems++;
                if (WorkItemTypeUtil.getOutcome(workItem) != null) {
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

    public static Integer accountForClosingStates(Integer stage, AccessCertificationCampaignStateType state) {
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
    @SuppressWarnings("unused")  // used by certification cases report
    public @NotNull static List<ObjectReferenceType> getCurrentlyAssignedReviewers(@NotNull AccessCertificationCaseType aCase) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        aCase.getWorkItem().forEach(workItem -> rv.addAll(getCurrentlyAssignedReviewers(workItem, or0(aCase.getStageNumber()))));
        return rv;
    }

    public @NotNull
    static List<ObjectReferenceType> getCurrentlyAssignedReviewers(
            @NotNull AccessCertificationWorkItemType certItem, int certCaseStageNumber) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (ObjectReferenceType assigneeRef : certItem.getAssigneeRef()) {
            if (certItem.getCloseTimestamp() == null
                    && Objects.equals(certItem.getStageNumber(), certCaseStageNumber)) {
                rv.add(assigneeRef);
            }
        }
        return rv;
    }

    public @NotNull
    static List<ObjectReferenceType> getAssignedReviewersForStage(@NotNull AccessCertificationCaseType aCase,
            int certCaseStageNumber) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        aCase.getWorkItem().forEach(certItem -> {
            for (ObjectReferenceType assigneeRef : certItem.getAssigneeRef()) {
                if (Objects.equals(certItem.getStageNumber(), certCaseStageNumber)) {
                    boolean alreadyInList = rv.stream().anyMatch(r -> r.getOid().equals(assigneeRef.getOid()));
                    if (!alreadyInList) {
                        rv.add(assigneeRef);
                    }
                }
            }
        });
        return rv;
    }

    public @NotNull static List<ObjectReferenceType> getAllAssignees(@NotNull AccessCertificationCaseType aCase) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
            rv.addAll(workItem.getAssigneeRef());
        }
        return rv;
    }

    public static @NotNull List<ObjectReferenceType> getAllCandidateAssignees(@NotNull AccessCertificationCaseType aCase) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (var workItem : aCase.getWorkItem()) {
            rv.addAll(workItem.getCandidateRef());
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

    public static List<String> getCommentsForStage(PrismContainerValue<AccessCertificationCaseType> pcv, int stageNumber) {
        List<String> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : pcv.asContainerable().getWorkItem()) {
            if (workItem.getStageNumber() != stageNumber) {
                continue;
            }
            if (!StringUtils.isEmpty(WorkItemTypeUtil.getComment(workItem))) {
                rv.add(WorkItemTypeUtil.getComment(workItem));
            }
        }
        return rv;
    }

    public static ObjectQuery createCasesForCampaignQuery(String campaignOid) {
        return PrismContext.get().queryFor(AccessCertificationCaseType.class)
                .ownerId(campaignOid)
                .build();
    }

    public static ObjectQuery createWorkItemsForCampaignQuery(String campaignOid) {
        return PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                .exists(PrismConstants.T_PARENT)
                   .ownerId(campaignOid)
                .build();
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
                .filter(wi -> or0(wi.getStageNumber()) == or0(aCase.getStageNumber()))
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
                .filter(e -> norm(e.getIteration()) == iteration)
                .map(e -> (StageCompletionEventType) e)
                .collect(toList());
    }

    @NotNull
    public static List<StageCompletionEventType> getCompletedStageEvents(AccessCertificationCaseType aCase, Integer stage, Integer iteration) {
        return aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType)
                .filter(e -> stage == null || e.getStageNumber() != null && e.getStageNumber() == stage.intValue())
                .filter(e -> iteration == null || norm(e.getIteration()) == iteration)
                .map(e -> (StageCompletionEventType) e)
                .collect(toList());
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

    // see ApprovalContextUtil.getEscalationLevelInfo
    @Nullable
    public static String getEscalationLevelInfo(AccessCertificationCampaignType campaign) {
        if (campaign == null) {
            return null;
        }
        AccessCertificationStageType stage = getCurrentStage(campaign);
        return stage != null ? ApprovalContextUtil.getEscalationLevelInfo(stage.getEscalationLevel()) : null;
    }

    // returns reviewers for non-closed work items
    public static Collection<String> getActiveReviewers(List<AccessCertificationCaseType> caseList) {
        Set<String> oids = new HashSet<>();
        for (AccessCertificationCaseType aCase : caseList) {
            for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
                if (workItem.getCloseTimestamp() == null) { // This basically means current active stage
                    for (ObjectReferenceType reviewerRef : workItem.getAssigneeRef()) {
                        oids.add(reviewerRef.getOid());
                    }
                }
            }
        }
        return oids;
    }

    public static int norm(Integer iteration) {
        return ObjectUtils.defaultIfNull(iteration, 1);
    }

    public static boolean isReiterable(AccessCertificationCampaignType campaign) {
        return campaign.getState() == AccessCertificationCampaignStateType.CLOSED &&
                (campaign.getReiterationDefinition() == null || campaign.getReiterationDefinition().getLimit() == null
                        || norm(campaign.getIteration()) < campaign.getReiterationDefinition().getLimit());
    }
}
