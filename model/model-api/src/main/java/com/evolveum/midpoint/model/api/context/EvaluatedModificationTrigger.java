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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class EvaluatedModificationTrigger extends EvaluatedPolicyRuleTrigger<ModificationPolicyConstraintType> {

    @NotNull private final Collection<PrismObject<?>> matchingTargets;

    public EvaluatedModificationTrigger(@NotNull PolicyConstraintKindType kind, @NotNull ModificationPolicyConstraintType constraint,
            @Nullable PrismObject<?> targetObject, LocalizableMessage message, LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
        matchingTargets = targetObject != null ? singleton(targetObject) : emptySet();
    }

    @Override
    public EvaluatedModificationTriggerType toEvaluatedPolicyRuleTriggerBean(PolicyRuleExternalizationOptions options) {
        EvaluatedModificationTriggerType rv = new EvaluatedModificationTriggerType();
        fillCommonContent(rv);
        return rv;
    }

    @Override
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        return matchingTargets;
    }
}
