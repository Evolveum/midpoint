/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AccessCertificationWorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

/**
 * BEWARE: CertificationManager is responsible for authorizing all actions carried out through it.
 */
public interface CertificationManager {

    /**
     * Creates a certification campaign: creates AccessCertificationCampaignType object, based on
     * general information in certification definition.
     *
     * Mandatory information in the certification definition are:
     *  - definition name
     *  - definition description
     *  - handlerUri
     *  - scope definition
     *  - stage(s) definition
     *
     * Optional information in the certification definition:
     *  - tenant reference
     *
     * Owner of newly created campaign is the currently logged-on user.
     *
     * The campaign will NOT be started upon creation. It should be started explicitly by calling openNextStage method.
     *
     * @param definitionOid OID of certification definition for this campaign.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return Object for the created campaign. It will be stored in the repository as well.
     */
    AccessCertificationCampaignType createCampaign(String definitionOid, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Opens the next review stage in the certification campaign.
     *
     * If the stage being opened is the first stage, certification cases will be generated for the campaign,
     * depending on the certification definition (scope and handler). In all stages, reviewers will be assigned
     * to cases, based again on the definition (reviewer specification in stage definition and handler).
     * @param campaignOid Certification campaign OID.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     */
    void openNextStage(String campaignOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Opens the next stage in the certification campaign.
     *
     * If the stage being opened is the first stage, certification cases will be generated for the campaign,
     * depending on the certification definition (scope and handler). In all stages, reviewers will be assigned
     * to cases, based again on the definition (reviewer specification in stage definition and handler).
     * @param campaignOid Certification campaign OID.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     *
     */
    void closeCurrentStage(String campaignOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Starts the remediation phase for the campaign.
     * The campaign has to be in the last stage and that stage has to be already closed.
     */
    void startRemediation(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Closes a campaign.
     */
    void closeCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Reiterates a closed campaign.
     */
    void reiterateCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Records a particular decision of a reviewer.
     *
     * @param workItemId Complex ID of the work item to which the decision belongs.
     * @param response The response.
     * @param comment Reviewer's comment.
     * @param preAuthorized Is the request already authorized?
     * @param task Task in context of which all operations will take place.
     * @param result Result for the operations.
     */
    void recordDecision(
            @NotNull AccessCertificationWorkItemId workItemId,
            AccessCertificationResponseType response,
            String comment,
            boolean preAuthorized,
            Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Provides statistical information about outcomes of cases in a given campaign.
     *
     * Doesn't require special authorization. Delegates authorization decisions to model (i.e. requires READ authorization on the campaign object).
     *
     * @param campaignOid OID of the campaign to report on
     * @param currentStageOnly Whether to report on stage outcomes for current-stage cases (if true), or to report on overall outcomes of all cases (if false).
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return filled-in statistics object
     */
    AccessCertificationCasesStatisticsType getCampaignStatistics(String campaignOid, boolean currentStageOnly, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    void registerCertificationEventListener(AccessCertificationEventListener listener);

    void cleanupCampaigns(@NotNull CleanupPolicyType policy, Task task, OperationResult result);
}
