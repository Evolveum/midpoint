/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.report;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;

import java.util.Collection;
import java.util.List;

/**
 * todo comments [lazyman]
 *
 * WORK IN PROGRESS
 *
 * @author lazyman
 */
public interface ReportManager {

    /**
     * todo comments [lazyman]
     *
     * @param oid
     * @param options
     * @param parentResult
     * @return
     * @throws ObjectNotFoundException
     * @throws SchemaException
     */
    PrismObject<ReportType> getReport(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
                                      OperationResult parentResult) 
                            throws ObjectNotFoundException, SchemaException, ConfigurationException, 
                            CommunicationException, SecurityViolationException;

    /**
     * todo comments [lazyman]
     *
     * @param object
     * @param parentResult
     * @return
     * @throws ObjectAlreadyExistsException
     * @throws SchemaException
     */
    String addReport(PrismObject<ReportType> object, OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException, SecurityViolationException, 
            PolicyViolationException, ConfigurationException, CommunicationException,
            ExpressionEvaluationException, ObjectNotFoundException;

    /**
     * todo comments [lazyman]
     *
     * @param query
     * @param options
     * @param parentResult
     * @return
     * @throws SchemaException
     */
    List<PrismObject<ReportType>> searchReports(ObjectQuery query,
                                                Collection<SelectorOptions<GetOperationOptions>> options,
                                                OperationResult parentResult)
                                  throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
                                  SecurityViolationException;

    /**
     * todo comments [lazyman]
     *
     * @param query
     * @param parentResult
     * @return
     * @throws SchemaException
     */
    int countReports(ObjectQuery query, OperationResult parentResult) throws SchemaException;

    /**
     * todo comments [lazyman]
     *
     * @param oid
     * @param modifications
     * @param parentResult
     * @throws ObjectNotFoundException
     * @throws SchemaException
     * @throws ObjectAlreadyExistsException
     */
    void modifyReport(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult)
    		throws ObjectAlreadyExistsException, SchemaException, SecurityViolationException, 
    		PolicyViolationException, ConfigurationException, CommunicationException,
    		ExpressionEvaluationException, ObjectNotFoundException;

    /**
     * todo comments [lazyman]
     *
     * @param oid
     * @param parentResult
     * @throws ObjectNotFoundException
     */
    void deleteReport(String oid, OperationResult parentResult) 
    		throws ObjectNotFoundException, ConsistencyViolationException,
			CommunicationException, SchemaException, ConfigurationException, PolicyViolationException,
			SecurityViolationException;

    /**
     * todo comments [lazyman]
     *
     * @param report
     * @param parentResult describes report which has to be created
     */
    void runReport(PrismObject<ReportType> report, Task task, OperationResult parentResult);

    /**
     * todo comments [lazyman]
     * todo how to return progress
     *
     * @param cleanupPolicy
     * @param parentResult
     */
    void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult);
}
