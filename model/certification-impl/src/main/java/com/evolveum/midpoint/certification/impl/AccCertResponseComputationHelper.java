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

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.outcomeStrategies.OutcomeStrategy;
import com.evolveum.midpoint.certification.impl.outcomeStrategies.ResponsesSummary;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ALL_MUST_ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ONE_ACCEPT_ACCEPTS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

/**
 * @author mederly
 */
@Component
public class AccCertResponseComputationHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertResponseComputationHelper.class);

    public static final AccessCertificationCaseOutcomeStrategyType DEFAULT_CASE_STAGE_OUTCOME_STRATEGY = ONE_ACCEPT_ACCEPTS;
    public static final AccessCertificationCaseOutcomeStrategyType DEFAULT_CASE_OVERALL_OUTCOME_STRATEGY = ALL_MUST_ACCEPT;

    Map<AccessCertificationCaseOutcomeStrategyType, OutcomeStrategy> outcomeStrategyMap = new HashMap<>();

    public void registerOutcomeStrategy(AccessCertificationCaseOutcomeStrategyType strategyName, OutcomeStrategy strategy) {
        outcomeStrategyMap.put(strategyName, strategy);
    }

    private OutcomeStrategy getOutcomeStrategy(AccessCertificationCaseOutcomeStrategyType outcomeStrategy) {
        OutcomeStrategy strategyImpl = outcomeStrategyMap.get(outcomeStrategy);
        if (strategyImpl == null) {
            throw new IllegalStateException("Unknown/unsupported outcome strategy " + outcomeStrategy);
        }
        return strategyImpl;
    }

    // should be the case enabled in the following stage?
    public boolean computeEnabled(AccessCertificationCampaignType campaign, AccessCertificationCaseType _case, List<AccessCertificationResponseType> outcomesToStopOn) {
        if (_case.getStageNumber() != campaign.getStageNumber()) {
            return false;           // it is not enabled in the current stage at all
        }
        AccessCertificationResponseType currentOutcome = OutcomeUtils.fromUri(_case.getCurrentStageOutcome());
        if (currentOutcome == null) {
            currentOutcome = NO_RESPONSE;
        }
        return !outcomesToStopOn.contains(currentOutcome);
    }

    public List<AccessCertificationResponseType> getOutcomesToStopOn(AccessCertificationCampaignType campaign) {
        final List<AccessCertificationResponseType> rv;
        AccessCertificationStageDefinitionType stageDefinition = CertCampaignTypeUtil.getCurrentStageDefinition(campaign);
        if (!stageDefinition.getStopReviewOn().isEmpty() || !stageDefinition.getAdvanceToNextStageOn().isEmpty()) {
            rv = CertCampaignTypeUtil.getOutcomesToStopOn(stageDefinition.getStopReviewOn(), stageDefinition.getAdvanceToNextStageOn());
        } else {
            final AccessCertificationCaseReviewStrategyType reviewStrategy = campaign.getReviewStrategy();
            if (reviewStrategy != null && (!reviewStrategy.getStopReviewOn().isEmpty() || !reviewStrategy.getAdvanceToNextStageOn().isEmpty())) {
                rv = CertCampaignTypeUtil.getOutcomesToStopOn(reviewStrategy.getStopReviewOn(), reviewStrategy.getAdvanceToNextStageOn());
            } else {
                final OutcomeStrategy outcomeStrategy = getOverallOutcomeStrategy(campaign);
                rv = outcomeStrategy.getOutcomesToStopOn();
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Outcomes to stop on for campaign {}, stage {}: {}", toShortString(campaign), campaign.getStageNumber(), rv);
        }
        return rv;
    }

    private OutcomeStrategy getOverallOutcomeStrategy(AccessCertificationCampaignType campaign) {
        final AccessCertificationCaseOutcomeStrategyType strategyName;
        if (campaign.getReviewStrategy() != null && campaign.getReviewStrategy().getOutcomeStrategy() != null) {
            strategyName = campaign.getReviewStrategy().getOutcomeStrategy();
        } else {
            strategyName = DEFAULT_CASE_OVERALL_OUTCOME_STRATEGY;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Outcome strategy for {} is {}", toShortString(campaign), strategyName);
        }
        return getOutcomeStrategy(strategyName);
    }

    public AccessCertificationResponseType computeOutcomeForStage(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign, int stageNunber) {
        List<AccessCertificationResponseType> allResponses = getResponses(_case, stageNunber);
        return computeResponseForStageInternal(allResponses, _case, campaign, stageNunber);
    }

    // allResponses must include null ones (i.e. not provided)
    private AccessCertificationResponseType computeResponseForStageInternal(
            List<AccessCertificationResponseType> allResponses, AccessCertificationCaseType _case, AccessCertificationCampaignType campaign,
            int stageNumber) {
        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stageNumber);
        AccessCertificationCaseOutcomeStrategyType outcomeStrategy = ObjectUtils.defaultIfNull(stageDef.getOutcomeStrategy(), DEFAULT_CASE_STAGE_OUTCOME_STRATEGY);
		OutcomeStrategy strategyImpl = getOutcomeStrategy(outcomeStrategy);
        if (allResponses.isEmpty()) {
            return stageDef.getOutcomeIfNoReviewers();
        } else {
            ResponsesSummary summary = summarize(allResponses);        // TODO eventually merge extraction and summarizing
            return strategyImpl.computeOutcome(summary);
        }
    }

    private List<AccessCertificationResponseType> getResponses(AccessCertificationCaseType _case, int stageNumber) {
        return _case.getWorkItem().stream()
				.filter(wi -> wi.getStageNumber() == stageNumber)
				.map(wi -> ObjectUtils.defaultIfNull(
				                OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(wi)),
                                AccessCertificationResponseType.NO_RESPONSE))
				.collect(Collectors.toList());
    }

    private ResponsesSummary summarize(List<AccessCertificationResponseType> responses) {
        ResponsesSummary summary = new ResponsesSummary();
        for (AccessCertificationResponseType response : responses) {
            if (response == null) {
                summary.add(NO_RESPONSE);
            } else {
                summary.add(response);
            }
        }
        return summary;
    }

    public AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign) {
		final OutcomeStrategy strategy = getOverallOutcomeStrategy(campaign);
		return strategy.computeOutcome(summarize(OutcomeUtils.fromUri(CertCampaignTypeUtil.getOutcomesFromCompletedStages(aCase))));
    }

    // aCase contains outcomes from stages 1..N-1. Outcome from stage N is in currentStageOutcome
	AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
			String currentStageOutcome) {
    	return computeOverallOutcome(aCase, campaign, OutcomeUtils.fromUri(currentStageOutcome));
	}

	AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
			AccessCertificationResponseType currentStageOutcome) {
        final OutcomeStrategy strategy = getOverallOutcomeStrategy(campaign);
        final List<AccessCertificationResponseType> stageOutcomes = new ArrayList<>();
        stageOutcomes.addAll(OutcomeUtils.fromUri(CertCampaignTypeUtil.getOutcomesFromCompletedStages(aCase)));
        stageOutcomes.add(currentStageOutcome);
        return strategy.computeOutcome(summarize(stageOutcomes));
    }
}
