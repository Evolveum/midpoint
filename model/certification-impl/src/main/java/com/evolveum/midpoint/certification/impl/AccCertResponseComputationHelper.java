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

import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationApprovalStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 * @author mederly
 */
@Component
public class AccCertResponseComputationHelper {

    // should be the case enabled in the following stage?
    public boolean computeEnabled(AccessCertificationCampaignType campaign, AccessCertificationCaseType _case) {
        if (_case.getCurrentStageNumber() != campaign.getStageNumber()) {
            return false;
        }
        if (_case.getCurrentResponse() == null) {
            return true;
        }
        switch (_case.getCurrentResponse()) {
            case REVOKE: return false;
            case REDUCE: return false;
            case ACCEPT: return true;
            case DELEGATE: return true;         // TODO
            case NO_RESPONSE: return true;
            case NOT_DECIDED: return true;
            default: throw new IllegalStateException("Unknown response: " + _case.getCurrentResponse());
        }
    }

    public AccessCertificationResponseType computeResponseForStage(AccessCertificationCaseType _case, AccessCertificationDecisionType newDecision,
                                                                   AccessCertificationCampaignType campaign) {
        int stageNumber = campaign.getStageNumber();
        List<AccessCertificationDecisionType> allDecisions = getDecisions(_case, newDecision, stageNumber);
        return computeResponseForStageInternal(allDecisions, _case, campaign);
    }

    public AccessCertificationResponseType computeResponseForStage(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign) {
        List<AccessCertificationDecisionType> allDecisions = getDecisions(_case, campaign.getStageNumber());
        return computeResponseForStageInternal(allDecisions, _case, campaign);
    }

    private AccessCertificationResponseType computeResponseForStageInternal(List<AccessCertificationDecisionType> allDecisions, AccessCertificationCaseType _case, AccessCertificationCampaignType campaign) {
        int stageNumber = campaign.getStageNumber();
        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stageNumber);
        AccessCertificationApprovalStrategyType approvalStrategy = null;
        if (stageDef != null && stageDef.getReviewerSpecification() != null) {
            approvalStrategy = stageDef.getReviewerSpecification().getApprovalStrategy();
        }
        if (approvalStrategy == null) {
            approvalStrategy = AccessCertificationApprovalStrategyType.ONE_APPROVAL_APPROVES;
        }
        switch (approvalStrategy) {
            case ALL_MUST_APPROVE: return computeUnderAllMustApprove(allDecisions, _case);
            case APPROVED_IF_NOT_DENIED: return computeUnderApprovedIfNotDenied(allDecisions);
            case ONE_APPROVAL_APPROVES: return computeUnderOneApprovalApproves(allDecisions);
            case ONE_DENY_DENIES: return computeUnderOneDenyDenies(allDecisions);
            default: throw new IllegalStateException("Unknown approval strategy: " + approvalStrategy);
        }
    }

    private AccessCertificationResponseType computeUnderApprovedIfNotDenied(List<AccessCertificationDecisionType> allDecisions) {
        AccessCertificationResponseType finalResponse = null;
        for (AccessCertificationDecisionType decision : allDecisions) {
            AccessCertificationResponseType response = decision.getResponse();
            finalResponse = lower(finalResponse, response);
        }
        if (finalResponse == REVOKE || finalResponse == REDUCE) {
            return finalResponse;
        } else {
            return ACCEPT;
        }
    }

    private AccessCertificationResponseType computeUnderAllMustApprove(List<AccessCertificationDecisionType> allDecisions, AccessCertificationCaseType _case) {
        AccessCertificationResponseType finalResponse = null;
        for (AccessCertificationDecisionType decision : allDecisions) {
            AccessCertificationResponseType response = decision.getResponse();
            finalResponse = lower(finalResponse, response);
        }
        // but now check if all reviewers said "APPROVE"
        // we can do that easily: if # of decisions is less than # of reviewers, and final decision seems to be APPROVED, someone must have provided no response
        if (finalResponse == ACCEPT) {
            if (allDecisions.size() < _case.getReviewerRef().size()) {
                return NO_RESPONSE;
            } else {
                return ACCEPT;
            }
        } else {
            return finalResponse != null ? finalResponse : NO_RESPONSE;
        }
    }

    private AccessCertificationResponseType computeUnderOneDenyDenies(List<AccessCertificationDecisionType> allDecisions) {
        AccessCertificationResponseType finalResponse = null;
        boolean atLeastOneApprove = false;
        for (AccessCertificationDecisionType decision : allDecisions) {
            AccessCertificationResponseType response = decision.getResponse();
            if (response == ACCEPT) {
                atLeastOneApprove = true;
            }
            finalResponse = lower(finalResponse, response);
        }
        if (!atLeastOneApprove || finalResponse == REVOKE || finalResponse == REDUCE) {
            return finalResponse != null ? finalResponse : NO_RESPONSE;
        } else {
            return ACCEPT;
        }
    }

    private AccessCertificationResponseType computeUnderOneApprovalApproves(List<AccessCertificationDecisionType> allDecisions) {
        AccessCertificationResponseType finalResponse = null;
        for (AccessCertificationDecisionType decision : allDecisions) {
            final AccessCertificationResponseType response = decision.getResponse();
            if (ACCEPT.equals(response)) {
                return ACCEPT;
            }
            finalResponse = lower(finalResponse, response);
        }
        return finalResponse != null ? finalResponse : NO_RESPONSE;
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

    private AccessCertificationResponseType lower(AccessCertificationResponseType resp1, AccessCertificationResponseType resp2) {
        if (resp1 == null) {
            return resp2;
        } else if (resp2 == null) {
            return resp1;
        }
        if (resp1 == REVOKE || resp2 == REVOKE) {
            return REVOKE;
        }
        if (resp1 == REDUCE || resp2 == REDUCE) {
            return REDUCE;
        }
        if (resp1 == NOT_DECIDED || resp2 == NOT_DECIDED) {
            return NOT_DECIDED;
        }
        if (resp1 == NO_RESPONSE || resp2 == NO_RESPONSE || resp1 == DELEGATE || resp2 == DELEGATE) {
            return NO_RESPONSE;
        }
        if (resp1 == ACCEPT && resp2 == ACCEPT) {
            return ACCEPT;
        }
        throw new IllegalStateException("Unsupported combination: resp1 = " + resp1 + ", resp2 = " + resp2);
    }

}
