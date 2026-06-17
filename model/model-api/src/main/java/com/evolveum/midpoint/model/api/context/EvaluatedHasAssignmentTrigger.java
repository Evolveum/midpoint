/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class EvaluatedHasAssignmentTrigger extends EvaluatedClockworkPolicyRuleTrigger<HasAssignmentPolicyConstraintType> {

    @NotNull private final Collection<PrismObject<?>> matchingTargets;

    public EvaluatedHasAssignmentTrigger(
            @NotNull PolicyConstraintKind kind,
            @NotNull HasAssignmentPolicyConstraintType constraint,
            @NotNull Collection<PrismObject<?>> matchingTargets,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
        this.matchingTargets = matchingTargets;
    }

    @Override
    public EvaluatedHasAssignmentTriggerType toEvaluatedPolicyRuleTriggerBean(@NotNull PolicyRuleExternalizationOptions options) {
        return toEvaluatedPolicyRuleTriggerBean(options, EvaluatedHasAssignmentTriggerType::new);
    }

    @Override
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        return matchingTargets;
    }
}
