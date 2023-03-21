/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Description of a situation that caused a trigger of the policy rule.
 *
 * @author semancik
 */
public abstract class EvaluatedPolicyRuleTrigger<CT extends AbstractPolicyConstraintType> implements DebugDumpable, Serializable {

    @NotNull private final PolicyConstraintKindType constraintKind;
    @NotNull private final CT constraint;
    private final LocalizableMessage message;
    private final LocalizableMessage shortMessage;

    /**
     * If true, this trigger is to be reported as {@link PolicyViolationException} regardless of specified policy rule action.
     * Used e.g. for disallowing assignment of two pruned roles (MID-4766).
     */
    private final boolean enforcementOverride;

    public EvaluatedPolicyRuleTrigger(
            @NotNull PolicyConstraintKindType constraintKind, @NotNull CT constraint,
            LocalizableMessage message, LocalizableMessage shortMessage,
            boolean enforcementOverride) {
        this.constraintKind = constraintKind;
        this.constraint = constraint;
        this.message = message;
        this.shortMessage = shortMessage;
        this.enforcementOverride = enforcementOverride;
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
        if (!(o instanceof EvaluatedPolicyRuleTrigger)) {
            return false;
        }
        EvaluatedPolicyRuleTrigger<?> that = (EvaluatedPolicyRuleTrigger<?>) o;
        return constraintKind == that.constraintKind
                && Objects.equals(constraint, that.constraint)
                && Objects.equals(message, that.message)
                && Objects.equals(shortMessage, that.shortMessage)
                && enforcementOverride == that.enforcementOverride;
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
        DebugUtil.debugDumpWithLabelToStringLn(sb, "targetObjects", getTargetObjects(), indent + 1);
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

    public EvaluatedPolicyRuleTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedPolicyRuleTriggerType rv = new EvaluatedPolicyRuleTriggerType();
        fillCommonContent(rv);
        return rv;
    }

    protected void fillCommonContent(EvaluatedPolicyRuleTriggerType tt) {
        tt.setConstraintKind(constraintKind);
        tt.setMessage(LocalizationUtil.createLocalizableMessageType(message));
        tt.setShortMessage(LocalizationUtil.createLocalizableMessageType(shortMessage));
        PolicyConstraintPresentationType presentation = constraint.getPresentation();
        if (presentation != null) {
            tt.setFinal(Boolean.TRUE.equals(presentation.isFinal()));
            tt.setHidden(Boolean.TRUE.equals(presentation.isHidden())); // null would mean it would be overridden by isHiddenByDefault
            tt.setPresentationOrder(presentation.getDisplayOrder());
        }
    }

    public Collection<EvaluatedPolicyRuleTrigger<?>> getInnerTriggers() {
        return Collections.emptySet();
    }

    public boolean isEnforcementOverride() {
        return enforcementOverride;
    }

    /**
     * @return Target object(s) that were matched by constraint that produced this trigger.
     * For example: target of the assignment that was added (and that matched "assignment" constraint).
     */
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        return Collections.emptyList();
    }

    /**
     * Use in connection to foreign policy rules - see the documentation in {@link AssociatedPolicyRule}.
     *
     * For exclusion triggers the behavior of this method is quite clear.
     *
     * But for other kinds of triggers (on the foreign rule) we return always `true`, as we have no way of knowing whether they
     * are relevant in the context of "the other side". This may change in the future, after we'll learn how to understand this.
     * Hope it will not cause any harm in the meanwhile.
     */
    public boolean isRelevantForNewOwner(@Nullable EvaluatedAssignment newOwner) {
        return true;
    }
}
