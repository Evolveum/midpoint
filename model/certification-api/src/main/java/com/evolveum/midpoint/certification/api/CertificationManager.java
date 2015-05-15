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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import java.util.List;

/**
 * @author mederly
 */
public interface CertificationManager {

    /**
     * Starts a certification campaign.
     *
     * @param certificationDefinition Certification definition for this campaign.
     * @param campaign Specific values for this campaign (optional).
     *                It must not be persistent, i.e. its OID must not be set.
     * @param task
     * @param parentResult
     * @return Information about created campaign. It will be created in the repository as well.
     */
    AccessCertificationCampaignType createCampaign(AccessCertificationDefinitionType certificationDefinition, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    void startStage(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType certDefinition, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    /**
     * Returns a set of certification cases that match a given query.
     *
     * @param campaignOid
     * @param query
     * @param task
     * @param parentResult
     * @return
     * @throws SchemaException
     */
    List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    void recordReviewerDecision(String campaignOid, long caseId, AccessCertificationDecisionType decision) throws ObjectNotFoundException, SchemaException;

}
