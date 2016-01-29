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

package com.evolveum.midpoint.certification.api;

import com.evolveum.midpoint.model.api.PolicyViolationException;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

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
     */
    AccessCertificationCampaignType createCampaign(String definitionOid, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException;

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
    void openNextStage(String campaignOid, int stageNumber, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException;

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
     */
    void closeCurrentStage(String campaignOid, int stageNumber, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException;

    /**
     * Starts the remediation phase for the campaign.
     * The campaign has to be in the last stage and that stage has to be already closed.
     *
     * @param campaignOid
     * @param task
     * @param result
     */
    void startRemediation(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException;

    /**
     * Closes a campaign.
     *
     * @param campaignOid
     * @param task
     * @param result
     */
    void closeCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException;

    /**
     * Returns a set of certification decisions that match a given query.
     * Each decision is returned in context of its certification case.
     * So, in contrast to searchCases method that returns specified cases with all their decisions,
     * this one returns a list of cases where each case has at most one decision: the one that corresponds
     * to specified reviewer and current certification stage. Zero decisions means that the reviewer has not
     * provided any decision yet.
     *
     * Query argument for cases is the same as in the searchCases call.
     * Contrary to searchCases, this method allows to collect cases for more than one campaign
     * (e.g. to present a reviewer all of his/her cases).
     * So, instead of campaignOid it will be (when implemented) possible to specify conditions for the campaign in the caseQuery.
     *
     * Contrary to all the other methods, cases returned from this method have campaignRef set - both reference and the campaign object itself.
     * (THIS MAY CHANGE IN THE FUTURE.)
     *
     * Sorting is supported as this:
     *  - name of object, by setting paging.orderBy = objectRef
     *  - name of target, by setting paging.orderBy = targetRef
     *  - name of campaign, by setting paging.orderBy = campaignRef
     *  - name of tenant, by setting paging.orderBy = tenantRef
     *  - name of org referenced, by setting paging.orderBy = orgRef
     *  - deadline or reviewRequestedTimestamp, by setting paging.orderBy = reviewDeadline/reviewRequestedTimestamp
     * @param caseQuery Specification of the cases to retrieve. (In future it may contain restrictions on owning campaign(s).)
     * @param notDecidedOnly If true, only response==(NO_DECISION or null) should be returned.
     *                       Although it can be formulated in Query API terms, this would refer to implementation details - so
     *                       the cleaner way is keep this knowledge inside certification module only.
     * @param options Options to use (currently supported is RESOLVE_NAMES).
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return A list of relevant certification cases.
     *
     */
    List<AccessCertificationCaseType> searchDecisionsToReview(ObjectQuery caseQuery, boolean notDecidedOnly,
                                                              Collection<SelectorOptions<GetOperationOptions>> options,
                                                              Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException;

    /**
     * Records a particular decision of a reviewer.
     *
     * @param campaignOid OID of the campaign to which the decision belongs.
     * @param caseId ID of the certification case to which the decision belongs.
     * @param decision The decision itself.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     */
    void recordDecision(String campaignOid, long caseId, AccessCertificationDecisionType decision,
                        Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException;

    AccessCertificationCasesStatisticsType getCampaignStatistics(String campaignOid, boolean currentStageOnly, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException;

    void registerCertificationEventListener(AccessCertificationEventListener listener);
}
