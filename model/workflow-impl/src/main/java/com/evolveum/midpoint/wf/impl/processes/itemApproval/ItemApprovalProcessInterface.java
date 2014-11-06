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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.wf.impl.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.processes.BaseProcessMidPointInterface;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ItemApprovalProcessState;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ItemApprovalRequestType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessSpecificState;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class ItemApprovalProcessInterface extends BaseProcessMidPointInterface {

    public static final String PROCESS_DEFINITION_KEY = "ItemApproval";

    @Autowired
    private PrismContext prismContext;

    public void prepareStartInstruction(JobCreationInstruction instruction, ApprovalRequest approvalRequest, String approvalTaskName) {
        instruction.setProcessDefinitionKey(PROCESS_DEFINITION_KEY);
        instruction.setSimple(false);
        instruction.setSendStartConfirmation(true);
        instruction.addProcessVariable(ProcessVariableNames.APPROVAL_REQUEST, approvalRequest);
        instruction.addProcessVariable(ProcessVariableNames.APPROVAL_TASK_NAME, approvalTaskName);
        instruction.setProcessInterfaceBean(this);
    }

    @Override
    public ProcessSpecificState externalizeProcessInstanceState(Map<String, Object> variables) {
        PrismContainerDefinition<ItemApprovalProcessState> extDefinition = prismContext.getSchemaRegistry().findContainerDefinitionByType(ItemApprovalProcessState.COMPLEX_TYPE);
        PrismContainer<ItemApprovalProcessState> extStateContainer = extDefinition.instantiate();
        ItemApprovalProcessState extState = extStateContainer.createNewValue().asContainerable();

        PrismContainer extRequestContainer = extDefinition.findContainerDefinition(ItemApprovalProcessState.F_APPROVAL_REQUEST).instantiate();
        ItemApprovalRequestType extRequestType = (ItemApprovalRequestType) extRequestContainer.createNewValue().asContainerable();

        ApprovalRequest<?> intApprovalRequest = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        intApprovalRequest.setPrismContext(prismContext);

        ApprovalSchemaType approvalSchemaType = (ApprovalSchemaType) extRequestContainer.getDefinition().findContainerDefinition(ItemApprovalRequestType.F_APPROVAL_SCHEMA).instantiate().createNewValue().asContainerable();
        intApprovalRequest.getApprovalSchema().toApprovalSchemaType(approvalSchemaType);
        extRequestType.setApprovalSchema(approvalSchemaType);
        extRequestType.setItemToApprove(intApprovalRequest.getItemToApprove());
        extState.setApprovalRequest(extRequestType);

        List<Decision> intDecisions = (List<Decision>) variables.get(ProcessVariableNames.ALL_DECISIONS);
        if (intDecisions != null) {
            for (Decision intDecision : intDecisions) {
                extState.getDecisions().add(intDecision.toDecisionType());
            }
        }

        extState.asPrismContainerValue().setConcreteType(ItemApprovalProcessState.COMPLEX_TYPE);
        return extState;
    }

    @Override
    public List<ObjectReferenceType> prepareApprovedBy(ProcessEvent event) {
        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        if (!ApprovalUtils.isApproved(event.getAnswer())) {
            return retval;
        }
        List<Decision> allDecisions = (List<Decision>) event.getVariable(ProcessVariableNames.ALL_DECISIONS);
        for (Decision decision : allDecisions) {
            if (decision.isApproved()) {
                retval.add(MiscSchemaUtil.createObjectReference(decision.getApproverOid(), SchemaConstants.C_USER_TYPE));
            }
        }
        return retval;
    }


}
