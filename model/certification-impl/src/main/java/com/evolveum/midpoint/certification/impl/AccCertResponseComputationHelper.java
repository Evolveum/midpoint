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

import com.evolveum.midpoint.certification.impl.outcomeStrategies.OutcomeStrategy;
import com.evolveum.midpoint.certification.impl.outcomeStrategies.ResponsesSummary;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.fromUri;
import static com.evolveum.midpoint.certification.api.OutcomeUtils.normalizeToNonNull;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortStringLazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ALL_MUST_ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ONE_ACCEPT_ACCEPTS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
@Component
public class AccCertResponseComputationHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertResponseComputationHelper.class);

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

	// should be the case enabled in the following stage?
    boolean advancesToNextStage(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign,
		    List<AccessCertificationResponseType> outcomesToStopOn) {
        if (!AccCertUtil.isCaseRelevantForStage(_case, campaign)) {
            return false;       // it is not relevant even for the current stage (so it won't advance)
        }
        AccessCertificationResponseType currentOutcome = normalizeToNonNull(fromUri(_case.getCurrentStageOutcome()));
        return !outcomesToStopOn.contains(currentOutcome);
    }

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
    AccessCertificationResponseType computeOutcomeForStage(AccessCertificationCaseType _case,
		    AccessCertificationCampaignType campaign, int stageNumber) {
        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stageNumber);
        List<AccessCertificationResponseType> allResponses = getResponses(_case, stageNumber, campaign.getIteration());
	    AccessCertificationResponseType outcome;
        if (allResponses.isEmpty()) {
            outcome = stageDef.getOutcomeIfNoReviewers();
        } else {
            AccessCertificationCaseOutcomeStrategyType outcomeStrategy = defaultIfNull(stageDef.getOutcomeStrategy(), DEFAULT_CASE_STAGE_OUTCOME_STRATEGY);
            OutcomeStrategy strategyImpl = getOutcomeStrategy(outcomeStrategy);
            ResponsesSummary summary = summarize(allResponses);        // TODO eventually merge extraction and summarizing
            outcome = strategyImpl.computeOutcome(summary);
        }
        return normalizeToNonNull(outcome);
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
		return normalizeToNonNull(strategy.computeOutcome(summarize(getOutcomesFromCompletedStages(aCase))));
    }

    // aCase contains outcomes from stages 1..N-1. Outcome from stage N is in currentStageOutcome
	@NotNull
	AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
			String currentStageOutcome) {
    	return computeOverallOutcome(aCase, campaign, fromUri(currentStageOutcome));
	}

	// aCase contains outcomes from stages 1..N-1. Outcome from stage N is in currentStageOutcome
	@NotNull
	AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
			AccessCertificationResponseType currentStageOutcome) {
		List<AccessCertificationResponseType> stageOutcomes = new ArrayList<>(getOutcomesFromCompletedStages(aCase));
        stageOutcomes.add(currentStageOutcome);
        return normalizeToNonNull(getOverallOutcomeStrategy(campaign).computeOutcome(summarize(stageOutcomes)));
    }

	// see https://wiki.evolveum.com/display/midPoint/On+Certification+Campaigns+Iteration
	private List<AccessCertificationResponseType> getResponses(AccessCertificationCaseType _case, int stageNumber, int iteration) {
		List<AccessCertificationResponseType> rv = new ArrayList<>();
		for (AccessCertificationWorkItemType wi : _case.getWorkItem()) {
			if (wi.getStageNumber() == stageNumber) {
				AccessCertificationResponseType response = normalizeToNonNull(fromUri(WorkItemTypeUtil.getOutcome(wi)));
				if (response != AccessCertificationResponseType.NO_RESPONSE || wi.getIteration() == iteration) {
					// i.e. NO_RESPONSE answers from earlier iterations are not taken into account
					rv.add(response);
				}
			}
		}
		return rv;
	}

	// see https://wiki.evolveum.com/display/midPoint/On+Certification+Campaigns+Iteration
	private List<AccessCertificationResponseType> getOutcomesFromCompletedStages(AccessCertificationCaseType aCase) {
		List<AccessCertificationResponseType> rv = new ArrayList<>();
		for (CaseEventType event : aCase.getEvent()) {
			if (event instanceof StageCompletionEventType) {
				StageCompletionEventType stageCompletionEvent = (StageCompletionEventType) event;
				AccessCertificationResponseType outcome = normalizeToNonNull(fromUri(stageCompletionEvent.getOutcome()));
				int iteration = defaultIfNull(stageCompletionEvent.getIteration(), 1);
				if (outcome != NO_RESPONSE || iteration == aCase.getIteration()) {
					rv.add(outcome);
				}
			}
		}
		return rv;
	}
}
