/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.report.api;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import java.io.IOException;
import java.io.InputStream;

/**
 * todo comments [lazyman]
 *
 * WORK IN PROGRESS
 *
 * @author lazyman
 * @author katkav
 */
public interface ReportManager {
    /**
     * todo comments [lazyman]
     *
     * @param report
     * @param parentResult describes report which has to be created
     */
    void runReport(PrismObject<ReportType> object, PrismContainer<ReportParameterType> params, Task task, OperationResult parentResult);

    /**
     * todo comments [lazyman]
     * todo how to return progress
     *
     * @param cleanupPolicy
     * @param parentResult
     */
    void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult);

    /**
     * todo comments [lazyman]
     *
     * @param reportOutputOid
     * @param parentResult
     * @return
     */
	InputStream getReportOutputData(String reportOutputOid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException, IOException;
    
   
    void deleteReportOutput(ReportOutputType reportOutput, OperationResult parentResult) throws Exception;
}
