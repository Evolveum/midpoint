/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyConstraintsType;

import javax.xml.namespace.QName;

public class ActivityCompositeTrigger extends EvaluatedActivityPolicyRuleTrigger<ActivityPolicyConstraintsType> {

    private final @NotNull QName kind;

    private final @NotNull Collection<EvaluatedActivityPolicyRuleTrigger<?>> innerTriggers;

    public ActivityCompositeTrigger(
            @NotNull QName kind,
            @NotNull ActivityPolicyConstraintsType constraint,
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
