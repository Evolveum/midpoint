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

import com.evolveum.midpoint.certification.api.AccessCertificationEventListener;
import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CREATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;

/**
 * @author mederly
 */
@Service(value = "certificationManager")
public class CertificationManagerImpl implements CertificationManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(CertificationManager.class);

    public static final String INTERFACE_DOT = CertificationManager.class.getName() + ".";
    public static final String OPERATION_CREATE_CAMPAIGN = INTERFACE_DOT + "createCampaign";
    public static final String OPERATION_OPEN_NEXT_STAGE = INTERFACE_DOT + "openNextStage";
    public static final String OPERATION_CLOSE_CURRENT_STAGE = INTERFACE_DOT + "closeCurrentStage";
    public static final String OPERATION_RECORD_DECISION = INTERFACE_DOT + "recordDecision";
    public static final String OPERATION_SEARCH_CASES = INTERFACE_DOT + "searchCases";
    public static final String OPERATION_SEARCH_DECISIONS = INTERFACE_DOT + "searchDecisions";
    public static final String OPERATION_CLOSE_CAMPAIGN = INTERFACE_DOT + "closeCampaign";
    public static final String OPERATION_GET_CAMPAIGN_STATISTICS = INTERFACE_DOT + "getCampaignStatistics";

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelService modelService;

    @Autowired
    protected SecurityEnforcer securityEnforcer;

    @Autowired
    protected AccCertGeneralHelper helper;

    @Autowired
    protected AccCertEventHelper eventHelper;

    @Autowired
    protected AccCertResponseComputationHelper computationHelper;

    @Autowired
    protected AccCertQueryHelper queryHelper;

    @Autowired
    protected AccCertUpdateHelper updateHelper;

    @Autowired
    private AccessCertificationRemediationTaskHandler remediationTaskHandler;

    // TODO temporary hack because of some problems in model service...
    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    private Map<String,CertificationHandler> registeredHandlers = new HashMap<>();

    public void registerHandler(String handlerUri, CertificationHandler handler) {
        if (registeredHandlers.containsKey(handlerUri)) {
            throw new IllegalStateException("There is already a handler with URI " + handlerUri);
        }
        registeredHandlers.put(handlerUri, handler);
    }

    public CertificationHandler findCertificationHandler(AccessCertificationCampaignType campaign) {
        if (StringUtils.isBlank(campaign.getHandlerUri())) {
            throw new IllegalArgumentException("No handler URI for access certification campaign " + ObjectTypeUtil.toShortString(campaign));
        }
        CertificationHandler handler = registeredHandlers.get(campaign.getHandlerUri());
        if (handler == null) {
            throw new IllegalStateException("No handler for URI " + campaign.getHandlerUri());
        }
        return handler;
    }

    @Override
    public AccessCertificationCampaignType createCampaign(AccessCertificationDefinitionType certDefinition, AccessCertificationCampaignType campaign,
                                                          Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");
        if (certDefinition == null && campaign == null) {
            throw new IllegalArgumentException("Both certDefinition and campaign are null");
        }

        OperationResult result = parentResult.createSubresult(OPERATION_CREATE_CAMPAIGN);
        try {
            AccessCertificationCampaignType newCampaign = helper.createCampaignObject(certDefinition, campaign, task, result);
            updateHelper.addObject(newCampaign, task, result);
            return newCampaign;
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't create certification campaign: unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void openNextStage(String campaignOid, int requestedStageNumber, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        OperationResult result = parentResult.createSubresult(OPERATION_OPEN_NEXT_STAGE);
        result.addParam("campaignOid", campaignOid);
        result.addParam("requestedStageNumber", requestedStageNumber);

        try {
            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, result);
            result.addParam("campaign", ObjectTypeUtil.toShortString(campaign));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("openNextStage starting for {}", ObjectTypeUtil.toShortString(campaign));
            }

            final int currentStageNumber = campaign.getCurrentStageNumber();
            final int stages = CertCampaignTypeUtil.getNumberOfStages(campaign);
            final AccessCertificationCampaignStateType state = campaign.getState();
            LOGGER.trace("openNextStage: currentStageNumber={}, stages={}, requestedStageNumber={}, state={}", currentStageNumber, stages, requestedStageNumber, state);
            if (IN_REVIEW_STAGE.equals(state)) {
                result.recordFatalError("Couldn't advance to review stage " + requestedStageNumber + " as the stage " + currentStageNumber + " is currently open.");
            } else if (IN_REMEDIATION.equals(state)) {
                result.recordFatalError("Couldn't advance to review stage " + requestedStageNumber + " as the campaign is currently in the remediation phase.");
            } else if (CLOSED.equals(state)) {
                result.recordFatalError("Couldn't advance to review stage " + requestedStageNumber + " as the campaign is already closed.");
            } else if (!REVIEW_STAGE_DONE.equals(state) && !CREATED.equals(state)) {
                throw new IllegalStateException("Unexpected campaign state: " + state);
            } else if (REVIEW_STAGE_DONE.equals(state) && requestedStageNumber != currentStageNumber+1) {
                result.recordFatalError("Couldn't advance to review stage " + requestedStageNumber + " as the campaign is currently in stage " + currentStageNumber);
            } else if (CREATED.equals(state) && requestedStageNumber != 1) {
                result.recordFatalError("Couldn't advance to review stage " + requestedStageNumber + " as the campaign was just created");
            } else if (requestedStageNumber > stages) {
                result.recordFatalError("Couldn't advance to review stage " + requestedStageNumber + " as the campaign has only " + stages + " stages");
            } else {
                CertificationHandler handler = findCertificationHandler(campaign);
                AccessCertificationStageType stage = updateHelper.createStage(campaign, currentStageNumber+1);
                updateHelper.moveToNextStage(campaign, stage, handler, task, result);
                updateHelper.recordMoveToNextStage(campaign, stage, task, result);
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't move to certification campaign stage " + requestedStageNumber + ": unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void closeCurrentStage(String campaignOid, int stageNumberToClose, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        OperationResult result = parentResult.createSubresult(OPERATION_CLOSE_CURRENT_STAGE);
        result.addParam("campaignOid", campaignOid);
        result.addParam("stageNumber", stageNumberToClose);

        try {
            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, result);
            result.addParam("campaign", ObjectTypeUtil.toShortString(campaign));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("closeCurrentStage starting for {}", ObjectTypeUtil.toShortString(campaign));
            }

            final int currentStageNumber = campaign.getCurrentStageNumber();
            final int stages = CertCampaignTypeUtil.getNumberOfStages(campaign);
            final AccessCertificationCampaignStateType state = campaign.getState();
            LOGGER.trace("closeCurrentStage: currentStageNumber={}, stages={}, stageNumberToClose={}, state={}", currentStageNumber, stages, stageNumberToClose, state);

            if (stageNumberToClose != currentStageNumber) {
                result.recordFatalError("Couldn't close review stage " + stageNumberToClose + " as the campaign is not in that stage");
            } else if (!IN_REVIEW_STAGE.equals(state)) {
                result.recordFatalError("Couldn't close review stage " + stageNumberToClose + " as it is currently not open");
            } else {
                updateHelper.recordCloseCurrentState(campaign, task, result);
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't close certification campaign stage " + stageNumberToClose+ ": unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void startRemediation(String campaignOid, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {
        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        OperationResult result = parentResult.createSubresult(OPERATION_CLOSE_CURRENT_STAGE);
        result.addParam("campaignOid", campaignOid);

        try {
            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, result);
            result.addParam("campaign", ObjectTypeUtil.toShortString(campaign));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("startRemediation starting for {}", ObjectTypeUtil.toShortString(campaign));
            }

            final int currentStageNumber = campaign.getCurrentStageNumber();
            final int lastStageNumber = CertCampaignTypeUtil.getNumberOfStages(campaign);
            final AccessCertificationCampaignStateType state = campaign.getState();
            LOGGER.trace("startRemediation: currentStageNumber={}, stages={}, state={}", currentStageNumber, lastStageNumber, state);

            if (currentStageNumber != lastStageNumber) {
                result.recordFatalError("Couldn't start the remediation as the campaign is not in its last stage ("+lastStageNumber+"); current stage: "+currentStageNumber);
            } else if (!REVIEW_STAGE_DONE.equals(state)) {
                result.recordFatalError("Couldn't start the remediation as the last stage was not properly closed.");
            } else {
                if (CertCampaignTypeUtil.isRemediationAutomatic(campaign)) {
                    remediationTaskHandler.launch(campaign, task, result);
                } else {
                    result.recordWarning("The automated remediation is not configured. The campaign state was set to IN REMEDIATION, but all remediation actions have to be done by hand.");
                }
                updateHelper.setStageNumberAndState(campaign, lastStageNumber + 1, IN_REMEDIATION, task, result);

                campaign = updateHelper.refreshCampaign(campaign, task, result);
                eventHelper.onCampaignStageStart(campaign, task, result);
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't start the remediation: unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query,
                                                         Collection<SelectorOptions<GetOperationOptions>> options,
                                                         Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        Validate.notEmpty(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        OperationResult result = parentResult.createSubresult(OPERATION_SEARCH_CASES);

        // temporary implementation: simply fetches the whole campaign and selects requested items by itself
        try {
            return queryHelper.searchCases(campaignOid, query, options, task, result);
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't search for certification cases: unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public List<AccessCertificationCaseType> searchDecisions(ObjectQuery caseQuery,
                                                             String reviewerOid, boolean notDecidedOnly,
                                                             Collection<SelectorOptions<GetOperationOptions>> options,
                                                             Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        Validate.notNull(reviewerOid, "reviewerOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        OperationResult result = parentResult.createSubresult(OPERATION_SEARCH_DECISIONS);

        try {
            return queryHelper.searchDecisions(caseQuery, reviewerOid, notDecidedOnly, options, task, result);
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't search for certification decisions: unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void recordDecision(String campaignOid, long caseId, AccessCertificationDecisionType decision,
                               Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException, ObjectAlreadyExistsException {

        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(decision, "decision");

        OperationResult result = parentResult.createSubresult(OPERATION_RECORD_DECISION);
        try {
            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, result);
            updateHelper.recordDecision(campaign, caseId, decision, result);
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't record reviewer decision: unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void closeCampaign(String campaignOid, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {
        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        OperationResult result = parentResult.createSubresult(OPERATION_CLOSE_CAMPAIGN);

        try {
            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, result);
            updateHelper.closeCampaign(campaign, task, result);
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't close certification campaign: unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public AccessCertificationCasesStatisticsType getCampaignStatistics(String campaignOid, boolean currentStageOnly, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException, ObjectAlreadyExistsException {
        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        OperationResult result = parentResult.createSubresult(OPERATION_GET_CAMPAIGN_STATISTICS);
        try {
            AccessCertificationCasesStatisticsType stat = new AccessCertificationCasesStatisticsType(prismContext);

            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, result);
            int currentStageNumber = campaign.getCurrentStageNumber();
            AccessCertificationCampaignStateType state = campaign.getState();

            List<AccessCertificationCaseType> campaignCases = queryHelper.searchCases(campaign.getOid(), null, null, task, result);

            int accept=0, revoke=0, revokeRemedied=0, reduce=0, reduceRemedied=0, delegate=0, noDecision=0, noResponse=0;
            for (AccessCertificationCaseType _case : campaignCases) {
                if (currentStageOnly && !_case.isEnabled()) {
                    continue;
                }
                AccessCertificationResponseType response = _case.getCurrentResponse();
                if (response == null) {
                    response = AccessCertificationResponseType.NO_RESPONSE;
                }
                switch (response) {
                    case ACCEPT: accept++; break;
                    case REVOKE: revoke++;
                                 if (_case.getRemediedTimestamp() != null) {
                                     revokeRemedied++;
                                 }
                                 break;
                    case REDUCE: reduce++;
                                 if (_case.getRemediedTimestamp() != null) {
                                    reduceRemedied++;       // currently not possible
                                 }
                                 break;
                    case DELEGATE: delegate++; break;
                    case NOT_DECIDED: noDecision++; break;
                    case NO_RESPONSE: noResponse++; break;
                    default: throw new IllegalStateException("Unexpected response: "+response);
                }
            }
            stat.setMarkedAsAccept(accept);
            stat.setMarkedAsRevoke(revoke);
            stat.setMarkedAsRevokeAndRemedied(revokeRemedied);
            stat.setMarkedAsReduce(reduce);
            stat.setMarkedAsReduceAndRemedied(reduceRemedied);
            stat.setMarkedAsDelegate(delegate);
            stat.setMarkedAsNotDecide(noDecision);
            stat.setWithoutResponse(noResponse);
            return stat;
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't get campaign statistics: unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void registerCertificationEventListener(AccessCertificationEventListener listener) {
        eventHelper.registerEventListener(listener);
    }
}
