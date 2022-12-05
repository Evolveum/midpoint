/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaHelper;
import com.evolveum.midpoint.wf.impl.processors.ConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpStartInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;
import static com.evolveum.midpoint.prism.delta.ChangeType.ADD;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

/**
 * Part of PolicyRuleBasedAspect related to assignments.
 */
@Component
public class AssignmentPolicyAspectPart {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentPolicyAspectPart.class);
    private static final String OP_EXTRACT_ASSIGNMENT_BASED_INSTRUCTIONS = AssignmentPolicyAspectPart.class.getName() +
            ".extractAssignmentBasedInstructions";

    @Autowired private PolicyRuleBasedAspect main;
    @Autowired protected ApprovalSchemaHelper approvalSchemaHelper;
    @Autowired protected PrismContext prismContext;
    @Autowired protected ConfigurationHelper configurationHelper;
    @Autowired protected LocalizationService localizationService;
    @Autowired protected ModelInteractionService modelInteractionService;

    void extractAssignmentBasedInstructions(ObjectTreeDeltas<?> objectTreeDeltas, PrismObject<? extends FocusType> requester,
            List<PcpStartInstruction> instructions, ModelInvocationContext<?> ctx, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(OP_EXTRACT_ASSIGNMENT_BASED_INSTRUCTIONS)
                .setMinor()
                .build();
        ApprovalProcessStartInstructionCreationTraceType trace;
        if (result.isTracingNormal(ApprovalProcessStartInstructionCreationTraceType.class)) {
            trace = new ApprovalProcessStartInstructionCreationTraceType(prismContext);
            result.addTrace(trace);
        } else {
            trace = null;
        }
        try {
            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = ((LensContext<?>) ctx.modelContext)
                    .getEvaluatedAssignmentTriple();
            LOGGER.trace("Processing evaluatedAssignmentTriple:\n{}", DebugUtil.debugDumpLazily(evaluatedAssignmentTriple));
            if (evaluatedAssignmentTriple == null) {
                return;
            }

            List<PcpStartInstruction> newInstructions = new ArrayList<>();
            for (EvaluatedAssignment assignmentAdded : evaluatedAssignmentTriple.union()) {
                addIgnoreNull(newInstructions,
                        createInstructionFromAssignment(assignmentAdded, objectTreeDeltas, requester, ctx, result));
            }
            int newInstructionsCount = newInstructions.size();
            if (trace != null) {
                for (PcpStartInstruction newInstruction : newInstructions) {
                    trace.getCaseRef().add(ObjectTypeUtil.createObjectRefWithFullObject(newInstruction.getCase(), prismContext));
                }
            }
            instructions.addAll(newInstructions);
            CompiledGuiProfile adminGuiConfiguration;
            try {
                adminGuiConfiguration = modelInteractionService.getCompiledGuiProfile(ctx.task, result);
            } catch (CommunicationException | ConfigurationException | SecurityViolationException
                    | ExpressionEvaluationException e) {
                throw new SystemException(e.getMessage(), e);
            }
            Integer limit = adminGuiConfiguration.getRoleManagement() != null ?
                    adminGuiConfiguration.getRoleManagement().getAssignmentApprovalRequestLimit() : null;
            LOGGER.trace("Assignment-related approval instructions: {}; limit is {}", newInstructionsCount, limit);
            if (limit != null && newInstructionsCount > limit) {
                // TODO think about better error reporting
                throw new IllegalStateException(
                        "Assignment approval request limit (" + limit + ") exceeded: you are trying to submit "
                                + newInstructionsCount + " requests");
            }
            if (limit != null) {
                result.addContext("assignmentApprovalRequestLimit", limit);
            }
            result.addReturn("instructionsCreated", newInstructions.size());
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private PcpStartInstruction createInstructionFromAssignment(
            EvaluatedAssignment evaluatedAssignment, @NotNull ObjectTreeDeltas<?> objectTreeDeltas,
            PrismObject<? extends FocusType> requester, ModelInvocationContext<?> ctx, OperationResult result) throws SchemaException {

        // We collect all target rules; hoping that only relevant ones are triggered.
        // For example, if we have assignment policy rule on induced role, it will get here.
        // But projector will take care not to trigger it unless the rule is capable (e.g. configured)
        // to be triggered in such a situation
        List<EvaluatedPolicyRule> triggeredApprovalActionRules =
                main.selectTriggeredApprovalActionRules(evaluatedAssignment.getAllTargetsPolicyRules());
        logApprovalActions(evaluatedAssignment, triggeredApprovalActionRules);

        // Currently we can deal only with assignments that have a specific target
        PrismObject<?> targetObject = evaluatedAssignment.getTarget();
        if (targetObject == null) {
            if (!triggeredApprovalActionRules.isEmpty()) {
                throw new IllegalStateException("No target in " + evaluatedAssignment + ", but with "
                        + triggeredApprovalActionRules.size() + " triggered approval action rule(s)");
            } else {
                return null;
            }
        }

        if (ctx.modelContext.getFocusContext().isDelete()) {
            LOGGER.debug("Focus is going to be deleted. There's no point in approving any assignment changes.");
            return null;
        }

        // Let's construct the approval schema plus supporting triggered approval policy rule information
        // Here we also treat default "rules" when no policy rules match.
        ApprovalSchemaBuilder.Result approvalSchemaResult = createSchemaWithRules(triggeredApprovalActionRules,
                evaluatedAssignment, ctx, result);
        if (approvalSchemaHelper.shouldBeSkipped(approvalSchemaResult.schemaType)) {
            return null;
        }

        // Cut assignment from delta, prepare task instruction
        ObjectDelta<? extends ObjectType> deltaToApprove;
        if (evaluatedAssignment.isBeingAdded() || evaluatedAssignment.isBeingDeleted()) {
            deltaToApprove = factorOutAssignmentValue(evaluatedAssignment, objectTreeDeltas, ctx);
        } else {
            deltaToApprove = factorOutAssignmentModifications(evaluatedAssignment, objectTreeDeltas);
        }
        if (deltaToApprove == null) {
            return null;
        }

        ObjectDelta<? extends ObjectType> focusDelta = objectTreeDeltas.getFocusChange();
        if (focusDelta.isAdd()) {
            generateFocusOidIfNeeded(ctx.modelContext, focusDelta);
        }
        return prepareAssignmentRelatedStartInstruction(approvalSchemaResult, evaluatedAssignment, deltaToApprove, requester,
                ctx, result);
    }

    private void generateFocusOidIfNeeded(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change) {
        if (modelContext.getFocusContext().getOid() != null) {
            return;
        }

        String newOid = OidUtil.generateOid();
        LOGGER.trace("This is ADD operation with no focus OID provided. Generated new OID to be used: {}", newOid);
        if (change.getChangeType() != ADD) {
            throw new IllegalStateException("Change type is not ADD for no-oid focus situation: " + change);
        } else if (change.getObjectToAdd() == null) {
            throw new IllegalStateException("Object to add is null for change: " + change);
        } else if (change.getObjectToAdd().getOid() != null) {
            throw new IllegalStateException("Object to add has already an OID present: " + change);
        }
        change.getObjectToAdd().setOid(newOid);
        ((LensFocusContext<?>) modelContext.getFocusContext()).setOid(newOid);
    }

    private <T extends ObjectType> ObjectDelta<T> factorOutAssignmentModifications(EvaluatedAssignment evaluatedAssignment,
            ObjectTreeDeltas<T> objectTreeDeltas) {
        Long id = evaluatedAssignment.getAssignmentId();
        if (id == null) {
            // Should never occur: assignments to be modified must have IDs.
            throw new IllegalStateException("None or unnumbered assignment in " + evaluatedAssignment);
        }
        ItemPath assignmentValuePath = ItemPath.create(FocusType.F_ASSIGNMENT, id);

        ObjectDelta<T> focusDelta = objectTreeDeltas.getFocusChange();
        assert focusDelta != null;
        ObjectDelta.FactorOutResultSingle<T> factorOutResult = focusDelta.factorOut(singleton(assignmentValuePath), false);
        if (factorOutResult.offspring == null) {
            LOGGER.trace("No modifications for an assignment, skipping approval action(s). Assignment = {}", evaluatedAssignment);
            return null;
        }
        return factorOutResult.offspring;
    }

    private ObjectDelta<? extends ObjectType> factorOutAssignmentValue(EvaluatedAssignment evaluatedAssignment,
            @NotNull ObjectTreeDeltas<?> objectTreeDeltas, ModelInvocationContext<?> ctx) throws SchemaException {
        assert evaluatedAssignment.isBeingAdded() || evaluatedAssignment.isBeingDeleted();
        @SuppressWarnings("unchecked")
        PrismContainerValue<AssignmentType> assignmentValue = evaluatedAssignment.getAssignment().asPrismContainerValue();
        boolean assignmentRemoved = evaluatedAssignment.isBeingDeleted();
        boolean reallyRemoved = objectTreeDeltas.subtractFromFocusDelta(FocusType.F_ASSIGNMENT, assignmentValue, assignmentRemoved, false);
        if (!reallyRemoved) {
            ObjectDelta<?> secondaryDelta = ctx.modelContext.getFocusContext().getSecondaryDelta();
            if (secondaryDelta != null && secondaryDelta.subtract(FocusType.F_ASSIGNMENT, assignmentValue, assignmentRemoved, true)) {
                LOGGER.trace("Assignment to be added/deleted was not found in primary delta. It is present in secondary delta, so there's nothing to be approved.");
                return null;
            }
            String message = "Assignment to be added/deleted was not found in primary nor secondary delta."
                    + "\nAssignment:\n" + assignmentValue.debugDump()
                    + "\nPrimary delta:\n" + objectTreeDeltas.debugDump();
            throw new IllegalStateException(message);
        }
        String objectOid = ctx.getFocusObjectOid();
        return assignmentToDelta(ctx.modelContext.getFocusClass(),
                evaluatedAssignment.getAssignment(), assignmentRemoved, objectOid);
    }

    private void logApprovalActions(EvaluatedAssignment assignment,
            List<EvaluatedPolicyRule> triggeredApprovalActionRules) {
        if (LOGGER.isDebugEnabled() && !triggeredApprovalActionRules.isEmpty()) {
            LOGGER.trace("-------------------------------------------------------------");
            String verb = assignment.isBeingAdded() ? "added" :
                                assignment.isBeingDeleted() ? "deleted" :
                                        "modified";
            LOGGER.debug("Assignment to be {}: {}: {} this target policy rules, {} triggered approval actions:",
                    verb, assignment, assignment.getThisTargetPolicyRules().size(), triggeredApprovalActionRules.size());
            for (EvaluatedPolicyRule t : triggeredApprovalActionRules) {
                LOGGER.debug(" - Approval actions: {}", t.getEnabledActions(ApprovalPolicyActionType.class));
                for (EvaluatedPolicyRuleTrigger<?> trigger : t.getTriggers()) {
                    LOGGER.debug("   - {}", trigger);
                }
            }
        }
    }

    private ApprovalSchemaBuilder.Result createSchemaWithRules(List<EvaluatedPolicyRule> triggeredApprovalRules,
            @NotNull EvaluatedAssignment evaluatedAssignment, ModelInvocationContext<?> ctx,
            OperationResult result) throws SchemaException {

        PrismObject<?> targetObject = evaluatedAssignment.getTarget();
        ApprovalSchemaBuilder builder = new ApprovalSchemaBuilder(main, approvalSchemaHelper);

        // default policy action (only if adding)
        if (triggeredApprovalRules.isEmpty() && evaluatedAssignment.isBeingAdded() &&
                configurationHelper.getUseDefaultApprovalPolicyRules(ctx.wfConfiguration) != DefaultApprovalPolicyRulesUsageType.NEVER) {
            if (builder.addPredefined(targetObject, RelationKindType.APPROVER, result)) {
                LOGGER.trace("Added default approval action, as no explicit one was found for {}", evaluatedAssignment);
            }
        }

        // (3) actions from triggered rules
        for (EvaluatedPolicyRule approvalRule : triggeredApprovalRules) {
            for (ApprovalPolicyActionType approvalAction : approvalRule.getEnabledActions(ApprovalPolicyActionType.class)) {
                builder.add(main.getSchemaFromAction(approvalAction), approvalAction, targetObject, approvalRule);
            }
        }
        return builder.buildSchema(ctx, result);
    }

    private PcpStartInstruction prepareAssignmentRelatedStartInstruction(
            ApprovalSchemaBuilder.Result builderResult,
            EvaluatedAssignment evaluatedAssignment, ObjectDelta<? extends ObjectType> deltaToApprove,
            PrismObject<? extends FocusType> requester, ModelInvocationContext<?> ctx, OperationResult result) throws SchemaException {

        ModelContext<?> modelContext = ctx.modelContext;
        @SuppressWarnings("unchecked")
        PrismObject<? extends ObjectType> target = (PrismObject<? extends ObjectType>) evaluatedAssignment.getTarget();
        Validate.notNull(target, "assignment target is null");

        LocalizableMessage processName = main.createProcessName(builderResult, evaluatedAssignment, ctx, result);
        if (main.useDefaultProcessName(processName)) {
            processName = createDefaultProcessName(ctx, evaluatedAssignment, target);
        }
        String processNameInDefaultLocale = localizationService.translate(processName, Locale.getDefault(), "(unnamed)");

        PcpStartInstruction instruction =
                PcpStartInstruction
                        .createItemApprovalInstruction(main.getChangeProcessor(),
                                builderResult.schemaType, builderResult.attachedRules);

        instruction.prepareCommonAttributes(main, modelContext, requester);
        instruction.setDeltasToApprove(deltaToApprove);
        instruction.setObjectRef(ctx);
        instruction.setTargetRef(createObjectRef(target, prismContext), result);
        instruction.setName(processNameInDefaultLocale, processName);

        return instruction;
    }

    private LocalizableMessage createDefaultProcessName(ModelInvocationContext<?> ctx, EvaluatedAssignment assignment,
            PrismObject<? extends ObjectType> target) {

        ObjectType focus = ctx.getFocusObjectNewOrOld();

        String operationKey = assignment.isBeingAdded() ? "Added" :
                assignment.isBeingDeleted() ? "Deleted" :
                        "Modified";

        return new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + "assignmentModification.toBe" + operationKey)
                .arg(ObjectTypeUtil.createDisplayInformation(target, false))
                .arg(ObjectTypeUtil.createDisplayInformation(asPrismObject(focus), false))
                .build();
    }

    // creates an ObjectDelta that will be executed after successful approval of the given assignment
    private ObjectDelta<? extends FocusType> assignmentToDelta(Class<? extends Objectable> focusClass,
            AssignmentType assignmentType, boolean assignmentRemoved, String objectOid) throws SchemaException {
        PrismContainerValue<?> value = assignmentType.clone().asPrismContainerValue();
        S_ValuesEntry item = prismContext.deltaFor(focusClass)
                .item(FocusType.F_ASSIGNMENT);
        S_ItemEntry op = assignmentRemoved ? item.delete(value) : item.add(value);
        return op.asObjectDelta(objectOid);
    }

}
