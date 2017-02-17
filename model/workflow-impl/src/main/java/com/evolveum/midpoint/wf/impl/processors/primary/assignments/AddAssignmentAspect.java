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
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalSpecificContent;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectName;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectOid;

/**
 * Aspect for adding assignments of any type (abstract role or resource).
 *
 * T is type of the objects being assigned (AbstractRoleType, ResourceType).
 * F is the type of the objects to which assignments are made (UserType, AbstractRoleType).
 *
 * @author mederly
 */
@Component
public abstract class AddAssignmentAspect<T extends ObjectType, F extends FocusType> extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(AddAssignmentAspect.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

    @Autowired
    protected AssignmentHelper assignmentHelper;

    //region ------------------------------------------------------------ Things that execute on request arrival

    @NotNull
    @Override
    public List<PcpChildWfTaskCreationInstruction> prepareTasks(@NotNull ObjectTreeDeltas objectTreeDeltas,
			ModelInvocationContext ctx, @NotNull OperationResult result) throws SchemaException {
        if (!isFocusRelevant(ctx.modelContext) || objectTreeDeltas.getFocusChange() == null) {
            return Collections.emptyList();
        }
        List<ApprovalRequest<AssignmentType>> approvalRequestList = getApprovalRequests(ctx.modelContext,
                baseConfigurationHelper.getPcpConfiguration(ctx.wfConfiguration), objectTreeDeltas.getFocusChange(), ctx.taskFromModel, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return Collections.emptyList();
        }
        return prepareTaskInstructions(ctx.modelContext, ctx.taskFromModel, result, approvalRequestList);
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequests(ModelContext<?> modelContext,
            PrimaryChangeProcessorConfigurationType wfConfigurationType, ObjectDelta<? extends ObjectType> change,
            Task taskFromModel, OperationResult result) {
        if (change.getChangeType() != ChangeType.ADD && change.getChangeType() != ChangeType.MODIFY) {
            return null;
        }
        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfigurationType, this);
        if (change.getChangeType() == ChangeType.ADD) {
            return getApprovalRequestsFromFocusAdd(config, change, modelContext, taskFromModel, result);
        } else {
            return getApprovalRequestsFromFocusModify(config, modelContext.getFocusContext().getObjectOld(), change, modelContext, taskFromModel, result);
        }
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequestsFromFocusAdd(PcpAspectConfigurationType config,
            ObjectDelta<? extends ObjectType> change,
            ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        LOGGER.trace("Relevant assignments in focus add delta:");

        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<>();
        FocusType focusType = (FocusType) change.getObjectToAdd().asObjectable();
        Iterator<AssignmentType> assignmentTypeIterator = focusType.getAssignment().iterator();
        while (assignmentTypeIterator.hasNext()) {
            AssignmentType a = assignmentTypeIterator.next();
            if (isAssignmentRelevant(a)) {
                T specificObjectType = getAssignmentApprovalTarget(a, result);
                boolean approvalRequired = shouldAssignmentBeApproved(config, specificObjectType);
                LOGGER.trace(" - {} (approval required = {})", specificObjectType, approvalRequired);
                if (approvalRequired) {
                    AssignmentType aCopy = cloneAndCanonicalizeAssignment(a);
                    approvalRequestList.add(createApprovalRequest(config, aCopy, specificObjectType, modelContext, taskFromModel, result));
                    assignmentTypeIterator.remove();
					miscDataUtil.generateFocusOidIfNeeded(modelContext, change);
                }
            }
        }
        return approvalRequestList;
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequestsFromFocusModify(PcpAspectConfigurationType config,
            PrismObject<?> focusOld,
            ObjectDelta<? extends ObjectType> change, ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        LOGGER.trace("Relevant assignments in focus modify delta:");

        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<>();
        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        final ItemPath ASSIGNMENT_PATH = new ItemPath(FocusType.F_ASSIGNMENT);

        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();
            if (!ASSIGNMENT_PATH.equivalent(delta.getPath())) {
                continue;
            }

            if (delta.getValuesToAdd() != null && !delta.getValuesToAdd().isEmpty()) {
                Iterator<PrismContainerValue<AssignmentType>> valueIterator = delta.getValuesToAdd().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<AssignmentType> assignmentValue = valueIterator.next();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Assignment to add = {}", assignmentValue.debugDump());
                    }
                    ApprovalRequest<AssignmentType> req = processAssignmentToAdd(config, assignmentValue, modelContext, taskFromModel, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            if (delta.getValuesToReplace() != null && !delta.getValuesToReplace().isEmpty()) {
                Iterator<PrismContainerValue<AssignmentType>> valueIterator = delta.getValuesToReplace().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<AssignmentType> assignmentValue = valueIterator.next();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Assignment to replace = {}", assignmentValue.debugDump());
                    }
                    if (existsEquivalentValue(focusOld, assignmentValue)) {
                        continue;
                    }
                    ApprovalRequest<AssignmentType> req = processAssignmentToAdd(config, assignmentValue, modelContext,
                            taskFromModel, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            // let's sanitize the delta
            if (delta.getValuesToAdd() != null && delta.getValuesToAdd().isEmpty()) {         // empty set of values to add is an illegal state
                delta.resetValuesToAdd();
            }
            if (delta.getValuesToAdd() == null && delta.getValuesToReplace() == null && delta.getValuesToDelete() == null) {
                deltaIterator.remove();
            }
        }
        return approvalRequestList;
    }

    private boolean existsEquivalentValue(PrismObject<?> focusOld, PrismContainerValue<AssignmentType> assignmentValue) {
        FocusType focusType = (FocusType) focusOld.asObjectable();
        for (AssignmentType existing : focusType.getAssignment()) {
            if (existing.asPrismContainerValue().equalsRealValue(assignmentValue)) {
                return true;
            }
        }
        return false;
    }

    private ApprovalRequest<AssignmentType> processAssignmentToAdd(PcpAspectConfigurationType config,
            PrismContainerValue<AssignmentType> assignmentCVal, ModelContext<?> modelContext, Task taskFromModel,
            OperationResult result) {
        AssignmentType assignmentType = assignmentCVal.asContainerable();
        if (isAssignmentRelevant(assignmentType)) {
            T specificObjectType = getAssignmentApprovalTarget(assignmentType, result);
            boolean approvalRequired = shouldAssignmentBeApproved(config, specificObjectType);
            LOGGER.trace(" - {} (approval required = {})", specificObjectType, approvalRequired);
            if (approvalRequired) {
                AssignmentType aCopy = cloneAndCanonicalizeAssignment(assignmentType);
                return createApprovalRequest(config, aCopy, specificObjectType, modelContext, taskFromModel, result);
            }
        }
        return null;
    }

    private List<PcpChildWfTaskCreationInstruction> prepareTaskInstructions(ModelContext<?> modelContext, Task taskFromModel,
            OperationResult result, List<ApprovalRequest<AssignmentType>> approvalRequestList) throws SchemaException {

        List<PcpChildWfTaskCreationInstruction> instructions = new ArrayList<>();
		String assigneeOid = getFocusObjectOid(modelContext);
        String assigneeName = getFocusObjectName(modelContext);
        PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(taskFromModel, result);

        for (ApprovalRequest<AssignmentType> approvalRequest : approvalRequestList) {

            LOGGER.trace("Approval request = {}", approvalRequest);
            AssignmentType assignmentType = approvalRequest.getItemToApprove();
            T target = getAssignmentApprovalTarget(assignmentType, result);
            Validate.notNull(target, "No target in assignment to be approved");

            String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";
            String approvalTaskName = "Approve adding " + targetName + " to " + assigneeName;

            PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> instruction =
                    PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(
                            getChangeProcessor(), approvalTaskName, approvalRequest.getApprovalSchema(),
                            approvalRequest.getApprovalSchemaType(),
							null);

            instruction.prepareCommonAttributes(this, modelContext, requester);

            ObjectDelta<? extends FocusType> delta = assignmentToDelta(modelContext, assignmentType, assigneeOid);
            instruction.setDeltasToProcess(delta);

            instruction.setObjectRef(modelContext, result);
            instruction.setTargetRef(createObjectRef(target), result);

            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
            instruction.setTaskName("Approval " + andExecuting + "of assigning " + targetName + " to " + assigneeName);
            instruction.setProcessInstanceName("Assigning " + targetName + " to " + assigneeName);

            itemApprovalProcessInterface.prepareStartInstruction(instruction);

            instructions.add(instruction);
        }
        return instructions;
    }

    // creates an ObjectDelta that will be executed after successful approval of the given assignment
	@SuppressWarnings("unchecked")
    private ObjectDelta<? extends FocusType> assignmentToDelta(ModelContext<?> modelContext, AssignmentType assignmentType, String objectOid) {
        PrismObject<FocusType> focus = (PrismObject<FocusType>) modelContext.getFocusContext().getObjectNew();
        PrismContainerDefinition<AssignmentType> prismContainerDefinition = focus.getDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);

        ItemDelta<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> addRoleDelta = new ContainerDelta<>(new ItemPath(), FocusType.F_ASSIGNMENT, prismContainerDefinition, prismContext);
        PrismContainerValue<AssignmentType> assignmentValue = assignmentType.asPrismContainerValue().clone();
        addRoleDelta.addValueToAdd(assignmentValue);

        Class focusClass = primaryChangeAspectHelper.getFocusClass(modelContext);
        return ObjectDelta.createModifyDelta(objectOid, addRoleDelta, focusClass, modelContext.getPrismContext());
    }

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
    protected abstract ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config,
            AssignmentType assignmentType, T target, ModelContext<?> modelContext, Task taskFromModel, OperationResult result);

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