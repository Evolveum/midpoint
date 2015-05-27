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

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.model.api.PolicyViolationException;
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

/**
 * @author mederly
 */
public interface CertificationHandler {

    /**
     * Creates a new Certification Campaign from a Certification Type and (prototype) Certification Run.
     * By default, merges all the data from type and run (specific data from the run take precedence).
     * Individual handlers can modify this behavior if needed.
     *
     * @param certificationTypeType
     * @param runType
     * @param task
     * @param result
     * @return
     * @throws SecurityViolationException
     */
//    AccessCertificationCampaignType createCampaign(AccessCertificationDefinitionType certificationDefinition, AccessCertificationCampaignType campaign, Task task, OperationResult result) throws SecurityViolationException;

    /**
     * Starts the campaign. E.g. creates all the certification cases.
     *
     * @param runType
     * @param task
     * @param result
     */
    void moveToNextStage(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException;

    void doRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, Task task, OperationResult caseResult) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;
}
