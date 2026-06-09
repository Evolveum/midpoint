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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.ASSIGNMENT_MODIFICATION;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.OBJECT_MODIFICATION;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class EvaluatedModificationTrigger<C extends ModificationPolicyConstraintType>
        extends EvaluatedClockworkPolicyRuleTrigger<C> {

    @NotNull private final Collection<PrismObject<?>> matchingTargets;

    EvaluatedModificationTrigger(
            @NotNull PolicyConstraintKind kind,
            @NotNull C constraint,
            @Nullable PrismObject<?> targetObject,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
        matchingTargets = targetObject != null ? singleton(targetObject) : emptySet();
    }

    @Override
    public EvaluatedModificationTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options) {
        return toEvaluatedPolicyRuleTriggerBean(options, EvaluatedModificationTriggerType::new);
    }

    @Override
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        return matchingTargets;
    }

    public static class EvaluatedAssignmentModificationTrigger
            extends EvaluatedModificationTrigger<AssignmentModificationPolicyConstraintType> {
        public EvaluatedAssignmentModificationTrigger(
                @NotNull AssignmentModificationPolicyConstraintType constraint,
                @Nullable PrismObject<?> targetObject,
                LocalizableMessage message,
                LocalizableMessage shortMessage) {
            super(ASSIGNMENT_MODIFICATION, constraint, targetObject, message, shortMessage);
        }
    }

    public static class EvaluatedObjectModificationTrigger
            extends EvaluatedModificationTrigger<ModificationPolicyConstraintType> {
        public EvaluatedObjectModificationTrigger(
                @NotNull PolicyConstraintKindType kind,
                @NotNull ModificationPolicyConstraintType constraint,
                @Nullable PrismObject<?> targetObject,
                LocalizableMessage message,
                LocalizableMessage shortMessage) {
            super(OBJECT_MODIFICATION, constraint, targetObject, message, shortMessage);
        }
    }
}
