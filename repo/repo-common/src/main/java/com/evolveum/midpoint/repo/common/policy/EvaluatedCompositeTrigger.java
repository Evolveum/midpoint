/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedLogicalTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

/**
 * Trigger for
 *
 * - {@link PolicyConstraintKind#AND}
 * - {@link PolicyConstraintKind#OR}
 * - {@link PolicyConstraintKind#NOT}
 */
public class EvaluatedCompositeTrigger extends EvaluatedPolicyRuleTrigger<PolicyConstraintsType> {

    @NotNull private final Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers;

    public EvaluatedCompositeTrigger(
            @NotNull PolicyConstraintKind kind,
            @NotNull PolicyConstraintsType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage,
            @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers) {
        super(kind, constraint, message, shortMessage, false);
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
                .map(EvaluatedPolicyRuleTrigger::toDiagShortcut)
                .distinct()
                .collect(Collectors.joining("+", "(", ")"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluatedCompositeTrigger)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EvaluatedCompositeTrigger that = (EvaluatedCompositeTrigger) o;
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
    public EvaluatedLogicalTriggerType toEvaluatedPolicyRuleTriggerBean(@NotNull PolicyRuleExternalizationOptions options) {
        var rv = toEvaluatedPolicyRuleTriggerBean(options, EvaluatedLogicalTriggerType::new);
        if (!isFinal()) {
            innerTriggers.stream()
                    .filter(trigger -> options.matchesSelector(trigger))
                    .forEach(t ->
                            rv.getEmbedded().add(
                                    t.toEvaluatedPolicyRuleTriggerBean(options)));
        }
        return rv;
    }
}
