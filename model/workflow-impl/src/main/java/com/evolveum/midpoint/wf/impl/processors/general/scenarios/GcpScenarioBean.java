/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.general.scenarios;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.tasks.StartInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author mederly
 */
public interface GcpScenarioBean {

    /**
     * Determines whether the process should be run in a given situation.
     * (Is applied only if activation condition is null or evaluates to TRUE.)
     *
     * @param scenarioType
     * @param context
     * @param taskFromModel
     * @param result
     * @return
     */
    boolean determineActivation(GeneralChangeProcessorScenarioType scenarioType, ModelContext context, Task taskFromModel, OperationResult result);

    AuditEventRecord prepareProcessInstanceAuditRecord(WfContextType wfContext, CaseType aCase, AuditEventStage stage, OperationResult result);

    AuditEventRecord prepareWorkItemCreatedAuditRecord(CaseWorkItemType workItem, CaseType aCase, OperationResult result);

    AuditEventRecord prepareWorkItemDeletedAuditRecord(CaseWorkItemType workItem, WorkItemEventCauseInformationType cause,
            CaseType aCase, OperationResult result);

    StartInstruction prepareJobCreationInstruction(GeneralChangeProcessorScenarioType scenarioType, LensContext<?> context, CaseType rootCase, Task taskFromModel, OperationResult result) throws SchemaException;
}
