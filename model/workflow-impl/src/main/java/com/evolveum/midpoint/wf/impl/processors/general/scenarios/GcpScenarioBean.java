/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.general.scenarios;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processors.StartInstruction;
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

    AuditEventRecord prepareProcessInstanceAuditRecord(ApprovalContextType wfContext, CaseType aCase, AuditEventStage stage, OperationResult result);

    AuditEventRecord prepareWorkItemCreatedAuditRecord(CaseWorkItemType workItem, CaseType aCase, OperationResult result);

    AuditEventRecord prepareWorkItemDeletedAuditRecord(CaseWorkItemType workItem, WorkItemEventCauseInformationType cause,
            CaseType aCase, OperationResult result);

    StartInstruction prepareJobCreationInstruction(GeneralChangeProcessorScenarioType scenarioType, LensContext<?> context, CaseType rootCase, Task taskFromModel, OperationResult result) throws SchemaException;
}
