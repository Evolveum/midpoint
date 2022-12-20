/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Objects;

/**
 * Takes care of updating policySituation and triggered rules for focus and assignments. (Originally was a part of PolicyRuleProcessor.)
 *
 * @author semancik
 */
@Component
public class PolicyStateRecorder {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyStateRecorder.class);

    @Autowired private PrismContext prismContext;

    public <AH extends AssignmentHolderType> void applyObjectState(LensContext<AH> context, List<EvaluatedPolicyRule> rulesToRecord) throws SchemaException {
        // compute policySituation and triggeredPolicyRules and compare it with the expected state
        // note that we use the new state for the comparison, because if values match we do not need to do anything
        LensFocusContext<AH> focusContext = context.getFocusContext();
        if (focusContext.isDelete()) {
            return;
        }
        AH objectNew = focusContext.getObjectNew().asObjectable();
        ComputationResult cr = compute(rulesToRecord, objectNew.getPolicySituation(), objectNew.getTriggeredPolicyRule());
        if (cr.situationsNeedUpdate) {
            focusContext.addToPendingObjectPolicyStateModifications(prismContext.deltaFor(ObjectType.class)
                    .item(ObjectType.F_POLICY_SITUATION)
                            .oldRealValues(cr.oldPolicySituations)
                            .replaceRealValues(cr.newPolicySituations)
                    .asItemDelta());
        }
        if (cr.rulesNeedUpdate) {
            focusContext.addToPendingObjectPolicyStateModifications(prismContext.deltaFor(ObjectType.class)
                    .item(ObjectType.F_TRIGGERED_POLICY_RULE)
                            .oldRealValues(cr.oldTriggeredRules)
                            .replaceRealValues(cr.newTriggeredRules)
                    .asItemDelta());
        }
    }

    <F extends AssignmentHolderType> void applyAssignmentState(LensContext<F> context,
            EvaluatedAssignmentImpl<F> evaluatedAssignment, PlusMinusZero mode, List<EvaluatedPolicyRule> rulesToRecord) throws SchemaException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext.isDelete()) {
            return;
        }
        AssignmentType assignmentNew = evaluatedAssignment.getAssignment(false);
        AssignmentType assignmentOld = evaluatedAssignment.getAssignment(true);
        if (assignmentOld == null && assignmentNew == null) {
            throw new IllegalStateException("Policy situation/rules for assignment cannot be updated, because the "
                    + "assignment itself is missing in "+evaluatedAssignment+", in object "+focusContext.getObjectAny());
        }
        // this value is to be used to find correct assignment in objectDelta to apply the modifications (if no ID is present)
        @NotNull AssignmentType assignmentToMatch = assignmentOld != null ? assignmentOld : assignmentNew;
        // this value is used to compute policy situation/rules modifications
        @NotNull AssignmentType assignmentToCompute = assignmentNew != null ? assignmentNew : assignmentOld;

        Long id = assignmentToMatch.getId();
        ComputationResult cr = compute(rulesToRecord, assignmentToCompute.getPolicySituation(), assignmentToCompute.getTriggeredPolicyRule());
        if (cr.situationsNeedUpdate) {
            focusContext.addToPendingAssignmentPolicyStateModifications(assignmentToMatch,
                    mode, prismContext.deltaFor(FocusType.class)
                    .item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_POLICY_SITUATION)
                    .oldRealValues(cr.oldPolicySituations)
                    .replaceRealValues(cr.newPolicySituations)
                    .asItemDelta());
        }
        if (cr.rulesNeedUpdate) {
            focusContext.addToPendingAssignmentPolicyStateModifications(assignmentToMatch,
                    mode, prismContext.deltaFor(FocusType.class)
                    .item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_TRIGGERED_POLICY_RULE)
                    .oldRealValues(cr.oldTriggeredRules)
                    .replaceRealValues(cr.newTriggeredRules)
                    .asItemDelta());
        }
    }

    private ComputationResult compute(@NotNull List<EvaluatedPolicyRule> rulesToRecord, @NotNull List<String> existingPolicySituation,
            @NotNull List<EvaluatedPolicyRuleType> existingTriggeredPolicyRule) {
        ComputationResult cr = new ComputationResult();
        for (EvaluatedPolicyRule rule : rulesToRecord) {
            cr.newPolicySituations.add(rule.getPolicySituation());
            RecordPolicyActionType recordAction = rule.getEnabledAction(RecordPolicyActionType.class);
            if (recordAction.getPolicyRules() != TriggeredPolicyRulesStorageStrategyType.NONE) {
                PolicyRuleExternalizationOptions externalizationOptions = new PolicyRuleExternalizationOptions(
                        recordAction.getPolicyRules(), false, true);
                rule.addToEvaluatedPolicyRuleBeans(cr.newTriggeredRules, externalizationOptions, null);
            }
        }
        cr.oldPolicySituations.addAll(existingPolicySituation);
        cr.oldTriggeredRules.addAll(existingTriggeredPolicyRule);
        cr.situationsNeedUpdate = !Objects.equals(cr.oldPolicySituations, cr.newPolicySituations);
        cr.rulesNeedUpdate = !Objects.equals(cr.oldTriggeredRules, cr.newTriggeredRules);   // hope hashCode is computed well
        return cr;
    }

    private static class ComputationResult {
        private final Set<String> oldPolicySituations = new HashSet<>();
        private final Set<String> newPolicySituations = new HashSet<>();
        private final Set<EvaluatedPolicyRuleType> oldTriggeredRules = new HashSet<>();
        private final Set<EvaluatedPolicyRuleType> newTriggeredRules = new HashSet<>();
        private boolean situationsNeedUpdate;
        private boolean rulesNeedUpdate;
    }
}
