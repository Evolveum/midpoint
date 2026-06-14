/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import static com.evolveum.midpoint.repo.common.policy.TriggerPresentationUtil.isHiddenByDefault;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Arranges externalized triggers (beans) into trees according to presentation instructions (hidden, final, displayOrder).
 *
 * We support:
 *
 * - attaching additional client-specific data (e.g. highlighting in the GUI case), see {@link TriggerWithData}
 * - additional filtering (e.g. to eliminate redundant triggers), see {@link TriggerWithDataPredicate}
 *
 * TODO document the methods
 */
public class TriggerBeanPresentationUtil {

    /** A trigger coupled with additional client-specific data. */
    public record TriggerWithData<AD extends AdditionalData>(EvaluatedPolicyRuleTriggerType trigger, AD additionalData) {
    }

    /** Marker interface for client-specific data. */
    public interface AdditionalData {
    }

    /** Predicate for triggers-with-data. Used e.g. for eliminating redundant (already shown) triggers. */
    public interface TriggerWithDataPredicate<AD extends AdditionalData> {
        boolean test(TriggerWithData<AD> newTriggerWithData);
    }

    // Main entry point for plain triggers: tree-izes and sorts the triggers.
    public static List<TreeNode<EvaluatedPolicyRuleTriggerType>> arrangeForPresentationExt(
            List<EvaluatedPolicyRuleTriggerType> triggers) {
        // augment
        List<TriggerWithData<AdditionalData>> triggerWithData = triggers.stream()
                .map(t -> new TriggerWithData<>(t, null))
                .collect(Collectors.toList());
        List<TreeNode<TriggerWithData<AdditionalData>>> trees = arrangeForPresentationExt(triggerWithData, null);
        // de-augment
        return trees.stream()
                .map(tree -> tree.transform(at -> at.trigger))
                .collect(Collectors.toList());
    }

    // Main entry point for augmented triggers: tree-izes and sorts the triggers.
    public static <AD extends AdditionalData> List<TreeNode<TriggerWithData<AD>>> arrangeForPresentationExt(
            List<TriggerWithData<AD>> triggers, TriggerWithDataPredicate<AD> additionalFilter) {
        TreeNode<TriggerWithData<AD>> root = new TreeNode<>();
        for (TriggerWithData<AD> trigger : triggers) {
            arrangeForPresentationExt(root, trigger, additionalFilter);
        }
        sortTriggersExt(root);
        return root.getChildren();
    }

    private static <AD extends AdditionalData> void arrangeForPresentationExt(
            TreeNode<TriggerWithData<AD>> root,
            TriggerWithData<AD> trigger,
            TriggerWithDataPredicate<AD> additionalPredicate) {
        boolean hidden = isHidden(trigger.trigger);
        boolean isFinal = Boolean.TRUE.equals(trigger.trigger.isFinal());
        if (!hidden) {
            if (additionalPredicate != null && !additionalPredicate.test(trigger)) {
                return;
            }
            TreeNode<TriggerWithData<AD>> newNode = new TreeNode<>(trigger);
            root.add(newNode);
            root = newNode;
        }
        if (!isFinal) { // it's possible that this was pre-filtered e.g. by policy enforcer hook
            for (EvaluatedPolicyRuleTriggerType innerTrigger : getChildTriggers(trigger.trigger)) {
                arrangeForPresentationExt(root, new TriggerWithData<>(innerTrigger, trigger.additionalData), additionalPredicate);
            }
        }
    }

    private static boolean isHidden(EvaluatedPolicyRuleTriggerType trigger) {
        if (trigger.isHidden() != null) {
            return trigger.isHidden();
        } else {
            return isHiddenByDefault(trigger.getConstraintKind());
        }
    }

    private static <AD extends AdditionalData> void sortTriggersExt(TreeNode<TriggerWithData<AD>> node) {
        Comparator<? super TreeNode<TriggerWithData<AD>>> comparator = (t1, t2) -> {
            int o1 = getIfNull(t1.getUserObject().trigger.getPresentationOrder(), Integer.MAX_VALUE);
            int o2 = getIfNull(t2.getUserObject().trigger.getPresentationOrder(), Integer.MAX_VALUE);
            return Integer.compare(o1, o2);
        };
        node.getChildren().sort(comparator);
        node.getChildren().forEach(child -> sortTriggersExt(child));
    }

    private static List<EvaluatedPolicyRuleTriggerType> getChildTriggers(EvaluatedPolicyRuleTriggerType trigger) {
        if (trigger instanceof EvaluatedEmbeddingTriggerType embeddingTrigger) {
            return embeddingTrigger.getEmbedded();
        } else if (trigger instanceof EvaluatedSituationTriggerType evaluatedSituation) {
            List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
            for (EvaluatedPolicyRuleType rule : evaluatedSituation.getSourceRule()) {
                rv.addAll(rule.getTrigger());
            }
            return rv;
        } else {
            return Collections.emptyList();
        }
    }
}
