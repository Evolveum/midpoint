/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedTransitionTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransitionPolicyConstraintType;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.TRANSITION;

/** For {@link PolicyConstraintKind#TRANSITION}. */
public class EvaluatedTransitionTrigger extends EvaluatedClockworkPolicyRuleTrigger<TransitionPolicyConstraintType> {

    @NotNull private final Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers;

    public EvaluatedTransitionTrigger(
            @NotNull TransitionPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage,
            @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers) {
        super(TRANSITION, constraint, message, shortMessage, false);
        this.innerTriggers = innerTriggers;
    }

    @NotNull
    public Collection<EvaluatedPolicyRuleTrigger<?>> getInnerTriggers() {
        return innerTriggers;
    }

    @Override
    public String toDiagShortcut() {
        return super.toDiagShortcut()
                + innerTriggers.stream()
                .map(trigger -> trigger.toDiagShortcut())
                .distinct()
                .collect(Collectors.joining("+", "(", ")"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof EvaluatedTransitionTrigger that)) {return false;}
        if (!super.equals(o)) {return false;}
        return Objects.equals(innerTriggers, that.innerTriggers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), innerTriggers);
    }

    @Override
    protected void debugDumpSpecific(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "innerTriggers", innerTriggers, indent + 1);
    }

    @Override
    public EvaluatedTransitionTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options) {
        var rv = toEvaluatedPolicyRuleTriggerBean(options, EvaluatedTransitionTriggerType::new);
        if (!isFinal()) {
            innerTriggers.stream()
                    .filter(t -> options.matchesSelector(t))
                    .forEach(t ->
                            rv.getEmbedded().add(
                                    t.toEvaluatedPolicyRuleTriggerBean(options)));
        }
        return rv;
    }
}
