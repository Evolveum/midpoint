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

package com.evolveum.midpoint.wf.impl.processors.primary.other;

import com.evolveum.midpoint.model.api.context.ModelContext;
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
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is a preliminary version of 'password approval' process aspect. The idea is that in some cases, a user may request
 * changing (resetting) his password, but if not enough authentication information is available, the change may be
 * subject to manual approval.
 *
 * Exact conditions should be coded into getApprovalRequestList method. Currently, we only test for ANY password change
 * request and push it into approval process.
 *
 * DO NOT USE THIS ASPECT IN PRODUCTION UNLESS YOU KNOW WHAT YOU ARE DOING
 *
 * @author mederly
 */
@Component
public class ChangePasswordAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordAspect.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ItemApprovalProcessInterface itemApprovalProcessInterface;

    @NotNull
	@Override
    public List<PcpChildWfTaskCreationInstruction> prepareTasks(@NotNull ObjectTreeDeltas objectTreeDeltas,
			ModelInvocationContext ctx, @NotNull OperationResult result) throws SchemaException {

        List<ApprovalRequest<String>> approvalRequestList = new ArrayList<>();
        List<PcpChildWfTaskCreationInstruction> instructions = new ArrayList<>();

        ObjectDelta changeRequested = objectTreeDeltas.getFocusChange();

        if (changeRequested == null || changeRequested.getChangeType() != ChangeType.MODIFY) {
            return Collections.emptyList();
        }

        Iterator<? extends ItemDelta> deltaIterator = changeRequested.getModifications().iterator();

        ItemPath passwordPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();

            // this needs to be customized and enhanced; e.g. to start wf process only when not enough authentication info is present in the request
            // also, what if we replace whole 'credentials' container?
            if (passwordPath.equivalent(delta.getPath())) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Found password-changing delta, moving it into approval request. Delta = " + delta.debugDump());
                }
                ApprovalRequest<String> approvalRequest = createApprovalRequest(delta, ctx.modelContext, ctx.taskFromModel, result);
                approvalRequestList.add(approvalRequest);
                instructions.add(createStartProcessInstruction(ctx.modelContext, delta, approvalRequest, ctx.taskFromModel, result));
                deltaIterator.remove();
            }
        }
        return instructions;
    }

    private ApprovalRequest<String> createApprovalRequest(ItemDelta delta, ModelContext<?> modelContext, Task taskFromModel,
			OperationResult result) {

        ObjectReferenceType approverRef = new ObjectReferenceType();
        approverRef.setOid(SystemObjectsType.USER_ADMINISTRATOR.value());
        approverRef.setType(UserType.COMPLEX_TYPE);

        List<ObjectReferenceType> approvers = new ArrayList<ObjectReferenceType>();
        approvers.add(approverRef);

        ApprovalRequest<String> request = new ApprovalRequestImpl<>("Password change", null, null, approvers,
                Collections.emptyList(), null, prismContext);
        approvalSchemaHelper.prepareSchema(request.getApprovalSchemaType(), createRelationResolver((PrismObject) null, result),
                createReferenceResolver(modelContext, taskFromModel, result));
        return request;
    }

    private PcpChildWfTaskCreationInstruction createStartProcessInstruction(ModelContext<?> modelContext, ItemDelta delta, ApprovalRequest approvalRequest, Task taskFromModel, OperationResult result) throws SchemaException {

        String userName = MiscDataUtil.getFocusObjectName(modelContext);
        String objectOid = MiscDataUtil.getFocusObjectOid(modelContext);
        PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(taskFromModel, result);

        String approvalTaskName = "Approve changing password for " + userName;

        // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
        PcpChildWfTaskCreationInstruction instruction =
                PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(
                        getChangeProcessor(), approvalTaskName, approvalRequest.getApprovalSchema(),
                        approvalRequest.getApprovalSchemaType(), null);

        // set some common task/process attributes
        instruction.prepareCommonAttributes(this, modelContext, requester);

        // prepare and set the delta that has to be approved
        instruction.setDeltasToProcess(itemDeltaToObjectDelta(objectOid, delta));

        instruction.setObjectRef(modelContext, result);
        instruction.setTargetRef(null, result);

        // set the names of midPoint task and activiti process instance
        instruction.setTaskName("Approval of password change for " + userName);
        instruction.setProcessInstanceName("Changing password for " + userName);

        // setup general item approval process
        itemApprovalProcessInterface.prepareStartInstruction(instruction);

        return instruction;
    }

    private ObjectDelta<Objectable> itemDeltaToObjectDelta(String objectOid, ItemDelta delta) {
        return (ObjectDelta<Objectable>) (ObjectDelta) ObjectDelta.createModifyDelta(objectOid, delta, UserType.class, prismContext);
    }

}
