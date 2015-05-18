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

package com.evolveum.midpoint.model.api;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import java.util.Collection;
import java.util.List;

/**
 * Interface to access certification related functionality. E.g. launching certification campaigns,
 * filling-in certification forms, etc.
 *
 * EXPERIMENTAL. May (and probably will) change in the future.
 *
 * Please use CertificationManager for now.
 *
 * @author mederly
 */
public interface AccessCertificationService {

    AccessCertificationCampaignType createCampaign(AccessCertificationDefinitionType certificationDefinition, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    void startStage(AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException;

    List<AccessCertificationCaseType> searchDecisions(String campaignOid, String reviewerOid,
                                                      ObjectQuery query,
                                                      Collection<SelectorOptions<GetOperationOptions>> options,
                                                      Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException;

    void recordReviewerDecision(String campaignOid, long caseId, AccessCertificationDecisionType decision,
                                Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException, ObjectAlreadyExistsException;
}
