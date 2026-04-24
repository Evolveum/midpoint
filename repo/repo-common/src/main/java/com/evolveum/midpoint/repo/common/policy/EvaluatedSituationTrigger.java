/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.schema.policy.PolicyRuleDumpUtil;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedSituationTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicySituationPolicyConstraintType;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.SITUATION;

/** Trigger for {@link PolicyConstraintKind#SITUATION}. */
public class EvaluatedSituationTrigger extends EvaluatedPolicyRuleTrigger<PolicySituationPolicyConstraintType> {

    /** Rules that caused this trigger to be fired. Immutable. */
    @NotNull private final Collection<EvaluatedPolicyRule> sourceRules;

    public EvaluatedSituationTrigger(
            @NotNull PolicySituationPolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage,
            @NotNull Collection<? extends EvaluatedPolicyRule> sourceRules) {
        super(SITUATION, constraint, message, shortMessage, false);
        this.sourceRules = List.copyOf(sourceRules);
    }

    @NotNull
    public Collection<EvaluatedPolicyRule> getSourceRules() {
        return List.copyOf(sourceRules);
    }

    // lists all source rules (recursively)
    @NotNull
    private Collection<EvaluatedPolicyRule> getAllSourceRules() {
        List<EvaluatedPolicyRule> rv = new ArrayList<>();
        for (EvaluatedPolicyRule sourceRule : sourceRules) {
            rv.add(sourceRule);
            for (EvaluatedPolicyRuleTrigger<?> trigger : sourceRule.getTriggers()) {
                if (trigger instanceof EvaluatedSituationTrigger) {
                    rv.addAll(((EvaluatedSituationTrigger) trigger).getAllSourceRules());
                }
            }
        }
        return rv;
    }

    public Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers() {
        List<EvaluatedPolicyRuleTrigger<?>> rv = new ArrayList<>();
        rv.add(this);
        getAllSourceRules().forEach(r -> rv.addAll(r.getTriggers()));
        return rv;
    }

    @Override
    public String toDiagShortcut() {
        return super.toDiagShortcut()
                + sourceRules.stream()
                .map(sr -> PolicyRuleDumpUtil.toShortString(sr.getPolicyConstraints()))
                .distinct()
                .collect(Collectors.joining("+", "(", ")"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof EvaluatedSituationTrigger that)) {return false;}
        if (!super.equals(o)) {return false;}
        return Objects.equals(sourceRules, that.sourceRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceRules);
    }

    @Override
    protected void debugDumpSpecific(StringBuilder sb, int indent) {
        // cannot debug dump in details, as we might go into infinite loop
        DebugUtil.debugDumpWithLabel(sb, "sourceRules", sourceRules.stream().map(Object::toString).collect(Collectors.toList()), indent + 1);
    }

    public EvaluatedSituationTriggerType toEvaluatedPolicyRuleTriggerBean(@NotNull PolicyRuleExternalizationOptions options) {
        var rv = toEvaluatedPolicyRuleTriggerBean(options, EvaluatedSituationTriggerType::new);
        if (!isFinal()) {
            var sourceRuleBeans = rv.getSourceRule();
            for (EvaluatedPolicyRule sourceRule : sourceRules) {
                // TODO check if we propagate the selectors (in options + explicit one) correctly
                sourceRuleBeans.addAll(
                        sourceRule.toEvaluatedPolicyRuleBeans(options, null));
            }
        }
        return rv;
    }
}
