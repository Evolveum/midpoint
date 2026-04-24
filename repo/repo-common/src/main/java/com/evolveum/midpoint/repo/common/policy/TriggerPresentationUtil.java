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
 * TODO document the methods
 */
public class TriggerPresentationUtil {

    public static @NotNull List<TreeNode<LocalizableMessage>> extractMessages(
            Collection<EvaluatedPolicyRuleTrigger<?>> triggers, MessageKind kind) {
        return extractMessages(arrangeForPresentationInt(triggers), kind);
    }

    public static @NotNull List<TreeNode<LocalizableMessage>> extractMessages(
            List<TreeNode<EvaluatedPolicyRuleTrigger<?>>> triggerTreeList, MessageKind kind) {
        List<TreeNode<LocalizableMessage>> messageTreeList = new ArrayList<>();
        for (TreeNode<EvaluatedPolicyRuleTrigger<?>> tree : triggerTreeList) {
            messageTreeList.add(tree.transform(trigger -> getMessage(trigger, kind)));
        }
        return messageTreeList;
    }

    /**
     * Arranges triggers into trees according to presentation instructions (hidden, final, displayOrder).
     */
    private static List<TreeNode<EvaluatedPolicyRuleTrigger<?>>> arrangeForPresentationInt(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        TreeNode<EvaluatedPolicyRuleTrigger<?>> root = new TreeNode<>();
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            arrangeForPresentationInt(root, trigger);
        }
        sortTriggersInt(root);
        return root.getChildren();
    }

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

    private static boolean isHidden(PolicyConstraintPresentationType presentation, PolicyConstraintKindType kind) {
        if (presentation != null && presentation.isHidden() != null) {
            return presentation.isHidden();
        } else {
            return isHiddenByDefault(kind);
        }
    }

    public static boolean isHiddenByDefault(PolicyConstraintKindType kind) {
        // consider also "not"
        return kind == PolicyConstraintKindType.TRANSITION || kind == PolicyConstraintKindType.SITUATION
                || kind == PolicyConstraintKindType.AND || kind == PolicyConstraintKindType.OR;
    }

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
