/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.impl.outcomeStrategies.OutcomeStrategy;
import com.evolveum.midpoint.certification.impl.outcomeStrategies.ResponsesSummary;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.fromUri;
import static com.evolveum.midpoint.certification.api.OutcomeUtils.normalizeToNonNull;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortStringLazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ALL_MUST_ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ONE_ACCEPT_ACCEPTS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
@Component
public class AccCertResponseComputationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AccCertResponseComputationHelper.class);

    private static final AccessCertificationCaseOutcomeStrategyType DEFAULT_CASE_STAGE_OUTCOME_STRATEGY = ONE_ACCEPT_ACCEPTS;
    private static final AccessCertificationCaseOutcomeStrategyType DEFAULT_CASE_OVERALL_OUTCOME_STRATEGY = ALL_MUST_ACCEPT;

    private final Map<AccessCertificationCaseOutcomeStrategyType, OutcomeStrategy> outcomeStrategyMap = new HashMap<>();

    public void registerOutcomeStrategy(AccessCertificationCaseOutcomeStrategyType strategyName, OutcomeStrategy strategy) {
        outcomeStrategyMap.put(strategyName, strategy);
    }

    @NotNull
    private OutcomeStrategy getOutcomeStrategy(AccessCertificationCaseOutcomeStrategyType outcomeStrategy) {
        OutcomeStrategy strategyImpl = outcomeStrategyMap.get(outcomeStrategy);
        if (strategyImpl == null) {
            throw new IllegalStateException("Unknown/unsupported outcome strategy " + outcomeStrategy);
        }
        return strategyImpl;
    }

//    // should be the case enabled in the following stage?
//    boolean advancesToNextStage(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
//            List<AccessCertificationResponseType> outcomesToStopOn) {
//        if (aCase.getReviewFinishedTimestamp() != null) {
//            LOGGER.trace("Case {} review process is already finished", aCase.getId());
//            return false;       // it is not relevant even for the current stage (so it won't advance)
//        }
//        AccessCertificationResponseType currentOutcome = normalizeToNonNull(fromUri(aCase.getCurrentStageOutcome()));
//        boolean amongStopped = outcomesToStopOn.contains(currentOutcome);
//        // TODO !!!!!!!!!!!!!! is this OK when reiterating? what if stages are skipped?
//        LOGGER.trace("Current outcome of case {} ({}: from stage {}) {} among outcomes we stop on", aCase.getId(),
//                currentOutcome, aCase.getStageNumber(), amongStopped ? "is" : "is not");
//        return !amongStopped;
//    }

    List<AccessCertificationResponseType> getOutcomesToStopOn(AccessCertificationCampaignType campaign) {
        List<AccessCertificationResponseType> rv;
        AccessCertificationStageDefinitionType stageDefinition = CertCampaignTypeUtil.getCurrentStageDefinition(campaign);
        if (!stageDefinition.getStopReviewOn().isEmpty() || !stageDefinition.getAdvanceToNextStageOn().isEmpty()) {
            rv = CertCampaignTypeUtil.getOutcomesToStopOn(stageDefinition.getStopReviewOn(), stageDefinition.getAdvanceToNextStageOn());
        } else {
            AccessCertificationCaseReviewStrategyType reviewStrategy = campaign.getReviewStrategy();
            if (reviewStrategy != null && (!reviewStrategy.getStopReviewOn().isEmpty() || !reviewStrategy.getAdvanceToNextStageOn().isEmpty())) {
                rv = CertCampaignTypeUtil.getOutcomesToStopOn(reviewStrategy.getStopReviewOn(), reviewStrategy.getAdvanceToNextStageOn());
            } else {
                rv = getOverallOutcomeStrategy(campaign).getOutcomesToStopOn();
            }
        }
        LOGGER.trace("Outcomes to stop on for campaign {}, stage {}: {}", toShortStringLazy(campaign), campaign.getStageNumber(), rv);
        return rv;
    }

    @NotNull
    private OutcomeStrategy getOverallOutcomeStrategy(AccessCertificationCampaignType campaign) {
        AccessCertificationCaseOutcomeStrategyType strategyName;
        if (campaign.getReviewStrategy() != null && campaign.getReviewStrategy().getOutcomeStrategy() != null) {
            strategyName = campaign.getReviewStrategy().getOutcomeStrategy();
        } else {
            strategyName = DEFAULT_CASE_OVERALL_OUTCOME_STRATEGY;
        }
        LOGGER.trace("Outcome strategy for {} is {}", toShortStringLazy(campaign), strategyName);
        return getOutcomeStrategy(strategyName);
    }

    @NotNull
    AccessCertificationResponseType computeOutcomeForStage(AccessCertificationCaseType aCase,
            AccessCertificationCampaignType campaign, int stageNumber) {
        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stageNumber);
        List<AccessCertificationResponseType> allResponses = getResponses(aCase, stageNumber, norm(campaign.getIteration()));
        AccessCertificationResponseType outcome;
        String base;
        if (allResponses.isEmpty()) {
            outcome = stageDef.getOutcomeIfNoReviewers();
            base = "<no reviewers available>";
        } else {
            AccessCertificationCaseOutcomeStrategyType outcomeStrategy = defaultIfNull(stageDef.getOutcomeStrategy(), DEFAULT_CASE_STAGE_OUTCOME_STRATEGY);
            OutcomeStrategy strategyImpl = getOutcomeStrategy(outcomeStrategy);
            ResponsesSummary summary = summarize(allResponses);        // TODO eventually merge extraction and summarizing
            outcome = strategyImpl.computeOutcome(summary);
            base = allResponses.toString();
        }
        AccessCertificationResponseType rv = normalizeToNonNull(outcome);
        LOGGER.trace("computeOutcomeForStage for case {} (stageNumber: {}) returned {} based on {}", aCase.getId(), stageNumber, rv, base);
        return rv;
    }

    private ResponsesSummary summarize(List<AccessCertificationResponseType> responses) {
        ResponsesSummary summary = new ResponsesSummary();
        for (AccessCertificationResponseType response : responses) {
            summary.add(normalizeToNonNull(response));
        }
        return summary;
    }

    @NotNull
    public AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign) {
        OutcomeStrategy strategy = getOverallOutcomeStrategy(campaign);
        return normalizeToNonNull(strategy.computeOutcome(summarize(getOutcomesFromCompletedStages(aCase, null, null))));
    }

    // aCase contains outcomes from previous (closed) stages. Outcome from the current (not yet closed) stage (additionalStageNumber)
    // is in additionalStageResponse.
    @NotNull
    AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase,
            AccessCertificationCampaignType campaign, int additionalStageNumber, AccessCertificationResponseType additionalStageOutcome) {
        List<AccessCertificationResponseType> stageOutcomes = getOutcomesFromCompletedStages(aCase, additionalStageNumber, additionalStageOutcome);
        return normalizeToNonNull(getOverallOutcomeStrategy(campaign).computeOutcome(summarize(stageOutcomes)));
    }

    // We take into account _all_ responses relevant to this stage!
    // See https://wiki.evolveum.com/display/midPoint/On+Certification+Campaigns+Iteration.
    private List<AccessCertificationResponseType> getResponses(AccessCertificationCaseType aCase, int stageNumber, int iteration) {
        List<AccessCertificationResponseType> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType wi : aCase.getWorkItem()) {
            if (wi.getStageNumber() == stageNumber) {
                AccessCertificationResponseType response = normalizeToNonNull(fromUri(WorkItemTypeUtil.getOutcome(wi)));
                if (response != AccessCertificationResponseType.NO_RESPONSE || norm(wi.getIteration()) == iteration) {
                    // i.e. NO_RESPONSE answers from earlier iterations are not taken into account
                    rv.add(response);
                }
            }
        }
        return rv;
    }

    // see https://wiki.evolveum.com/display/midPoint/On+Certification+Campaigns+Iteration
    private List<AccessCertificationResponseType> getOutcomesFromCompletedStages(AccessCertificationCaseType aCase,
            Integer additionalStageNumber, AccessCertificationResponseType additionalStageResponse) {
        LOGGER.trace("getOutcomesFromCompletedStages: additionalStageNumber={}, additionalStageResponse={}", additionalStageNumber, additionalStageResponse);
        SetValuedMap<Integer, AccessCertificationResponseType> allOutcomes = new HashSetValuedHashMap<>();
        for (CaseEventType event : aCase.getEvent()) {
            if (event instanceof StageCompletionEventType) {
                StageCompletionEventType stageCompletionEvent = (StageCompletionEventType) event;
                if (event.getStageNumber() == null) {
                    throw new IllegalStateException("Missing stage number in StageCompletionEventType: " + event);
                }
                allOutcomes.put(event.getStageNumber(), normalizeToNonNull(fromUri(stageCompletionEvent.getOutcome())));
            }
        }
        if (additionalStageNumber != null) {
            allOutcomes.put(additionalStageNumber, normalizeToNonNull(additionalStageResponse));
        }
        List<AccessCertificationResponseType> rv = new ArrayList<>();
        for (Integer stage : allOutcomes.keySet()) {
            Set<AccessCertificationResponseType> stageOutcomes = allOutcomes.get(stage);
            addIgnoreNull(rv, extractStageOutcome(stageOutcomes, aCase.getId(), stage));
        }
        return rv;
    }

    AccessCertificationResponseType getStageOutcome(AccessCertificationCaseType aCase, int stageNumber) {
        Set<AccessCertificationResponseType> stageOutcomes = aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType && e.getStageNumber() == stageNumber)
                .map(e -> normalizeToNonNull(fromUri(((StageCompletionEventType) e).getOutcome())))
                .collect(Collectors.toSet());
        return extractStageOutcome(stageOutcomes, aCase.getId(), stageNumber);
    }

    private AccessCertificationResponseType extractStageOutcome(Set<AccessCertificationResponseType> stageOutcomes, Long caseId, int stage) {
        Collection<AccessCertificationResponseType> nonNullOutcomes = CollectionUtils.subtract(stageOutcomes, singleton(NO_RESPONSE));
        if (nonNullOutcomes.size() > 1) {
            throw new IllegalStateException("Contradictory outcomes for case " + caseId + " in stage " + stage + ": " + stageOutcomes);
        } else if (!nonNullOutcomes.isEmpty()) {
            return nonNullOutcomes.iterator().next();
        } else if (!stageOutcomes.isEmpty()) {
            return NO_RESPONSE;
        } else {
            return null;
        }
    }
}
