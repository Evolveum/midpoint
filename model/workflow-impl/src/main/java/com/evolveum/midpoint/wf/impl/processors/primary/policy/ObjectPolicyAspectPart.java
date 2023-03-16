/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaHelper;
import com.evolveum.midpoint.wf.impl.processors.ConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpStartInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.policy.ProcessSpecifications.ProcessSpecification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

@Component
public class ObjectPolicyAspectPart {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyAspectPart.class);
    private static final String OP_EXTRACT_OBJECT_BASED_INSTRUCTIONS = ObjectPolicyAspectPart.class.getName()
            + ".extractObjectBasedInstructions";

    @Autowired private PolicyRuleBasedAspect main;
    @Autowired protected ApprovalSchemaHelper approvalSchemaHelper;
    @Autowired protected PrismContext prismContext;
    @Autowired protected ConfigurationHelper configurationHelper;
    @Autowired protected LocalizationService localizationService;

    <T extends ObjectType> void extractObjectBasedInstructions(@NotNull ObjectTreeDeltas<T> objectTreeDeltas,
            @Nullable PrismObject<? extends FocusType> requester, @NotNull List<PcpStartInstruction> instructions,
            @NotNull ModelInvocationContext<T> ctx, @NotNull OperationResult parentResult)
            throws SchemaException, ConfigurationException {

        OperationResult result = parentResult.subresult(OP_EXTRACT_OBJECT_BASED_INSTRUCTIONS)
                .setMinor()
                .build();
        ApprovalProcessStartInstructionCreationTraceType trace;
        if (result.isTracingNormal(ApprovalProcessStartInstructionCreationTraceType.class)) {
            trace = new ApprovalProcessStartInstructionCreationTraceType();
            result.addTrace(trace);
        } else {
            trace = null;
        }
        try {
            ObjectDelta<T> focusDelta = objectTreeDeltas.getFocusChange();
            LensFocusContext<T> focusContext = ctx.modelContext.getFocusContext();
            PrismObject<T> object = focusContext.getObjectOld() != null ?
                    focusContext.getObjectOld() : focusContext.getObjectNew();

            List<? extends EvaluatedPolicyRule> triggeredApprovalActionRules =
                    main.selectTriggeredApprovalActionRules(focusContext.getObjectPolicyRules());
            LOGGER.trace("extractObjectBasedInstructions: triggeredApprovalActionRules:\n{}",
                    debugDumpLazily(triggeredApprovalActionRules));

            List<PcpStartInstruction> newInstructions = new ArrayList<>();
            if (!triggeredApprovalActionRules.isEmpty()) {
                generateObjectOidIfNeeded(focusDelta, ctx.modelContext);
                ProcessSpecifications processSpecifications = ProcessSpecifications
                        .createFromRules(triggeredApprovalActionRules, prismContext);
                LOGGER.trace("Process specifications:\n{}", debugDumpLazily(processSpecifications));
                for (ProcessSpecification processSpecificationEntry : processSpecifications.getSpecifications()) {
                    if (focusDelta.isEmpty()) {
                        break; // we're done
                    }
                    WfProcessSpecificationType processSpecification = processSpecificationEntry.basicSpec;
                    List<ObjectDelta<T>> deltasToApprove = extractDeltasToApprove(focusDelta, processSpecification);
                    LOGGER.trace("Deltas to approve:\n{}", debugDumpLazily(deltasToApprove));
                    if (deltasToApprove.isEmpty()) {
                        continue;
                    }
                    LOGGER.trace("Remaining delta:\n{}", debugDumpLazily(focusDelta));
                    ApprovalSchemaBuilder builder = new ApprovalSchemaBuilder(main, approvalSchemaHelper);
                    builder.setProcessSpecification(processSpecificationEntry);
                    for (Pair<ApprovalPolicyActionType, EvaluatedPolicyRule> actionWithRule :
                            processSpecificationEntry.actionsWithRules) {
                        ApprovalPolicyActionType approvalAction = actionWithRule.getLeft();
                        builder.add(
                                main.getSchemaFromAction(approvalAction),
                                approvalAction,
                                object,
                                actionWithRule.getRight());
                    }
                    buildSchemaForObject(requester, newInstructions, ctx, result, deltasToApprove, builder);
                }
            } else if (configurationHelper.getUseDefaultApprovalPolicyRules(ctx.wfConfiguration)
                    != DefaultApprovalPolicyRulesUsageType.NEVER) {
                // default rule
                ApprovalSchemaBuilder builder = new ApprovalSchemaBuilder(main, approvalSchemaHelper);
                if (builder.addPredefined(object, RelationKindType.OWNER, result)) {
                    LOGGER.trace("Added default approval action, as no explicit one was found");
                    generateObjectOidIfNeeded(focusDelta, ctx.modelContext);
                    List<ObjectDelta<T>> deltasToApprove = singletonList(focusDelta.clone());
                    focusDelta.clear();
                    buildSchemaForObject(requester, newInstructions, ctx, result, deltasToApprove, builder);
                }
            }
            if (trace != null) {
                for (PcpStartInstruction newInstruction : newInstructions) {
                    trace.getCaseRef().add(ObjectTypeUtil.createObjectRefWithFullObject(newInstruction.getCase()));
                }
            }
            instructions.addAll(newInstructions);
            result.addReturn("instructionsCreated", newInstructions.size());
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <T extends ObjectType> void generateObjectOidIfNeeded(ObjectDelta<T> focusDelta, ModelContext<T> modelContext) {
        if (focusDelta.isAdd()) {
            if (focusDelta.getObjectToAdd().getOid() == null) {
                String newOid = OidUtil.generateOid();
                focusDelta.getObjectToAdd().setOid(newOid);
                ((LensFocusContext<?>) modelContext.getFocusContext()).setOid(newOid);
            }
        }
    }

    private <T extends ObjectType> void buildSchemaForObject(PrismObject<? extends FocusType> requester,
            List<PcpStartInstruction> instructions, ModelInvocationContext<T> ctx,
            @NotNull OperationResult result, List<ObjectDelta<T>> deltasToApprove,
            ApprovalSchemaBuilder builder) throws SchemaException {
        ApprovalSchemaBuilder.Result builderResult = builder.buildSchema(ctx, result);
        if (!approvalSchemaHelper.shouldBeSkipped(builderResult.schema)) {
            prepareObjectRelatedTaskInstructions(instructions, builderResult, deltasToApprove, requester, ctx, result);
        }
    }

    private <T extends ObjectType> List<ObjectDelta<T>> extractDeltasToApprove(ObjectDelta<T> focusDelta, WfProcessSpecificationType processSpecification)
            throws SchemaException {
        List<ObjectDelta<T>> rv = new ArrayList<>();
        if (focusDelta.isDelete() || processSpecification == null || processSpecification.getDeltaFrom().isEmpty()) {
            return takeWholeDelta(focusDelta, rv);
        }
        for (DeltaSourceSpecificationType sourceSpec : processSpecification.getDeltaFrom()) {
            if (sourceSpec == null || sourceSpec.getItem().isEmpty() && sourceSpec.getItemValue() == null) {
                return takeWholeDelta(focusDelta, rv);
            } else if (!sourceSpec.getItem().isEmpty()) {
                ObjectDelta.FactorOutResultSingle<T> out =
                        focusDelta.factorOut(ItemPathType.toItemPathList(sourceSpec.getItem()), false);
                addIgnoreNull(rv, out.offspring);
            } else {
                assert sourceSpec.getItemValue() != null;
                ObjectDelta.FactorOutResultMulti<T> out =
                        focusDelta.factorOutValues(prismContext.toUniformPath(sourceSpec.getItemValue()), false);
                rv.addAll(out.offsprings);
            }
        }
        return rv;
    }

    @NotNull
    private <T extends ObjectType> List<ObjectDelta<T>> takeWholeDelta(ObjectDelta<T> focusDelta, List<ObjectDelta<T>> rv) {
        rv.add(focusDelta.clone());
        focusDelta.clear();
        return rv;
    }

    private <T extends ObjectType> void prepareObjectRelatedTaskInstructions(
            List<PcpStartInstruction> instructions, ApprovalSchemaBuilder.Result builderResult,
            List<ObjectDelta<T>> deltasToApprove, PrismObject<? extends FocusType> requester, ModelInvocationContext<T> ctx,
            OperationResult result) throws SchemaException {

        for (ObjectDelta<T> deltaToApprove : deltasToApprove) {
            LocalizableMessage processName = main.createProcessName(builderResult, null, ctx, result);
            if (main.useDefaultProcessName(processName)) {
                processName = createDefaultProcessName(ctx, deltaToApprove);
            }
            String processNameInDefaultLocale = localizationService.translate(processName, Locale.getDefault(), "(unnamed)");

            PcpStartInstruction instruction =
                    PcpStartInstruction
                            .createItemApprovalInstruction(main.getChangeProcessor(),
                                    builderResult.schema, builderResult.attachedRules);

            instruction.prepareCommonAttributes(main, ctx.modelContext, requester);
            instruction.setDeltasToApprove(deltaToApprove);
            instruction.setObjectRef(ctx);
            instruction.setName(processNameInDefaultLocale, processName);

            instructions.add(instruction);
        }
    }

    private <T extends ObjectType> LocalizableMessage createDefaultProcessName(ModelInvocationContext<T> ctx,
            ObjectDelta<T> deltaToApprove) {
        ObjectType focus = ctx.getFocusObjectNewOrOld();
        String opKey;
        if (deltaToApprove.isAdd()) {
            opKey = "Added";
        } else if (deltaToApprove.isDelete()) {
            opKey = "Deleted";
        } else {
            opKey = "Modified";
        }
        return new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + "objectModification.toBe" + opKey)
                .args(ObjectTypeUtil.createDisplayInformation(asPrismObject(focus), false))
                .build();
    }
}
