/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedLogicalTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class EvaluatedCompositeTrigger extends EvaluatedPolicyRuleTrigger<PolicyConstraintsType> {

    @NotNull private final Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers;

    public EvaluatedCompositeTrigger(@NotNull PolicyConstraintKindType kind, @NotNull PolicyConstraintsType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage, @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers) {
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
                    .map(trigger -> trigger.toDiagShortcut())
                    .distinct()
                    .collect(Collectors.joining("+", "(", ")"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EvaluatedCompositeTrigger))
            return false;
        if (!super.equals(o))
            return false;
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
    public EvaluatedLogicalTriggerType toEvaluatedPolicyRuleTriggerType(PolicyRuleExternalizationOptions options,
            PrismContext prismContext) {
        EvaluatedLogicalTriggerType rv = new EvaluatedLogicalTriggerType();
        fillCommonContent(rv);
        if (!options.isRespectFinalFlag() || !isFinal()) {
            innerTriggers.forEach(t -> rv.getEmbedded().add(t.toEvaluatedPolicyRuleTriggerType(options, prismContext)));
        }
        return rv;
    }
}
