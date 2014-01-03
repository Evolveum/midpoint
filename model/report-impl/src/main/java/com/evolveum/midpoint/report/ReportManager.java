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

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JasperDesign;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;

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
     * @param report
     * @param parentResult describes report which has to be created
     */
    void runReport(PrismObject<ReportType> object, Task task, OperationResult parentResult);

    /**
     * todo comments [lazyman]
     * todo how to return progress
     *
     * @param cleanupPolicy
     * @param parentResult
     */
    void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult);
	
}
