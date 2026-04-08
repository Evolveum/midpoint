/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintPresentationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

public abstract class EvaluatedPolicyRuleTrigger<CT extends AbstractPolicyConstraintType> implements DebugDumpable, Serializable {

    @NotNull private final PolicyConstraintKindType constraintKind;

    @NotNull private final CT constraint;

    private final LocalizableMessage message;

    private final LocalizableMessage shortMessage;

    public EvaluatedPolicyRuleTrigger(
            @NotNull PolicyConstraintKindType constraintKind,
            @NotNull CT constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        this.constraintKind = constraintKind;
        this.constraint = constraint;
        this.message = message;
        this.shortMessage = shortMessage;
    }

    /**
     * The kind of constraint that caused the trigger.
     */
    @NotNull
    public PolicyConstraintKindType getConstraintKind() {
        return constraintKind;
    }

    @NotNull
    public CT getConstraint() {
        return constraint;
    }

    /**
     * Human-readable message associated with this trigger. The message
     * explain why the rule was triggered. It can be used
     * in the logs, as an error message, in the audit trail
     * and so on.
     */
    public LocalizableMessage getMessage() {
        return message;
    }

    public LocalizableMessage getShortMessage() {
        return shortMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluatedPolicyRuleTrigger<?>)) {
            return false;
        }
        EvaluatedPolicyRuleTrigger<?> that = (EvaluatedPolicyRuleTrigger<?>) o;
        return constraintKind == that.constraintKind
                && Objects.equals(constraint, that.constraint)
                && Objects.equals(message, that.message)
                && Objects.equals(shortMessage, that.shortMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraintKind, constraint, message);
    }

    @Override
    public String toString() {
        return "EvaluatedPolicyRuleTrigger(" +
                (constraint.getName() != null ? "[" + constraint.getName() + "] " : "") +
                constraintKind + ": " + message + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        debugDumpCommon(sb, indent + 1);
        debugDumpSpecific(sb, indent + 1);
        return sb.toString();
    }

    private void debugDumpCommon(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraintName", constraint.getName(), indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraintKind", constraintKind, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraint", constraint, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "message", message, indent);
        if (isFinal()) {
            DebugUtil.debugDumpWithLabelToStringLn(sb, "final", true, indent);
        }
        if (isHidden()) {
            DebugUtil.debugDumpWithLabelToStringLn(sb, "hidden", true, indent);
        }
    }

    public boolean isHidden() {
        PolicyConstraintPresentationType presentation = constraint.getPresentation();
        return presentation != null && Boolean.TRUE.equals(presentation.isHidden());
    }

    public boolean isFinal() {
        PolicyConstraintPresentationType presentation = constraint.getPresentation();
        return presentation != null && Boolean.TRUE.equals(presentation.isFinal());
    }

    protected void debugDumpSpecific(StringBuilder sb, int indent) {
    }

    public String toDiagShortcut() {
        return PolicyRuleTypeUtil.toDiagShortcut(constraintKind);
    }
}
