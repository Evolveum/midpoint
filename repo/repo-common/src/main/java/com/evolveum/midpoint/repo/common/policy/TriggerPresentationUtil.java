/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintPresentationType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Deals with presenting the triggers, mainly by collecting their messages into a form understandable by the user.
 *
 * The triggers of a policy rule form a tree (a composite constraint has inner triggers). Before their messages can be
 * shown, that tree is rearranged according to the constraints' presentation instructions (hidden / final / displayOrder)
 * and then each trigger is replaced by its localizable message. The result is a forest of {@link LocalizableMessage}
 * trees mirroring the (visible) trigger structure.
 */
public class TriggerPresentationUtil {

    /**
     * Arranges the given (flat collection of, possibly nested) triggers for presentation and extracts their messages.
     * This is the usual entry point; see {@link #arrangeForPresentationInt(Collection)} for the arrangement rules.
     */
    public static @NotNull List<TreeNode<LocalizableMessage>> extractMessages(
            Collection<EvaluatedPolicyRuleTrigger<?>> triggers, MessageKind kind) {
        return extractMessages(arrangeForPresentationInt(triggers), kind);
    }

    /**
     * Extracts messages from triggers that have already been arranged into presentation trees: each trigger node is
     * replaced by its message of the requested {@code kind}, preserving the tree structure.
     */
    public static @NotNull List<TreeNode<LocalizableMessage>> extractMessages(
            List<TreeNode<EvaluatedPolicyRuleTrigger<?>>> triggerTreeList, MessageKind kind) {
        List<TreeNode<LocalizableMessage>> messageTreeList = new ArrayList<>();
        for (TreeNode<EvaluatedPolicyRuleTrigger<?>> tree : triggerTreeList) {
            messageTreeList.add(tree.transform(trigger -> getMessage(trigger, kind)));
        }
        return messageTreeList;
    }

    /**
     * Arranges triggers into trees according to presentation instructions (hidden, final, displayOrder). The returned
     * list holds the top-level (visible) triggers; hidden ones are dropped while their visible descendants are pulled up.
     */
    private static List<TreeNode<EvaluatedPolicyRuleTrigger<?>>> arrangeForPresentationInt(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        TreeNode<EvaluatedPolicyRuleTrigger<?>> root = new TreeNode<>();
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            arrangeForPresentationInt(root, trigger);
        }
        sortTriggersInt(root);
        return root.getChildren();
    }

    /**
     * Places a single trigger (and, recursively, its inner triggers) under {@code root}. A hidden trigger is not added,
     * so its inner triggers are attached directly to {@code root} (i.e. they replace it); otherwise they become its
     * children. A {@code final} constraint stops the descent, so its inner triggers are not presented at all.
     */
    private static void arrangeForPresentationInt(TreeNode<EvaluatedPolicyRuleTrigger<?>> root, EvaluatedPolicyRuleTrigger<?> trigger) {
        PolicyConstraintPresentationType presentation = trigger.getConstraint().getPresentation();
        boolean hidden = isHidden(presentation, trigger.getConstraintKind());
        boolean isFinal = presentation != null && Boolean.TRUE.equals(presentation.isFinal());
        if (!hidden) {
            TreeNode<EvaluatedPolicyRuleTrigger<?>> newNode = new TreeNode<>(trigger);
            root.add(newNode);
            root = newNode;
        }
        if (!isFinal) {
            for (EvaluatedPolicyRuleTrigger<?> innerTrigger : trigger.getInnerTriggers()) {
                arrangeForPresentationInt(root, innerTrigger);
            }
        }
    }

    /** Whether the trigger of a constraint should be hidden: taken from its presentation, or from the kind default. */
    private static boolean isHidden(PolicyConstraintPresentationType presentation, PolicyConstraintKindType kind) {
        if (presentation != null && presentation.isHidden() != null) {
            return presentation.isHidden();
        } else {
            return isHiddenByDefault(kind);
        }
    }

    /**
     * Constraint kinds hidden by default: the structural/meta ones (transition, situation, and, or) that carry no
     * message of their own worth showing - only their inner constraints do.
     */
    public static boolean isHiddenByDefault(PolicyConstraintKindType kind) {
        // consider also "not"
        return kind == PolicyConstraintKindType.TRANSITION || kind == PolicyConstraintKindType.SITUATION
                || kind == PolicyConstraintKindType.AND || kind == PolicyConstraintKindType.OR;
    }

    /** Recursively sorts sibling triggers by their {@code displayOrder} (unspecified order comes last). */
    private static void sortTriggersInt(TreeNode<EvaluatedPolicyRuleTrigger<?>> node) {
        Comparator<? super TreeNode<EvaluatedPolicyRuleTrigger<?>>> comparator = (t1, t2) -> {
            PolicyConstraintPresentationType p1 = t1.getUserObject().getConstraint().getPresentation();
            PolicyConstraintPresentationType p2 = t2.getUserObject().getConstraint().getPresentation();
            int o1 = p1 != null && p1.getDisplayOrder() != null ? p1.getDisplayOrder() : Integer.MAX_VALUE;
            int o2 = p2 != null && p2.getDisplayOrder() != null ? p2.getDisplayOrder() : Integer.MAX_VALUE;
            return Integer.compare(o1, o2);
        };
        node.getChildren().sort(comparator);
        node.getChildren().forEach(child -> sortTriggersInt(child));
    }

    private static LocalizableMessage getMessage(EvaluatedPolicyRuleTrigger<?> trigger, TriggerPresentationUtil.MessageKind kind) {
        return switch (kind) {
            case NORMAL -> trigger.getMessage();
            case SHORT -> trigger.getShortMessage();
        };
    }

    public enum MessageKind {NORMAL, SHORT, /*LONG*/}
}
