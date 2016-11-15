/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.*;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectName;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectOid;

/**
 *
 * @author mederly
 */
@Component
public abstract class RuleBasedAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(RuleBasedAspect.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

	@Autowired
	protected ApprovalSchemaHelper approvalSchemaHelper;

    //region ------------------------------------------------------------ Things that execute on request arrival

    @NotNull
	@Override
    public List<PcpChildWfTaskCreationInstruction> prepareTasks(@NotNull ModelContext<?> modelContext,
            PrimaryChangeProcessorConfigurationType wfConfigurationType, @NotNull ObjectTreeDeltas objectTreeDeltas,
            @NotNull Task taskFromModel, @NotNull OperationResult result) throws SchemaException {

		List<PcpChildWfTaskCreationInstruction> instructions = new ArrayList<>();
		PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(taskFromModel, result);

		DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = ((LensContext<?>) modelContext).getEvaluatedAssignmentTriple();
		LOGGER.trace("Processing evaluatedAssignmentTriple:\n{}", DebugUtil.debugDumpLazily(evaluatedAssignmentTriple));
        for (EvaluatedAssignment<?> newAssignment : evaluatedAssignmentTriple.getPlusSet()) {
			LOGGER.trace("Assignment to be added: -> {} ({} policy rules)", newAssignment.getTarget(), newAssignment.getPolicyRules().size());
			List<ApprovalPolicyActionType> approvalActions = new ArrayList<>();
            for (EvaluatedPolicyRule rule : newAssignment.getPolicyRules()) {
				if (rule.getTriggers().isEmpty()) {
					LOGGER.trace("Skipping rule {} that is present but not triggered", rule.getName());
					continue;
				}
				if (rule.getActions() == null || rule.getActions().getApproval() == null) {
					LOGGER.trace("Skipping rule {} that doesn't contain an approval action", rule.getName());
					continue;
				}
				approvalActions.add(rule.getActions().getApproval());
            }
            if (approvalActions.isEmpty()) {
				LOGGER.trace("This assignment hasn't triggered approval actions, continuing with a next one.");
				continue;
			}
			PrismContainerValue<AssignmentType> assignmentValue = newAssignment.getAssignmentType().asPrismContainerValue();
			boolean removed = objectTreeDeltas.subtractFromFocusDelta(new ItemPath(FocusType.F_ASSIGNMENT), assignmentValue);
			if (!removed) {
				throw new IllegalStateException("Assignment with a value of " + assignmentValue.debugDump()
						+ " was not found in deltas: " + objectTreeDeltas.debugDump());
			}
			ApprovalRequest<?> request = createAssignmentApprovalRequest(newAssignment, approvalActions);
			instructions.add(
					prepareAssignmentRelatedTaskInstruction(request, newAssignment, modelContext, taskFromModel, requester, result));
        }
        return instructions;
    }

	private ApprovalRequest<AssignmentType> createAssignmentApprovalRequest(EvaluatedAssignment<?> newAssignment,
			List<ApprovalPolicyActionType> approvalActions) {
		ApprovalSchemaType approvalSchema = null;
		for (ApprovalPolicyActionType action : approvalActions) {
            if (action.getApprovalSchema() != null) {
                approvalSchema = approvalSchemaHelper.mergeIntoSchema(approvalSchema, action.getApprovalSchema());
            } else {
                approvalSchema = approvalSchemaHelper
						.mergeIntoSchema(approvalSchema, action.getApproverExpression(), action.getAutomaticallyApproved());
            }
		}
		assert approvalSchema != null;
		return new ApprovalRequestImpl<>(newAssignment.getAssignmentType(), approvalSchema, prismContext);
	}

	private PcpChildWfTaskCreationInstruction prepareAssignmentRelatedTaskInstruction(ApprovalRequest<?> approvalRequest,
			EvaluatedAssignment<?> evaluatedAssignment, ModelContext<?> modelContext, Task taskFromModel,
			PrismObject<UserType> requester, OperationResult result) throws SchemaException {

		String objectOid = getFocusObjectOid(modelContext);
		String objectName = getFocusObjectName(modelContext);

		assert approvalRequest.getPrismContext() != null;

		LOGGER.trace("Approval request = {}", approvalRequest);

		PrismObject<? extends ObjectType> target = (PrismObject<? extends ObjectType>) evaluatedAssignment.getTarget();
		Validate.notNull(target, "assignment target is null");

		String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";
		String approvalTaskName = "Approve adding " + targetName + " to " + objectName;				// TODO adding?

		PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> instruction =
				PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(getChangeProcessor(), approvalTaskName, approvalRequest);

		instruction.prepareCommonAttributes(this, modelContext, requester);

		ObjectDelta<? extends FocusType> delta = assignmentToDelta(evaluatedAssignment.getAssignmentType(), objectOid);
		instruction.setDeltasToProcess(delta);

		instruction.setObjectRef(modelContext, result);
		instruction.setTargetRef(createObjectRef(target), result);

		String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
		instruction.setTaskName("Approval " + andExecuting + "of assigning " + targetName + " to " + objectName);
		instruction.setProcessInstanceName("Assigning " + targetName + " to " + objectName);

		itemApprovalProcessInterface.prepareStartInstruction(instruction);

		return instruction;
    }

    // creates an ObjectDelta that will be executed after successful approval of the given assignment
	@SuppressWarnings("unchecked")
    private ObjectDelta<? extends FocusType> assignmentToDelta(AssignmentType assignmentType, String objectOid) throws SchemaException {
		return (ObjectDelta<? extends FocusType>) DeltaBuilder.deltaFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT).add(assignmentType.clone().asPrismContainerValue())
				.asObjectDelta(objectOid);
    }

    //endregion

}