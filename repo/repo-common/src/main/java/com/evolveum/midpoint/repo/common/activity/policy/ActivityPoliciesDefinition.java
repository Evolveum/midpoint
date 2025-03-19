/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesType;

public class ActivityPoliciesDefinition {

    private final ActivityPoliciesType bean;

    public ActivityPoliciesDefinition(ActivityPoliciesType bean) {
        this.bean = bean;
    }

    public static @NotNull ActivityPoliciesDefinition create(@Nullable ActivityDefinitionType activityDefinitionBean) {
        if (activityDefinitionBean == null) {
            return new ActivityPoliciesDefinition(new ActivityPoliciesType());
        }

        ActivityPoliciesType policies = activityDefinitionBean.getPolicies();
        if (policies == null) {
            policies = new ActivityPoliciesType();
        } else {
            policies = policies.clone();
        }

        return new ActivityPoliciesDefinition(policies);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityPoliciesDefinition clone() {
        return new ActivityPoliciesDefinition(bean.clone());
    }

    public ActivityPoliciesType getPolicies() {
        return bean;
    }
}
