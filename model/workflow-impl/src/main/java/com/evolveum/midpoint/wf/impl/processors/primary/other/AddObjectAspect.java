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

package com.evolveum.midpoint.wf.impl.processors.primary.other;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildJobCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.AddObjectApprovalFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Change aspect that manages addition of an object.
 *
 * @author mederly
 */
public abstract class AddObjectAspect<T extends ObjectType> extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(AddObjectAspect.class);

    //region ------------------------------------------------------------ Things that execute on request arrival

    protected abstract Class<T> getObjectClass();
    protected abstract String getObjectLabel(T object);

    @Override
    public List<PcpChildJobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext,
                                                                               WfConfigurationType wfConfigurationType,
                                                                               ObjectDelta<? extends ObjectType> change,
                                                                               Task taskFromModel, OperationResult result) throws SchemaException {
        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfigurationType, this);
        if (config == null) {
            return null;            // this should not occur (because this aspect is not enabled by default), but check it just to be sure
        }
        if (!primaryChangeAspectHelper.isRelatedToType(modelContext, getObjectClass())) {
            return null;
        }
        List<ApprovalRequest<T>> approvalRequestList = getApprovalRequests(modelContext, config, change, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return null;
        }
        return prepareJobCreateInstructions(modelContext, taskFromModel, result, approvalRequestList);
    }

    private List<ApprovalRequest<T>> getApprovalRequests(ModelContext<?> modelContext, PcpAspectConfigurationType config,
                                                         ObjectDelta<? extends ObjectType> change, OperationResult result) {
        if (change.getChangeType() != ChangeType.ADD) {
            return null;
        }
        T objectType = (T) change.getObjectToAdd().asObjectable().clone();
        change.setObjectToAdd(null);            // make the change empty
        return Arrays.asList(createApprovalRequest(config, objectType));
    }

    // creates an approval request for a given role create request
    private ApprovalRequest<T> createApprovalRequest(PcpAspectConfigurationType config, T objectType) {

        return new ApprovalRequestImpl(objectType, config, prismContext);
    }

    private List<PcpChildJobCreationInstruction> prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel, OperationResult result,
                                                                              List<ApprovalRequest<T>> approvalRequestList) throws SchemaException {
        List<PcpChildJobCreationInstruction> instructions = new ArrayList<>();

        for (ApprovalRequest<T> approvalRequest : approvalRequestList) {     // there should be just one

            assert approvalRequest.getPrismContext() != null;

            LOGGER.trace("Approval request = {}", approvalRequest);

            T objectToAdd = approvalRequest.getItemToApprove();
            Validate.notNull(objectToAdd);
            String objectLabel = getObjectLabel(objectToAdd);

            PrismObject<UserType> requester = primaryChangeAspectHelper.getRequester(taskFromModel, result);

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpChildJobCreationInstruction instruction =
                    PcpChildJobCreationInstruction.createInstruction(getChangeProcessor());

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, modelContext, null, requester);       // objectOid is null (because object does not exist yet)

            // prepare and set the delta that has to be approved
            ObjectDelta<? extends ObjectType> delta = assignmentToDelta(modelContext);
            instruction.setDeltaProcessAndTaskVariables(delta);

            // set the names of midPoint task and activiti process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and executing " : "";
            instruction.setTaskName("Workflow for approving " + andExecuting + "creation of " + objectLabel);
            instruction.setProcessInstanceName("Creating " + objectLabel);

            // setup general item approval process
            String approvalTaskName = "Approve creating " + objectLabel;
            itemApprovalProcessInterface.prepareStartInstruction(instruction, approvalRequest, approvalTaskName);

            instructions.add(instruction);
        }
        return instructions;
    }

    private ObjectDelta<? extends ObjectType> assignmentToDelta(ModelContext<?> modelContext) {
        return modelContext.getFocusContext().getPrimaryDelta();
    }

    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    @Override
    public PrismObject<? extends QuestionFormType> prepareQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getRequestSpecific starting: execution id {}, pid {}, variables = {}", task.getExecutionId(), task.getProcessInstanceId(), variables);
        }

        PrismObjectDefinition<AddObjectApprovalFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(AddObjectApprovalFormType.COMPLEX_TYPE);
        PrismObject<AddObjectApprovalFormType> formPrism = formDefinition.instantiate();
        AddObjectApprovalFormType form = formPrism.asObjectable();

        ApprovalRequest<T> request = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        request.setPrismContext(prismContext);
        Validate.notNull(request, "Approval request is not present among process variables");
        form.setObjectToAdd(getObjectLabel(request.getItemToApprove()));

        form.setRequesterComment(null);     // TODO

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = {}", formPrism.debugDump());
        }
        return formPrism;
    }

    @Override
    public PrismObject<? extends ObjectType> prepareRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ApprovalRequest<T> request = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        request.setPrismContext(prismContext);
        Validate.notNull(request, "Approval request is not present among process variables");
        return request.getItemToApprove().asPrismObject();
    }

    //endregion
}