/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import java.util.Objects;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintPresentationType;

/**
 * Description of a situation that caused a trigger of the policy rule.
 *
 * @author semancik
 */
public abstract class EvaluatedFocusPolicyRuleTrigger<CT extends AbstractPolicyConstraintType>
        extends EvaluatedPolicyRuleTrigger<CT> {

    /**
     * If true, this trigger is to be reported as {@link PolicyViolationException} regardless of specified policy rule action.
     * Used e.g. for disallowing assignment of two pruned roles (MID-4766).
     */
    private final boolean enforcementOverride;

    public EvaluatedFocusPolicyRuleTrigger(
            @NotNull PolicyConstraintKind constraintKind, @NotNull CT constraint,
            LocalizableMessage message, LocalizableMessage shortMessage,
            boolean enforcementOverride) {
        super(constraintKind, constraint, message, shortMessage);

        this.enforcementOverride = enforcementOverride;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {return false;}
        if (!super.equals(o)) {return false;}
        EvaluatedFocusPolicyRuleTrigger<?> that = (EvaluatedFocusPolicyRuleTrigger<?>) o;
        return enforcementOverride == that.enforcementOverride;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), enforcementOverride);
    }

    @Override
    protected void debugDumpSpecific(StringBuilder sb, int indent) {
        super.debugDumpSpecific(sb, indent);

        DebugUtil.debugDumpWithLabelToStringLn(sb, "targetObjects", getTargetObjects(), indent + 1);
    }

    public EvaluatedPolicyRuleTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedPolicyRuleTriggerType rv = new EvaluatedPolicyRuleTriggerType();
        fillCommonContent(rv);
        return rv;
    }

    protected void fillCommonContent(EvaluatedPolicyRuleTriggerType tt) {
        tt.setConstraintKind(getConstraintKind());
        tt.setMessage(LocalizationUtil.createLocalizableMessageType(getMessage()));
        tt.setShortMessage(LocalizationUtil.createLocalizableMessageType(getShortMessage()));
        PolicyConstraintPresentationType presentation = getConstraint().getPresentation();
        if (presentation != null) {
            tt.setFinal(Boolean.TRUE.equals(presentation.isFinal()));
            tt.setHidden(Boolean.TRUE.equals(presentation.isHidden())); // null would mean it would be overridden by isHiddenByDefault
            tt.setPresentationOrder(presentation.getDisplayOrder());
        }
    }

    public boolean isEnforcementOverride() {
        return enforcementOverride;
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
