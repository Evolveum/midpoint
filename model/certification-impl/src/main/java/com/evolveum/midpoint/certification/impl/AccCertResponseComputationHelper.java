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

import com.evolveum.midpoint.certification.impl.outcomeStrategies.ResponsesSummary;
import com.evolveum.midpoint.certification.impl.outcomeStrategies.OutcomeStrategy;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseReviewStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseStageOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ALL_MUST_ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ONE_ACCEPT_ACCEPTS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.DELEGATE;
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
        if (_case.getCurrentStageNumber() != campaign.getStageNumber()) {
            return false;           // it is not enabled in the current stage at all
        }
        final AccessCertificationResponseType currentOutcome;
        if (_case.getCurrentStageOutcome() != null) {
            currentOutcome =_case.getCurrentStageOutcome();
        } else {
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

    public AccessCertificationResponseType computeOutcomeForStage(AccessCertificationCaseType _case, AccessCertificationDecisionType newDecision,
                                                                  AccessCertificationCampaignType campaign) {
        int stageNumber = campaign.getStageNumber();
        List<AccessCertificationDecisionType> allDecisions = getDecisions(_case, newDecision, stageNumber);
        return computeResponseForStageInternal(allDecisions, _case, campaign, campaign.getStageNumber());
    }

    public AccessCertificationResponseType computeInitialResponseForStage(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign, int stageNumber) {
        return computeResponseForStageInternal(new ArrayList<AccessCertificationDecisionType>(), _case, campaign, stageNumber);
    }

    public AccessCertificationResponseType computeOutcomeForStage(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign) {
        List<AccessCertificationDecisionType> allDecisions = getDecisions(_case, campaign.getStageNumber());
        return computeResponseForStageInternal(allDecisions, _case, campaign, campaign.getStageNumber());
    }

    private AccessCertificationResponseType computeResponseForStageInternal(
            List<AccessCertificationDecisionType> allDecisions, AccessCertificationCaseType _case, AccessCertificationCampaignType campaign,
            int stageNumber) {
        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stageNumber);
        AccessCertificationCaseOutcomeStrategyType outcomeStrategy = stageDef.getOutcomeStrategy();
        AccessCertificationResponseType outcomeIfNoReviewers = stageDef.getOutcomeIfNoReviewers();
        if (outcomeStrategy == null) {
            outcomeStrategy = DEFAULT_CASE_STAGE_OUTCOME_STRATEGY;
        }
        OutcomeStrategy strategyImpl = getOutcomeStrategy(outcomeStrategy);

        if (_case.getCurrentReviewerRef().isEmpty()) {
            return outcomeIfNoReviewers;
        } else {
            List<AccessCertificationResponseType> responses = extractResponses(allDecisions, _case.getCurrentReviewerRef());
            ResponsesSummary summary = summarize(responses);        // TODO eventually merge extraction and summarizing
            return strategyImpl.computeOutcome(summary);
        }
    }

    private List<AccessCertificationResponseType> extractResponses(List<AccessCertificationDecisionType> decisions, List<ObjectReferenceType> reviewerRef) {
        List<AccessCertificationResponseType> rv = new ArrayList<>(reviewerRef.size());
        for (AccessCertificationDecisionType decision : decisions) {
            rv.add(decision.getResponse());
        }
        while (rv.size() < reviewerRef.size()) {
            rv.add(null);
        }
        return rv;
    }

    private List<AccessCertificationDecisionType> getDecisions(AccessCertificationCaseType _case, AccessCertificationDecisionType newDecision, int stageNumber) {
        List<AccessCertificationDecisionType> rv = new ArrayList<>();
        for (AccessCertificationDecisionType decision : _case.getDecision()) {
            if (decision.getStageNumber() == stageNumber && !Objects.equals(decision.getReviewerRef().getOid(), newDecision.getReviewerRef().getOid())) {
                rv.add(decision);
            }
        }
        rv.add(newDecision);
        return rv;
    }

    private List<AccessCertificationDecisionType> getDecisions(AccessCertificationCaseType _case, int stageNumber) {
        List<AccessCertificationDecisionType> rv = new ArrayList<>();
        for (AccessCertificationDecisionType decision : _case.getDecision()) {
            if (decision.getStageNumber() == stageNumber) {
                rv.add(decision);
            }
        }
        return rv;
    }

    private ResponsesSummary summarize(List<AccessCertificationResponseType> responses) {
        ResponsesSummary summary = new ResponsesSummary();
        for (AccessCertificationResponseType response : responses) {
            if (response == null || response == DELEGATE) {
                summary.add(NO_RESPONSE);
            } else {
                summary.add(response);
            }
        }
        return summary;
    }

    public AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign) {
		final OutcomeStrategy strategy = getOverallOutcomeStrategy(campaign);
		final List<AccessCertificationResponseType> stageOutcomes = new ArrayList<>();
		for (AccessCertificationCaseStageOutcomeType stageOutcome : aCase.getCompletedStageOutcome()) {
			stageOutcomes.add(stageOutcome.getOutcome());
		}
		return strategy.computeOutcome(summarize(stageOutcomes));
    }

    // aCase contains outcomes from stages 1..N-1. Outcome from stage N is in currentStageOutcome
	AccessCertificationResponseType computeOverallOutcome(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
			AccessCertificationResponseType currentStageOutcome) {
        final OutcomeStrategy strategy = getOverallOutcomeStrategy(campaign);
        final List<AccessCertificationResponseType> stageOutcomes = new ArrayList<>();
        for (AccessCertificationCaseStageOutcomeType stageOutcome : aCase.getCompletedStageOutcome()) {
            stageOutcomes.add(stageOutcome.getOutcome());
        }
        stageOutcomes.add(currentStageOutcome);
        return strategy.computeOutcome(summarize(stageOutcomes));
    }
}
