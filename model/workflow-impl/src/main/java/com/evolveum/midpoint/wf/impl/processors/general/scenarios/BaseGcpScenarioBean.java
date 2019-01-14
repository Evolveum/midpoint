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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processors.BaseAuditHelper;
import com.evolveum.midpoint.wf.impl.processors.general.GcpExternalizationHelper;
import com.evolveum.midpoint.wf.impl.processors.general.GeneralChangeProcessorSpecificContent;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralChangeProcessorScenarioType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Base implementation of GcpScenarioBean.
 * Delegates everything to helpers.
 *
 * @author mederly
 */
@Component
public class BaseGcpScenarioBean implements GcpScenarioBean {

    @Autowired
    private GcpExternalizationHelper gcpExternalizationHelper;

    @Autowired
    private BaseAuditHelper baseAuditHelper;

    @Autowired
    private PrismContext prismContext;

    @Override
    public boolean determineActivation(GeneralChangeProcessorScenarioType scenarioType, ModelContext context, com.evolveum.midpoint.task.api.Task taskFromModel, OperationResult result) {
        return true;
    }

//    @Override
//    public PrismObject<? extends WorkItemContents> externalizeWorkItemContents(Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {
//        PrismObject<? extends WorkItemContents> prism = gcpExternalizationHelper.createNewWorkItemContents();
//        gcpExternalizationHelper.fillInQuestionForm(prism.asObjectable().getQuestionForm().asPrismObject(), task, processInstanceVariables, result);
//        return prism;
//    }

//    @Override
//    public ProcessSpecificState externalizeInstanceState(Map<String, Object> variables) throws SchemaException {
//        if (variables.containsKey(CommonProcessVariableNames.VARIABLE_PROCESS_INTERFACE_BEAN_NAME)) {
//            return processInterfaceFinder.getProcessInterface(variables).externalizeProcessInstanceState(variables);
//        } else {
//            return null;
//        }
//    }

    @Override
    public AuditEventRecord prepareProcessInstanceAuditRecord(WfContextType wfContext, WfTask wfTask, AuditEventStage stage, OperationResult result) {
        return baseAuditHelper.prepareProcessInstanceAuditRecord(wfTask, stage, result);
        // TODO what with missing data (delta, result)? We could at least attempt to determine them ...
    }

    @Override
    public AuditEventRecord prepareWorkItemCreatedAuditRecord(WorkItemType workItem, WfTask wfTask,
			OperationResult result) {
        return baseAuditHelper.prepareWorkItemCreatedAuditRecord(workItem, wfTask, result);
        // TODO fill-in missing delta somehow
    }

    @Override
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(WorkItemType workItem, WorkItemEventCauseInformationType cause,
            WfTask wfTask, OperationResult result) {
        return baseAuditHelper.prepareWorkItemDeletedAuditRecord(workItem, cause, wfTask, result);
        // TODO fill-in missing delta somehow
    }

    @Override
    public WfTaskCreationInstruction prepareJobCreationInstruction(GeneralChangeProcessorScenarioType scenarioType, LensContext<?> context, WfTask rootWfTask, com.evolveum.midpoint.task.api.Task taskFromModel, OperationResult result) throws SchemaException {

        GeneralChangeProcessorSpecificContent processorInstruction = new GeneralChangeProcessorSpecificContent(context);
        processorInstruction.setScenarioBeanName(scenarioType.getBeanName());

        WfTaskCreationInstruction instruction = WfTaskCreationInstruction.createWfOnly(rootWfTask.getChangeProcessor(), processorInstruction, null);
        instruction.setProcessName(scenarioType.getProcessName());
        instruction.setRequesterRef(taskFromModel.getOwner());
        instruction.setTaskName("Workflow-monitoring task");
        return instruction;
    }

}
