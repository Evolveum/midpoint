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
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRunType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationTypeType;

import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public interface CertificationManager {

    /**
     * Starts a certification run.
     *
     * @param certificationTypeType Certification type that is a "template" for this certification run. May be null - in that case,
     *                              all the necessary information must be specified in runType parameter.
     * @param runType If present, this can bring additional specifics that are to be merged with data in certification type.
     *                It must not be persistent, i.e. its OID must not be set.
     * @param task
     * @param parentResult
     * @return Information about created run. It must be created in the repository as well.
     */
    AccessCertificationRunType startCertificationRun(AccessCertificationTypeType certificationTypeType, AccessCertificationRunType runType, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException;

    /**
     * Returns a set of certification cases that a given certifier should decide.
     * TODO add search criteria
     *
     * @param certifierOid
     * @param paging Currently, only maxSize is supported
     * @return
     * @throws SchemaException
     */
    List<AccessCertificationCase> getCertificationCasesToDecide(String certifierOid, ObjectPaging paging) throws SchemaException;

}
