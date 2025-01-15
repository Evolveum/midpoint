/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentSpec;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.delta.ItemDelta;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Context related to evaluation and processing of policy rules.
 */
public class PolicyRulesContext implements Serializable, DebugDumpable {

    /**
     * Evaluated object-level policy rules, coming both from global configuration and assignments.
     * Note that target-level rules are stored exclusively in {@link EvaluatedAssignmentImpl}.
     *
     * Life cycle: Cleared at the beginning of each policy rules evaluation.
     */
    @NotNull private final Collection<EvaluatedPolicyRuleImpl> objectPolicyRules = new ArrayList<>();

    /**
     * Policy state modifications that should be applied.
     * Currently we apply them in {@link ChangeExecutor#executeChanges(LensContext, Task, OperationResult)} only.
     *
     * In the future we plan to be able to apply some state modifications even
     * if the clockwork is exited in non-standard way (e.g. in primary state or with an exception).
     * But we must be sure what policy state to store, because some constraints might be triggered
     * because of expectation of future state (like conflicting assignment is added etc.)
     *
     * Life cycle: Cleared in the `processFocus` iteration as well as after state modifications are flushed.
     *
     * Contains changes related to:
     *
     * * {@link ObjectType#F_POLICY_SITUATION}
     * * {@link ObjectType#F_TRIGGERED_POLICY_RULE}
     * * {@link ObjectType#F_EFFECTIVE_MARK_REF}
     */
    @NotNull private final List<ItemDelta<?,?>> pendingObjectPolicyStateModifications = new ArrayList<>();

    /**
     * Policy state modifications for assignments.
     *
     * Although we put here also deltas for assignments that are to be deleted, we do not execute these
     * (because we implement execution only for the standard exit-path from the clockwork).
     *
     * Life cycle: Cleared in the `processFocus` iteration as well as after state modifications are flushed.
     */
    @NotNull private final Map<AssignmentSpec, List<ItemDelta<?,?>>> pendingAssignmentPolicyStateModifications = new HashMap<>();

    /**
     * Last known values of policy rules counters; indexed by policy rule identifiers.
     * It is to avoid counting a rule more than once - and to preserve reached values.
     */
    @NotNull private final Map<String, Integer> counterMap = new HashMap<>();

    @NotNull Collection<EvaluatedPolicyRuleImpl> getObjectPolicyRules() {
        return objectPolicyRules;
    }

    void addObjectPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        this.objectPolicyRules.add(policyRule);
    }

    void addObjectPolicyRules(Collection<EvaluatedPolicyRuleImpl> policyRules) {
        this.objectPolicyRules.addAll(policyRules);
    }

    void clearObjectPolicyRules() {
        objectPolicyRules.clear();
    }

    @NotNull
    List<ItemDelta<?, ?>> getPendingObjectPolicyStateModifications() {
        return pendingObjectPolicyStateModifications;
    }

    void clearPendingPolicyStateModifications() {
        pendingObjectPolicyStateModifications.clear();
        pendingAssignmentPolicyStateModifications.clear();
    }

    void addToPendingObjectPolicyStateModifications(ItemDelta<?, ?> modification) {
        pendingObjectPolicyStateModifications.add(modification);
    }

    @NotNull
    Map<AssignmentSpec, List<ItemDelta<?, ?>>> getPendingAssignmentPolicyStateModifications() {
        return pendingAssignmentPolicyStateModifications;
    }

    void addToPendingAssignmentPolicyStateModifications(@NotNull AssignmentType assignment, @NotNull PlusMinusZero mode, @NotNull ItemDelta<?, ?> modification) {
        AssignmentSpec spec = new AssignmentSpec(assignment, mode);
        pendingAssignmentPolicyStateModifications.computeIfAbsent(spec, k -> new ArrayList<>()).add(modification);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Pending object policy state modifications:");
        if (getPendingObjectPolicyStateModifications().isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(DebugUtil.debugDump(getPendingObjectPolicyStateModifications(), indent + 1));
        }

        for (Map.Entry<AssignmentSpec, List<ItemDelta<?, ?>>> entry : getPendingAssignmentPolicyStateModifications().entrySet()) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent);
            sb.append("Pending assignment policy state modifications for ").append(entry.getKey()).append(":");
            if (entry.getValue().isEmpty()) {
                sb.append(" empty");
            } else {
                sb.append("\n");
                sb.append(DebugUtil.debugDump(entry.getValue(), indent + 1));
            }
        }

        LensContext.dumpRules(sb, "Object policy rules", indent, getObjectPolicyRules());
        sb.append("\n");

        Set<String> allEventMarksOids = getAllConsideredEventMarksOids();
        Set<String> triggeredEventMarksOids = getTriggeredEventMarksOids();
        DebugUtil.debugDumpWithLabelLn(sb, "Triggered event marks", triggeredEventMarksOids, indent);
        DebugUtil.debugDumpWithLabel(
                sb, "Other (non-triggered) event marks", Sets.difference(allEventMarksOids, triggeredEventMarksOids), indent);
        return sb.toString();
    }

    void setCounter(String policyRuleIdentifier, int value) {
        counterMap.put(policyRuleIdentifier, value);
    }

    Integer getCounter(String policyRuleIdentifier) {
        return counterMap.get(policyRuleIdentifier);
    }

    void copyFrom(@NotNull PolicyRulesContext other) {
        objectPolicyRules.addAll(other.objectPolicyRules);
        pendingObjectPolicyStateModifications.addAll(other.pendingObjectPolicyStateModifications);
        pendingAssignmentPolicyStateModifications.putAll(other.pendingAssignmentPolicyStateModifications);
        counterMap.putAll(other.counterMap);
    }

    // TEMPORARY IMPLEMENTATION
    @NotNull Set<String> getTriggeredEventMarksOids() {
        return objectPolicyRules.stream()
                .flatMap(rule -> rule.getTriggeredEventMarksOids().stream())
                .collect(Collectors.toSet());
    }

    // TEMPORARY IMPLEMENTATION
    @NotNull Set<String> getAllConsideredEventMarksOids() {
        return objectPolicyRules.stream()
                .flatMap(rule -> rule.getAllEventMarksOids().stream())
                .collect(Collectors.toSet());
    }
}
