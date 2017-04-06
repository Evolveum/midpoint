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

package com.evolveum.midpoint.wf.impl.processors.primary.assignments;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ReferenceResolver;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.RelationResolver;
import com.evolveum.midpoint.wf.impl.processes.modifyAssignment.AssignmentModification;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Change aspect that manages assignment modification approval.
 * It starts one process instance for each assignment change that has to be approved.
 *
 * T is type of the objects being assigned (AbstractRoleType, ResourceType).
 * F is the type of the objects to which assignments are made (UserType, AbstractRoleType).
 *
 * Assumption: assignment target is never modified
 *
 * @author mederly
 */
@Component
public abstract class ModifyAssignmentAspect<T extends ObjectType, F extends FocusType> extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyAssignmentAspect.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

	//region ------------------------------------------------------------ Things that execute on request arrival

    @NotNull
    @Override
    public List<PcpChildWfTaskCreationInstruction> prepareTasks(@NotNull ObjectTreeDeltas objectTreeDeltas,
			ModelInvocationContext ctx, @NotNull OperationResult result) throws SchemaException {
        if (!isFocusRelevant(ctx.modelContext) || objectTreeDeltas.getFocusChange() == null) {
            return Collections.emptyList();
        }
        List<ApprovalRequest<AssignmentModification>> approvalRequestList = getApprovalRequests(ctx.modelContext,
                ctx.wfConfiguration, objectTreeDeltas.getFocusChange(), ctx.taskFromModel, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return Collections.emptyList();
        }
        return prepareJobCreateInstructions(ctx.modelContext, ctx.taskFromModel, result, approvalRequestList);
    }

    private List<ApprovalRequest<AssignmentModification>> getApprovalRequests(ModelContext<?> modelContext,
            WfConfigurationType wfConfigurationType, ObjectDelta<? extends ObjectType> change, Task taskFromModel,
            OperationResult result) throws SchemaException {
        if (change.getChangeType() != ChangeType.MODIFY) {
            return null;
        }
        PrismObject<F> focusOld = (PrismObject<F>) modelContext.getFocusContext().getObjectOld();
        F focusTypeOld = focusOld.asObjectable();

        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfigurationType, this);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Relevant assignments in focus modify delta: ");
        }

        List<ApprovalRequest<AssignmentModification>> approvalRequestList = new ArrayList<>();

        final ItemPath ASSIGNMENT_PATH = new ItemPath(UserType.F_ASSIGNMENT);

        PrismContainer<AssignmentType> assignmentsOld = focusOld.findContainer(ASSIGNMENT_PATH);

        // deltas sorted by assignment to which they are related
        Map<Long,List<ItemDeltaType>> deltasById = new HashMap<>();

        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();
        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();
            if (!ASSIGNMENT_PATH.isSubPath(delta.getPath())) {
                continue;
            }

            Long id = getAssignmentIdFromDeltaPath(assignmentsOld, delta.getPath());            // id may be null
            AssignmentType assignmentType = getAssignmentToBeModified(assignmentsOld, id);
            if (isAssignmentRelevant(assignmentType)) {
                T target = getAssignmentApprovalTarget(assignmentType, result);
                boolean approvalRequired = shouldAssignmentBeApproved(config, target);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(" - target: {} (approval required = {})", target, approvalRequired);
                }
                if (approvalRequired) {
                    addToDeltas(deltasById, assignmentType.getId(), delta);
                    deltaIterator.remove();
                }
            }
        }

        if (!deltasById.isEmpty()) {
            for (Map.Entry<Long,List<ItemDeltaType>> entry : deltasById.entrySet()) {
                Long id = entry.getKey();
                AssignmentType assignmentType = getAssignmentToBeModified(assignmentsOld, id);
                AssignmentType aCopy = cloneAndCanonicalizeAssignment(assignmentType);
                T target = getAssignmentApprovalTarget(assignmentType, result);
                ApprovalRequest approvalRequest = createApprovalRequestForModification(config, aCopy, target, entry.getValue(),
                        createRelationResolver(target, result), createReferenceResolver(modelContext, taskFromModel, result));
                approvalRequestList.add(approvalRequest);
            }
        }
        return approvalRequestList;
    }

    private void addToDeltas(Map<Long, List<ItemDeltaType>> deltasById, Long id, ItemDelta delta) throws SchemaException {
        List<ItemDeltaType> deltas = deltasById.get(id);
        if (deltas == null) {
            deltas = new ArrayList<>();
            deltasById.put(id, deltas);
        }
        Collection<ItemDeltaType> itemDeltaTypes = DeltaConvertor.toItemDeltaTypes(delta);
        deltas.addAll(itemDeltaTypes);
    }

    // path's first segment is "assignment"
    public static Long getAssignmentIdFromDeltaPath(PrismContainer<AssignmentType> assignmentsOld, ItemPath path) throws SchemaException {
        assert path.getSegments().size() > 1;
        ItemPathSegment idSegment = path.getSegments().get(1);
        if (idSegment instanceof IdItemPathSegment) {
            return ((IdItemPathSegment) idSegment).getId();
        }
        // id-less path, e.g. assignment/validFrom -- we try to determine ID from the objectOld.
        if (assignmentsOld.size() == 0) {
            return null;
        } else if (assignmentsOld.size() == 1) {
            return assignmentsOld.getValues().get(0).getId();
        } else {
            throw new SchemaException("Illegal path " + path + ": cannot determine which assignment to modify");
        }
    }

    public static AssignmentType getAssignmentToBeModified(PrismContainer<AssignmentType> assignmentsOld, Long id) {
        if (id == null && assignmentsOld.size() == 1) {
            return assignmentsOld.getValue().asContainerable();
        }
        PrismContainerValue<AssignmentType> value = assignmentsOld.getValue(id);
        if (value == null) {
            throw new IllegalStateException("No assignment value with id " + id + " in user old");
        }
        return value.asContainerable();
    }


    private List<PcpChildWfTaskCreationInstruction> prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel, OperationResult result, List<ApprovalRequest<AssignmentModification>> approvalRequestList) throws SchemaException {
        List<PcpChildWfTaskCreationInstruction> instructions = new ArrayList<>();

        String focusName = MiscDataUtil.getFocusObjectName(modelContext);

        for (ApprovalRequest<AssignmentModification> approvalRequest : approvalRequestList) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Approval request = {}", approvalRequest);
            }

            AssignmentModification itemToApprove = approvalRequest.getItemToApprove();

            T target = (T) itemToApprove.getTarget();
            Validate.notNull(target);
            String targetName = getTargetDisplayName(target);

            String focusOid = MiscDataUtil.getFocusObjectOid(modelContext);
            PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(taskFromModel, result);

			String approvalTaskName = "Approve modifying assignment of " + targetName + " to " + focusName;

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpChildWfTaskCreationInstruction instruction =
                    PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(
                            getChangeProcessor(), approvalTaskName,
							approvalRequest.getApprovalSchemaType(), null);

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, modelContext, requester);

            // prepare and set the delta that has to be approved
            ObjectDelta<? extends ObjectType> delta = requestToDelta(modelContext, approvalRequest, focusOid);
            instruction.setDeltasToProcess(delta);

            instruction.setObjectRef(modelContext, result);
            instruction.setTargetRef(ObjectTypeUtil.createObjectRef(target), result);

            // set the names of midPoint task and activiti process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
            instruction.setTaskName("Approval " + andExecuting + " of modifying assignment of " + targetName + " to " + focusName);
            instruction.setProcessInstanceName("Modifying assignment of " + targetName + " to " + focusName);

            // setup general item approval process
            itemApprovalProcessInterface.prepareStartInstruction(instruction);

            instructions.add(instruction);
        }
        return instructions;
    }

    private ObjectDelta<? extends ObjectType> requestToDelta(ModelContext<?> modelContext, ApprovalRequest<AssignmentModification> approvalRequest, String objectOid) throws SchemaException {
        List<ItemDelta> modifications = new ArrayList<>();
        Class<? extends ObjectType> focusClass = primaryChangeAspectHelper.getFocusClass(modelContext);
        for (ItemDeltaType itemDeltaType : approvalRequest.getItemToApprove().getModifications()) {
            modifications.add(DeltaConvertor.createItemDelta(itemDeltaType, focusClass, prismContext));
        }
        return ObjectDelta.createModifyDelta(objectOid, modifications, focusClass, ((LensContext) modelContext).getPrismContext());
    }
    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    //endregion


    //region ------------------------------------------------------------ Things to override in concrete aspect classes

    // a quick check whether expected focus type (User, Role) matches the actual focus type in current model operation context
    protected abstract boolean isFocusRelevant(ModelContext modelContext);

    // is the assignment relevant for a given aspect? (e.g. is this an assignment of a role?)
    protected abstract boolean isAssignmentRelevant(AssignmentType assignmentType);

    // should the given assignment be approved? (typically, does the target object have an approver specified?)
    protected abstract boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, T target);

    // before creating a delta for the assignment, it has to be cloned and canonicalized by removing full target object
    protected abstract AssignmentType cloneAndCanonicalizeAssignment(AssignmentType a);

    // creates an approval requests (e.g. by providing approval schema) for a given assignment and a target
    protected abstract ApprovalRequest<AssignmentModification> createApprovalRequestForModification(PcpAspectConfigurationType config,
            AssignmentType assignmentType, T target, List<ItemDeltaType> modifications, RelationResolver relationResolver,
            ReferenceResolver referenceResolver);

    // retrieves the relevant target for a given assignment - a role, an org, or a resource
    protected abstract T getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result);

    // creates name to be displayed in the question form (may be overriden by child objects)
    protected String getTargetDisplayName(T target) {
        if (target.getName() != null) {
            return target.getName().getOrig();
        } else {
            return target.getOid();
        }
    }
    //endregion
}