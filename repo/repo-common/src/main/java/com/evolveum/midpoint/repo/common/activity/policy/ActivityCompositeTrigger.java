/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

public class ActivityCompositeTrigger extends EvaluatedActivityPolicyRuleTrigger<PolicyConstraintsType> {

    private final @NotNull QName kind;

    private final @NotNull Collection<EvaluatedActivityPolicyRuleTrigger<?>> innerTriggers;

    public ActivityCompositeTrigger(
            @NotNull QName kind,
            @NotNull PolicyConstraintsType constraint,
            @NotNull Collection<EvaluatedActivityPolicyRuleTrigger<?>> innerTriggers) {
        super(constraint, null, null);

        this.kind = kind;
        this.innerTriggers = innerTriggers;
    }

    public @NotNull QName getKind() {
        return kind;
    }

    public @NotNull Collection<EvaluatedActivityPolicyRuleTrigger<?>> getInnerTriggers() {
        return innerTriggers;
    }
}
