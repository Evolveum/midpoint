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

package com.evolveum.midpoint.wf.impl.processors.primary.user;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.addrole.AddRoleVariableNames;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.modifyResourceAssignment.ResourceAssignmentModification;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildJobCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.ResourceAssignmentModificationApprovalFormType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Change aspect that manages resource assignment modification approval.
 * It starts one process instance for each resource that has to be approved.
 *
 * @author mederly
 */
@Component
public class ModifyResourceAssignmentAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyResourceAssignmentAspect.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ItemApprovalProcessInterface itemApprovalProcessInterface;

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    //region ------------------------------------------------------------ Things that execute on request arrival

    @Override
    public List<PcpChildJobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, WfConfigurationType wfConfigurationType, ObjectDelta<? extends ObjectType> change, Task taskFromModel, OperationResult result) throws SchemaException {

        if (!primaryChangeAspectHelper.isUserRelated(modelContext)) {
            return null;
        }
        List<ApprovalRequest<ResourceAssignmentModification>> approvalRequestList = getApprovalRequests(modelContext, wfConfigurationType, change, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return null;
        }
        return prepareJobCreateInstructions(modelContext, taskFromModel, result, approvalRequestList);
    }

    private List<ApprovalRequest<ResourceAssignmentModification>> getApprovalRequests(ModelContext<?> modelContext, WfConfigurationType wfConfigurationType, ObjectDelta<? extends ObjectType> change, OperationResult result) throws SchemaException {
        if (change.getChangeType() != ChangeType.MODIFY) {
            return null;
        }
        PrismObject<UserType> userOld = (PrismObject) modelContext.getFocusContext().getObjectOld();

        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(
                wfConfigurationType, this);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resource-related assignments in user modify delta: ");
        }

        List<ApprovalRequest<ResourceAssignmentModification>> approvalRequestList = new ArrayList<>();

        final ItemPath ASSIGNMENT_PATH = new ItemPath(UserType.F_ASSIGNMENT);

        PrismContainer<AssignmentType> assignmentsOld = userOld.findContainer(ASSIGNMENT_PATH);

        // deltas sorted by assignment to which they are related
        Map<Long,List<ItemDeltaType>> deltasById = new HashMap<>();

        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();
        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();
            if (!ASSIGNMENT_PATH.isSubPath(delta.getPath())) {
                continue;
            }

            Long id = ModifyRoleAssignmentAspect.getAssignmentIdFromDeltaPath(assignmentsOld, delta.getPath());            // id may be null
            AssignmentType assignmentType = ModifyRoleAssignmentAspect.getAssignmentToBeModified(assignmentsOld, id);

            ResourceType resourceType = primaryChangeAspectHelper.resolveResourceRef(assignmentType, result);
            if (resourceType == null) {
                continue;
            }
            boolean approvalRequired = shouldAssignmentBeApproved(config, resourceType);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(" - resource: {} (approval required = {})", resourceType, approvalRequired);
            }
            if (approvalRequired) {
                addToDeltas(deltasById, assignmentType.getId(), delta);
                deltaIterator.remove();
            }
        }

        if (!deltasById.isEmpty()) {
            for (Map.Entry<Long,List<ItemDeltaType>> entry : deltasById.entrySet()) {
                Long id = entry.getKey();
                AssignmentType assignmentType = ModifyRoleAssignmentAspect.getAssignmentToBeModified(assignmentsOld, id);
                AssignmentType aCopy = assignmentType.clone();
                PrismContainerValue.copyDefinition(aCopy, assignmentType);
                ResourceType resourceType = primaryChangeAspectHelper.resolveResourceRef(assignmentType, result);   // todo eliminate redundant fetch
                ResourceAssignmentModification itemToApprove = new ResourceAssignmentModification(aCopy, resourceType, entry.getValue());
                ApprovalRequest approvalRequest = new ApprovalRequestImpl(itemToApprove, config, null, resourceType.getBusiness().getApproverRef(), null, null, prismContext);
                approvalRequestList.add(approvalRequest);
            }
        }
        return approvalRequestList;
    }

    // TODO deduplicate
    protected boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, ResourceType resourceType) {
        return primaryChangeAspectHelper.hasApproverInformation(config) ||
                (resourceType.getBusiness() != null && !resourceType.getBusiness().getApproverRef().isEmpty());
    }


    private void addToDeltas(Map<Long, List<ItemDeltaType>> deltasById, Long id, ItemDelta delta) throws SchemaException {
        List<ItemDeltaType> deltas = deltasById.get(id);
        if (deltas == null) {
            deltas = new ArrayList<>();
            deltasById.put(id, deltas);
        }
        Collection<ItemDeltaType> itemDeltaTypes = DeltaConvertor.toPropertyModificationTypes(delta);
        deltas.addAll(itemDeltaTypes);
    }

    private List<PcpChildJobCreationInstruction> prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel, OperationResult result, List<ApprovalRequest<ResourceAssignmentModification>> approvalRequestList) throws SchemaException {
        List<PcpChildJobCreationInstruction> instructions = new ArrayList<>();

        String userName = MiscDataUtil.getFocusObjectName(modelContext);

        for (ApprovalRequest<ResourceAssignmentModification> approvalRequest : approvalRequestList) {

            assert(approvalRequest.getPrismContext() != null);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Approval request = {}", approvalRequest);
            }

            ResourceAssignmentModification itemToApprove = approvalRequest.getItemToApprove();

            ResourceType resourceType = itemToApprove.getResourceType();
            Validate.notNull(resourceType);
            Validate.notNull(resourceType.getName());
            String resourceName = resourceType.getName().getOrig();

            String objectOid = primaryChangeAspectHelper.getObjectOid(modelContext);
            PrismObject<UserType> requester = primaryChangeAspectHelper.getRequester(taskFromModel, result);

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpChildJobCreationInstruction instruction =
                    PcpChildJobCreationInstruction.createInstruction(getChangeProcessor());

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, modelContext, objectOid, requester);

            // prepare and set the delta that has to be approved
            ObjectDelta<? extends ObjectType> delta = requestToDelta(modelContext, approvalRequest, objectOid);
            instruction.setDeltaProcessAndTaskVariables(delta);

            // set the names of midPoint task and activiti process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and executing " : "";
            instruction.setTaskName("Workflow for approving " + andExecuting + "modifying assignment of " + resourceName + " to " + userName);
            instruction.setProcessInstanceName("Modifying assignment of " + resourceName + " to " + userName);

            // setup general item approval process
            String approvalTaskName = "Approve modifying assignment of " + resourceName + " to " + userName;
            itemApprovalProcessInterface.prepareStartInstruction(instruction, approvalRequest, approvalTaskName);

            // set some aspect-specific variables
            instruction.addProcessVariable(AddRoleVariableNames.USER_NAME, userName);

            instructions.add(instruction);
        }
        return instructions;
    }

    private ObjectDelta<? extends ObjectType> requestToDelta(ModelContext<?> modelContext, ApprovalRequest<ResourceAssignmentModification> approvalRequest, String objectOid) throws SchemaException {
        List<ItemDelta> modifications = new ArrayList<>();
        for (ItemDeltaType itemDeltaType : approvalRequest.getItemToApprove().getModifications()) {
            modifications.add(DeltaConvertor.createItemDelta(itemDeltaType, UserType.class, prismContext));
        }
        return ObjectDelta.createModifyDelta(objectOid, modifications, UserType.class, ((LensContext) modelContext).getPrismContext());
    }
    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    @Override
    public PrismObject<? extends QuestionFormType> prepareQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getRequestSpecific starting: execution id " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);
        }

        PrismObjectDefinition<ResourceAssignmentModificationApprovalFormType> formDefinition =
                prismContext.getSchemaRegistry().findObjectDefinitionByType(ResourceAssignmentModificationApprovalFormType.COMPLEX_TYPE);
        PrismObject<ResourceAssignmentModificationApprovalFormType> formPrism = formDefinition.instantiate();
        ResourceAssignmentModificationApprovalFormType form = formPrism.asObjectable();

        form.setUser((String) variables.get(AddRoleVariableNames.USER_NAME));

        // todo check type compatibility
        ApprovalRequest request = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        request.setPrismContext(prismContext);
        Validate.notNull(request, "Approval request is not present among process variables");

        ResourceAssignmentModification itemToApprove = (ResourceAssignmentModification) request.getItemToApprove();
        Validate.notNull(itemToApprove, "Approval request does not contain an item to approve");

        ResourceType resourceType = itemToApprove.getResourceType();
        form.setResource(resourceType.getName() == null ? resourceType.getOid() : resourceType.getName().getOrig());        // ==null should not occur
        //TODO form.setRequesterComment(itemToApprove.getDescription());

        ObjectDeltaType objectDeltaType = new ObjectDeltaType();
        objectDeltaType.setOid("?");
        objectDeltaType.setChangeType(ChangeTypeType.MODIFY);
        objectDeltaType.setObjectType(UserType.COMPLEX_TYPE);
        objectDeltaType.getItemDelta().addAll(itemToApprove.getModifications());
        form.setModification(objectDeltaType);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + formPrism.debugDump());
        }
        return formPrism;
    }

    @Override
    public PrismObject<? extends ObjectType> prepareRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ApprovalRequest<ResourceAssignmentModification> approvalRequest = (ApprovalRequest<ResourceAssignmentModification>)
                variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        approvalRequest.setPrismContext(prismContext);
        if (approvalRequest == null) {
            throw new IllegalStateException("No approval request in activiti task " + task);
        }

        return repositoryService.getObject(ResourceType.class, approvalRequest.getItemToApprove().getResourceType().getOid(), null, result);
    }

    //endregion
}