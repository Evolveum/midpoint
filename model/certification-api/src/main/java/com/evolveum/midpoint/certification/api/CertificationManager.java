/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.certification.api;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
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

import java.util.Collection;
import java.util.List;

/**
 * BEWARE: CertificationManager is responsible for authorizing all actions carried out through it.
 *
 * @author mederly
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
     * @throws ExpressionEvaluationException 
     */
    AccessCertificationCampaignType createCampaign(String definitionOid, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Opens the next review stage in the certification campaign.
     *
     * If the stage being opened is the first stage, certification cases will be generated for the campaign,
     * depending on the certification definition (scope and handler). In all stages, reviewers will be assigned
     * to cases, based again on the definition (reviewer specification in stage definition and handler).
     *
     * @param campaignOid Certification campaign OID.
     * @param stageNumber Stage that has to be open. This has to be the stage after the current one (or the first one).
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations. 
     */
    void openNextStage(String campaignOid, int stageNumber, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Opens the next stage in the certification campaign.
     *
     * If the stage being opened is the first stage, certification cases will be generated for the campaign,
     * depending on the certification definition (scope and handler). In all stages, reviewers will be assigned
     * to cases, based again on the definition (reviewer specification in stage definition and handler).
     *
     * @param campaignOid Certification campaign OID.
     * @param stageNumber Stage that has to be closed. This has to be the current stage.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations. 
     * 
     */
    void closeCurrentStage(String campaignOid, int stageNumber, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Starts the remediation phase for the campaign.
     * The campaign has to be in the last stage and that stage has to be already closed.
     *
     * @param campaignOid
     * @param task
     * @param result 
     */
    void startRemediation(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Closes a campaign.
     *
     * @param campaignOid
     * @param task
     * @param result 
     */
    void closeCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Returns a set of certification decisions for currently logged-in user.
     * Each decision is returned in context of its certification case. Case has to match a given query.
     * So, in contrast to model.searchContainers(AccessCertificationCaseType...) method that returns specified cases
     * with all their decisions, this one returns a list of cases where each case has at most one decision:
     * the one that corresponds to specified reviewer and current certification stage. Zero decisions means that
     * the reviewer has not provided any decision yet.
     *
     * Query argument for cases is the same as in the model.searchContainers(AccessCertificationCaseType...) call.
     *
     * @param caseQuery Specification of the cases to retrieve.
     * @param notDecidedOnly If true, only response==(NO_DECISION or null) should be returned.
     *                       Although it can be formulated in Query API terms, this would refer to implementation details - so
     *                       the cleaner way is keep this knowledge inside certification module only.
     * @param options Options to use (e.g. RESOLVE_NAMES).
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return A list of relevant certification cases.
     *
     */
    @Deprecated
    List<AccessCertificationCaseType> searchDecisionsToReview(ObjectQuery caseQuery, boolean notDecidedOnly,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException;

    /**
     * Returns a set of certification work items for currently logged-in user.
     * Query argument for cases is the same as in the model.searchContainers(AccessCertificationCaseType...) call.
     *
     * @param caseQuery Specification of the cases to retrieve.
     * @param notDecidedOnly If true, only response==(NO_DECISION or null) should be returned.
     *                       Although it can be formulated in Query API terms, this would refer to implementation details - so
     *                       the cleaner way is keep this knowledge inside certification module only.
     * @param options Options to use (e.g. RESOLVE_NAMES).
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return A list of relevant certification cases.
     * @throws ExpressionEvaluationException 
     *
     */
    List<AccessCertificationWorkItemType> searchOpenWorkItems(ObjectQuery caseQuery, boolean notDecidedOnly,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    int countOpenWorkItems(ObjectQuery caseQuery, boolean notDecidedOnly,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Records a particular decision of a reviewer.
     *  @param campaignOid OID of the campaign to which the decision belongs.
     * @param caseId ID of the certification case to which the decision belongs.
     * @param workItemId ID of the work item to which the decision belongs.
     * @param response The response.
     * @param comment Reviewer's comment.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations. 
     */
    void recordDecision(String campaignOid, long caseId, long workItemId, AccessCertificationResponseType response,
            String comment,
            Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

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
