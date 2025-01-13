/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.context.AssociatedPolicyRule;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.EvaluatedPolicyStatements;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Takes care of updating `policySituation` and triggered rules for focus and assignments.
 * (Originally was a part of `PolicyRuleEvaluator`.)
 *
 * TODO migrate to marks somehow
 *
 * @author semancik
 */
class PolicyStateRecorder {

    <O extends ObjectType> void applyObjectState(
            @NotNull LensElementContext<O> elementContext,
            @NotNull List<? extends EvaluatedPolicyRule> rulesToRecord)
            throws SchemaException {
        // compute policySituation and triggeredPolicyRules and compare it with the expected state
        // note that we use the new state for the comparison, because if values match we do not need to do anything
        if (elementContext.isDelete()) {
            return;
        }
        O objectNew = elementContext.getObjectNew().asObjectable();

        EvaluatedPolicyStatements evaluatedPolicyStatements = elementContext.getEvaluatedPolicyStatements();

        ComputationResult cr = compute(
                rulesToRecord, objectNew.getPolicySituation(),
                objectNew.getEffectiveMarkRef(),
                objectNew.getTriggeredPolicyRule());
        if (cr.situationsNeedUpdate) {
            elementContext.addToPendingObjectPolicyStateModifications(
                    PrismContext.get().deltaFor(ObjectType.class)
                            .item(ObjectType.F_POLICY_SITUATION)
                            .oldRealValues(cr.oldPolicySituations)
                            .replaceRealValues(cr.newPolicySituations)
                            .asItemDelta());
        }
        if (cr.rulesNeedUpdate) {
            elementContext.addToPendingObjectPolicyStateModifications(
                    PrismContext.get().deltaFor(ObjectType.class)
                            .item(ObjectType.F_TRIGGERED_POLICY_RULE)
                            .oldRealValues(cr.oldTriggeredRules)
                            .replaceRealValues(cr.newTriggeredRules)
                            .asItemDelta());
        }

        for (EvaluatedPolicyRule rule : rulesToRecord) {
            computeNewMarks(rule, evaluatedPolicyStatements);
        }

        // apply collected deltas for effective marks (policyStatements + policyRules)
        // We use "existing" values (taken from object new, see above) to avoid phantom adds and deletes.
        var newMarkRefs = objectNew.getEffectiveMarkRef();
        if (evaluatedPolicyStatements.isNotEmpty()) {
            elementContext.addToPendingObjectPolicyStateModifications(
                    PrismContext.get().deltaFor(ObjectType.class)
                            .item(ObjectType.F_EFFECTIVE_MARK_REF)
                            .deleteRealValues(
                                    CloneUtil.cloneCollectionMembers(
                                            evaluatedPolicyStatements.collectMarksToDelete(newMarkRefs)))
                            .addRealValues(
                                    CloneUtil.cloneCollectionMembers(
                                            evaluatedPolicyStatements.collectMarksToAdd(newMarkRefs)))
                            .asItemDelta());
        }

        // something strange here - probably something forgotten, let's clean up
        // TODO What if there are some evaluated policy statements, but an extra effectiveMarkRef? We should clean that up as well.
        if (evaluatedPolicyStatements.isEmpty()) {
            elementContext.addToPendingObjectPolicyStateModifications(
                    PrismContext.get().deltaFor(ObjectType.class)
                            .item(ObjectType.F_EFFECTIVE_MARK_REF)
                            .deleteRealValues(CloneUtil.cloneCollectionMembers(newMarkRefs))
                            .asItemDelta());
        }

    }

    void applyAssignmentState(
            LensContext<?> context,
            EvaluatedAssignmentImpl<?> evaluatedAssignment,
            List<? extends AssociatedPolicyRule> rulesToRecord)
            throws SchemaException {
        LensFocusContext<?> focusContext = context.getFocusContext();
        if (focusContext.isDelete()) {
            return;
        }
        AssignmentType assignmentNew = evaluatedAssignment.getAssignment(false);
        AssignmentType assignmentOld = evaluatedAssignment.getAssignment(true);
        if (assignmentOld == null && assignmentNew == null) {
            throw new IllegalStateException(String.format(
                    "Policy situation/rules for assignment cannot be updated, because the assignment itself is missing"
                            + " in %s, in object %s", evaluatedAssignment, focusContext.getObjectAny()));
        }
        // this value is to be used to find correct assignment in objectDelta to apply the modifications (if no ID is present)
        @NotNull AssignmentType assignmentToMatch = assignmentOld != null ? assignmentOld : assignmentNew;
        // this value is used to compute policy situation/rules modifications
        @NotNull AssignmentType assignmentToCompute = assignmentNew != null ? assignmentNew : assignmentOld;

        // a bit of hack, but hopefully it will work
        PlusMinusZero mode =
                evaluatedAssignment.getOrigin().isBeingDeleted() ? PlusMinusZero.MINUS : evaluatedAssignment.getMode();

        Long id = assignmentToMatch.getId();
        //TODO manual marks not supported for assignments for now
        ComputationResult cr = compute(
                rulesToRecord,
                assignmentToCompute.getPolicySituation(),
                assignmentToCompute.getEffectiveMarkRef(),
                assignmentToCompute.getTriggeredPolicyRule());
        if (cr.situationsNeedUpdate) {
            focusContext.addToPendingAssignmentPolicyStateModifications(
                    assignmentToMatch,
                    mode,
                    PrismContext.get().deltaFor(FocusType.class)
                            .item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_POLICY_SITUATION)
                            .oldRealValues(cr.oldPolicySituations)
                            .replaceRealValues(cr.newPolicySituations)
                            .asItemDelta());
        }
        if (cr.rulesNeedUpdate) {
            focusContext.addToPendingAssignmentPolicyStateModifications(
                    assignmentToMatch,
                    mode,
                    PrismContext.get().deltaFor(FocusType.class)
                            .item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_TRIGGERED_POLICY_RULE)
                            .oldRealValues(cr.oldTriggeredRules)
                            .replaceRealValues(cr.newTriggeredRules)
                            .asItemDelta());
        }
        if (cr.markNeedUpdate) {
            focusContext.addToPendingAssignmentPolicyStateModifications(
                    assignmentToMatch,
                    mode,
                    PrismContext.get().deltaFor(FocusType.class)
                            .item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_EFFECTIVE_MARK_REF)
                            .oldRealValues(CloneUtil.cloneCollectionMembers(cr.oldMarkOid))
                            .replaceRealValues(CloneUtil.cloneCollectionMembers(cr.newMarkOid))
                            .asItemDelta());
        }
    }

    private ComputationResult compute(
            @NotNull List<? extends AssociatedPolicyRule> rulesToRecord,
            @NotNull List<String> existingPolicySituation,
            @NotNull List<ObjectReferenceType> existingMarkOids,
            @NotNull List<EvaluatedPolicyRuleType> existingTriggeredPolicyRule) {
        ComputationResult cr = new ComputationResult();
        for (AssociatedPolicyRule rule : rulesToRecord) {
            cr.newPolicySituations.add(rule.getPolicySituation());
            cr.newMarkOid.addAll(rule.getPolicyMarkRef());
            PolicyActionConfigItem<RecordPolicyActionType> recordAction = rule.getEnabledAction(RecordPolicyActionType.class);
            assert recordAction != null;
            var rulesStorageStrategy = recordAction.value().getPolicyRules();
            if (rulesStorageStrategy != TriggeredPolicyRulesStorageStrategyType.NONE) {
                PolicyRuleExternalizationOptions externalizationOptions =
                        new PolicyRuleExternalizationOptions(rulesStorageStrategy, false);
                rule.addToEvaluatedPolicyRuleBeans(
                        cr.newTriggeredRules, externalizationOptions, null, rule.getNewOwner());
            }
        }

        cr.oldPolicySituations.addAll(existingPolicySituation);
        cr.oldTriggeredRules.addAll(existingTriggeredPolicyRule);
        cr.oldMarkOid.addAll(existingMarkOids);
        cr.situationsNeedUpdate = !Objects.equals(cr.oldPolicySituations, cr.newPolicySituations);
        cr.markNeedUpdate = compareMarkOids(cr.oldMarkOid, cr.newMarkOid); //TODO might not be needed after assignments supports manual marking
        cr.rulesNeedUpdate = !Objects.equals(cr.oldTriggeredRules, cr.newTriggeredRules);   // hope hashCode is computed well
        return cr;
    }

    private @NotNull Collection<ObjectReferenceType> computeNewMarks(AssociatedPolicyRule rule, EvaluatedPolicyStatements evaluatedPolicyStatements) {
        Set<ObjectReferenceType> newMarkRefs = new HashSet<>();
        for (ObjectReferenceType markFromPolicy : rule.getPolicyMarkRef()) {
            if (evaluatedPolicyStatements.isExclude(markFromPolicy)) {
                continue;
                //we don't want to add effectiveMarkRef if manual policy exclude it
            }
            evaluatedPolicyStatements.addMarkRefToAdd(markFromPolicy);
        }
        return newMarkRefs;
    }

    private boolean compareMarkOids(Set<ObjectReferenceType> oldMarkOid, Set<ObjectReferenceType> newMarkOid) {
        Set<String> oldMarkOids = oldMarkOid.stream().map(ObjectReferenceType::getOid).collect(Collectors.toSet());
        Set<String> newMarkOids = newMarkOid.stream().map(ObjectReferenceType::getOid).collect(Collectors.toSet());
        return !Objects.equals(oldMarkOids, newMarkOids);
    }

    private static class ComputationResult {
        private final Set<String> oldPolicySituations = new HashSet<>();
        private final Set<String> newPolicySituations = new HashSet<>();

        private final Set<ObjectReferenceType> oldMarkOid = new HashSet<>();
        private final Set<ObjectReferenceType> newMarkOid = new HashSet<>();

        private final Set<EvaluatedPolicyRuleType> oldTriggeredRules = new HashSet<>();
        private final Set<EvaluatedPolicyRuleType> newTriggeredRules = new HashSet<>();
        private boolean situationsNeedUpdate;
        private boolean rulesNeedUpdate;
        private boolean markNeedUpdate;
    }
}
