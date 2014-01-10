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

package com.evolveum.midpoint.wf.processors.primary.user;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is a preliminary version of 'password approval' process wrapper. The idea is that in some cases, a user may request
 * changing (resetting) his password, but if not enough authentication information is available, the change may be
 * subject to manual approval.
 *
 * Exact conditions should be coded into getApprovalRequestList method. Currently, we only test for ANY password change
 * request and push it into approval process.
 *
 * DO NOT USE THIS WRAPPER IN PRODUCTION UNLESS YOU KNOW WHAT YOU ARE DOING :)
 *
 * @author mederly
 */
@Component
public class ChangePasswordWrapper extends BaseWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordWrapper.class);

    @Autowired
    private PrismContext prismContext;

    @Override
    public List<JobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change, Task taskFromModel, OperationResult result) throws SchemaException {

        List<ApprovalRequest<String>> approvalRequestList = new ArrayList<ApprovalRequest<String>>();
        List<JobCreationInstruction> instructions = new ArrayList<JobCreationInstruction>();

        if (change.getChangeType() != ChangeType.MODIFY) {
            return null;
        }

        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        ItemPath passwordPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();

            // this needs to be customized and enhanced; e.g. to start wf process only when not enough authentication info is present in the request
            // also, what if we replace whole 'credentials' container?
            if (passwordPath.equivalent(delta.getPath())) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Found password-changing delta, moving it into approval request. Delta = " + delta.debugDump());
                }
                ApprovalRequest<String> approvalRequest = createApprovalRequest(delta);
                approvalRequestList.add(approvalRequest);
                instructions.add(createStartProcessInstruction(modelContext, delta, approvalRequest, taskFromModel, result));
                deltaIterator.remove();
            }
        }
        return instructions;
    }

    @Override
    public PrismObject<? extends ObjectType> getRequestSpecificData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) {
        return null;        // todo implement this
    }

    @Override
    public PrismObject<? extends ObjectType> getRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) {
        return null;        // todo implement this
    }

    private ApprovalRequest<String> createApprovalRequest(ItemDelta delta) {

        ObjectReferenceType approverRef = new ObjectReferenceType();
        approverRef.setOid(SystemObjectsType.USER_ADMINISTRATOR.value());
        approverRef.setType(UserType.COMPLEX_TYPE);

        List<ObjectReferenceType> approvers = new ArrayList<ObjectReferenceType>();
        approvers.add(approverRef);

        return new ApprovalRequestImpl("Password change", null, approvers, null, null, prismContext);
    }

    private JobCreationInstruction createStartProcessInstruction(ModelContext<?> modelContext, ItemDelta delta, ApprovalRequest approvalRequest, Task taskFromModel, OperationResult result) throws SchemaException {

        String userName = MiscDataUtil.getObjectName(modelContext);
        String objectOid = wrapperHelper.getObjectOid(modelContext);
        PrismObject<UserType> requester = wrapperHelper.getRequester(taskFromModel, result);

        JobCreationInstruction instruction = JobCreationInstruction.createWfProcessChildJob(getChangeProcessor());

        wrapperHelper.prepareCommonInstructionAttributes(changeProcessor, instruction, modelContext, objectOid, requester);

        instruction.setProcessDefinitionKey(GENERAL_APPROVAL_PROCESS);
        instruction.setSimple(false);

        instruction.setTaskName(new PolyStringType("Workflow for approving password change for " + userName));
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME, "Changing password for " + userName);
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_START_TIME, new Date());

        instruction.addProcessVariable(ProcessVariableNames.APPROVAL_REQUEST, approvalRequest);
        instruction.addProcessVariable(ProcessVariableNames.APPROVAL_TASK_NAME, "Approve changing password for " + userName);

        instruction.setExecuteApprovedChangeImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));

        ObjectDelta objectDelta = itemDeltaToObjectDelta(objectOid, delta);
        wrapperHelper.setDeltaProcessAndTaskVariables(instruction, objectDelta);

        return instruction;
    }

    private ObjectDelta<Objectable> itemDeltaToObjectDelta(String objectOid, ItemDelta delta) {
        return (ObjectDelta<Objectable>) (ObjectDelta) ObjectDelta.createModifyDelta(objectOid, delta, UserType.class, prismContext);
    }

}
