/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class EvaluatedHasAssignmentTrigger extends EvaluatedPolicyRuleTrigger<HasAssignmentPolicyConstraintType> {

    @NotNull private final Collection<PrismObject<?>> matchingTargets;

    public EvaluatedHasAssignmentTrigger(
            @NotNull PolicyConstraintKindType kind, @NotNull HasAssignmentPolicyConstraintType constraint,
            @NotNull Collection<PrismObject<?>> matchingTargets,
            LocalizableMessage message, LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
        this.matchingTargets = matchingTargets;
    }

    @Override
    public EvaluatedHasAssignmentTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedHasAssignmentTriggerType rv = new EvaluatedHasAssignmentTriggerType();
        fillCommonContent(rv);
        return rv;
    }

    @Override
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        return matchingTargets;
    }
}
